package name.icpm.mixin;

import name.icpm.common.CraftingTimeHelper;
import name.icpm.common.EnumQuality;
import name.icpm.component.ICPMDataComponents;
import name.icpm.component.QualityComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 实现ICPM合成时间机制
 *
 * 基于ICPM R196的合成时间计算：
 * - quality_adjusted_difficulty = difficulty * 2^(quality.ordinal - average.ordinal)
 * - 基础时间：
 *   - difficulty < 25: 25 tick
 *   - difficulty > 100: round((difficulty - 100)^0.8) + 100
 *   - 25 <= difficulty <= 100: round(difficulty)
 * - 最终时间 = max(基础时间 / (1 + 速度修正), 25)
 *
 * 工作台交互：
 * - 副手为空且空手右键工作台：切换手持物品品质
 */
@Mixin(CraftingTableBlock.class)
public class CraftingTimeMixin {

    /**
     * 注入工作台使用逻辑
     * 右键切换品质（玩家空手时）
     */
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void icpm$onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (!heldItem.isEmpty() && heldItem.has(ICPMDataComponents.QUALITY)) {
            QualityComponent current = heldItem.get(ICPMDataComponents.QUALITY);
            EnumQuality currentQuality = current.quality();
            EnumQuality next = currentQuality.next();
            if (next == null) {
                // 已达最高品质，循环回绕到最低品质（ICPM 品质循环）
                next = EnumQuality.WRETCHED;
            }
            heldItem.set(ICPMDataComponents.QUALITY, new QualityComponent(next));
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("品质已切换为: " + next.getName()),
                true
            );
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    /**
     * 计算合成时间（基于品质和难度），委托 CraftingTimeHelper 公式
     *
     * @param quality 物品品质
     * @param baseDifficulty 基础难度
     * @param speedModifier 速度修正（0表示无修正）
     * @return 合成所需时间（tick）
     */
    @Unique
    private static int calculateCraftingTime(EnumQuality quality, float baseDifficulty, float speedModifier) {
        return CraftingTimeHelper.calculateCraftingTime(quality, baseDifficulty, speedModifier);
    }
}
