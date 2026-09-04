package name.icpm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 摔落伤半血动物 10% 掉肉 —— R196（Debris/sky 实测）：
 * 生物所受摔落伤害 ≥ 最大生命一半时，只有 10% 概率掉落物品，否则不掉。
 * （针对被动生物 CREATURE；玩家不受此规则约束）
 *
 * <p>死亡掉落只走 3 参重载 dropFromLootTable(ServerLevel, DamageSource, boolean)
 * （4 参重载为外部专用入口，勿注入同名方法）。
 */
@Mixin(LivingEntity.class)
public class FallHalfDropMixin {

    /** 本次死亡是否因"摔落半血以上"触发（本 tick 置位，掉落判据）。 */
    @Unique
    private boolean icpmFallHalfKill = false;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void icpm$markFallHalf(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (source.is(DamageTypes.FALL)
                && self.getType().getCategory() == MobCategory.CREATURE
                && amount >= self.getMaxHealth() / 2.0f) {
            icpmFallHalfKill = true;
        }
    }

    @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void icpm$fallHalfDropGate(ServerLevel level, DamageSource source, boolean p, CallbackInfo ci) {
        if (!icpmFallHalfKill) {
            return;
        }
        icpmFallHalfKill = false;
        if (source.is(DamageTypes.FALL)) {
            // 90% 概率不产生任何掉落
            if (level.random.nextFloat() >= 0.1f) {
                ci.cancel();
            }
        }
    }
}
