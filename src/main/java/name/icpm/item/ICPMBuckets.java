package name.icpm.item;

import net.minecraft.world.item.Item;
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
}
