package name.icpm.mixin;

import name.icpm.item.ICPMBucketItem;
import name.icpm.item.ICPMBucketRules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 水源·桶机制——原版铁桶三件套（bucket/water_bucket/lava_bucket）。
 *
 * <p>ICPMBucketItem 覆写了 use()，本 mixin 不会作用于 ICPM 桶（由 ICPMBucketItem.use
 * 调用 {@link ICPMBucketRules#handleUse}），两类行为完全一致。
 * 仅接管类本身即为 BucketItem（非鱼桶/粉雪桶等子类）且内容为 空/水/岩浆 的物品。
 */
@Mixin(BucketItem.class)
public abstract class BucketItemR196Mixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void icpm$r196BucketUse(Level level, Player player, InteractionHand hand,
                                    CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BucketItem bucket)) {
            return;
        }
        Fluid content = bucket.getContent();
        if (content != Fluids.EMPTY && content != Fluids.WATER && content != Fluids.LAVA) {
            return;
        }
        // 仅接管原版"铁桶"三件套（class 恰为 BucketItem）；ICPM 子类有自己 use
        if (bucket.getClass() == BucketItem.class || bucket instanceof ICPMBucketItem) {
            cir.setReturnValue(ICPMBucketRules.handleUse(level, player, hand));
        }
    }
}
