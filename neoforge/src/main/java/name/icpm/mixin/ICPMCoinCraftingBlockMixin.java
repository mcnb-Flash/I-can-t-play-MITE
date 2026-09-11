package name.icpm.mixin;

import name.icpm.common.ICPMCoinHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁止在原版 3x3 工作台（CraftingMenu）中合成金属币。
 *
 * 金属币必须在 ICPM 工作台（对应等级 + 消耗经验）中合成，
 * 原版工作台没有等级/经验机制，因此结果槽为币时直接清空。
 * 分解（币→粒，coin_uncraft 配方）不受影响，原版工作台仍可分解。
 */
@Mixin(CraftingMenu.class)
public class ICPMCoinCraftingBlockMixin {

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void icpm$blockCoinInVanillaCraftingTable(Container container, CallbackInfo ci) {
        if (!(container instanceof CraftingContainer)) {
            return;
        }
        CraftingMenu self = (CraftingMenu) (Object) this;
        Slot resultSlot = self.getSlot(0);
        ItemStack result = resultSlot.getItem();
        if (!result.isEmpty()) {
            String path = BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
            if (ICPMCoinHelper.xpForCoin(path) > 0) {
                resultSlot.set(ItemStack.EMPTY);
            }
        }
    }
}
