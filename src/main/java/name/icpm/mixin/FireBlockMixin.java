package name.icpm.mixin;

import name.icpm.common.CombustionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 接管受控火的熄灭：短燃火与满 8 次点燃的火在熄灭时由 {@link CombustionHandler} 处理，
 * 不影响原版火（下界、普通蔓延火）的正常逻辑。
 */
@Mixin(net.minecraft.world.level.block.FireBlock.class)
public class FireBlockMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void icpm$tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (CombustionHandler.handleFireTick(level, pos)) {
            ci.cancel();
        }
    }
}
