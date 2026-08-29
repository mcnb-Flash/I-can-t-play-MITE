package name.icpm.client.mixin;

import name.icpm.client.hud.NutritionHUD;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 营养值 HUD 渲染 Mixin
 * 只在游戏内 HUD（非容器界面）左下角渲染蛋白质和植物营养素条，避免挡住物品栏/容器界面
 */
@Mixin(Gui.class)
public class NutritionHUDMixin {
    
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHotbarAndDecorations(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void icpm$onRender(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        NutritionHUD.INSTANCE.render(graphics, graphics.guiWidth(), graphics.guiHeight());
    }
}
