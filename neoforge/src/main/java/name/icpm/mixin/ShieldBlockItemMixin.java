package name.icpm.mixin;

import name.icpm.ICPM;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 装盾工具右键格挡 —— 忠实移植 R196 的"格挡"机制到 1.21.11（原版无此机制）。
 *
 * 在对应等级工作台将 ICPM 工具与盾牌合成后，工具获得 SHIELD_ATTACHED 组件。
 * 本 Mixin 让带该组件的工具：
 *  1) getUseAnimation → BLOCK，使 LivingEntity.isBlocking() 在持用时可能为真（抬手格挡姿态）；
 *  2) getUseDuration → 72000（与盾牌一致），保证右键可长时间保持"使用物品"状态（即格挡态）；
 *  3) use → 右键开始使用物品（startUsingItem）并返回 CONSUME，使玩家按住右键即进入格挡态。
 *
 * 真正的"伤害减半 + 工具扣耐久"逻辑在 ShieldBlockHurtMixin 中（modifyAppliedDamage）。
 *
 * 注：1.21.11 的 LivingEntity.isBlocking() 依赖 ITEM_SHIELD_BLOCK 物品标签，
 * 普通工具不在该标签内，故除本 Mixin 改 getUseAnimation 外，ShieldBlockHurtMixin
 * 另重写 isBlocking() 使装盾工具在格挡态下返回 true（姿态 + 一致性），
 * 但原版盾牌逻辑（hurtCurrentlyUsedShield 仅对 ShieldItem 生效）不会被误触发。
 */
@Mixin(Item.class)
public class ShieldBlockItemMixin {

    /** 右键开始格挡（按住右键保持使用物品态）。 */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void icpm$shieldUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.has(ICPM.SHIELD_ATTACHED)) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
            cir.cancel();
        }
    }

    /** 格挡可长时间保持（与盾牌一致）。 */
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void icpm$shieldUseDuration(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (stack.has(ICPM.SHIELD_ATTACHED)) {
            cir.setReturnValue(72000);
        }
    }

    /** 使用动画为 BLOCK，使 isBlocking() 可能为真（抬手姿态）。 */
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void icpm$shieldUseAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (stack.has(ICPM.SHIELD_ATTACHED)) {
            cir.setReturnValue(ItemUseAnimation.BLOCK);
        }
    }
}
