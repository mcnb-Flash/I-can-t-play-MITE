package name.icpm.client.mixin;

import name.icpm.client.ZoomHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * C 键视角缩放：按住 C 键时缩小 FOV。
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void icpm$onGetFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (ZoomHandler.isZooming()) {
            cir.setReturnValue((float) ZoomHandler.applyZoom(cir.getReturnValue()));
        }
    }
}
