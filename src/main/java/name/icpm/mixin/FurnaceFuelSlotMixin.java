package name.icpm.mixin;

import name.icpm.blockentity.ICPMFurnaceBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 熔炉燃料槽判定修复。
 *
 * 原版燃料槽 FurnaceFuelSlot.mayPlace = menu.isFuel(stack) || isBucket(stack)，
 * 其中 AbstractFurnaceMenu.isFuel 走 level.fuelValues().isFuel（原版燃料表），
 * isBucket 仅匹配原版空桶（stack.is(Items.BUCKET)）—— 完全绕过
 * ICPMFurnaceBlockEntity.canPlaceItem 的 ICPM 热量等级体系，
 * 导致 ICPM 岩浆桶、粪便等自定义燃料无法放入熔炉燃料槽。
 *
 * 注入 AbstractFurnaceMenu.isFuel：当容器为 ICPMFurnaceBlockEntity 时，
 * 改走 canPlaceItem(1, stack)（热量等级 1..maxHeatLevel 判定，对齐 R196 SlotFuel）。
 * 非 ICPM 熔炉（原版高炉/烟熏炉）不受影响，仍走原版燃料表。
 */
@Mixin(AbstractFurnaceMenu.class)
public class FurnaceFuelSlotMixin {

    @Shadow
    @Final
    protected Container container;

    @Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
    private void icpm$furnaceFuelIsFuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.container instanceof ICPMFurnaceBlockEntity furnace) {
            cir.setReturnValue(furnace.canPlaceItem(1, stack));
        }
    }
}
