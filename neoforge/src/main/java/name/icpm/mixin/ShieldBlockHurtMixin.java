package name.icpm.mixin;

import name.icpm.ICPM;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 装盾工具格挡减伤 + 工具扣耐久 —— 忠实移植 R196 Damage.applyTargetDefenseModifiers。
 *
 * R196 逻辑：若受击者是玩家、伤害不无视寻常护甲、且玩家正在格挡（isBlocking），
 * 则 amount /= 2 并 floor 到 1；若手持 ItemTool，则工具承受
 * tryDamageItem((int)(amount * 攻击衰减率)) 的耐久消耗。
 *
 * 本 Mixin 在 LivingEntity.modifyAppliedDamage（伤害路径末端、护甲减伤之后）将最终伤害减半，
 * 并对装盾工具扣对应耐久。严格遵循用户选择"仅伤害减半(下限1) + 工具扣耐久；
 * 不挡箭、不免疫击退"：
 *  - 不挡箭：跳过 IS_PROJECTILE 来源（原版 hurtCurrentlyUsedShield 仅对 ShieldItem 生效，
 *    装盾工具非 ShieldItem，本就不挡箭；此处再显式排除，双保险）；
 *  - 不免疫击退：本 Mixin 不触碰击退逻辑。
 *
 * 减伤与扣耐久仅在服务端执行（level().isClientSide() 守卫），避免客户端重复。
 */
@Mixin(LivingEntity.class)
public class ShieldBlockHurtMixin {

    /**
     * 在伤害路径的"护甲减伤、魔抗计算之后"将最终伤害减半（下限 1），并对装盾工具扣耐久。
     *
     * <p>1.21.11 伤害链：hurtServer → getDamageAfterArmorAbsorb(护甲) → getDamageAfterMagicAbsorb(魔抗)
     * → absorption → actuallyHurt。R196 的 applyTargetDefenseModifiers 在护甲减伤之后做格挡减半；
     * 1.21.11 最贴近的注入点是 {@code getDamageAfterMagicAbsorb(DamageSource, float)} 的 RETURN
     * （护甲与魔抗均已结算，修改返回值即对最终可减伤害减半；无抗性药水时与 R196 顺序完全等价）。
     *
     * <p>注意：不能用 @ModifyArgs 注入该方法 HEAD（@ModifyArgs 只能作用于方法体内的调用指令），
     * 故用 @Inject(RETURN, cancellable) + setReturnValue + cancel 修改返回值。
     */
    @Inject(method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("RETURN"), cancellable = true)
    private void icpm$blockModifyDamage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (isBlockingWithShield(player, source)) {
            // R196：amount /= 2，floor 到 1
            float reduced = Math.max(cir.getReturnValue() / 2.0f, 1.0f);
            applyBlockDurability(player, reduced);
            cir.setReturnValue(reduced);
            cir.cancel();
        }
    }

    /**
     * 重写 isBlocking()，使"正在使用装盾工具且使用动画为 BLOCK"时返回 true，
     * 从而呈现抬手格挡姿态，并与任何检查 isBlocking() 的原版逻辑保持一致。
     * 仅在真正格挡（使用物品态）时返回 true，非格挡时不拦截原版（原版盾牌仍正常）。
     */
    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void icpm$isBlocking(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && player.isUsingItem()) {
            ItemStack useItem = player.getUseItem();
            if (useItem.has(ICPM.SHIELD_ATTACHED) && useItem.getUseAnimation() == ItemUseAnimation.BLOCK) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    /** 玩家是否正用装盾工具格挡（且来源可被格挡）。 */
    private boolean isBlockingWithShield(Player player, DamageSource source) {
        if (!player.isUsingItem()) {
            return false;
        }
        ItemStack useItem = player.getUseItem();
        if (!useItem.has(ICPM.SHIELD_ATTACHED)) {
            return false;
        }
        if (useItem.getUseAnimation() != ItemUseAnimation.BLOCK) {
            return false;
        }
        // 无视寻常护甲的伤害（如摔落/虚空）不被格挡
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return false;
        }
        // 不挡箭（严格 R196 选择）
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return false;
        }
        return true;
    }

    /** 对格挡工具扣耐久，公式 (int)(减半后伤害 × 攻击衰减率)，由 hurtAndBreak 处理耐久三（忠实 R196 tryDamageItem）。 */
    private void applyBlockDurability(Player player, float reduced) {
        ItemStack tool = player.getUseItem();
        if (tool.isEmpty()) {
            return;
        }
        float rate = ICPMToolProperties.getAttackDecayRate(tool);
        int cost = (int) (reduced * rate);
        if (cost <= 0) {
            return;
        }
        EquipmentSlot slot = (tool == player.getMainHandItem()) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        tool.hurtAndBreak(cost, player, slot);
    }
}
