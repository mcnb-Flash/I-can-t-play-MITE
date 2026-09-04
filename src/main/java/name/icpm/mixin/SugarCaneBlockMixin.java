package name.icpm.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 甘蔗生长 —— R196 BlockReed.updateTick 忠实移植。
 *
 * <p>R196 原文（src_deobf BlockReed.updateTick）：
 * <pre>
 *   1) random.nextFloat() &gt; biome.temperature - 0.2  → 不生长   // 成功率 = clamp(t-0.2,0,1)
 *   2) random.nextFloat() &lt; 0.8                       → 不生长   // 再乘 0.2
 *   3) getBlockLightValue &lt; 15                        → 不生长   // 需光 15（实际=白天露天）
 *   4) 上方 canOccurAt（上方是 air 且可见天空）且高 &lt; 3 → metadata+1；
 *      metadata==16 → 上方再长一节并重置本节约 0
 * </pre>
 * 生长与"底部是否水/沙"无关（仅种植合法需要水邻接/长在甘蔗上）。
 */
@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void icpm$randomTickR196(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // 1) 温度概率：P = 0.2 × clamp(biomeTemperature - 0.2, 0, 1)
        float t = level.getBiome(pos).value().getBaseTemperature();
        float growthChance = 0.2f * Math.max(0.0f, Math.min(1.0f, t - 0.2f));
        if (growthChance <= 0.0f || random.nextFloat() >= growthChance) {
            ci.cancel();
            return;
        }
        // 3) 光照 15（白天露天约等于满亮度）
        if (level.getMaxLocalRawBrightness(pos.above()) < 15) {
            ci.cancel();
            return;
        }
        // 4) 上方为空气、整株高度 < 3（R196 isLegalAt：reed 堆高上限 3）
        if (!level.isEmptyBlock(pos.above())) {
            ci.cancel();
            return;
        }
        int height = 1;
        while (level.getBlockState(pos.below(height)).is(Blocks.SUGAR_CANE)) {
            ++height;
        }
        if (height >= 3) {
            ci.cancel();
            return;
        }
        // metadata(AGE) +1；满 16 → 上方新长一节，本节约 0
        int age = state.getValue(SugarCaneBlock.AGE);
        if (age == 15) {
            level.setBlockAndUpdate(pos.above(), Blocks.SUGAR_CANE.defaultBlockState());
            level.setBlock(pos, state.setValue(SugarCaneBlock.AGE, 0), 2);
        } else {
            level.setBlock(pos, state.setValue(SugarCaneBlock.AGE, age + 1), 2);
        }
        ci.cancel();
    }
}
