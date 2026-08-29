package name.icpm.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ICPM 耕地肥力管理器（1.6.4 BlockFarmland.fertility 移植）
 *
 * 粪便（ItemManure）右键耕地增加肥力（0-3 级），肥力越高作物生长越快：
 * - CropBlockMixin 注入 getGrowthSpeed：下方耕地肥力 1 级 +1.0，2 级 +2.5，3 级 +4.5
 * - 耕地被破坏/替换时肥力丢失（内存态，与 1.6.4 语义一致：耕地变成泥土肥力消失）
 *
 * 肥力状态仅存于服务端内存，按维度分桶，维度卸载时清理。
 */
public final class ICPMFarmlandFertility {

    private ICPMFarmlandFertility() {}

    public static final int MAX_FERTILITY = 3;

    /** 维度 -> 耕地位置 -> 肥力等级(0-3) */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> FERTILITY = new ConcurrentHashMap<>();

    public static int get(ResourceKey<Level> dim, BlockPos pos) {
        Map<BlockPos, Integer> map = FERTILITY.get(dim);
        if (map == null) {
            return 0;
        }
        return map.getOrDefault(pos, 0);
    }

    /** 增加肥力，返回新等级（不超过 MAX_FERTILITY） */
    public static int add(ResourceKey<Level> dim, BlockPos pos, int amount) {
        Map<BlockPos, Integer> map = FERTILITY.computeIfAbsent(dim, k -> new HashMap<>());
        int current = map.getOrDefault(pos, 0);
        int next = Math.min(MAX_FERTILITY, current + amount);
        map.put(pos.immutable(), next);
        return next;
    }

    /**
     * 作物吸收肥力：每次生长阶段推进或收获时调用，扣除 1 级肥力（不低于 0）。
     * MITE 中肥力会被作物持续吸收而递减，玩家需不断施肥维持产量（还原核心循环）。
     * @return 扣除前的肥力等级（供调用方决定本次生长是否仍享受加速）
     */
    public static int consume(ResourceKey<Level> dim, BlockPos pos) {
        Map<BlockPos, Integer> map = FERTILITY.get(dim);
        if (map == null) {
            return 0;
        }
        int current = map.getOrDefault(pos, 0);
        if (current <= 0) {
            return 0;
        }
        map.put(pos.immutable(), current - 1);
        return current;
    }

    /** 耕地被破坏/替换时清除 */
    public static void onBlockRemoved(ResourceKey<Level> dim, BlockPos pos) {
        Map<BlockPos, Integer> map = FERTILITY.get(dim);
        if (map != null) {
            map.remove(pos);
        }
    }

    /** 维度卸载时清理 */
    public static void clearDimension(ResourceKey<Level> dim) {
        FERTILITY.remove(dim);
    }
}
