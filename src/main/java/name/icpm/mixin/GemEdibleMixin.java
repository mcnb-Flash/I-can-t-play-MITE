package name.icpm.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 宝石可食（1.6.4 ItemRock.getExperienceValueWhenSacrificed 移植）
 *
 * 钻石 / 绿宝石 / 青金石 手持右键可以"吃"（进食动画），吃完获得经验值：
 * - 钻石 = 500 XP
 * - 绿宝石 = 250 XP
 * - 青金石 = 25 XP
 *
 * 只给经验，不提供 FOOD 组件 → 无法获得饱食度和饱和度。
 */
@Mixin(Item.class)
public abstract class GemEdibleMixin {

    /** 宝石物品的经验值（1.6.4 ItemRock 数值） */
    @Unique
    private static int icpm$gemXp(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.DIAMOND) return 500;
        if (item == Items.EMERALD) return 250;
        if (item == Items.LAPIS_LAZULI) return 25;
        return 0;
    }

    /** 右键开始进食 */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void icpm$gemUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (icpm$gemXp(player.getItemInHand(hand)) > 0) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    /** 进食时长（1.6 秒 = 32 tick） */
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void icpm$gemUseDuration(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (icpm$gemXp(stack) > 0) {
            cir.setReturnValue(32);
        }
    }

    /** 进食动画 */
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void icpm$gemUseAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (icpm$gemXp(stack) > 0) {
            cir.setReturnValue(ItemUseAnimation.EAT);
        }
    }

    /** 吃完获得经验，消耗 1 个 */
    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void icpm$gemFinishUsing(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        int xp = icpm$gemXp(stack);
        if (xp > 0) {
            if (!level.isClientSide() && entity instanceof Player player) {
                player.giveExperiencePoints(xp);
            }
            stack.shrink(1);
            cir.setReturnValue(stack);
        }
    }
}
