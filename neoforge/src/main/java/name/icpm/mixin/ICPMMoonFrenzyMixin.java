package name.icpm.mixin;

import name.icpm.common.ICPMMoonPhase;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 血月：敌对怪物狂暴（力量 I + 速度 I），每 40 tick 检查一次。
 * 注意：Monster 未覆写 tick()，需注入 LivingEntity.tick 再判断 instanceof Monster。
 */
@Mixin(LivingEntity.class)
public abstract class ICPMMoonFrenzyMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void icpm$bloodMoonFrenzy(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Monster monster) || self instanceof EnderMan) {
            return;
        }
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!ICPMMoonPhase.isBloodMoonNight(serverLevel) || self.tickCount % 40 != 0) {
            return;
        }
        if (!self.hasEffect(MobEffects.STRENGTH)) {
            self.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0));
        }
        if (!self.hasEffect(MobEffects.SPEED)) {
            self.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0));
        }
    }
}
