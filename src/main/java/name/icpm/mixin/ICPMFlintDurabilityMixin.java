package name.icpm.mixin;

import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 ItemFlintAndSteel.setMaxDamage(16)：打火石最大耐久固定为 16（原版 64），忠实 MITE 手感。
 */
@Mixin(value = ItemStack.class, priority = 900)
public class ICPMFlintDurabilityMixin {

    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void icpm$flintMaxDamage(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof FlintAndSteelItem) {
            cir.setReturnValue(16);
        }
    }
}
