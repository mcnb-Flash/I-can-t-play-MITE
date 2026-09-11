package name.icpm.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ICPM「世界恶意」配置的运行时引擎（服务端）：
 * - 技术不佳 poorTechnique（0~4 档）：每档 耐久消耗 ×(1+0.25档)、攻击力与挖掘速度 ×(1-0.25档)。
 *   各项惩罚不碰被 ICPM mixin 覆盖后的"原版接口"，而是乘在 ICPM 自己的运算输出上：
 *     耐久：ICPMDurabilityBreakMixin（挖掘 pending cost）/ ICPMDurabilityAttackMixin（攻击衰减）
 *           以及 vanilla 汇点 CurseDecayMixin(ItemStack.hurtAndBreak 4参)；
 *     挖掘：ICPMToolRulesMixin 完全接管的 BlockBehaviour.getDestroyProgress 返回值（ICPMTechMiningMixin RETURN 缩放）；
 *     攻击：Player.attack 最终伤害出口（Entity.hurtOrSimulate 参数，ICPMTechAttackMixin Redirect）。
 * - 噩梦时代 nightmareEra：每 tick 将主世界 dayTime 锁进夜晚段（13000..24000），天数照常推进；
 *   ICPMMoonPhase 的"每日 80% 血月"开关同步。
 *
 * 女巫低吟 witchWhisper 作为独立 MobEffect 效果由 ICPMCurseManager.syncWhisper 施加（不在本类）。
 */
public final class ICPMWorldEvil {

    /** 每个玩家当前提示过的档位（服务端会话内，仅用于开关提示）。 */
    private static final Map<UUID, Integer> NOTIFIED_TECH = new HashMap<>();

    private ICPMWorldEvil() {
    }

    /** 技术不佳档位（0~4）。 */
    public static int level() {
        return Math.max(0, Math.min(4, ICPMConfig.poorTechniqueLevel()));
    }

    /** 耐久损耗倍率 = 1 + 0.25 × 档。 */
    public static float durabilityMult() {
        return 1.0f + 0.25f * level();
    }

    /**
     * 在"任意最终扣损点"对 ICPM/原版计算出的耐久消耗做统一层叠（R196 忠实移植，不做平衡性裁剪）：
     * 倍率 = 腐蚀性皮肤诅咒(装备加速腐坏)×2 × 技术不佳(1+0.25档)。
     * 若玩家命中腐蚀性皮肤诅咒，则在 ICPM 耐久计算之上再次增加一倍消耗。
     */
    public static int layerDurability(ServerPlayer player, ItemStack stack, int baseCost) {
        if (baseCost <= 0) {
            return baseCost;
        }
        float mult = durabilityMult();
        if (name.icpm.curse.ICPMCurseManager.isCursed(
                player, name.icpm.curse.ICPMCurse.EQUIPMENT_DECAYS_FASTER, true)) {
            mult *= 2.0f;
        }
        int r = (int) (baseCost * mult);
        return r < 1 ? 1 : r;
    }

    /** 攻击力 / 挖掘速度倍率 = max(0, 1 - 0.25 × 档)。 */
    public static float penaltyFactor() {
        return Math.max(0.0f, 1.0f - 0.25f * level());
    }

    /** END_SERVER_TICK：同步噩梦时代开关 + 夜锁 + 技术不佳档位提示。 */
    public static void onServerTick(MinecraftServer server) {
        boolean nightmare = ICPMConfig.isNightmareEnabled();
        ICPMMoonPhase.setNightmare(nightmare);

        int lvl = level();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncTechniqueMessage(player, lvl);
        }

        if (nightmare) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension() != Level.OVERWORLD) {
                    continue;
                }
                long t = level.getDayTime() % 24000L;
                if (t < 13000L) {
                    // 快进到黄昏：白天段被跳过 → 世界始终处于夜晚，天数每 24000 tick 照常 +1
                    level.setDayTime(level.getDayTime() + (13000L - t));
                }
            }
        }
    }

    /** 玩家登出清理（避免 map 泄漏）。 */
    public static void onPlayerDisconnect(ServerPlayer player) {
        NOTIFIED_TECH.remove(player.getUUID());
    }

    /** 档位变化时提示（不改任何属性——攻击/挖掘/耐久缩放见各类的运算点注入）。 */
    private static void syncTechniqueMessage(ServerPlayer player, int lvl) {
        UUID uuid = player.getUUID();
        Integer notified = NOTIFIED_TECH.get(uuid);
        if (notified != null && notified == lvl) {
            return;
        }
        if (lvl > 0) {
            int pct = 25 * lvl;
            player.sendSystemMessage(Component.literal(
                    "§5[女巫诅咒·智力下降]§r（技术不佳 Lv" + lvl + "）：耐久消耗 +" + pct
                            + "%，攻击力与挖掘速度 -" + pct + "%。（不占女巫诅咒位）"));
        } else if (notified != null && notified > 0) {
            player.sendSystemMessage(Component.literal("§a[技术不佳] 已解除，你恢复了正常技艺。"));
        }
        NOTIFIED_TECH.put(uuid, lvl);
    }
}
