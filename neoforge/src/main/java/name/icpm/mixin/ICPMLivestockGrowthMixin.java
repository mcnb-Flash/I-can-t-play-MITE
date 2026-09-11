package name.icpm.mixin;

import name.icpm.entity.LivestockState;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MITE 牲畜成长门槛 —— 挂到 {@link AgeableMob}（setAge 声明于此）。
 *
 * 不能直接挂到 Animal 目标：AgeableMob 有 setAge(int) 与 setAge(int,boolean) 两个重载，
 * 裸命名时 AP 无法消歧、显式描述符又只查 Animal 自身（不含父类 AgeableMob），
 * 均无法生成 refmap 条目。故直接挂在声明类 AgeableMob，由 isLivestock 守卫仅对
 * Cow/Pig/Sheep/Chicken 生效（Villager 等同为 AgeableMob 但非 Animal，会被守卫跳过）。
 */
@Mixin(AgeableMob.class)
public abstract class ICPMLivestockGrowthMixin {

    @Inject(method = "setAge(I)V", at = @At("HEAD"), cancellable = true)
    private void icpm$gateGrowth(int age, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Animal animal) || !LivestockState.isLivestock(animal)) {
            return;
        }
        AgeableMob am = (AgeableMob) animal;
        LivestockState s = LivestockState.get(animal);
        if (am.isBaby() && age > am.getAge() && (s.isDesperateForFood() || s.isDesperateForWater())) {
            ci.cancel();
        }
    }
}
