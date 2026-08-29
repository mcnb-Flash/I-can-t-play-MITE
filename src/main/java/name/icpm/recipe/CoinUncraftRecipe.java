package name.icpm.recipe;

import com.mojang.serialization.MapCodec;
import name.icpm.ICPM;
import name.icpm.common.ICPMCoinHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * ICPM 金属币分解配方（1.6.4 ItemCoin 分解）
 *
 * 金属币可在背包 2x2 合成栏（或任意合成台）直接分解为金属粒：
 * - 合成格中 1~N 个**同种**金属币 → 分解为 N 个对应金属粒
 * - 支持多重分解：填满 2x2（4 币）→ 4 粒；1 币 → 1 粒
 * - 取走成品时返还经验（见 CoinXpRefundMixin，在 ResultSlot.onTake 注入）
 *
 * 匹配规则：非空格子必须全部是同一种 ICPM 金属币（不允许混合不同币）。
 */
public class CoinUncraftRecipe extends CustomRecipe {

    public CoinUncraftRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        Item coinItem = null;
        int coinCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!ICPMCoinHelper.isCoin(stack)) {
                return false;
            }
            if (coinItem == null) {
                coinItem = stack.getItem();
            } else if (coinItem != stack.getItem()) {
                return false; // 混合不同币不允许
            }
            coinCount++;
        }
        return coinCount >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        Item coinItem = null;
        int coinCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (coinItem == null) {
                coinItem = stack.getItem();
            }
            coinCount++;
        }
        if (coinItem == null) {
            return ItemStack.EMPTY;
        }
        Item nugget = ICPMCoinHelper.nuggetForCoin(new ItemStack(coinItem));
        if (nugget == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(nugget, coinCount);
        // 记录应返还的经验（取走时 CoinXpRefundMixin 读取并返还）
        int xp = ICPMCoinHelper.xpForCoin(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(coinItem).getPath());
        result.set(ICPM.COIN_XP_COMPONENT, xp * coinCount);
        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return name.icpm.recipe.ICPMRecipes.COIN_UNCRAFT;
    }

    public static class Serializer implements RecipeSerializer<CoinUncraftRecipe> {
        private static final MapCodec<CoinUncraftRecipe> CODEC = CraftingBookCategory.CODEC
                .optionalFieldOf("category", CraftingBookCategory.MISC)
                .xmap(CoinUncraftRecipe::new, CoinUncraftRecipe::category);

        @Override
        public MapCodec<CoinUncraftRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CoinUncraftRecipe> streamCodec() {
            return StreamCodec.composite(
                    CraftingBookCategory.STREAM_CODEC,
                    CoinUncraftRecipe::category,
                    CoinUncraftRecipe::new
            );
        }
    }
}
