package name.icpm.mixin;

import name.icpm.common.ICPMFuelValues;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 燃料表 Mixin（原版 / 高炉 / 烟熏炉的燃烧时长）。
 *
 * <p>1.21.11 的燃料表是代码内置的 {@code FuelValues.vanillaBurnTimes}，无法通过数据包追加，
 * 且与 R196 差异很大（原木 300→1600、岩浆桶 20000→3200、火把原本不能当燃料→800…）。
 * 故在 {@code AbstractFurnaceBlockEntity.getBurnDuration} 处按 R196 全表覆盖。
 *
 * <p>判决源与完整表格见 {@link ICPMFuelValues}（含 R196 文件:行号）。
 * 判定为 0 的条目<b>不</b>改写原版结果 —— 即只在 R196 明确给定 &gt; 0 的条目上覆盖，
 * 让 1.21 新增燃料（R196 时代不存在）保持可用。
 *
 * <p>燃料槽的「能否放入」另见 {@code FurnaceFuelSlotMixin}（ICPM 熔炉走热量等级判定）。
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public class FurnaceFuelMixin {

    @Inject(method = "getBurnDuration", at = @At("RETURN"), cancellable = true)
    private void icpm$miteFuelBurnTime(FuelValues fuelValues, ItemStack itemStack,
                                       CallbackInfoReturnable<Integer> cir) {
        int mite = ICPMFuelValues.burnTime(itemStack);
        if (mite > 0) {
            cir.setReturnValue(mite);
        }
    }
}
