package name.icpm.mixin;

import name.icpm.common.MobGoalSelectorAccess;
import name.icpm.entity.ai.SkeletonEatBoneGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 骷髅家族：注册吃骨头回血目标（R196 EntityAIMoveToRepairItem + isRepairItem(bone) + getHealFX=repair）。
 *
 * <p>挂 {@code registerGoals} 的声明类 {@link Mob}（铁律 2026-08-19：AbstractSkeleton 不重写
 * registerGoals，挂 @Mixin(AbstractSkeleton) 会运行期崩 "could not find any targets matching 'registerGoals'"），
 * 用 instanceof 过滤只对骷髅生效。
 */
@Mixin(Mob.class)
public abstract class SkeletonFamilyGoalsMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void icpm$registerEatBone(CallbackInfo ci) {
        if (!((Object) this instanceof AbstractSkeleton self)) {
            return;
        }
        var gs = MobGoalSelectorAccess.get(self);
        if (gs != null) {
            gs.addGoal(5, new SkeletonEatBoneGoal(self));
        }
    }
}
