package name.icpm.mixin;

import name.icpm.common.ICPMHealProgressManager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 处理原版回血：
 * 1. ICPM 自定义回血 → 正常生效
 * 2. 生命回复效果（Regeneration）→ 增加25%回血间隔（每5次放行4次）
 * 3. 原版饱食度回血 → 取消
 */
@Mixin(LivingEntity.class)
public abstract class DisableVanillaHealingMixin {

    // Regeneration 回血计数器：每5次放行4次（相当于增加25%时间）
    @Unique
    private int icpm$regenHealCounter = 0;

    /**
     * 拦截 heal 调用：
     * - ICPM 回血：放行
     * - Regeneration 效果：增加25%间隔（每5次放行4次）
     * - 原版饱食度回血：取消
     */
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void icpm$onHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }

        // ICPM 回血：放行
        if (ICPMHealProgressManager.isMitHealing()) {
            return;
        }

        // Regeneration 效果：增加25%回血间隔
        if (player.hasEffect(MobEffects.REGENERATION)) {
            icpm$regenHealCounter++;
            // 每5次请求，跳过第5次（相当于增加25%时间）
            if (icpm$regenHealCounter % 5 == 0) {
                icpm$regenHealCounter = 0; // 重置计数器
                ci.cancel(); // 跳过这次回血
            }
            // 其他4次放行
            return;
        }

        // 原版饱食度回血：取消
        ci.cancel();
    }
}