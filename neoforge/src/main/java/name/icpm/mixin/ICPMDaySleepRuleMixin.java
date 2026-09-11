package name.icpm.mixin;

import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 白天小睡：允许在白天躺下睡觉（BedRule.Rule.WHEN_DARK 白天返回 false，
 * 注入改为 true；NEVER 规则（地狱/末地床）不受影响）。
 */
@Mixin(targets = "net.minecraft.world.attribute.BedRule$Rule")
public abstract class ICPMDaySleepRuleMixin {

    @Inject(method = "test", at = @At("RETURN"), cancellable = true)
    private void icpm$allowDaySleep(Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && level.getDayTime() % 24000L < 13000L) {
            cir.setReturnValue(true);
        }
    }
}
