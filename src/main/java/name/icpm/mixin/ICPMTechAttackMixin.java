package name.icpm.mixin;

import name.icpm.common.ICPMWorldEvil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 世界恶意「技术不佳」：近战攻击伤害 ×(1 - 0.25×档)。
 *
 * 攻击力在 vanilla/ICPM 最终都经 Player.attack → Entity.hurtOrSimulate(伤害源, 伤害量) 结算；
 * 这里直接在调用出口处改写伤害量，覆盖一切来源的最终近战伤害（含武器附魔等加成之后），
 * 且不依赖玩家 ATTACK_DAMAGE 属性（部分 mod 会自行重算属性，属性修饰会被绕过）。
 */
@Mixin(Player.class)
public abstract class ICPMTechAttackMixin {

    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean icpm$reduceMeleeDamage(Entity target, DamageSource source, float amount) {
        float factor = ICPMWorldEvil.penaltyFactor();
        if (factor >= 1.0f || amount <= 0.0f) {
            return target.hurtOrSimulate(source, amount);
        }
        return target.hurtOrSimulate(source, amount * factor);
    }
}
