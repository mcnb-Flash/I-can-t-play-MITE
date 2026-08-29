package name.icpm.mixin;

import name.icpm.common.EnumQuality;

/**
 * ICPM 合成时间机制 工具类
 *
 * Minecraft 1.21.11 的 Recipe.assemble 是即时完成，无法直接注入延迟。
 * 真正的合成延迟由 ICPMCraftingDelayMixin (ResultSlot.onTake 10 tick 冷却)
 * 与 ICPMWorkbenchMenu.quickMoveStack 冷却检查共同实现（已注册加载）。
 *
 * 本类提供 R196 的质量-难度-时间计算公式，供 CraftingTimeMixin / 未来
 * 基于方块实体的合成队列机制调用。
 *
 * 规则：
 * - quality_adjusted_difficulty = difficulty * 2^(quality.ordinal - average.ordinal)
 * - 基础时间：difficulty<25 -> 25 tick；>100 -> round((d-100)^0.8)+100；else round(d)
 * - 最终时间 = max(基础时间 / (1 + 速度修正), 25)
 */
public final class ICPMCraftingTimeMixin {

    private ICPMCraftingTimeMixin() {
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
