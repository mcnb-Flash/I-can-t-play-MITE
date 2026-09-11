package name.icpm.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 锄头耕地耐久消耗 Mixin
 * 
 * 锄地一次消耗50点耐久（ICPM R196规则）
 * 原版每次只扣1点，这里改为50点
 */
@Mixin(HoeItem.class)
public class HoeTillDurabilityMixin {

    @Inject(method = "useOn", at = @At("RETURN"))
    private void icpm$hoeTillDurability(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() != InteractionResult.SUCCESS) {
            return;
        }

        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || stack.isEmpty()) {
            return;
        }

        // 原版已经扣了1点耐久，额外再扣49点，总共50点
        // 使用 hurtAndBreak 会触发耐久附魔减免
        stack.hurtAndBreak(49, player, EquipmentSlot.MAINHAND);
    }
}
