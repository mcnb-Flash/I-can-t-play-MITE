package name.icpm.mixin;

import name.icpm.common.ICPMWorldEvil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 世界恶意「技术不佳」：挖掘进度 ×(1 - 0.25×档)。
 *
 * BlockBehaviour.getDestroyProgress 是实际破坏循环每 tick 使用的进度值，且 1.21.11 下被
 * ICPMToolRulesMixin 完全接管（HEAD cancel 后按 R196 工具规则自算返回）——在此 RETURN 处
 * 缩放即缩放 ICPM 运算后的最终结果，能真实延长破坏所需 tick 数。
 */
@Mixin(BlockBehaviour.class)
public abstract class ICPMTechMiningMixin {

    @Inject(method = "getDestroyProgress(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
            at = @At("RETURN"), cancellable = true)
    private void icpm$slowMining(BlockState state, Player player, BlockGetter level, BlockPos pos,
                                 CallbackInfoReturnable<Float> cir) {
        if (player == null || player.isCreative()) {
            return;
        }
        float factor = ICPMWorldEvil.penaltyFactor();
        float v = cir.getReturnValueF();
        // 零硬度方块原版返回 Infinity，乘惩罚前必须排除（Infinity×factor 仍 Infinite 无碍，但 ×0 会 NaN）
        if (factor < 1.0f && v > 0.0f && !Float.isInfinite(v) && !Float.isNaN(v)) {
            cir.setReturnValue(v * factor);
        }
    }
}
