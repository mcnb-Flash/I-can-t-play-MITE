package name.icpm.client.mixin;

import name.icpm.common.ICPMMoonPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * 血月红色月亮（客户端渲染）。
 *
 * 1.21.11 月亮走 GPU RenderPass 管线，颜色由 DynamicUniforms.Transform 的
 * colorModulator（Vector4f，与纹理颜色相乘）决定。
 * 拦截 SkyRenderer.renderMoon 内部 DynamicUniforms.writeTransform 调用，
 * 血月之夜把 colorModulator 的 RGB 改为暗红 (1, 0.12, 0.12)，月亮即整体染红。
 */
@Mixin(SkyRenderer.class)
public class BloodMoonSkyMixin {

    @Unique
    private static final float BLOOD_RED = 1.0f;
    @Unique
    private static final float BLOOD_GREEN = 0.12f;
    @Unique
    private static final float BLOOD_BLUE = 0.12f;

    @ModifyArgs(
            method = "renderMoon",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")
    )
    private void icpm$bloodMoonTint(Args args) {
        var level = Minecraft.getInstance().level;
        if (level == null || !ICPMMoonPhase.isBloodMoonNight(level.getDayTime())) {
            return;
        }
        Vector4fc original = (Vector4fc) args.get(1);
        args.set(1, new Vector4f(BLOOD_RED, BLOOD_GREEN, BLOOD_BLUE, original.w()));
    }
}
