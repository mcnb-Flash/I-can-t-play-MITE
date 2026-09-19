package name.icpm.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ICPM 耕地肥力管理器（R196 {@code BlockFarmland} 的 **bit 8 fertilized** 移植）。
 *
 * <p>R196 判决源：
 * <ul>
 *   <li>{@code BlockFarmland.java:46} —— "Bits 1, 2, and 4 used for wetness, <b>bit 8 set if fertilized</b>"
 *       ⇒ 肥力是**单比特**（已施肥 / 未施肥），不是多级；</li>
 *   <li>{@code BlockCrops.java:185-187} —— 已施肥时 {@code var5 *= 1.5f}（生长速率 **×1.5**，乘算）；</li>
 *   <li>{@code BlockCrops.java:124-125} —— 生长成功时 {@code rand.nextInt(256) == 0}（**1/256**）取消施肥；</li>
 *   <li>{@code BlockFarmland.java:239-252} —— 只有**未施肥**的耕地才能被粪便施肥（已施肥则不消耗物品）。</li>
 * </ul>
 *
 * <p>1.21 的耕地没有空闲 bit，故用内存态集合保存"已施肥"的耕地位置（按维度分桶，维度卸载清理）。
 * 语义与 R196 一致：施肥后**长期有效**，仅在作物生长时以 1/256 概率被消耗；
 * 耕地被破坏 / 替换即失效（与 1.6.4 中耕地变泥土后 metadata 丢失一致）。
 */
public final class ICPMFarmlandFertility {

    private ICPMFarmlandFertility() {}

    /** R196 是单比特：只有"已施肥 / 未施肥"两种状态。 */
    public static final int MAX_FERTILITY = 1;

    /** 维度 -> 已施肥的耕地位置 -> 1 */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> FERTILITY = new ConcurrentHashMap<>();

    public static int get(ResourceKey<Level> dim, BlockPos pos) {
        Map<BlockPos, Integer> map = FERTILITY.get(dim);
        if (map == null) {
            return 0;
        }
        return map.getOrDefault(pos, 0);
    }

    /** 是否已施肥（R196 {@code BlockFarmland.isFertilized}）。 */
    public static boolean isFertilized(ResourceKey<Level> dim, BlockPos pos) {
        return get(dim, pos) > 0;
    }

    /**
     * 施肥（R196 {@code BlockFarmland.setFertilized(world,x,y,z,true)}）。
     * 返回施肥后的状态值（0 或 1）。
     */
    public static int add(ResourceKey<Level> dim, BlockPos pos, int amount) {
        if (amount <= 0) {
            return get(dim, pos);
        }
        Map<BlockPos, Integer> map = FERTILITY.computeIfAbsent(dim, k -> new HashMap<>());
        map.put(pos.immutable(), MAX_FERTILITY);
        return MAX_FERTILITY;
    }

    /**
     * 取消施肥（R196 {@code setFertilized(..., false)}）。
     * 触发点：R196 {@code BlockCrops.java:124-125} —— 作物生长成功时 1/256 概率；以及耕地失效。
     *
     * @return 取消前的状态（1 = 本次确实取消了施肥）
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
        map.remove(pos);
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
