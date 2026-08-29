package name.icpm.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/**
 * ICPM 泥土重力机制 Mixin
 *
 * 移植自 R196 BlockUnderminable/BlockFalling：
 * 泥土、灰化土、缠根泥土、泥巴等"软土"方块
 * 在下方悬空时会像沙子/沙砾一样下落（重力）。
 * 注意：草方块、菌丝不参与此物理效果（ICPM 设定）。
 *
 * 通过镜像原版 FallingBlock 的行为实现：
 * - onPlace：放置后调度 2 tick 后的下落检查
 * - updateShape：邻居方块变化时调度下落检查
 * - tick：若下方方块可穿过（空气/可替换），生成 FallingBlockEntity 使本块下落
 */
@Mixin(BlockBehaviour.class)
public class ICPMDirtGravityMixin {

    // 具有重力的软土方块集合
    private static final Set<Block> SOFT_DIRT_BLOCKS = new HashSet<>();

    @Unique
    private static void icpm$registerSoftDirtBlock(Block block) {
        SOFT_DIRT_BLOCKS.add(block);
    }

    @Unique
    private static void icpm$initDefaultSoftDirtBlocks() {
        icpm$registerSoftDirtBlock(Blocks.DIRT);
        icpm$registerSoftDirtBlock(Blocks.COARSE_DIRT);
        icpm$registerSoftDirtBlock(Blocks.ROOTED_DIRT);
        icpm$registerSoftDirtBlock(Blocks.PODZOL);
        icpm$registerSoftDirtBlock(Blocks.MUD);
    }

    @Unique
    private static boolean icpm$isSoftDirt(BlockState state) {
        if (SOFT_DIRT_BLOCKS.isEmpty()) {
            icpm$initDefaultSoftDirtBlocks();
        }
        return SOFT_DIRT_BLOCKS.contains(state.getBlock());
    }

    /**
     * onPlace：放置后调度下落检查
     */
    @Inject(method = "onPlace", at = @At("TAIL"))
    private void icpm$dirtGravityOnPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved, CallbackInfo ci) {
        if (level.isClientSide()) return;
        if (!icpm$isSoftDirt(state)) return;
        Block block = state.getBlock();
        level.scheduleTick(pos, block, 2);
    }

    /**
     * updateShape：邻居方块变化时调度下落检查
     */
    @Inject(method = "updateShape", at = @At("TAIL"))
    private void icpm$dirtGravityUpdateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        if (!icpm$isSoftDirt(state)) return;
        Block block = state.getBlock();
        tickAccess.scheduleTick(pos, block, 2);
    }

    /**
     * tick：下方悬空时生成下落实体
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void icpm$dirtGravityTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!icpm$isSoftDirt(state)) return;
        if (canFallBelow(level.getBlockState(pos.below())) && pos.getY() >= level.getMinY()) {
            FallingBlockEntity.fall(level, pos, state);
            ci.cancel();
        }
    }

    @Unique
    private static boolean canFallBelow(BlockState below) {
        if (below.isAir()) return true;
        if (below.canBeReplaced()) return true;
        if (below.is(BlockTags.FIRE)) return true;
        return false;
    }
}
