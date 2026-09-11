package name.icpm.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import name.icpm.item.ICPMItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * ICPM 银制工具悬浮提示 Mixin（仅客户端）
 *
 * 当玩家手持 / 查看银制工具时：
 *  - 默认状态下不显示 "亡灵杀手 I" 字样
 *  - 按住 Shift 时，在 tooltip 末尾追加 "亡灵杀手 I" 信息行
 *
 * 这样既保留了"强制获得亡灵杀手一的效果"，又通过 Shift 控制信息提示的显隐。
 *
 * 该 Mixin 在所有 Item 的 appendHoverText 末尾运行，只对银制工具进行特殊处理。
 */
@Mixin(Item.class)
public class ICPMSilverTooltipMixin {

    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void icpm$addSilverSmiteTooltip(
        ItemStack stack,
        Item.TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> tooltipAdder,
        TooltipFlag flags,
        CallbackInfo ci
    ) {
        Item self = (Item) (Object) this;
        if (!isSilverTool(self)) {
            return;
        }

        // 仅在高级 tooltip（按住 Shift）或创造模式下显示
        boolean advanced = flags.isAdvanced();
        boolean shiftDown = icpm$isShiftKeyDown();
        if (!shiftDown && !advanced) {
            return;
        }

        // 显示 "亡灵杀手 I"（灰色文本，与原版附魔颜色一致）
        MutableComponent smiteLine = Component.literal("亡灵杀手 I")
            .withStyle(ChatFormatting.GRAY);
        MutableComponent damageLine = Component.literal("  对亡灵生物造成额外伤害")
            .withStyle(ChatFormatting.DARK_GRAY);
        tooltipAdder.accept(smiteLine);
        tooltipAdder.accept(damageLine);
    }

    /**
     * 检测左/右 Shift 键是否按下
     *
     * 在 Minecraft 1.21.11 中 Screen.hasShiftDown() 已被移除，
     * 这里直接通过 GLFW 查询物理按键状态。
     */
    private static boolean icpm$isShiftKeyDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /**
     * 判断当前 Item 是否为银制工具
     */
    private static boolean isSilverTool(Item item) {
        return item == ICPMItems.SILVER_SWORD
            || item == ICPMItems.SILVER_PICKAXE
            || item == ICPMItems.SILVER_AXE
            || item == ICPMItems.SILVER_SHOVEL
            || item == ICPMItems.SILVER_HOE
            || item == ICPMItems.SILVER_HATCHET
            || item == ICPMItems.SILVER_DAGGER
            || item == ICPMItems.SILVER_WAR_HAMMER
            || item == ICPMItems.SILVER_BATTLE_AXE
            || item == ICPMItems.SILVER_SCYTHE
            || item == ICPMItems.SILVER_MATTOCK;
    }
}

