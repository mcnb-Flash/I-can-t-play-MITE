package name.icpm.client.mixin;

import name.icpm.common.ICPMMoonPhase;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 血月红眼（MITE 移植，infx 开发计划参考）。
 *
 * A6 血月红眼：EntityRenderer.extractRenderState 在血月夜为敌对生物（Monster）设置
 * MITE 狂暴发光色 8527390——现代 MC 的 outlineColor 即 1.6.4 的 glow 色字段
 * （对齐 MITE EntityLivingBase.java:729-733）。
 *
 * 注入抽象基类 EntityRenderer.extractRenderState（所有实体的渲染状态快照入口），
 * LivingEntityRenderer 会调用 super.extractRenderState 填充 EntityRenderState 基类字段，
 * 故在基类方法 RETURN 覆盖 outlineColor 即可对所有 Monster 生效（客户端渲染，服务端无此类）。
 */
@Mixin(EntityRenderer.class)
public abstract class ICPMBloodMoonEyesMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void icpm$bloodMoonRedEyes(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof Monster)) {
            return;
        }
        Level level = entity.level();
        if (level == null || !ICPMMoonPhase.isBloodMoonNight(level)) {
            return;
        }
        // MITE 狂暴发光色 8527390（十进制），现代 MC outlineColor 即 1.6.4 glow 色字段
        state.outlineColor = 8527390;
    }
}
