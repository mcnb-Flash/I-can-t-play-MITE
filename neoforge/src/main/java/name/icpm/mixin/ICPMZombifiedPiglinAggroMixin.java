package name.icpm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * R196 EntityPigZombie 索敌语义移植（僵尸猪人"靠近索敌"）。
 *
 * 原版 1.21.11 的 ZombifiedPiglin 是完全中立实体：NearestAttackableTargetGoal 的
 * selector 是 NeutralMob.isAngryAt —— 未激怒时永远不把玩家当作目标。
 *
 * R196 则相反（EntityPigZombie.findPlayerToAttack）：
 * - 猪人始终把玩家列为潜在目标；
 * - 未激怒（angerLevel < 1）时索敌距离 = 正常距离 ÷ 4：
 *   正常距离 = min(followRange=40→clamp, 24) = 24 → 未激怒时仅 6 格内
 *   （这正是"靠近索敌"——玩家必须走到约 6 格内才会被盯上）；
 * - 索敌选中新玩家即 becomeAngryAt(target)（激怒，400~800 tick）；
 * - 激怒后以完整 24 格距离追击，直至 anger 耗尽恢复短索敌。
 *   1.21.11 侧复用 vanilla NeutralMob anger 机制表达：
 *   setPersistentAngerTarget(玩家) + startPersistentAngerTimer() 后，
 *   原版 NearestAttackableTargetGoal(isAngryAt) 会自动接管 24 格追击。
 *
 * 探测频率对齐 R196 updateEntityActionState：普通怪物每 10 tick 索敌一次。
 */
@Mixin(ZombifiedPiglin.class)
public abstract class ICPMZombifiedPiglinAggroMixin {

    /** R196：未激怒时索敌距离 = 24 / 4 = 6 格（含视线要求）。 */
    @Unique
    private static final float ICPM$CALM_TARGET_RANGE = 6.0f;

    /** R196 updateEntityActionState 普通怪玩家索敌频率（每 10 tick 一次）。 */
    @Unique
    private static final int ICPM$TARGET_SCAN_INTERVAL = 10;

    @Unique
    private int icpm$targetScanCounter = 0;

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void icpm$calmNearbyPlayerAggro(ServerLevel level, CallbackInfo ci) {
        ZombifiedPiglin piglin = (ZombifiedPiglin) (Object) this;
        if (!piglin.isAlive()) {
            return;
        }
        NeutralMob neutral = (NeutralMob) (Object) this;
        // 已激怒：交由 vanilla anger 目标（24 格追击）处理；激怒中不重复扫描
        if (neutral.isAngry()) {
            return;
        }
        // 已有其它目标（如其它僵尸猪灵攻击触发）时跳过
        if (piglin.getTarget() != null) {
            return;
        }
        icpm$targetScanCounter++;
        if (icpm$targetScanCounter < ICPM$TARGET_SCAN_INTERVAL) {
            return;
        }
        icpm$targetScanCounter = 0;

        // R196 findPlayerToAttack：取范围内最近玩家
        Player nearest = level.getNearestPlayer(piglin, ICPM$CALM_TARGET_RANGE);
        if (nearest == null) {
            return;
        }
        // R196 requiresLineOfSightToTargets=true：索敌需要视线
        if (!piglin.getSensing().hasLineOfSight(nearest)) {
            return;
        }

        // R196 becomeAngryAt(target)：选中新玩家即激怒（触发音效/移速提升/24 格追击均走 vanilla）
        neutral.setPersistentAngerTarget(EntityReference.of(nearest));
        neutral.startPersistentAngerTimer();
        piglin.setTarget(nearest);
    }
}
