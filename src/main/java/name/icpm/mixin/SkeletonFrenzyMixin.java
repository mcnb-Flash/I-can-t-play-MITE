package name.icpm.mixin;

import name.icpm.common.ICPMMoonPhase;
import name.icpm.common.MobGoalSelectorAccess;
import name.icpm.common.SkeletonFrenzyState;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 骷髅系 frenzy（MITE 骨王灵感 + 血月叠加 忠实移植，infx 开发计划参考）。
 *
 * <p>触发与效果：
 * <ul>
 *   <li>骨王灵感：附近（16 格内）存在 {@code BoneLordEntity} → 移速 ×1.2 + 100% 近战 + 破门 ×2；</li>
 *   <li>血月叠加：{@link ICPMMoonPhase#isBloodMoonNight} 当夜 → 100% 近战 + 破门 ×2
 *       （速度由全局 ICPMMoonFrenzyMixin 的 Speed I 提供，不重复叠加）；</li>
 *   <li>远程 CD ×0.67（R196 60→40）：所有骷髅族弓击间隔整体下调（基线 buff）。</li>
 * </ul>
 *
 * <p>每 tick 的状态推进在 {@link SkeletonFrenzyTickMixin}（挂 LivingEntity.tick）+ 静态表
 * {@link SkeletonFrenzyState}；本 mixin 只保留 AbstractSkeleton【自身声明】的方法注入
 * （reassessWeaponGoal / getAttackInterval / getHardAttackInterval），避免继承方法挂错声明类崩溃。
 *
 * <p>该 mixin 作用于 {@link AbstractSkeleton}，因此同时覆盖原版 Skeleton / Stray / WitherSkeleton / Bogged
 * 以及 ICPM 自定义骷髅变体（均继承自 Skeleton → AbstractSkeleton）。
 */
@Mixin(AbstractSkeleton.class)
public abstract class SkeletonFrenzyMixin {

    @Shadow
    protected RangedBowAttackGoal bowGoal;
    @Shadow
    protected MeleeAttackGoal meleeGoal;

    // 远程 CD ×0.67（R196 60→40）：所有骷髅族弓击间隔整体下调（基线 buff）。
    @Inject(method = "getAttackInterval", at = @At("RETURN"), cancellable = true)
    private void icpm$fasterBow(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) Math.max(1, Math.round(cir.getReturnValueI() * 0.67)));
    }

    @Inject(method = "getHardAttackInterval", at = @At("RETURN"), cancellable = true)
    private void icpm$fasterBowHard(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) Math.max(1, Math.round(cir.getReturnValueI() * 0.67)));
    }

    // 100% 近战：狂暴时强制使用近战目标，移除弓目标。
    @Inject(method = "reassessWeaponGoal", at = @At("RETURN"))
    private void icpm$forceMeleeWhenFrenzied(CallbackInfo ci) {
        AbstractSkeleton self = (AbstractSkeleton) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        boolean frenzied = ICPMMoonPhase.isBloodMoonNight(self.level())
                || SkeletonFrenzyState.get(self).boneLordNear;
        if (!frenzied) {
            return;
        }
        var gs = MobGoalSelectorAccess.get(self);
        if (gs == null) {
            return;
        }
        gs.removeGoal(bowGoal);
        gs.removeGoal(meleeGoal);
        gs.addGoal(4, meleeGoal);
    }
}
