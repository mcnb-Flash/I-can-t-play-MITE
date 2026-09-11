package name.icpm.mixin;

import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 食物数据修改（R196 FoodStats 双槽系统适配）：
 *
 * 原版 FoodData 仅作为"显示层"保留（foodLevel=nutrition、saturationLevel=satiation），
 * 其消耗/回血/饥饿/进食数值逻辑全部禁用，由 ICPMFoodStats（PlayerMixin 驱动）接管：
 * 1. tick HEAD cancel —— 禁用原版饱食度消耗、原版回血、饥饿伤害
 * 2. addExhaustion HEAD cancel —— 禁用原版活动疲劳累积（R196 消耗速率固定，不随活动变化）
 * 3. eat(FoodProperties) HEAD cancel —— 禁用原版食物数值（R196 satiation/nutrition 由进食钩子提供）
 * 4. setFoodLevel TAIL —— 钳制饱和度，防止显示层数据溢出
 */
@Mixin(FoodData.class)
public class FoodDataMixin {

    @Shadow
    private float saturationLevel;

    @Shadow
    private int foodLevel;

    /**
     * 禁用原版 FoodData.tick（消耗/回血/饥饿）。
     * R196 消耗与回血由 ICPMFoodStats.tick 在 PlayerMixin 中驱动（服务端）。
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void icpm$disableVanillaTick(CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * 禁用所有 exhaustion 累积（行走/奔跑/跳跃/游泳等活动疲劳）。
     * R196 的 hunger 为固定速率（0.002/tick），不随活动变化。
     */
    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void icpm$disableExhaustion(float value, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * 禁用原版进食数值（eat(FoodProperties)）。
     * R196 satiation/nutrition 数值由 FoodNutritionMixin 调用的 ICPMFoodStats.onFoodEaten 提供，
     * 避免与原版 foodLevel/saturation 数值双重叠加。
     */
    @Inject(method = "eat(Lnet/minecraft/world/food/FoodProperties;)V", at = @At("HEAD"), cancellable = true)
    private void icpm$disableVanillaEat(FoodProperties foodProperties, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * 任何代码调用 setFoodLevel 后，自动将饱和度钳制到新饱食度以内。
     * 防止显示层数据不一致（foodLevel 降低后 saturationLevel 超限）。
     */
    @Inject(method = "setFoodLevel", at = @At("TAIL"))
    private void icpm$clampSaturationAfterSetFoodLevel(CallbackInfo ci) {
        if (saturationLevel > foodLevel) {
            saturationLevel = foodLevel;
        }
        if (saturationLevel < 0.0f) {
            saturationLevel = 0.0f;
        }
    }
}
