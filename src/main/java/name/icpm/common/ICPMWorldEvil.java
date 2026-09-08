package name.icpm.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ICPM「世界恶意」配置的运行时引擎（服务端）：
 * - 技术不佳 poorTechnique（0~4 档）：耐久消耗 ×(1+0.25档)、攻击力 ×(1-0.25档)、挖掘速度 ×(1-0.25档)；
 * - 噩梦时代 nightmareEra：每 tick 将主世界 dayTime 锁进夜晚段（13000..24000），天数照常推进；
 *   ICPMMoonPhase 的"每日 80% 血月"开关同步。
 *
 * 女巫低吟 witchWhisper 的永久诅咒由 ICPMCurseManager.syncWhisper 处理（不在此类）。
 */
public final class ICPMWorldEvil {

    /** 攻击力削减修饰符固定 id（技术不佳）。 */
    private static final net.minecraft.resources.Identifier TECH_ATTACK_MOD =
            net.minecraft.resources.Identifier.parse("icpm:poor_technique");

    /** 每个玩家当前已应用的档位 + 对应修饰符对象（服务端会话内）。 */
    private static final Map<UUID, Integer> APPLIED_TECH = new HashMap<>();
    private static final Map<UUID, AttributeModifier> APPLIED_MOD = new HashMap<>();

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

    /** 攻击力 / 挖掘速度倍率 = max(0, 1 - 0.25 × 档)。 */
    public static float penaltyFactor() {
        return Math.max(0.0f, 1.0f - 0.25f * level());
    }

    /** END_SERVER_TICK：同步噩梦时代开关 + 夜锁 + 技术不佳攻击力修饰符。 */
    public static void onServerTick(MinecraftServer server) {
        boolean nightmare = ICPMConfig.isNightmareEnabled();
        ICPMMoonPhase.setNightmare(nightmare);

        int lvl = level();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncTechnique(player, lvl);
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

    /** 玩家登出清理（避免 map 泄漏；同名重进同 UUID，无害）。 */
    public static void onPlayerDisconnect(ServerPlayer player) {
        APPLIED_TECH.remove(player.getUUID());
        APPLIED_MOD.remove(player.getUUID());
    }

    private static void syncTechnique(ServerPlayer player, int lvl) {
        UUID uuid = player.getUUID();
        Integer applied = APPLIED_TECH.get(uuid);
        if (applied != null && applied == lvl) {
            return;
        }
        applyAttackModifier(player, lvl);
        boolean had = applied != null && applied > 0;
        if (lvl > 0 && !had) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.icpm.poor_technique_on", 25 * lvl));
        } else if (lvl == 0 && had) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.icpm.poor_technique_off"));
        }
        APPLIED_TECH.put(uuid, lvl);
    }

    private static void applyAttackModifier(ServerPlayer player, int lvl) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) {
            return;
        }
        // 先移除已应用的旧修饰符（以对象引用删除，兼容本映射无 removeModifier(UUID)）
        AttributeModifier old = APPLIED_MOD.remove(player.getUUID());
        if (old != null) {
            attr.removeModifier(old);
        }
        if (lvl > 0) {
            AttributeModifier mod = new AttributeModifier(
                    TECH_ATTACK_MOD, -0.25 * lvl,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            attr.addTransientModifier(mod);
            APPLIED_MOD.put(player.getUUID(), mod);
        }
    }
}
