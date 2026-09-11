package name.icpm.mixin;

import name.icpm.common.ICPMPortalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 地下世界/地狱睡眠（R196）：
 * - 地下世界点床 → 提示"你感觉到不够安全"，无法上床；
 * - 地狱点床 → 同样提示"你感觉到不够安全"，且不会引爆（R196 地狱床也不炸）。
 * 拦截 BlockBehaviour.useWithoutItem（床逻辑/爆炸所在地），地下世界与地狱都拦截；
 * 其余维度（主世界/末地）放行，睡眠正常。
 */
@Mixin(BedBlock.class)
public abstract class ICPMBedBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void icpm$unsafeBed(BlockState blockState, Level level, BlockPos blockPos, Player player,
                                BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) {
            return;
        }
        ResourceKey<Level> dim = level.dimension();
        if (dim == ICPMPortalHandler.UNDERWORLD_KEY || dim == Level.NETHER) {
            player.displayClientMessage(Component.literal("你感觉到不够安全"), true);
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        }
    }
}
