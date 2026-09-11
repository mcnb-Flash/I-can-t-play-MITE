package name.icpm.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MyceliumBlock;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 菌丝蘑菇生长 —— R196 BlockMycelium.updateTick 忠实移植（蘑菇部分）。
 *
 * <p>R196 原文（src_deobf/.../BlockMycelium.java）：
 * <pre>
 *   ... 上方光照 > 容差 或 室外：
 *       白天且(无降水/结冰) → 变泥土（菌丝退化）
 *   否则（低光遮蔽处）：
 *       if (random.nextInt(4) == 0 && random.nextInt(256) == 0 && 上方为空气) {
 *           统计 9×5×9(dx -4..4, dy -2..2, dz -4..4) 内蘑菇数；&gt;2 朵则放弃
 *           world.setBlock(x, y+1, z, mushroomBrown);   // 长棕色蘑菇
 *       }
 * </pre>
 * 综合触发率 ≈ 1/1024 / randomTick。
 *
 * <p>1.21.11 类结构差异：{@link MyceliumBlock} 不覆写 randomTick，蔓延/退化逻辑在父类
 * {@link SpreadingSnowyDirtBlock#randomTick}（GrassBlock 共用），且原版菌丝在暗处会被
 * 父类转回泥土（与 R196 洞穴菌丝生态相反）。故：HEAD 注入父类 randomTick，仅当方块为
 * 菌丝且处于「低光 + 非露天」时取消原版退化，并按 R196 概率长棕色蘑菇。
 */
@Mixin(SpreadingSnowyDirtBlock.class)
public abstract class MyceliumMushroomGrowthMixin {

    /** R196 BlockMycelium.getLightValueTolerance() 的低光容差近似（方块光照 ≤ 7）。 */
    private static final int LIGHT_TOLERANCE = 7;

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void icpm$myceliumMushroom(BlockState state, ServerLevel level, BlockPos pos,
                                       RandomSource random, CallbackInfo ci) {
        // 仅处理菌丝；草方块等照常走原版父类逻辑
        if (!((Object) this instanceof MyceliumBlock)) {
            return;
        }
        BlockPos up = pos.above();
        // 低光 + 遮蔽（非露天）才是 R196 蘑菇生长的生态位
        boolean dim = level.getBrightness(LightLayer.BLOCK, up) <= LIGHT_TOLERANCE;
        if (!dim || level.canSeeSky(pos)) {
            return;
        }
        // 暗处遮蔽：取消原版父类“转回泥土”的退化，让菌丝在暗处存活（R196 洞穴菌丝语义）
        ci.cancel();
        // R196：1/4 × 1/256 生长判定（≈1/1024 / randomTick）
        if (random.nextInt(4) != 0 || random.nextInt(256) != 0 || !level.isEmptyBlock(up)) {
            return;
        }
        // 9×5×9 区域内已有 >2 朵蘑菇则不再生长（R196 计数上限）
        int mushroomCount = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockState s = level.getBlockState(pos.offset(dx, dy, dz));
                    if ((s.is(Blocks.BROWN_MUSHROOM) || s.is(Blocks.RED_MUSHROOM))
                            && ++mushroomCount > 2) {
                        return;
                    }
                }
            }
        }
        level.setBlock(up, Blocks.BROWN_MUSHROOM.defaultBlockState(), 3);
    }
}
