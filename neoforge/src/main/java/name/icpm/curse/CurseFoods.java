package name.icpm.curse;

import name.icpm.common.PlayerNutritionManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.LingeringPotionItem;

import java.util.Set;

/**
 * 诅咒食物分类 —— R196 Item.isAnimalProduct / isPlantProduct / isDrinkable 语义映射。
 *
 * <p>分类依据（自动覆盖原版与 ICPM 全部食物）：
 * <ul>
 *   <li>动物源：FOOD_NUTRITION 蛋白质 &gt; 0（肉/鱼/蛋/奶…，与 R196 动物产品同向）；</li>
 *   <li>植物源：植物营养素 &gt; 0（蔬果/谷物/蜜…）；</li>
 *   <li>饮品：显式饮品清单（奶桶/蜜瓶/各类汤）+ 可饮用药水（非喷溅/滞留）。</li>
 * </ul>
 */
public final class CurseFoods {

    private static final Set<String> DRINKS = Set.of(
            "minecraft:milk_bucket",
            "minecraft:honey_bottle",
            "minecraft:mushroom_stew",
            "minecraft:rabbit_stew",
            "minecraft:beetroot_soup",
            "minecraft:suspicious_stew");

    private CurseFoods() {
    }

    private static String key(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static boolean isAnimalFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        int[] n = PlayerNutritionManager.getFoodNutrition(key(stack));
        return n != null && n[0] > 0;
    }

    public static boolean isPlantFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        int[] n = PlayerNutritionManager.getFoodNutrition(key(stack));
        return n != null && n[2] > 0;
    }

    /** 饮品（可喝）：显式汤/奶清单或可饮用药水（R196 isDrinkable；不含喷溅/滞留投掷药）。 */
    public static boolean isDrink(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var item = stack.getItem();
        if (DRINKS.contains(key(stack))) {
            return true;
        }
        return item instanceof PotionItem
                && !(item instanceof SplashPotionItem)
                && !(item instanceof LingeringPotionItem);
    }
}
