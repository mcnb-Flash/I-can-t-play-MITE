package name.icpm.client.mixin;

import name.icpm.client.InventoryCraftClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 背包界面（InventoryScreen）合成时间机制覆盖层（客户端）。
 *
 * 仅负责绘制：若背包 2x2 合成进行中，在结果槽上叠加进度条与百分比文字。
 * 时间门控与取走逻辑全部在服务端 InventoryMenu.clicked（ICPMInventoryCraftingMixin）完成，
 * 客户端不再拦截鼠标点击（1.21.11 的 mouseClicked 签名已改为 (MouseButtonEvent, boolean)，
 * 且方法名重载严重，客户端拦截不可靠）。
 *
 * 注意：本模组 mixin 无 refmap，故不 @Shadow。界面左上角用 (width-176)/2、(height-166)/2
 * 推导（背包 GUI 与原版一致，尺寸 176x166），结果槽坐标取自 getSlot(0) 的公开 x/y。
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenCraftMixin {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    @Inject(method = "render", at = @At("TAIL"))
    private void icpm$drawCraftProgress(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (!InventoryCraftClientState.active) {
            return;
        }
        InventoryScreen self = (InventoryScreen) (Object) this;
        AbstractContainerMenu menu = self.getMenu();
        if (!(menu instanceof InventoryMenu)) {
            return;
        }
        Slot resultSlot = menu.getSlot(0);
        int guiLeft = (self.width - GUI_WIDTH) / 2;
        int guiTop = (self.height - GUI_HEIGHT) / 2;
        int slotX = guiLeft + resultSlot.x;
        int slotY = guiTop + resultSlot.y;

        long currentTick = mc.level.getGameTime();
        float progress = InventoryCraftClientState.getProgress(currentTick);
        boolean complete = InventoryCraftClientState.isComplete(currentTick);

        // 结果槽底部向上填充的进度条
        int barMaxH = 16;
        int fillH = (int) (barMaxH * progress);
        int fillColor = complete ? 0x4000FF00 : 0x400066FF;
        guiGraphics.fill(slotX, slotY + (barMaxH - fillH), slotX + 16, slotY + barMaxH, fillColor);

        // 百分比文字（结果槽下方居中）
        String pct = (int) (progress * 100) + "%";
        int pctWidth = mc.font.width(pct);
        guiGraphics.drawString(mc.font, pct, slotX + 8 - pctWidth / 2, slotY + 18, 0x808080, false);
    }
}
