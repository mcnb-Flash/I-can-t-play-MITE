package name.icpm.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * ICPM 金属币辅助类（1.6.4 ItemCoin 移植）
 *
 * 规则：
 * - 合成：1 个金属粒在**对应等级工作台**上合成 1 个金属币，**消耗经验**
 *   （copper=5xp, silver=10xp, gold=15xp, ancient_metal=20xp, mithril=25xp, adamantium=30xp）
 * - 分解：金属币可在**背包 2x2 合成栏**直接分解为金属粒，**返还经验**，
 *   支持多重分解（填满 2x2 = 4 币 → 4 粒；1 币 → 1 粒）
 */
public final class ICPMCoinHelper {

    private ICPMCoinHelper() {}

    /** 币 -> 对应金属粒的注册名 */
    private static String nuggetIdForCoin(String coinId) {
        return switch (coinId) {
            case "copper_coin" -> "minecraft:copper_nugget";
            case "silver_coin" -> "icpm:silver_nugget";
            case "gold_coin" -> "minecraft:gold_nugget";
            case "ancient_metal_coin" -> "icpm:ancient_metal_nugget";
            case "mithril_coin" -> "icpm:mithril_nugget";
            case "adamantium_coin" -> "icpm:adamantium_nugget";
            default -> null;
        };
    }

    /** 粒 -> 对应币的注册名 */
    private static String coinIdForNugget(String nuggetId) {
        return switch (nuggetId) {
            case "minecraft:copper_nugget" -> "copper_coin";
            case "icpm:silver_nugget" -> "silver_coin";
            case "minecraft:gold_nugget" -> "gold_coin";
            case "icpm:ancient_metal_nugget" -> "ancient_metal_coin";
            case "icpm:mithril_nugget" -> "mithril_coin";
            case "icpm:adamantium_nugget" -> "adamantium_coin";
            default -> null;
        };
    }

    /** 该币合成所需/分解返还的经验值（R196 ItemCoin.getExperienceValue） */
    public static int xpForCoin(String coinId) {
        return switch (coinId) {
            case "copper_coin" -> 5;
            case "silver_coin" -> 25;
            case "gold_coin" -> 100;
            case "ancient_metal_coin" -> 500;
            case "mithril_coin" -> 2500;
            case "adamantium_coin" -> 10000;
            default -> 0;
        };
    }

    /** 判断一个物品是否是 ICPM 金属币 */
    public static boolean isCoin(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("icpm")
                && xpForCoin(id.getPath()) > 0;
    }

    /** 获取币对应的金属粒物品（无则返回 null） */
    public static Item nuggetForCoin(ItemStack coin) {
        if (coin == null || coin.isEmpty()) {
            return null;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(coin.getItem());
        if (id == null) {
            return null;
        }
        String nuggetId = nuggetIdForCoin(id.getPath());
        if (nuggetId == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getValue(Identifier.tryParse(nuggetId));
    }

    /** 获取粒对应的币物品（无则返回 null） */
    public static Item coinForNugget(ItemStack nugget) {
        if (nugget == null || nugget.isEmpty()) {
            return null;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(nugget.getItem());
        if (id == null) {
            return null;
        }
        String path = id.getNamespace().equals("minecraft")
                ? "minecraft:" + id.getPath()
                : id.getNamespace() + ":" + id.getPath();
        String coinId = coinIdForNugget(path);
        if (coinId == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("icpm", coinId));
    }

    /** 工作台合成币所需的等级（与 getRequiredWorkbenchTier 一致；铜/银/金硬币同级 tier 1） */
    public static int workbenchTierForCoin(String coinId) {
        return switch (coinId) {
            case "copper_coin", "silver_coin", "gold_coin" -> 1;
            case "iron_coin" -> 3;
            case "ancient_metal_coin" -> 4;
            case "mithril_coin" -> 5;
            case "adamantium_coin" -> 6;
            default -> 0;
        };
    }

    /** 从 ItemStack 取对应币的经验值（0 = 非币） */
    public static int xpForCoinByItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !id.getNamespace().equals("icpm")) {
            return 0;
        }
        return xpForCoin(id.getPath());
    }
}
