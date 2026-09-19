package name.icpm.client.mixin;

import name.icpm.common.ICPMPortalHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 地下世界 32 天雾色循环（R196 {@code WorldProviderUnderworld.getFogColor}）。
 *
 * <pre>
 * int day_of_cycle       = viewer.worldObj.getDayOfWorld() % 32;
 * int distance_from_peak = Math.abs(day_of_cycle - 16);
 * float grayscale        = distance_from_peak * 0.004f;
 * return (grayscale, grayscale, grayscale);
 * </pre>
 *
 * 即：第 16 天雾色最黑（0.0），第 0 / 32 天最亮（16 × 0.004 = 0.064），32 天一个循环。
 * 天数口径与项目其他处一致：{@code getDayOfWorld(tick) = (tick + 6000) / 24000 + 1}。
 *
 * <p>1.21.11 的雾色由 {@code AtmosphereFogEnvironment.getBaseColor} 提供（ARGB int）。
 * 本 mixin 只在**地下世界维度**改写 RGB，alpha 沿用原值，其余逻辑不受影响。
 */
@Mixin(AtmosphericFogEnvironment.class)
public class ICPMUnderworldFogMixin {

    @Inject(method = "getBaseColor", at = @At("RETURN"), cancellable = true)
    private void icpm$underworldFogColor(ClientLevel level, Camera camera, int tint, float partialTick,
                                         CallbackInfoReturnable<Integer> cir) {
        if (level == null || !level.dimension().equals(ICPMPortalHandler.UNDERWORLD_KEY)) {
            return;
        }
        int dayOfWorld = (int) ((level.getDayTime() + 6000L) / 24000L) + 1;
        int dayOfCycle = ((dayOfWorld % 32) + 32) % 32;
        float grayscale = Math.abs(dayOfCycle - 16) * 0.004f;
        int g = Mth.clamp((int) (grayscale * 255.0f), 0, 255);
        int original = cir.getReturnValue();
        // 仅替换 RGB，保留原 alpha 通道
        cir.setReturnValue((original & 0xFF000000) | (g << 16) | (g << 8) | g);
    }
}
