package name.icpm.mixin;

import name.icpm.common.ICPMDurability;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM耐久机制：攻击实体时按 ICPM R196 公式计算耐久消耗
 * 公式：max(int(100 × 攻击衰减率), 1)
 *
 * 耐久消耗基于：
 * 1. 工具类型攻击衰减率 (ToolType.attackDecayRate)
 * 2. 耐久三附魔等级 (每级有 15% 概率抵消1点)
 */
@Mixin(ServerPlayer.class)
public class ICPMDurabilityAttackMixin {

    @Inject(method = "attack", at = @At("TAIL"))
    private void icpm$applyAttackDurability(Entity target, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().isClientSide()) return;

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return;

        // 只对ICPM工具生效
        if (!ICPMToolProperties.isICPMTool(heldItem)) return;

        // 只对活体实体生效（玩家、生物等）
        if (!(target instanceof LivingEntity)) return;

        // 获取工具的攻击衰减率
        float attackDecay = ICPMToolProperties.getAttackDecayRate(heldItem);
        if (attackDecay <= 0) return;

        // 计算耐久消耗
        int durabilityCost = ICPMDurability.calculateAttackDecay(attackDecay);

        applyDurabilityCost(heldItem, durabilityCost, player);
    }

    @Unique
    private void applyDurabilityCost(ItemStack stack, int cost, ServerPlayer player) {
        int unbreakingLevel = getUnbreakingLevel(player, stack);

        for (int i = 0; i < cost; i++) {
            if (unbreakingLevel > 0) {
                // 耐久三每级有 15% 概率抵消 1 点耐久消耗
                if (player.getRandom().nextInt(100) < unbreakingLevel * 15) {
                    continue;
                }
            }
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
    }

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