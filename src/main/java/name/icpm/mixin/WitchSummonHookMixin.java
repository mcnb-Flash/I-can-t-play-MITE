package name.icpm.mixin;

import name.icpm.curse.WitchSummonManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 女巫召狼触发钩子：Witch 未必覆写 hurtServer，故在 LivingEntity.hurtServer 守卫 instanceof。
 * 被玩家打伤时登记一次召唤（一生一次语义在 WitchSummonManager 内）。
 */
@Mixin(LivingEntity.class)
public abstract class WitchSummonHookMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void icpm$witchSummonOnHurt(ServerLevel level, DamageSource source, float amount,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Witch witch)) {
            return;
        }
        if (source.getEntity() instanceof ServerPlayer player && !witch.level().isClientSide()) {
            WitchSummonManager.onWitchHurtByPlayer(witch, player);
        }
    }
}
