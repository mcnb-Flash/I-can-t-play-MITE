package name.icpm.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁止在背包 2x2 合成格中合成木板。
 *
 * 木板（*_planks）必须在 ICPM 工作台（至少燧石工作台，tier 0）中加工。
 * 背包内的 2x2 合成使用 InventoryMenu，与 ICPM 工作台无关，
 * 因此在此拦截：当 2x2 合成结果为木板时清空结果槽，使其无法在背包中合成。
 *
 * ICPM 工作台（ICPMWorkbenchMenu）走独立的合成逻辑、不受此处影响，
 * 且其等级判定对木板返回 tier 0，所以燧石工作台及以上的 ICPM 工作台均可正常合成木板。
 *
 * 注意：本模组 mixin 配置未生成/加载 refmap，因此不能用 @Shadow 访问字段
 * （会触发 "No refMap loaded" 运行时崩溃）。这里通过把 this 转型为 InventoryMenu
 * 后调用公开的 getSlot(0)（背包合成结果槽，索引 0）来访问结果，无需 refmap。
 */
@Mixin(InventoryMenu.class)
public class ICPMBackpackCraftingMixin {

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void icpm$blockPlanksInBackpack(Container container, CallbackInfo ci) {
        if (!(container instanceof CraftingContainer)) {
            return;
        }
        InventoryMenu self = (InventoryMenu) (Object) this;
        Slot resultSlot = self.getSlot(0);
        ItemStack result = resultSlot.getItem();
        if (!result.isEmpty()) {
            String path = BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
            // 木板必须用 ICPM 工作台
            if (path.endsWith("_planks")) {
                resultSlot.set(ItemStack.EMPTY);
                return;
            }
            // 金属币必须在对应等级 ICPM 工作台合成，背包 2x2 只允许分解（coin_uncraft 配方）
            // 粒→币的 1 格配方在背包也会匹配，这里直接清空，防止背包白嫖币
            if (name.icpm.common.ICPMCoinHelper.xpForCoinByItem(result) > 0) {
                resultSlot.set(ItemStack.EMPTY);
            }
        }
    }
}
