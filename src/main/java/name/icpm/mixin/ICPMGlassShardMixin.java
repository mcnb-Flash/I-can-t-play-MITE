package name.icpm.mixin;

import name.icpm.item.ICPMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * R196 BlockGlass.dropBlockAsEntityItem：玻璃被破坏时掉落 6 个玻璃碎片（shardGlass）。
 * 精准采集（silk touch）交回原版（getDrops 已掉整块玻璃）；其余情形掉 6 碎片。
 */
@Mixin(BlockBehaviour.class)
public abstract class ICPMGlassShardMixin {

    @Inject(method = "spawnAfterBreak", at = @At("HEAD"), cancellable = true)
    private void icpm$glassShards(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp, CallbackInfo ci) {
        // R196 BlockGlass/BlockPane：普通玻璃掉 6 碎片，玻璃板（thinGlass）掉 1 碎片。
        // 原版玻璃/玻璃板（含精准采集）loot 表恒为空，不会双掉整块。
        // 染色玻璃/染色玻璃板 R196 不掉碎片，故不处理。
        if (state.is(Blocks.GLASS)) {
            Block.popResource(level, pos, new ItemStack(ICPMItems.GLASS_SHARD, 6));
            ci.cancel();
        } else if (state.is(Blocks.GLASS_PANE)) {
            Block.popResource(level, pos, new ItemStack(ICPMItems.GLASS_SHARD, 1));
            ci.cancel();
        }
    }
}
