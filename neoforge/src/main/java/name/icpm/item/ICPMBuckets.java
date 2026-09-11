package name.icpm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;

/**
 * ICPM 多级桶的静态索引。
 * 各类桶在 ICPMItems 注册后通过 {@link #register(String, Item)} 登记，
 * 运行时由 ICPMBucketItem 等查询对应金属/内容的桶。
 */
public final class ICPMBuckets {
    private static final Map<String, Item> BUCKETS = new HashMap<>();

    public static void register(String id, Item item) {
        BUCKETS.put(id, item);
    }

    public static Item emptyOf(String metal) {
        return BUCKETS.get(metal + "_bucket");
    }

    public static Item waterOf(String metal) {
        return BUCKETS.get(metal + "_water_bucket");
    }

    public static Item lavaOf(String metal) {
        return BUCKETS.get(metal + "_lava_bucket");
    }

    public static Item stoneOf(String metal) {
        return BUCKETS.get(metal + "_stone_bucket");
    }

    /**
     * R196 忠实移植：岩浆桶遇水冷却 → 石桶（原铁/金属桶 + 凝固石头）。
     * <p>原版岩浆桶 = ICPM 铁桶族（lavaOf("iron") 登记为原版 lava bucket 时生效），
     * 否则显式把原版 LAVA_BUCKET 映射到铁石桶。
     *
     * @param lavaBucket 玩家/掉落物持有的岩浆桶物品
     * @return 对应石桶物品；非岩浆桶返回 null
     */
    public static Item stoneBucketFromLavaBucket(Item lavaBucket) {
        if (lavaBucket == null) {
            return null;
        }
        // 原版铁岩浆桶 → 铁石桶（铁桶族在 ICPM 中由原版桶代表）
        if (lavaBucket == Items.LAVA_BUCKET) {
            return BUCKETS.get("iron_stone_bucket");
        }
        for (Map.Entry<String, Item> e : BUCKETS.entrySet()) {
            String key = e.getKey();
            if (e.getValue() == lavaBucket && key.endsWith("_lava_bucket")) {
                return BUCKETS.get(key.substring(0, key.length() - "_lava_bucket".length()) + "_stone_bucket");
            }
        }
        return null;
    }
}
