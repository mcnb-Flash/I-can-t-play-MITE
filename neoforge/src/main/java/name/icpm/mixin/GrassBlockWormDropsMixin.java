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
 * ICPM 草方块掉活虫（1.6.4 BlockGrass.dropBlockAsEntityItem）
 *
 * 玩家（或任何方式）破坏草方块时，有概率掉落 1 条活虫（替代泥土掉落）：
 * - 正常：1/16 概率（1.6.4 无时运附魔时的概率）
 * - 下雨时：概率大幅提升至 1/4（1.6.4：下雨时 fortune +12，几近必掉）
 *
 * 注意：spawnAfterBreak 声明在 BlockBehaviour（Block 只继承不覆写），
 * 因此 @Mixin 目标必须是 BlockBehaviour，否则运行时找不到注入点。
 */
@Mixin(BlockBehaviour.class)
public abstract class GrassBlockWormDropsMixin {

    @Inject(method = "spawnAfterBreak", at = @At("HEAD"), cancellable = true)
    private void icpm$wormFromGrass(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp, CallbackInfo ci) {
        if (!state.is(Blocks.GRASS_BLOCK)) {
            return;
        }
        int chance = level.isRainingAt(pos.above()) ? 4 : 16;
        if (level.random.nextInt(chance) == 0) {
            // 掉落活虫，替代泥土（取消原掉落：泥土）
            Block.popResource(level, pos, new ItemStack(ICPMItems.WORM));
            ci.cancel();
        }
    }
}
