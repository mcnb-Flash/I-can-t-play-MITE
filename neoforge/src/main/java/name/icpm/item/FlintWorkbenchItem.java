package name.icpm.item;

import name.icpm.block.BlockICPMFlintWorkbench;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 燧石工作台物品（多原木衍生变体）
 *
 * 变体信息存放在原版 {@code minecraft:block_state} 组件里（键 "wood"，值为序号字符串），
 * 这样合成（配方 result.components）、破坏（战利品表 copy_state）、
 * 物品模型（select on block_state）三端都能用原版机制闭环，无需自定义数据组件。
 *
 * 放置时显式把组件应用到方块状态，不依赖原版是否自动应用，保证行为确定。
 */
public class FlintWorkbenchItem extends BlockItem {

    public FlintWorkbenchItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** 读取物品栈的木材变体；缺失或非法时回退为橡木 */
    public static BlockICPMFlintWorkbench.WoodType getWood(ItemStack stack) {
        if (stack != null) {
            BlockItemStateProperties props = stack.get(DataComponents.BLOCK_STATE);
            if (props != null) {
                Object raw = props.get(BlockICPMFlintWorkbench.WOOD);
                if (raw instanceof Integer index) {
                    return BlockICPMFlintWorkbench.WoodType.fromIndex(index);
                }
            }
        }
        return BlockICPMFlintWorkbench.WoodType.OAK;
    }

    /** 创建指定木材的燧石工作台物品栈 */
    public static ItemStack createStack(Block block, BlockICPMFlintWorkbench.WoodType wood) {
        ItemStack stack = new ItemStack(block);
        stack.set(
            DataComponents.BLOCK_STATE,
            new BlockItemStateProperties(Map.of("wood", Integer.toString(wood.getIndex())))
        );
        return stack;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        BlockItemStateProperties props = context.getItemInHand().get(DataComponents.BLOCK_STATE);
        return props != null ? props.apply(state) : state;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.literal(getWood(stack).getWorkbenchName());
    }
}
