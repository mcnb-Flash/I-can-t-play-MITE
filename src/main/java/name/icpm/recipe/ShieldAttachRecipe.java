package name.icpm.recipe;

import com.mojang.serialization.MapCodec;
import name.icpm.ICPM;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * ICPM 装盾配方（icpm:shield_attach）
 *
 * 在对应等级的工作台（结果=工具，其材质等级即所需工作台等级）将一件 ICPM 工具与
 * 一面 {@link Items#SHIELD} 合成：结果 = 同工具（带 {@link ICPM#SHIELD_ATTACHED} 组件，
 * 右键可格挡，效果与 R196 相同）；盾牌不消失，仅消耗 25% 最大耐久，可作为第二产出返还
 * （见 ICPMWorkbenchMenu 的取走逻辑），可继续使用或再次参与合成（约 4 次后损坏）。
 *
 * 匹配规则（必须严格：恰好 2 个非空格子）：
 * - 恰好 1 件 ICPM 工具（且尚未装盾）
 * - 恰好 1 面 minecraft:shield
 * - 其余格子必须为空
 */
public class ShieldAttachRecipe extends CustomRecipe {

    public ShieldAttachRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int toolCount = 0;
        int shieldCount = 0;
        int nonEmpty = 0;
        ItemStack toolStack = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            nonEmpty++;
            Item item = stack.getItem();
            if (item == Items.SHIELD) {
                shieldCount++;
            } else if (ICPMToolProperties.isICPMTool(stack) && !stack.has(ICPM.SHIELD_ATTACHED)) {
                toolCount++;
                toolStack = stack;
            } else {
                return false; // 其它任何物品都不允许
            }
        }
        // 必须恰好 1 工具 + 1 盾牌，且总非空格子为 2
        return nonEmpty == 2 && toolCount == 1 && shieldCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && ICPMToolProperties.isICPMTool(stack) && !stack.has(ICPM.SHIELD_ATTACHED)) {
                ItemStack result = stack.copy();
                result.set(ICPM.SHIELD_ATTACHED, Boolean.TRUE);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return name.icpm.recipe.ICPMRecipes.SHIELD_ATTACH;
    }

    public static class Serializer implements RecipeSerializer<ShieldAttachRecipe> {
        private static final MapCodec<ShieldAttachRecipe> CODEC = CraftingBookCategory.CODEC
                .optionalFieldOf("category", CraftingBookCategory.MISC)
                .xmap(ShieldAttachRecipe::new, ShieldAttachRecipe::category);

        @Override
        public MapCodec<ShieldAttachRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShieldAttachRecipe> streamCodec() {
            return StreamCodec.composite(
                    CraftingBookCategory.STREAM_CODEC,
                    ShieldAttachRecipe::category,
                    ShieldAttachRecipe::new
            );
        }
    }
}
