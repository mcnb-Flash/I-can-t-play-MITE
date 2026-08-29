package name.icpm.mixin;

import name.icpm.blockentity.ICPMFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版熔炉 = ICPM 原石熔炉 Mixin
 *
 * 将原版熔炉的方块实体替换为 ICPMFurnaceBlockEntity（maxHeat=2，默认值），
 * 使其获得 ICPM 热量系统：能烧木/炭/煤（热级 1-2），承受不了岩浆（热级 3 > 2），
 * 秘银矿（热级 3）无法冶炼。
 *
 * 本 mod 不单独注册 icpm:cobblestone_furnace，原版熔炉即原石熔炉（与 ICPM 1.6.4 一致）。
 */
@Mixin(FurnaceBlock.class)
public class FurnaceBlockMixin {

    @Inject(method = "newBlockEntity", at = @At("RETURN"), cancellable = true)
    private void icpm$vanillaFurnaceAsCobblestone(BlockPos pos, BlockState state, CallbackInfoReturnable<BlockEntity> cir) {
        cir.setReturnValue(new ICPMFurnaceBlockEntity(pos, state));
    }

    @Inject(method = "getTicker", at = @At("RETURN"), cancellable = true)
    private <T extends BlockEntity> void icpm$vanillaFurnaceTicker(
            Level level, BlockState state, BlockEntityType<T> type, CallbackInfoReturnable<BlockEntityTicker<T>> cir
    ) {
        if (level instanceof ServerLevel && type == ICPMFurnaceBlockEntity.TYPE) {
            cir.setReturnValue((l, p, s, be) -> ((ICPMFurnaceBlockEntity) be).miteTick());
        }
    }

    @Inject(method = "openContainer", at = @At("HEAD"), cancellable = true)
    private void icpm$vanillaFurnaceOpen(Level level, BlockPos pos, Player player, CallbackInfo ci) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ICPMFurnaceBlockEntity) {
            player.openMenu((MenuProvider) blockEntity);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
            ci.cancel();
        }
    }
}
