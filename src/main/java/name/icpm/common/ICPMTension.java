package name.icpm.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * ICPM 张力（Tension）难度体系（MITE 区域难度移植，infx 全局张力 reinterpretation）。
 *
 * 公式（对齐 MITE / infx 开发计划）：
 *   tension = clamp(居住时间 / 3,600,000, 0, 1) × (困难 ? 1.0 : 0.75) + 月相亮度因子 × 0.25
 *   困难难度封顶 1.5
 *
 * 居住时间取自 vanilla chunk.inhabitedTime（区块被玩家停留累积的 tick 数）；
 * 月相亮度因子取自 DimensionType.MOON_BRIGHTNESS_PER_PHASE[相位]（与 ICPMMoonPhase.phase 对齐）。
 *
 * 该值是所有怪物"装备概率 / 附魔概率 / 首领概率 / 土元素挖矿冷却 / 蜘蛛药水概率"的底层基准。
 */
public final class ICPMTension {

    private static final long INHABITED_FULL = 3_600_000L; // 居住满值 tick（50 小时）

    private ICPMTension() {
    }

    /**
     * 计算指定位置的全局张力（0 ~ 1.5）。
     * 非服务端世界（或无法取区块）返回 0（相当于全新区块，不穿甲/不附魔/无首领）。
     */
    public static float getTension(Level level, BlockPos pos) {
        long inhabited = getInhabitedTime(level, pos);
        float locality = clamp((float) inhabited / (float) INHABITED_FULL, 0.0f, 1.0f);

        Difficulty diff = level.getDifficulty();
        float diffMult = (diff == Difficulty.HARD) ? 1.0f : 0.75f;
        float moonFactor = getMoonPhaseFactor(level);

        float t = locality * diffMult + moonFactor * 0.25f;
        if (diff == Difficulty.HARD) {
            t = Math.min(t, 1.5f);
        }
        return Math.max(t, 0.0f);
    }

    /** 便捷：直接用实体的世界与位置 */
    public static float getTension(Level level, net.minecraft.world.entity.Entity entity) {
        return getTension(level, entity.blockPosition());
    }

    private static long getInhabitedTime(Level level, BlockPos pos) {
        if (level instanceof ServerLevel sl) {
            try {
                return sl.getChunk(pos).getInhabitedTime();
            } catch (Exception ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static float getMoonPhaseFactor(Level level) {
        int phase = ICPMMoonPhase.phase(level.getDayTime());
        float[] arr = DimensionType.MOON_BRIGHTNESS_PER_PHASE;
        if (phase >= 0 && phase < arr.length) {
            return arr[phase];
        }
        return 1.0f;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
