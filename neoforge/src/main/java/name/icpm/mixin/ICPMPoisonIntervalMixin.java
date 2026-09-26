package name.icpm.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 Potion.java:151 — 中毒（poison）的实际生效间隔 = 100 >> amplifier（I级=100t/II级=50t/III级=25t）。
 * 原版（1.21.4+ factor 模型）中毒大约每 25t 一次，与 R196 不符。
 *
 * 复用原版 MobEffect.applyEffectTick（伤害 1.0F，与 R196 performEffect 的 attackEntityFrom(poison,1.0f) 一致），
 * 仅把「是否在本 tick 应用」改成 R196 间隔。duration 从总时长递减，首次达到 interval 倍数时已延后一个周期，
 * 自然实现 R196「施加后首周期不立即扣血」的体感。
 */
@Mixin(MobEffect.class)
public abstract class ICPMPoisonIntervalMixin {

    @Inject(method = "shouldApplyEffectTickThisTick", at = @At("HEAD"), cancellable = true)
    private void icpm$poisonInterval(int duration, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != (Object) MobEffects.POISON.value()) {
            return; // 非中毒：交回原版逻辑
        }
        int interval = Math.max(1, 100 >> amplifier);
        cir.setReturnValue(duration % interval == 0);
        cir.cancel();
    }
}
