package name.icpm.mixin;

import name.icpm.common.ICPMFarmlandFertility;
import name.icpm.common.ICPMMoonPhase;
import name.icpm.common.ICPMPlantDisease;
import name.icpm.common.ICPMSeason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 作物机制（1.6.4 移植）
 *
 * 1. 病害：随机刻小概率患病（1/512），患病作物停止生长（cancel randomTick），
 *    必须用骨粉治疗（见 BoneMealMixin）。
 * 2. 肥力：下方耕地有肥力时生长速度大幅提升（getGrowthSpeed 加成）：
 *    肥力 1 级 +1.0、2 级 +2.5、3 级 +4.5（1.6.4 BlockFarmland.fertility 语义）。
 */
@Mixin(CropBlock.class)
public abstract class CropBlockMixin {

    /** 作物随机刻：病害检查 + 感染 */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void icpm$diseaseOnRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        ResourceKey<Level> dim = level.dimension();
        if (ICPMPlantDisease.isDiseased(dim, pos)) {
            // 患病作物不生长
            ci.cancel();
            // B1 枯萎死亡：每随机刻 1/64 直接死亡并掉种子（R196 枯萎作物死亡逻辑）
            if (random.nextInt(64) == 0) {
                ItemStack seed = new ItemStack(((CropBlockAccessor) (Object) this).icpm$getBaseSeedId());
                if (!seed.isEmpty()) {
                    Block.popResource(level, pos, seed);
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                ICPMPlantDisease.cure(dim, pos);
                return;
            }
            // B2 邻近传染：水平 4 邻居中健康且未成熟者，每随机刻 1/32 概率被感染
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                BlockState nState = level.getBlockState(neighbor);
                if (nState.getBlock() instanceof CropBlock
                        && !ICPMPlantDisease.isDiseased(dim, neighbor)
                        && nState.getValue(CropBlock.AGE) < CropBlock.MAX_AGE
                        && random.nextInt(32) == 0) {
                    ICPMPlantDisease.infect(dim, neighbor);
                }
            }
            return;
        }
        // 血月之夜：作物大量患病（R196 BlockCrops: 25% 概率患病）
        if (state.getValue(CropBlock.AGE) < CropBlock.MAX_AGE && ICPMMoonPhase.isBloodMoonNight(level) && random.nextFloat() < 0.25f) {
            ICPMPlantDisease.infect(dim, pos);
            ci.cancel();
            return;
        }
        // 健康作物小概率患病（1.6.4 病害传播概率），未成熟作物才可能患病
        if (state.getValue(CropBlock.AGE) < CropBlock.MAX_AGE && random.nextInt(512) == 0) {
            ICPMPlantDisease.infect(dim, pos);
            ci.cancel();
        }
    }

    /**
     * 作物吸收肥力：每次生长阶段推进（age 增大）时，下方耕地肥力 -1（不低于 0）。
     * 还原 MITE "作物持续吸收地力、需不断施肥"的核心循环。
     * 通过比较本次随机刻前后的 age 判断作物是否真的生长了一步。
     */
    @Inject(method = "randomTick", at = @At("TAIL"))
    private void icpm$consumeFertilityOnGrow(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (level.isClientSide()) {
            return;
        }
        int before = state.getValue(CropBlock.AGE);
        int after = level.getBlockState(pos).getValue(CropBlock.AGE);
        if (after > before && ICPMFarmlandFertility.get(level.dimension(), pos.below()) > 0) {
            ICPMFarmlandFertility.consume(level.dimension(), pos.below());
        }
    }

    /** 生长速度：耕地肥力加成 */
    @Inject(method = "getGrowthSpeed", at = @At("RETURN"), cancellable = true)
    private static void icpm$fertilityGrowthBoost(Block block, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!(level instanceof Level l)) {
            return;
        }
        BlockPos below = pos.below();
        if (l.getBlockState(below).is(Blocks.FARMLAND)) {
            int fertility = ICPMFarmlandFertility.get(l.dimension(), below);
            if (fertility > 0) {
                float boost = switch (fertility) {
                    case 1 -> 1.0f;
                    case 2 -> 2.5f;
                    default -> 4.5f; // 3 级
                };
                cir.setReturnValue(cir.getReturnValue() + boost);
            }
        }
        // 丰收之月：作物生长速度 +2.0（R196 丰收月作物加速）
        if (l instanceof net.minecraft.server.level.ServerLevel sl && ICPMMoonPhase.isHarvestMoonDay(sl)) {
            cir.setReturnValue(cir.getReturnValue() + 2.0f);
        }
        // 季节加成（春 +1.0 / 秋 +2.0 丰收 / 冬 -2.0 严寒）
        float seasonBonus = ICPMSeason.growthBonus(ICPMSeason.getSeason(l.getDayTime()));
        if (seasonBonus != 0.0f) {
            cir.setReturnValue(cir.getReturnValue() + seasonBonus);
        }
    }
}
