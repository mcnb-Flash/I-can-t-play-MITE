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
        // R196 水源·桶机制：与原版铁桶一致（接取不耗源头/放流动/创造放源等），见 ICPMBucketRules
        return ICPMBucketRules.handleUse(level, player, hand);
    }

    private boolean canBlockContainFluid(Player player, Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        return state.canBeReplaced(this.getContent())
                || (block instanceof LiquidBlockContainer
                    && ((LiquidBlockContainer) block).canPlaceLiquid(player, level, pos, state, this.getContent()));
    }
}
