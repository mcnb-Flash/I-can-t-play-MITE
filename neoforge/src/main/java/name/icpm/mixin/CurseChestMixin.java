package name.icpm.mixin;

import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 诅咒：无法开启箱子（fear/repel of chests）—— R196 BlockChest 语义移植。
 * 被诅咒玩家右键箱子（含陷阱箱，TrappedChestBlock 未覆写 useWithoutItem 继承此类）直接失败。
 */
@Mixin(ChestBlock.class)
public abstract class CurseChestMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void icpm$blockChestOpen(BlockState state, Level level, BlockPos pos, Player player,
                                     BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (ICPMCurseManager.isCursed(player, ICPMCurse.CANNOT_OPEN_CHESTS, true)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
