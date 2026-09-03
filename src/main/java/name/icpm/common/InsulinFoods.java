package name.icpm.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * 食物糖含量表 —— R196 Item.setFoodValue(..., sugar_content, ...) 的 ICPM 映射。
 *
 * <p>糖源食物按甜度赋糖值（量级参考 R196：糖值与其 satiation 同尺度、数千级；
 * 胰岛素反应 = sugar × MITEConstant.sugar_content_to_insulin_response(4.8)）。
 * 未列入 = 无糖（糖值 0）。
 */
public final class InsulinFoods {

    /** R196 MITEConstant.sugar_content_to_insulin_response */
    public static final float SUGAR_TO_INSULIN_RESPONSE = 4.8f;

    private static final Map<String, Integer> SUGAR = Map.ofEntries(
            Map.entry("minecraft:sugar", 2000),
            Map.entry("minecraft:honey_bottle", 1500),
            Map.entry("minecraft:cookie", 600),
            Map.entry("minecraft:cake", 2000),
            Map.entry("minecraft:pumpkin_pie", 800),
            Map.entry("minecraft:apple", 300),
            Map.entry("minecraft:golden_apple", 400),
            Map.entry("minecraft:enchanted_golden_apple", 400),
            Map.entry("minecraft:melon_slice", 250),
            Map.entry("minecraft:sweet_berries", 200),
            Map.entry("minecraft:glow_berries", 200),
            Map.entry("minecraft:milk_bucket", 600),
            // ICPM 甜食
            Map.entry("icpm:chocolate", 1500),
            Map.entry("icpm:ice_cream", 1500),
            Map.entry("icpm:sorbet", 1400),
            Map.entry("icpm:cheese", 200),
            Map.entry("icpm:orange", 350),
            Map.entry("icpm:banana", 450),
            Map.entry("icpm:blueberry", 150),
            Map.entry("icpm:cereal", 500),
            Map.entry("icpm:milk_bowl", 500),
            Map.entry("icpm:porridge", 200));

    private InsulinFoods() {
    }

    /** 食物糖含量（无糖记录返回 0）。 */
    public static int sugarOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Integer v = SUGAR.get(key);
        return v == null ? 0 : v;
    }

    /** R196 getInsulinResponse = (int)(sugar_content × 4.8)。 */
    public static int insulinResponse(ItemStack stack) {
        return (int) (sugarOf(stack) * SUGAR_TO_INSULIN_RESPONSE);
    }
}
