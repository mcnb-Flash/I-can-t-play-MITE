package name.icpm.recipe;

import name.icpm.ICPM;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * ICPM 自定义配方序列化器注册（NeoForge）
 *
 * NeoForge 中 RECIPE_SERIALIZER 注册表在 mod 构造期已冻结（区别于 DataComponent/MobEffect/Menu 等
 * 可直写注册的 registry），必须在 mod bus RegisterEvent 阶段注册。registerAll() 由 ICPMNeoForge
 * 挂 RegisterEvent(Registries.RECIPE_SERIALIZER) 调用；序列化器随 datapack 加载前就绪。
 */
public final class ICPMRecipes {

    private ICPMRecipes() {
    }

    private static RecipeSerializer<CoinUncraftRecipe> coinUncraft;
    private static RecipeSerializer<ShieldAttachRecipe> shieldAttach;

    /** RegisterEvent 回调：注册金属币分解 + 装盾配方序列化器。 */
    public static void registerAll() {
        if (coinUncraft != null) {
            return;
        }
        coinUncraft = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "coin_uncraft"),
                new CoinUncraftRecipe.Serializer()
        );
        shieldAttach = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "shield_attach"),
                new ShieldAttachRecipe.Serializer()
        );
    }

    public static RecipeSerializer<CoinUncraftRecipe> getCoinUncraft() {
        return coinUncraft;
    }

    public static RecipeSerializer<ShieldAttachRecipe> getShieldAttach() {
        return shieldAttach;
    }
}
