package name.icpm.recipe;

import name.icpm.ICPM;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * ICPM 自定义配方序列化器注册
 */
public final class ICPMRecipes {

    private ICPMRecipes() {}

    /** 金属币分解配方（币 → 粒） */
    public static final RecipeSerializer<CoinUncraftRecipe> COIN_UNCRAFT =
            Registry.register(
                    BuiltInRegistries.RECIPE_SERIALIZER,
                    Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "coin_uncraft"),
                    new CoinUncraftRecipe.Serializer()
            );

    /** 装盾配方（工具 + 盾牌 → 可格挡工具；盾牌返还 -25% 耐久） */
    public static final RecipeSerializer<ShieldAttachRecipe> SHIELD_ATTACH =
            Registry.register(
                    BuiltInRegistries.RECIPE_SERIALIZER,
                    Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "shield_attach"),
                    new ShieldAttachRecipe.Serializer()
            );

    public static void init() {
        // 静态字段加载即注册
    }
}
