package name.icpm.mixin;

import name.icpm.ICPM;
import name.icpm.common.EnumQuality;
import name.icpm.component.QualityComponent;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版工具ICPM耐久覆盖
 * 将原版工具（铜、铁、金、钻石）的最大耐久度替换为ICPM公式计算值
 * 
 * ICPM公式：4 × 部件数 × 材质耐久系数 × 100（平均品质）
 * 然后应用品质系数
 * 
 * 示例：
 * - 铜稿 = 4 × 3 × 4.0 × 100 = 4800（普通品质）
 * - 铁稿 = 4 × 3 × 8.0 × 100 = 9600（普通品质）
 * - 金稿 = 4 × 3 × 4.0 × 100 = 4800（普通品质）
 * - 钻石稿 = 4 × 3 × 16.0 × 100 = 19200（普通品质）
 */
@Mixin(value = ItemStack.class, priority = 900)
public class VanillaToolDurabilityMixin {

    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void icpm$overrideVanillaToolDurability(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        int originalMaxDamage = cir.getReturnValue();

        // 只对可损坏物品生效
        if (originalMaxDamage <= 0) {
            return;
        }

        // 检查是否是已注册的工具
        if (!ICPMToolProperties.isICPMTool(stack)) {
            return;
        }

        // 获取ICPM计算的耐久值（基础值）
        int miteDurability = ICPMToolProperties.getMaxDurability(stack);
        if (miteDurability <= 0) {
            return;
        }

        // 应用品质系数
        QualityComponent qualityComponent = stack.get(ICPM.QUALITY_COMPONENT);
        if (qualityComponent != null) {
            EnumQuality quality = qualityComponent.quality();
            float multiplier = quality.getDurabilityModifier();
            miteDurability = (int) (miteDurability * multiplier);
        }

        cir.setReturnValue(miteDurability);
    }
}
