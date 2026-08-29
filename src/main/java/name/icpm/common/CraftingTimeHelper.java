package name.icpm.common;

/**
 * ICPM 合成时间计算公式工具类
 *
 * 规则：
 * - quality_adjusted_difficulty = difficulty * 2^(quality.ordinal - average.ordinal)
 * - 基础时间：difficulty<25 -> 25 tick；>100 -> round((d-100)^0.8)+100；else round(d)
 * - 最终时间 = max(基础时间 / (1 + 速度修正), 25)
 */
public final class CraftingTimeHelper {

    private CraftingTimeHelper() {
    }

    public static int calculateCraftingTime(EnumQuality quality, float baseDifficulty, float speedModifier) {
        int averageLevel = EnumQuality.AVERAGE.ordinal();
        int qualityLevel = quality.ordinal();
        int diff = qualityLevel - averageLevel;
        float qualityAdjustedDifficulty = (float) (baseDifficulty * Math.pow(2, diff));

        int baseTime;
        if (qualityAdjustedDifficulty < 25) {
            baseTime = 25;
        } else if (qualityAdjustedDifficulty > 100) {
            baseTime = (int) Math.round(Math.pow(qualityAdjustedDifficulty - 100, 0.8)) + 100;
        } else {
            baseTime = (int) Math.round(qualityAdjustedDifficulty);
        }

        return (int) Math.max(baseTime / (1.0f + speedModifier), 25.0f);
    }
}
