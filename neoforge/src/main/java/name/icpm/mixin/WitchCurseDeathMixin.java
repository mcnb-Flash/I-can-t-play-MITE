package name.icpm.mixin;

import name.icpm.curse.ICPMCurseManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 女巫死亡 → 撤销其施放的全部诅咒（R196 EntityWitch.onDeath → removeCursesForWitch）。
 *
 * <p>die() 声明于 LivingEntity，Witch 未覆写——注入继承方法必须在声明类上做，
 * 故本 mixin 挂 @Mixin(LivingEntity) 并以 instanceof Witch 守卫（防启动 "target not found"）。
 */
@Mixin(LivingEntity.class)
public abstract class WitchCurseDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void icpm$removeWitchCursesOnDeath(DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof Witch witch) {
            ICPMCurseManager.removeForWitch(witch);
            name.icpm.curse.WitchSummonManager.onWitchRemoved(witch);
        }
    }
}
