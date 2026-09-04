package name.icpm.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * ICPM 群系气候（温度/湿度）—— R196 群系表（《MITE种植业大全》）。
 *
 * <p>值直接取自 R196 资料表：雪原 0/0.5、针叶林 0.05/0.8、高山 0.4/0.3、
 * 河海 0.5/0.5、森林 0.7/0.8、平原 0.8/0.4、沼泽 0.8/0.9、沙滩 1.0/0.4、
 * 丛林 1.2/0.9、地下世界 1.0/0、沙漠 1.6/0、地狱 2.0/0。
 * 未收录群系回退到原版 biome 基础温度 + 湿度 0.5。
 */
public final class ICPMClimate {

    private ICPMClimate() {}

    /** key = biome registry id（path），value = {温度, 湿度} */
    private static final Map<String, float[]> TABLE = new HashMap<>();

    private static void reg(String id, float t, float h) {
        TABLE.put(id, new float[]{t, h});
    }

    static {
        // 冷 / 雪
        reg("snowy_plains", 0f, 0.5f);
        reg("snowy_taiga", 0.05f, 0.8f);
        reg("ice_spikes", 0f, 0.5f);
        reg("frozen_ocean", 0f, 0.5f);
        reg("frozen_river", 0f, 0.5f);
        // 针叶林
        reg("taiga", 0.05f, 0.8f);
        reg("old_growth_pine_taiga", 0.05f, 0.8f);
        reg("old_growth_spruce_taiga", 0.05f, 0.8f);
        reg("windswept_forest", 0.05f, 0.8f);
        reg("grove", 0.05f, 0.8f);
        // 高山
        reg("windswept_hills", 0.4f, 0.3f);
        reg("windswept_gravelly_hills", 0.4f, 0.3f);
        reg("meadow", 0.4f, 0.3f);
        reg("stony_peaks", 0.4f, 0.3f);
        reg("jagged_peaks", 0.4f, 0.3f);
        reg("frozen_peaks", 0f, 0.5f);
        // 河 / 海
        reg("river", 0.5f, 0.5f);
        reg("ocean", 0.5f, 0.5f);
        reg("deep_ocean", 0.5f, 0.5f);
        reg("warm_ocean", 0.5f, 0.5f);
        reg("lukewarm_ocean", 0.5f, 0.5f);
        reg("cold_ocean", 0.5f, 0.5f);
        reg("deep_warm_ocean", 0.5f, 0.5f);
        reg("deep_lukewarm_ocean", 0.5f, 0.5f);
        reg("deep_cold_ocean", 0.5f, 0.5f);
        reg("deep_frozen_ocean", 0.5f, 0.5f);
        reg("stony_shore", 0.5f, 0.4f);
        // 森林
        reg("forest", 0.7f, 0.8f);
        reg("flower_forest", 0.7f, 0.8f);
        reg("birch_forest", 0.7f, 0.8f);
        reg("old_growth_birch_forest", 0.7f, 0.8f);
        reg("dark_forest", 0.7f, 0.8f);
        // 平原
        reg("plains", 0.8f, 0.4f);
        reg("sunflower_plains", 0.8f, 0.4f);
        // 沼泽
        reg("swamp", 0.8f, 0.9f);
        reg("mangrove_swamp", 0.8f, 0.9f);
        // 沙滩
        reg("beach", 1.0f, 0.4f);
        // 丛林（R196 湿度 0.9 > 0.85 → 西瓜加速）
        reg("jungle", 1.2f, 0.9f);
        reg("sparse_jungle", 1.2f, 0.9f);
        reg("bamboo_jungle", 1.2f, 0.9f);
        reg("jungle_river", 1.2f, 0.9f);
        // 沙漠
        reg("desert", 1.6f, 0f);
        // 地狱
        reg("nether_wastes", 2.0f, 0f);
        reg("soul_sand_valley", 2.0f, 0f);
        reg("crimson_forest", 2.0f, 0f);
        reg("warped_forest", 2.0f, 0f);
        reg("basalt_deltas", 2.0f, 0f);
        // 地下世界（ICPM underworld 群系与 R196 表）
        reg("underworld", 1.0f, 0f);
    }

    private static float[] lookup(Level level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        Identifier key = level.getBiome(pos).unwrapKey().map(k -> k.identifier()).orElse(null);
        if (key != null) {
            float[] v = TABLE.get(key.getPath());
            if (v != null) {
                return v;
            }
        }
        // 回退：原版基础温度，湿度 0.5
        return new float[]{biome.getBaseTemperature(), 0.5f};
    }

    /** 温度（R196 表；未收录 → 原版基础温度） */
    public static float temperature(Level level, BlockPos pos) {
        return lookup(level, pos)[0];
    }

    /** 湿度（R196 表；未收录 → 0.5） */
    public static float humidity(Level level, BlockPos pos) {
        return lookup(level, pos)[1];
    }

    /** 温度距离：t 到 [lo,hi] 区间的最小距离 */
    public static float temperatureDistance(float t, float lo, float hi) {
        if (t < lo) return lo - t;
        if (t > hi) return t - hi;
        return 0f;
    }

    /**
     * 群系影响（作物生长）：
     * 1 − |t − [0.8,1.2] 区间距离|；西瓜宜 [1,1.4]、南瓜宜 [0.6,1]。
     */
    public static float growthFactor(Level level, BlockPos pos, float lo, float hi) {
        float t = temperature(level, pos);
        return Math.max(0f, 1f - temperatureDistance(t, lo, hi));
    }

    /** 生病概率乘数：(1 − |t − [1.0,1.2] 区间距离|)，越偏离适宜区间生病越低 */
    public static float diseaseFactor(Level level, BlockPos pos) {
        float t = temperature(level, pos);
        return Math.max(0f, 1f - temperatureDistance(t, 1.0f, 1.2f));
    }
}
