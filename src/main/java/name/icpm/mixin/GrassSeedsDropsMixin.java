package name.icpm.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 打草固定 0.2 掉种子 —— R196 BlockTallGrass.dropBlockAsEntityItem：
 * {@code dropBlockAsEntityItem(info, Item.seeds, 0, 1, 0.2f)} —— 短草/高草/蕨被破坏时
 * 固定 20% 概率掉落 1 个种子；不吃「收获」附魔加成。
 * <p>另含枯灌木（sky：灌木掉落木棍概率 5%）。
 */
@Mixin(BlockBehaviour.class)
public class GrassSeedsDropsMixin {

    @Inject(method = "spawnAfterBreak", at = @At("HEAD"))
    private void icpm$seedsFromGrass(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp, CallbackInfo ci) {
        if (state.is(Blocks.DEAD_BUSH)) {
            if (level.random.nextFloat() < 0.05f) {
                Block.popResource(level, pos, new ItemStack(Items.STICK));
            }
            return;
        }
        if (!state.is(Blocks.SHORT_GRASS)
                && !state.is(Blocks.TALL_GRASS)
                && !state.is(Blocks.FERN)
                && !state.is(Blocks.LARGE_FERN)) {
            return;
        }
        if (level.random.nextFloat() < 0.2f) {
            Block.popResource(level, pos, new ItemStack(Items.WHEAT_SEEDS));
        }
    }
}
