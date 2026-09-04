package name.icpm.mixin;

import name.icpm.common.ICPMClimate;
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
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 作物机制（1.6.4 移植 + 冷知识校准）
 *
 * 1. 病害：随机刻概率患病（基础 0.0005/土豆 0.001 × 温度疾病因子 × 湿度×1.5 × 亮度折扣，
 *    见《MITE种植业大全》）；患病作物停止生长（cancel randomTick），骨粉治疗（见 BoneMealMixin）。
 * 2. 干湿规则：下方耕地未湿润 → 95% 直接结束随机刻（不生长）；5% 情况下若已成熟则以
 *    干旱掉落（作物死亡），未成熟则保持不生长（需浇水）。
 * 3. 肥力/月相/季节：下方耕地肥力加成 + 丰收之月 + 季节加成。
 * 4. 行植与围困：同作物直线相邻（东/西 或 南/北 单侧）→ 生长 ×(1+0.5×格数)，最多两层；
 *    四向或斜向被同作物围困 → ×0.5。
 * 5. 群系温度影响：生长 × 群系影响（默认宜 [0.8,1.2]；甜菜/瓜类等按需），未湿润耕地不适用。
 */
@Mixin(CropBlock.class)
public abstract class CropBlockMixin {

    private static boolean isWetFarmland(Level level, BlockPos below) {
        BlockState s = level.getBlockState(below);
        return s.is(Blocks.FARMLAND) && s.getValue(FarmBlock.MOISTURE) > 0;
    }

    /** 作物随机刻：干湿 + 病害检查 */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void icpm$diseaseOnRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        ResourceKey<Level> dim = level.dimension();
        boolean mature = state.getValue(CropBlock.AGE) >= CropBlock.MAX_AGE;
        BlockState below = level.getBlockState(pos.below());
        boolean onFarmland = below.is(Blocks.FARMLAND);
        boolean wet = isWetFarmland(level, pos.below());

        // ===== 干湿规则（R196：下方未湿润 → f=0）=====
        if (onFarmland && !wet) {
            // 0.95 概率直接结束本随机刻
            if (random.nextFloat() < 0.95f) {
                ci.cancel();
                return;
            }
            // 5% 深入：已成熟 → 干旱掉落（作物死亡）
            if (mature) {
                ItemStack seed = new ItemStack(((CropBlockAccessor) (Object) this).icpm$getBaseSeedId());
                if (!seed.isEmpty()) {
                    Block.popResource(level, pos, seed);
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                ICPMPlantDisease.cure(dim, pos);
                ci.cancel();
                return;
            }
            // 未成熟：本次不生长，等下次随机刻
            ci.cancel();
            return;
        }

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
        // 健康作物患病（R196：0.0005/t × 温度疾病因子 × (湿度>0.85?1.5) × (1-亮度/16)）
        if (state.getValue(CropBlock.AGE) < CropBlock.MAX_AGE) {
            boolean potato = state.is(Blocks.POTATOES);
            float chance = (potato ? 0.001f : 0.0005f)
                    * ICPMClimate.diseaseFactor(level, pos)
                    * (ICPMClimate.humidity(level, pos) > 0.85f ? 1.5f : 1.0f)
                    * (1.0f - Math.min(15, level.getMaxLocalRawBrightness(pos)) / 16.0f);
            if (random.nextFloat() < chance) {
                ICPMPlantDisease.infect(dim, pos);
                ci.cancel();
            }
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

    /** 生长速度：耕地肥力加成 + 群系温度 + 行植/围困（R196 种植规则，冷知识校准） */
    @Inject(method = "getGrowthSpeed", at = @At("RETURN"), cancellable = true)
    private static void icpm$fertilityGrowthBoost(Block block, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!(level instanceof Level l)) {
            return;
        }
        float speed = cir.getReturnValue();
        BlockPos below = pos.below();
        if (l.getBlockState(below).is(Blocks.FARMLAND)) {
            int fertility = ICPMFarmlandFertility.get(l.dimension(), below);
            if (fertility > 0) {
                float boost = switch (fertility) {
                    case 1 -> 1.0f;
                    case 2 -> 2.5f;
                    default -> 4.5f; // 3 级
                };
                speed += boost;
            }
        }
        // 丰收之月：作物生长速度 +2.0（R196 丰收月作物加速）
        if (l instanceof net.minecraft.server.level.ServerLevel sl && ICPMMoonPhase.isHarvestMoonDay(sl)) {
            speed += 2.0f;
        }
        // 季节加成（春 +1.0 / 秋 +2.0 丰收 / 冬 -2.0 严寒）
        speed += ICPMSeason.growthBonus(ICPMSeason.getSeason(l.getDayTime()));

        // ===== R196 群系温度影响（乘算）=====
        float biomeFactor = ICPMClimate.growthFactor(l, pos, 0.8f, 1.2f);
        // ===== 行植与围困 =====
        float rowFactor = 1.0f;
        boolean e = l.getBlockState(pos.east()).is(block);
        boolean w = l.getBlockState(pos.west()).is(block);
        boolean n = l.getBlockState(pos.north()).is(block);
        boolean s = l.getBlockState(pos.south()).is(block);
        boolean diag = l.getBlockState(pos.east().north()).is(block)
                || l.getBlockState(pos.west().north()).is(block)
                || l.getBlockState(pos.east().south()).is(block)
                || l.getBlockState(pos.west().south()).is(block);
        if (diag || (e && w && n && s)) {
            rowFactor = 0.5f; // 被同作物围困 → 减半
        } else {
            int straight = Math.max(
                    (e ? 1 : 0) + (w ? 1 : 0),
                    (n ? 1 : 0) + (s ? 1 : 0));
            if (straight > 0) {
                rowFactor = 1.0f + 0.5f * Math.min(2, straight); // 直线相邻每格 +50%，最多两层
            }
        }
        cir.setReturnValue(Math.max(0.0f, speed * biomeFactor * rowFactor));
    }
}
