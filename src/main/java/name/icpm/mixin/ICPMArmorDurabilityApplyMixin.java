package name.icpm.mixin;

import name.icpm.common.ICPMArmorDurabilityManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 护甲耐久应用（R196 ItemArmor / InventoryPlayer.tryDamageArmor 忠实移植）。
 *
 * <p>ICPMArmorDurabilityManager.handleArmorDurabilityDamage 是设计中的 MITE 护甲耐久系统，
 * 但此前从未被调用（死代码），导致玩家护甲挨打完全不掉耐久。本 Mixin 将其接入伤害流程：
 * <ul>
 *   <li>在 LivingEntity.hurtServer HEAD 对玩家调用 handleArmorDurabilityDamage，
 *       按 R196 逻辑把"本次伤害 vs 护甲防护值"的耐久池随机分摊到各护甲件。</li>
 *   <li>同步取消原版 hurtArmor 对玩家的耐久消耗（HEAD cancel），避免双重扣减。</li>
 * </ul>
 *
 * <p>守卫：仅服务端、仅玩家、非创造、非 bypasses_armor 伤害才走 MITE 逻辑（与 R196
 * InventoryPlayer.tryDamageArmor 跳过 isUnblockable 一致）。生物仍走原版 hurtArmor。
 */
@Mixin(LivingEntity.class)
public abstract class ICPMArmorDurabilityApplyMixin {

    /** 玩家受击时按 R196 方式分摊护甲耐久（替代原版 hurtArmor）。 */
    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void icpm$applyArmorDurability(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        if (!(self instanceof Player player)) {
            return;
        }
        if (player.isCreative()) {
            return;
        }
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }
        ICPMArmorDurabilityManager.handleArmorDurabilityDamage(player, amount);
    }

    /** 取消原版 hurtArmor 对玩家的耐久消耗（改由上面的 MITE 逻辑处理，避免双重扣减）。 */
    @Inject(method = "hurtArmor", at = @At("HEAD"), cancellable = true)
    private void icpm$cancelVanillaHurtArmor(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        if (self instanceof Player) {
            ci.cancel();
        }
    }
}
