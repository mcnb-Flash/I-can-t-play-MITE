package name.icpm.mixin;

import name.icpm.item.ICPMItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 粪便燃料 Mixin
 *
 * 原版燃料数据是代码内置（FuelValues.vanillaBurnTimes），无法通过数据包追加，
 * 故在 AbstractFurnaceBlockEntity.getBurnDuration 处注入：粪便燃烧 100 tick（ICPM 1.6.4 Item.getBurnTime）。
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public class FurnaceFuelMixin {

    @Inject(method = "getBurnDuration", at = @At("RETURN"), cancellable = true)
    private void icpm$manureAsFuel(FuelValues fuelValues, ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
        if (itemStack.is(ICPMItems.MANURE)) {
            cir.setReturnValue(100);
        }
    }
}
