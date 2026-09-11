package name.icpm.mixin;

import name.icpm.ICPM;
import name.icpm.common.EnumQuality;
import name.icpm.component.QualityComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 为物品添加品质支持
 *
 * 提供获取和设置物品品质的方法（供合成/右键品质切换等业务代码调用）。
 * tooltip 展示逻辑见独立的 QualityTooltipMixin（注入 Item.appendHoverText）。
 */
@Mixin(ItemStack.class)
public class QualityItemMixin {

    /**
     * 获取物品的品质
     * 如果物品没有品质数据，则返回普通品质（AVERAGE）
     */
    @Unique
    public EnumQuality icpm$getQuality() {
        ItemStack stack = (ItemStack) (Object) this;
        QualityComponent component = stack.get(ICPM.QUALITY_COMPONENT);
        return component != null ? component.quality() : EnumQuality.AVERAGE;
    }

    /**
     * 设置物品的品质
     */
    @Unique
    public void icpm$setQuality(EnumQuality quality) {
        ItemStack stack = (ItemStack) (Object) this;
        stack.set(ICPM.QUALITY_COMPONENT, QualityComponent.of(quality));
    }

    /**
     * 获取物品的品质等级（用于合成时间计算）
     */
    @Unique
    public int icpm$getQualityLevel() {
        return icpm$getQuality().getQualityLevel();
    }

    /**
     * 获取物品的耐久度修正系数
     */
    @Unique
    public float icpm$getDurabilityModifier() {
        return icpm$getQuality().getDurabilityModifier();
    }

    /**
     * 切换物品的品质等级（工作台右键交互）
     */
    @Unique
    public void icpm$cycleQuality() {
        EnumQuality current = icpm$getQuality();
        EnumQuality next = current.next();
        if (next != null) {
            icpm$setQuality(next);
        }
    }
}
