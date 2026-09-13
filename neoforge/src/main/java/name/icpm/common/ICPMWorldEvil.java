package name.icpm.common;

import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ICPM「世界恶意」配置的运行时引擎（服务端）：
 * - 技术不佳 poorTechnique（0~4 档）：每档 耐久消耗 ×(1+0.25档)、攻击力与挖掘速度 ×(1-0.25档)。
 *   <b>没有任何保底伤害</b>——攻击伤害直接乘倍率（{@code ICPMTechAttackMixin} 在
 *   {@code Player.attack} 的 {@code hurtOrSimulate} 出口缩放，乘多少结算多少）；
 *   挖掘进度乘倍率且带 0.25 下限（4 档不再归零，防止零硬度方块 Infinity×0=NaN 死锁）；
 *   耐久消耗乘 {@link #durabilityMult()}（{@code CurseDecayMixin} 统一汇点层叠）。
 *   档位提示使用语言键 {@code message.icpm.poor_technique_on/off}（不得硬编码"女巫诅咒"字样）。
 * - 噩梦时代 nightmareEra：每 tick 将主世界 dayTime 锁进夜晚段（13000..24000），天数照常推进；
 *   ICPMMoonPhase 的"每日 80% 血月"开关同步。
 * - 血月天气由 {@code ICPMWeatherMixin}（advanceWeatherCycle RETURN 注入：A1 闪电×5 / A3 昼雨夜晴）统一驱动。
 * - 女巫低吟 witchWhisper 作为独立 MobEffect 由 {@code ICPMCurseManager.syncWhisper} 施加（不在本类）。
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
     * 攻击力 / 挖掘速度倍率 = max(0.25, 1 - 0.25 × 档)。
     * 下限 0.25 仅防"进度归零死锁"（4 档 ×0 会让所有方块永不破坏）；
     * <b>攻击侧没有任何保底伤害</b>——0.25 档下 1 点普攻只结算 0.25。
     */
    public static float penaltyFactor() {
        return Math.max(0.25f, 1.0f - 0.25f * level());
    }

    /**
     * 在"任意最终扣损点"对 ICPM/原版计算出的耐久消耗做统一层叠（R196 忠实移植，不做平衡性裁剪）：
     * 倍率 = 腐蚀性皮肤诅咒(装备加速腐坏)×2 × 技术不佳(1+0.25档)。
     */
    public static int layerDurability(ServerPlayer player, ItemStack stack, int baseCost) {
        if (baseCost <= 0) {
            return baseCost;
        }
        float mult = durabilityMult();
        if (ICPMCurseManager.isCursed(player, ICPMCurse.EQUIPMENT_DECAYS_FASTER, true)) {
            mult *= 2.0f;
        }
        int r = (int) (baseCost * mult);
        return r < 1 ? 1 : r;
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

    /** 档位变化时提示（文案走语言键，与女巫诅咒无关）。 */
    private static void syncTechniqueMessage(ServerPlayer player, int lvl) {
        UUID uuid = player.getUUID();
        Integer notified = NOTIFIED_TECH.get(uuid);
        if (notified != null && notified == lvl) {
            return;
        }
        if (lvl > 0) {
            player.sendSystemMessage(Component.translatable(
                    "message.icpm.poor_technique_on", 25 * lvl, 25 * lvl));
        } else if (notified != null && notified > 0) {
            player.sendSystemMessage(Component.translatable("message.icpm.poor_technique_off"));
        }
        NOTIFIED_TECH.put(uuid, lvl);
    }
}
