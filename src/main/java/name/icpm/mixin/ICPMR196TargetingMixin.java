package name.icpm.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 怪物索敌距离全套移植（goal 系统一层）。
 *
 * R196 语义（EntityLiving.getMaxTargettingRange + EntityCreature.updateEntityActionState）：
 * <pre>
 *   max_range = followRange 属性值（EntityLiving 默认 16 / EntityMob 32 / 僵尸等 40 / 蜘蛛族 28）
 *   if (recentlyHit &gt; 0) max_range *= 2;            // 受伤后 100 tick 内索敌半径翻倍
 *   if (max_range &gt; 24) max_range = 24;              // R100 pathing 调整：硬上限 24 格
 * </pre>
 * 潜行 ×0.8 / 隐身 ×(0.7×穿甲,clamp≥0.1) / 需视线 —— 1.21.11 已由 vanilla
 * TargetingConditions.test + getVisibilityPercent 原生等价实现（isDiscrete→0.8，
 * invisible→armorCover 系数），无需重复注入。
 *
 * 1.21.11 侧映射：TargetGoal.getFollowDistance() 是全部目标类 goal
 * （NearestAttackableTargetGoal / HurtByTargetGoal 等）索取索敌距离的唯一来源
 * （= mob.getAttributeValue(FOLLOW_RANGE)）。在 RETURN 处应用 R196 钳制即全套怪物一次生效。
 * recentlyHit（R196 受伤置 100，每 tick -1）↔ lastHurtByMobTimestamp（受伤时刻 tick，
 * 距今 &lt;=100 判定）。
 */
@Mixin(TargetGoal.class)
public abstract class ICPMR196TargetingMixin {

    /** R196 受伤后索敌半径翻倍窗口（recentlyHit=100）。 */
    private static final long R196_RECENTLY_HIT_WINDOW_TICKS = 100L;
    /** R196 R100 pathing 调整硬上限。 */
    private static final double R196_MAX_TARGET_RANGE = 24.0;

    @Shadow
    @Final
    protected Mob mob;

    @Inject(method = "getFollowDistance", at = @At("RETURN"), cancellable = true)
    private void icpm$r196ClampRange(CallbackInfoReturnable<Double> cir) {
        double raw = cir.getReturnValue();
        double maxRange = raw;
        // R196: recentlyHit > 0（受伤 100 tick 内）→ ×2
        if (icpm$recentlyHit()) {
            maxRange *= 2.0;
        }
        // R196: R100 调整 → 索敌半径硬上限 24（含受伤翻倍后）
        if (maxRange > R196_MAX_TARGET_RANGE) {
            maxRange = R196_MAX_TARGET_RANGE;
        }
        cir.setReturnValue(maxRange);
    }

    /** R196 recentlyHit&gt;0 等价判定：lastHurtByMobTimestamp 距今 ≤100 tick。 */
    private boolean icpm$recentlyHit() {
        Mob m = this.mob;
        if (m == null || m.level() == null || m.level().isClientSide()) {
            return false;
        }
        LivingEntity last = m.getLastHurtByMob();
        if (last == null) {
            return false;
        }
        int ts = m.getLastHurtByMobTimestamp();
        if (ts <= 0) {
            return false;
        }
        long now = m.level().getGameTime();
        return (now - ts) <= R196_RECENTLY_HIT_WINDOW_TICKS;
    }
}
