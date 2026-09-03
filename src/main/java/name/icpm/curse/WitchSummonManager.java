package name.icpm.curse;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Witch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * R196 女巫召狼（EntityWitch.onLivingUpdate + summonWolves）忠实移植。
 *
 * <p>R196 原文语义：
 * <pre>
 *   if (summon_wolf_countdown > 0) { 倒数；归零 → summonWolves() 一次（成败都记 has_summoned） }
 *   else if (!has_summoned_wolves && getLastHarmingEntity() instanceof EntityPlayer) {
 *       summon_wolf_target = lastHarming; summon_wolf_countdown = 60;
 *   }
 *   // summonWolves：目标旁 8-16 格尝试生成 1-3 只狼（寻路可达才成功），
 *   // wolf.setWitchAlly() + wolf.setAttackTarget(target)；狼由该女巫指挥、不死不逃。
 * </pre>
 * 即：**每只女巫一生最多召唤一次狼群，触发条件是"被玩家打伤"，60 tick（3 秒）后在
 * 伤害者附近刷新 1-3 只敌意狼**。
 *
 * <p>1.21.11 差异处理：无 setWitchAlly；野狼默认不主动攻击。故用
 * {@code wolf.setLastHurtByMob(player) + setTarget(player) + setPersistenceRequired()}，
 * 使狼进入"被玩家激怒"的敌对追击状态（真实会咬人），并保活不自然消失。
 */
public final class WitchSummonManager {

    private static final class Pending {
        UUID witchUuid;
        UUID targetUuid;
        long spawnAtGameTime;
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** 已召唤过（含尝试过但失败）的女巫：一生一次（R196 has_summoned_wolves）。 */
    private static final Map<UUID, Boolean> HAS_SUMMONED = new HashMap<>();

    private WitchSummonManager() {
    }

    /** 女巫被玩家打伤时登记（R196 lastHarming 分支）；已登记/已召唤过则忽略。 */
    public static void onWitchHurtByPlayer(Witch witch, ServerPlayer player) {
        UUID id = witch.getUUID();
        if (HAS_SUMMONED.containsKey(id) || PENDING.containsKey(id)) {
            return;
        }
        Pending p = new Pending();
        p.witchUuid = id;
        p.targetUuid = player.getUUID();
        p.spawnAtGameTime = ((ServerLevel) player.level()).getGameTime() + 60L; // R196 60 tick
        PENDING.put(id, p);
    }

    /** 每服务端 tick 处理倒计时（R196 summon_wolf_countdown）。 */
    public static void onServerTick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Pending p = it.next().getValue();
            ServerPlayer target = server.getPlayerList().getPlayer(p.targetUuid);
            if (target == null || !target.level().isLoaded(target.blockPosition())) {
                it.remove();
                HAS_SUMMONED.put(p.witchUuid, Boolean.TRUE); // 尝试一次即记（R196 成败都算）
                continue;
            }
            if (((ServerLevel) target.level()).getGameTime() < p.spawnAtGameTime) {
                continue;
            }
            it.remove();
            HAS_SUMMONED.put(p.witchUuid, Boolean.TRUE);
            summonWolvesNear((ServerLevel) target.level(), target);
        }
    }

    /** 女巫死亡清理登记（避免悬空倒计时）。 */
    public static void onWitchRemoved(Witch witch) {
        UUID id = witch.getUUID();
        PENDING.remove(id);
        HAS_SUMMONED.remove(id);
    }

    private static void summonWolvesNear(ServerLevel level, ServerPlayer target) {
        int maxWolves = 1 + level.getRandom().nextInt(3); // R196 rand.nextInt(3)+1 → 1..3
        int spawned = 0;
        for (int attempts = 0; attempts < 16 && spawned < maxWolves; attempts++) {
            // R196 目标旁 8-16 格随机落点
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
            double dist = 8.0 + level.getRandom().nextDouble() * 8.0;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;
            double y = target.getY();
            Wolf wolf = EntityType.WOLF.create(level, net.minecraft.world.entity.EntitySpawnReason.NATURAL);
            if (wolf == null) {
                continue;
            }
            wolf.teleportTo(x, y, z);
            if (!level.noCollision(wolf) || level.containsAnyLiquid(wolf.getBoundingBox())) {
                continue; // 落点不可用则换下一次尝试
            }
            wolf.setPersistenceRequired(); // R196 refreshDespawnCounter(-9600) 保活
            wolf.setLastHurtByMob(target); // 使野狼进入可反击的敌对追击态
            wolf.setTarget(target);
            level.addFreshEntity(wolf);
            spawned++;
        }
    }
}
