package name.icpm.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 摔落缓冲 —— R196（sky 实测）：
 * 摔落在玻璃/雪块等易碎方块上时最大只受 5 点伤害，且该方块会被砸碎。
 * 1.21.11：LivingEntity.calculateFallDamage(double, float) -> int（距变 float multiplier）。
 */
@Mixin(LivingEntity.class)
public class FragileLandingMixin {

    private static final int FALL_CAP_ON_FRAGILE = 5;

    private static boolean isFragile(Block block) {
        return block == Blocks.GLASS
                || block == Blocks.GLASS_PANE
                || block == Blocks.WHITE_STAINED_GLASS
                || block == Blocks.SNOW_BLOCK;
    }

    @Inject(method = "calculateFallDamage", at = @At("RETURN"), cancellable = true)
    private void icpm$capFallOnFragile(double distance, float damageMultiplier, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        Level level = self.level();
        BlockPos foot = self.blockPosition();
        BlockState below = level.getBlockState(foot.below());
        if (!isFragile(below.getBlock())) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            // 方块被砸碎（不产生掉落）
            serverLevel.destroyBlock(foot.below(), false);
        }
        int damage = cir.getReturnValueI();
        if (damage > FALL_CAP_ON_FRAGILE) {
            cir.setReturnValue(FALL_CAP_ON_FRAGILE);
        }
    }
}
