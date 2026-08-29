package name.icpm.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * ICPM 多级桶。
 *
 * 与原版 BucketItem.use 行为一致（1.21.11）：
 *  - 空桶对水/岩浆源取水后返回本金属对应的水/岩浆桶；
 *  - 水/岩浆桶放置流体后返回本金属的空桶。
 * 物品变换通过 InteractionResult.heldItemTransformedTo 同步（原版做法），
 * 仅当成功时才消耗原桶，避免交互后物品凭空消失。
 */
public class ICPMBucketItem extends BucketItem {
    private final String metal;

    public ICPMBucketItem(Fluid content, String metal, Item.Properties properties) {
        super(content, properties.stacksTo(1));
        this.metal = metal;
    }

    /** 当前桶的金属类型（燃料消耗后返还对应空桶用） */
    public String getMetal() {
        return metal;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ClipContext.Fluid clip = this.getContent() == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, clip);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos hitPos = hit.getBlockPos();
        Direction direction = hit.getDirection();
        BlockPos adj = hitPos.relative(direction);
        if (!level.mayInteract(player, hitPos) || !player.mayUseItemAt(adj, direction, stack)) {
            return InteractionResult.FAIL;
        }

        if (this.getContent() == Fluids.EMPTY) {
            // 取水/岩浆
            BlockState state = level.getBlockState(hitPos);
            Block block = state.getBlock();
            FluidState fluidState = state.getFluidState();
            if (fluidState.isSource() && block instanceof BucketPickup) {
                BucketPickup pickup = (BucketPickup) block;
                ItemStack picked = pickup.pickupBlock(player, level, hitPos, state);
                if (!picked.isEmpty()) {
                    pickup.getPickupSound().ifPresent(s -> player.playSound(s, 1.0f, 1.0f));
                    level.gameEvent(player, GameEvent.FLUID_PICKUP, hitPos);
                    player.awardStat(Stats.ITEM_USED.get(this));
                    Fluid src = fluidState.getType();
                    ItemStack result;
                    if (src == Fluids.WATER && ICPMBuckets.waterOf(metal) != null) {
                        result = new ItemStack(ICPMBuckets.waterOf(metal));
                    } else if (src == Fluids.LAVA && ICPMBuckets.lavaOf(metal) != null) {
                        result = new ItemStack(ICPMBuckets.lavaOf(metal));
                    } else {
                        result = picked;
                    }
                    ItemStack transformed = ItemUtils.createFilledResult(stack, player, result);
                    return InteractionResult.SUCCESS.heldItemTransformedTo(transformed);
                }
            }
            return InteractionResult.FAIL;
        } else {
            // 放置流体
            BlockState state = level.getBlockState(hitPos);
            BlockPos target = canBlockContainFluid(player, level, hitPos, state) ? hitPos : adj;
            if (this.emptyContents(player, level, target, hit)) {
                this.checkExtraContent(player, level, stack, target);
                player.awardStat(Stats.ITEM_USED.get(this));
                ItemStack result = new ItemStack(ICPMBuckets.emptyOf(metal));
                ItemStack transformed = ItemUtils.createFilledResult(stack, player, result);
                return InteractionResult.SUCCESS.heldItemTransformedTo(transformed);
            }
            return InteractionResult.FAIL;
        }
    }

    private boolean canBlockContainFluid(Player player, Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        return state.canBeReplaced(this.getContent())
                || (block instanceof LiquidBlockContainer
                    && ((LiquidBlockContainer) block).canPlaceLiquid(player, level, pos, state, this.getContent()));
    }
}
