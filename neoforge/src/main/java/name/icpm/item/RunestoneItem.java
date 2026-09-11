package name.icpm.item;

import name.icpm.ICPM;
import name.icpm.block.BlockRunestone;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 符文石物品（R196 ItemRunestone，忠实移植）
 *
 * - 放置时按物品栈的 {@link ICPM#RUNESTONE_VARIANT} 数据组件设定方块变体（默认 0 / Nul）。
 * - 显示名追加魔法名："秘银符文石 \"Quas\""（R196 ItemRunestone.getItemDisplayName）。
 */
public class RunestoneItem extends BlockItem {

    public RunestoneItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /** 从物品栈读取符文变体（默认 0 / Nul） */
    public static int getVariant(ItemStack stack) {
        Integer v = stack.get(ICPM.RUNESTONE_VARIANT);
        return v == null ? 0 : (v & 15);
    }

    /** 创建指定变体的符文石物品栈 */
    public static ItemStack createStack(Block block, int variant) {
        ItemStack stack = new ItemStack(block);
        stack.set(ICPM.RUNESTONE_VARIANT, variant & 15);
        return stack;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        int variant = getVariant(context.getItemInHand());
        return state.setValue(BlockRunestone.VARIANT, variant);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.literal(super.getName(stack).getString() + " \"" + BlockRunestone.getMagicName(getVariant(stack)) + "\"");
    }

    /**
     * 手持符文石右键空中 → 循环物品栈的变体组件（R196 中变体在合成 UI 右键循环选取；
     * 本 mod 用空手右键物品来选变体，选好后再放置，4 角即带上对应变体编码 seed）。
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        int next = (getVariant(stack) + 1) & 15;
        stack.set(ICPM.RUNESTONE_VARIANT, next);
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal("符文石变体 → " + BlockRunestone.getMagicName(next)), true);
        }
        return InteractionResult.SUCCESS;
    }
}
