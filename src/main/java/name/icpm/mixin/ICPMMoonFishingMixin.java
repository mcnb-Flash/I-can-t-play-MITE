package name.icpm.mixin;

import name.icpm.common.ICPMMoonPhase;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 蓝月：钓鱼上钩等待时间大幅缩短（每 tick 额外 -2，R196 蓝月钓鱼加速）。
 */
@Mixin(FishingHook.class)
public abstract class ICPMMoonFishingMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void icpm$blueMoonFishing(CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        if (!(hook.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        if (!ICPMMoonPhase.isBlueMoonNight(serverLevel)) {
            return;
        }
        FishingHookAccessor accessor = (FishingHookAccessor) hook;
        int waiting = accessor.icpm$getTimeUntilLured();
        if (waiting > 60) {
            accessor.icpm$setTimeUntilLured(Math.max(60, waiting - 2));
        }
    }
}
