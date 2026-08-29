package name.icpm.mixin;

import name.icpm.common.ICPMDurability;
import name.icpm.common.ICPMMixinShared;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * ICPM 工具耐久消耗 Mixin
 *
 * 拦截 ItemStack.hurtAndBreak，对 ICPM 工具应用自定义耐久消耗公式
 *
 * 挖掘耐久消耗公式（来自 xj.java）：
 *   decay = 100 * baseDecayRate
 *   cost = max(max(int(hardness * decay), int(decay / 20)), 1)
 *
 * 攻击耐久消耗公式：
 *   cost = max(int(100 * attackDecayRate), 1)
 */
@Mixin(ItemStack.class)
public abstract class ICPMToolDurabilityMixin {

    /**
     * 拦截 hurtAndBreak，对所有工具应用ICPM耐久消耗
     */
    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"), cancellable = true)
    private void icpm$onHurtAndBreak(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof ServerPlayer player)) return;

        ItemStack stack = (ItemStack) (Object) this;
        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        // 检查是否有待处理的挖掘耐久消耗
        Integer pendingCost = ICPMMixinShared.getPendingBreakCost();
        if (pendingCost != null) {
            ICPMMixinShared.clearPendingBreakCost();
            ci.cancel();
            applyDurabilityCost(stack, pendingCost, player, slot);
            return;
        }

        // 默认情况：原版调用，消耗 1 点耐久
        // 这里不做额外处理，让原版逻辑继续
    }

    /**
     * 应用耐久消耗（含耐久三附魔减免）
     * 直接修改damageValue避免递归调用
     */
    @Unique
    private void applyDurabilityCost(ItemStack stack, int cost, ServerPlayer player, EquipmentSlot slot) {
        int unbreakingLevel = getUnbreakingLevel(player, stack);
        int actualDamage = 0;

        // 计算实际伤害（考虑耐久附魔）
        for (int i = 0; i < cost; i++) {
            if (unbreakingLevel > 0) {
                // 耐久三每级有 15% 概率抵消 1 点耐久消耗
                if (player.getRandom().nextInt(100) < unbreakingLevel * 15) {
                    continue;
                }
            }
            actualDamage++;
        }

        if (actualDamage > 0) {
            // 直接修改damageValue避免递归
            int newDamage = stack.getDamageValue() + actualDamage;
            stack.setDamageValue(newDamage);

            // 如果耐久度耗尽，触发破损逻辑
            if (newDamage >= stack.getMaxDamage()) {
                stack.shrink(1);
            }
        }
    }

    /**
     * 获取耐久三附魔等级
     */
    @Unique
    private int getUnbreakingLevel(ServerPlayer player, ItemStack stack) {
        var allEnchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var entry : allEnchants.entrySet()) {
            var key = entry.getKey();
            if (key.is(Enchantments.UNBREAKING)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
