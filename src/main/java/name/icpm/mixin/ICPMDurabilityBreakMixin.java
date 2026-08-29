package name.icpm.mixin;

import name.icpm.common.ICPMDurability;
import name.icpm.common.ICPMBlockHardness;
import name.icpm.common.ICPMMixinShared;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM挖掘耐久消耗
 *
 * 在destroyBlock前计算ICPM耐久消耗并存储
 * 由ICPMToolDurabilityMixin在hurtAndBreak中应用
 */
@Mixin(ServerPlayerGameMode.class)
public class ICPMDurabilityBreakMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"))
    private void icpm$beforeDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player == null) return;

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty() || !heldItem.isDamageableItem()) return;

        // 只对 ICPM 注册工具应用 R196 挖掘衰减公式。
        // 非 ICPM 工具（如原版木/石/皮革工具、恢复合成的原版木铲等）保持原版耐久消耗（每方块 +1），
        // 否则会因未命中材质映射而退避到 blockDecay=1.0，导致消耗飙高、几乎一挖即坏。
        if (!ICPMToolProperties.isICPMTool(heldItem)) return;

        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        // 使用 ICPM 方块硬度（木制方块按 R196 BlockHardness 缩放，如原木=1.0 而非原版 2.0；
        // 草本/植被返回 0.0，公式自带 max(…,1) 下限保证至少消耗 1 点耐久，无需强行提到 1.0）
        float hardness = ICPMBlockHardness.get(level, pos, state);

        // 使用手持工具的 R196 真实衰减率（ICPM 工具返回 r196 衰减率，非 ICPM 工具回落 1.0）
        float blockDecay = ICPMToolProperties.getBlockDecayRate(heldItem);
        if (blockDecay <= 0f) blockDecay = 1.0f;

        // ICPM公式: max(max(int(hardness * 100 * decayRate), int(100 * decayRate / 20)), 1)
        int cost = ICPMDurability.calculateBlockDecay(hardness, blockDecay);

        // 存储待处理的耐久消耗，供 ICPMToolDurabilityMixin 在 hurtAndBreak 中应用
        ICPMMixinShared.setPendingBreakCost(cost);
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"))
    private void icpm$afterDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player == null) return;

        Boolean success = cir.getReturnValue();
        if (success == null || !success) {
            ICPMMixinShared.clearPendingBreakCost();
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty() || !heldItem.isDamageableItem()) return;

        // 检查是否有待处理的耐久消耗（如果 hurtAndBreak 没被调用）
        Integer pendingCost = ICPMMixinShared.getPendingBreakCost();
        if (pendingCost != null) {
            ICPMMixinShared.clearPendingBreakCost();
            // hurtAndBreak 没被调用，直接应用耐久消耗
            applyIcpmDurabilityCost(heldItem, pendingCost, player);
        }
    }
    
    /**
     * 直接应用ICPM耐久消耗（含耐久三附魔减免）
     */
    @Unique
    private void applyIcpmDurabilityCost(ItemStack stack, int cost, ServerPlayer player) {
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
        var allEnchants = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var entry : allEnchants.entrySet()) {
            var key = entry.getKey();
            if (key.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
