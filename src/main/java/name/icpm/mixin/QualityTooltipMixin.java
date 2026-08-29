package name.icpm.mixin;

import name.icpm.ICPM;
import name.icpm.common.EnumQuality;
import name.icpm.component.CraftPreviewComponent;
import name.icpm.component.QualityComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * 物品品质 / 合成预览 tooltip 展示
 *
 * 注入 Item.appendHoverText（每个物品 tooltip 的通用钩子，对应 R196 ItemStack.getTooltip 中的
 * addInformation 段落），对任意物品统一展示：
 * - 品质描述符（灰）+ 耐久加成/惩罚（蓝/红）：任意带 QUALITY 组件的物品均显示（对齐 R196 灰色描述符 + 耐久修正）；
 * - 合成预览：经验消耗（黄）+ 可切换品质提示（黄）：仅挂在"工作台结果槽预览物品"上的 CRAFT_PREVIEW 组件触发。
 */
@Mixin(Item.class)
public class QualityTooltipMixin {

    @Inject(method = "appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V", at = @At("TAIL"))
    private void icpm$appendQualityTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        // 1) 品质描述符 + 耐久加成/惩罚（任意带品质物品通用，对齐 R196 ItemStack.getTooltip）
        QualityComponent qc = stack.get(ICPM.QUALITY_COMPONENT);
        if (qc != null) {
            EnumQuality q = qc.quality();
            tooltip.accept(Component.literal(q.getDescriptor()).withStyle(ChatFormatting.GRAY));
            float mod = q.getDurabilityModifier();
            if (mod < 1.0f) {
                tooltip.accept(Component.literal("耐久 -" + (int) ((1.0f - mod) * 100.0f) + "%").withStyle(ChatFormatting.RED));
            } else if (mod > 1.0f) {
                tooltip.accept(Component.literal("耐久 +" + (int) ((mod - 1.0f) * 100.0f) + "%").withStyle(ChatFormatting.BLUE));
            }
        }

        // 2) 合成预览：经验消耗 + 可切换品质提示（仅结果槽预览物品带此组件）
        CraftPreviewComponent cp = stack.get(ICPM.CRAFT_PREVIEW_COMPONENT);
        if (cp != null) {
            if (cp.maxQualityOrdinal() > cp.minQualityOrdinal()) {
                tooltip.accept(Component.literal("右键结果槽可切换品质").withStyle(ChatFormatting.YELLOW));
            }
            int xp = cp.xpCost();
            if (xp > 0) {
                tooltip.accept(Component.literal("合成消耗经验: " + xp).withStyle(ChatFormatting.YELLOW));
            }
        }

        // 3) 装盾提示：在对应等级工作台与盾牌合成后，右键可格挡（伤害减半）
        if (stack.has(ICPM.SHIELD_ATTACHED)) {
            tooltip.accept(Component.literal("右键格挡（伤害减半）").withStyle(ChatFormatting.GOLD));
        }
    }
}
