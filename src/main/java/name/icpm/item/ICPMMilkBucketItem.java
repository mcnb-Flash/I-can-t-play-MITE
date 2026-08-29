package name.icpm.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

/**
 * ICPM 牛奶桶：饮用后清除状态效果并返还本金属空桶。
 *
 * 仅服务端执行效果；物品变换通过 heldItemTransformedTo 同步，
 * 只有成功时才消耗原桶，避免交互后桶消失。
 */
public class ICPMMilkBucketItem extends Item {
    private final String metal;

    public ICPMMilkBucketItem(String metal, Item.Properties properties) {
        super(properties.stacksTo(1));
        this.metal = metal;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.removeAllEffects();
            player.playSound(SoundEvents.BUCKET_EMPTY, 1.0f, 1.0f);
        }
        ItemStack result = new ItemStack(ICPMBuckets.emptyOf(metal));
        ItemStack transformed = ItemUtils.createFilledResult(stack, player, result);
        return InteractionResult.SUCCESS.heldItemTransformedTo(transformed);
    }
}
