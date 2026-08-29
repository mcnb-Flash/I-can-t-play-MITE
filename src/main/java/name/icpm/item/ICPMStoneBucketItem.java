package name.icpm.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * ICPM 石头桶：右键放置圆石并返还本金属空桶（用于搬运圆石/石头）。
 *
 * 仅成功放置时才消耗原桶；物品变换通过 heldItemTransformedTo 同步，避免消失。
 */
public class ICPMStoneBucketItem extends Item {
    private final String metal;

    public ICPMStoneBucketItem(String metal, Item.Properties properties) {
        super(properties.stacksTo(1));
        this.metal = metal;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.FAIL;
        }
        BlockPos hitPos = hit.getBlockPos();
        Direction direction = hit.getDirection();
        BlockPos adj = hitPos.relative(direction);
        if (!level.mayInteract(player, hitPos) || !player.mayUseItemAt(adj, direction, stack)) {
            return InteractionResult.FAIL;
        }
        BlockState hitState = level.getBlockState(hitPos);
        BlockPos target = hitState.canBeReplaced() ? hitPos : adj;
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        if (level.setBlock(target, cobble, 11)) {
            player.awardStat(Stats.ITEM_USED.get(this));
            player.playSound(SoundEvents.STONE_PLACE, 1.0f, 1.0f);
            ItemStack result = new ItemStack(ICPMBuckets.emptyOf(metal));
            ItemStack transformed = ItemUtils.createFilledResult(stack, player, result);
            return InteractionResult.SUCCESS.heldItemTransformedTo(transformed);
        }
        return InteractionResult.FAIL;
    }
}
