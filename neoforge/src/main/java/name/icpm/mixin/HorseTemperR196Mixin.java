package name.icpm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 马「驯服逆反」—— EntityHorse（R196）受击惩罚移植：
 * 野马被玩家击伤（实际造成伤害）时 temper −10（R196：
 * {@code if (negatively_affected && wasCausedByPlayer && !isTame) increaseTemper(-10)}）。
 *
 * hurtServer 在 AbstractHorse 自身声明（继承方法注入需 @Mixin 声明类——ICPM 铁律）。
 * modifyTemper 为 AbstractHorse public 方法，可直接调用。
 */
@Mixin(AbstractHorse.class)
public abstract class HorseTemperR196Mixin {

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void icpm$hitUntamedTemperPenalty(ServerLevel level, DamageSource damageSource, float amount,
                                              CallbackInfoReturnable<Boolean> cir) {
        AbstractHorse self = (AbstractHorse) (Object) this;
        if (self.isTamed() || self.level().isClientSide()) {
            return;
        }
        // 仅当这次伤害真的生效且来源是玩家
        if (Boolean.TRUE.equals(cir.getReturnValue()) && damageSource.getEntity() instanceof Player) {
            self.modifyTemper(-10);
        }
    }
}
