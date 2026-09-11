package name.icpm.mixin;

import name.icpm.entity.ICPMLivestock;
import name.icpm.entity.LivestockState;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MITE 牲畜生理 tick —— 挂到 {@link LivingEntity}（tick 声明于 LivingEntity/Entity）。
 *
 * 不能挂到 {@link ICPMLivestockBehaviorMixin} 的 Animal 目标上：tick 声明于上层父类，
 * 注解处理器对该（Animal）目标无法写出 refmap 条目（裸命名静默失败、显式描述符直接报
 * "Cannot find target method"），运行时 remap 找不到中间名 method_5773 而崩溃。
 * 仿照项目内其它能正常工作的 tick mixin（如 ICPMMoonFrenzyMixin 直接 @Mixin(LivingEntity)），
 * 这里直接挂在 LivingEntity，由 isLivestock 守卫仅对 Cow/Pig/Sheep/Chicken 生效。
 */
@Mixin(LivingEntity.class)
public abstract class ICPMLivestockTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void icpm$tick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        if (!(self instanceof Animal animal) || !LivestockState.isLivestock(animal)) {
            return;
        }
        LivestockState.tickLogic(LivestockState.get(animal), animal);
    }
}
