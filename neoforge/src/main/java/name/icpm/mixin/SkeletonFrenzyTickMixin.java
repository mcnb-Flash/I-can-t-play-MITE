package name.icpm.mixin;

import name.icpm.common.SkeletonFrenzyState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 骷髅 frenzy 每 tick 驱动（挂在 {@code tick} 的声明类 LivingEntity 上）。
 *
 * <p>铁律（2026-08-19 实测）：{@code tick()} 声明于 LivingEntity，AbstractSkeleton 不重写，
 * 在 @Mixin(AbstractSkeleton) 里 @Inject(method="tick") 会运行期崩溃
 * "could not find any targets matching 'tick'"。故本 mixin 挂 LivingEntity，
 * 用 instanceof 过滤 + {@link SkeletonFrenzyState} 静态表驱动。
 */
@Mixin(LivingEntity.class)
public abstract class SkeletonFrenzyTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void icpm$skeletonFrenzyTick(CallbackInfo ci) {
        if (!((Object) this instanceof AbstractSkeleton sk)) {
            return;
        }
        if (sk.level().isClientSide()) {
            return;
        }
        SkeletonFrenzyState.tick(sk);
    }
}
