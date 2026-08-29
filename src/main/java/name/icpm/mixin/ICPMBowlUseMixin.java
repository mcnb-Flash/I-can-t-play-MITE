package name.icpm.mixin;

import name.icpm.item.ICPMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 空碗交互（1.6.4 ItemBowl.onItemRightClick 移植）
 *
 * 手持原版空碗（minecraft:bowl）右键水源：取水 → 变为水碗（icpm:water_bowl）。
 * 原版碗只是普通 Item，无此行为；注入 Item.use 仅对 BOWL 生效。
 * 对牛挤奶见 ICPMCowBowlMilkMixin（mobInteract）。
 */
@Mixin(Item.class)
public abstract class ICPMBowlUseMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void icpm$bowlPickupWater(Level level, Player player, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.BOWL)) {
            return;
        }
        // 射线检测水源（SOURCE_ONLY：只匹配流体源方块）
        // 注意：Item.getPlayerPOVHitResult 是 protected，mixin 中需自行实现
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = eye.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(new net.minecraft.world.level.ClipContext(eye, look, net.minecraft.world.level.ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), stack)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isSource() || fluidState.getType() != Fluids.WATER) {
            return;
        }
        // 空碗 → 水碗（1.6.4 convertOneOfHeldItem；不消耗水源，水源无限）
        if (!level.isClientSide()) {
            player.playSound(net.minecraft.sounds.SoundEvents.GENERIC_SPLASH, 1.0f, 1.0f);
        }
        ItemStack result = new ItemStack(ICPMItems.WATER_BOWL);
        ItemStack transformed = ItemUtils.createFilledResult(stack, player, result);
        cir.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(transformed));
    }
}
