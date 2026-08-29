package name.icpm.mixin;

import name.icpm.entity.ICPMLivestock;
import name.icpm.item.ICPMItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 空碗对牛挤奶（1.6.4 ItemVessel.onItemRightClickOnEntity 移植）
 *
 * 手持原版空碗（minecraft:bowl）右键牛：挤奶 → 变为奶碗（icpm:milk_bowl）。
 * 原版只允许空桶挤奶，此处扩展支持空碗。
 */
@Mixin(AbstractCow.class)
public abstract class ICPMCowBowlMilkMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void icpm$milkWithBowl(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.BOWL)) {
            return;
        }
        AbstractCow cow = (AbstractCow) (Object) this;
        if (cow.isBaby()) {
            return;
        }
        ICPMLivestock livestock = (ICPMLivestock) (Object) cow;
        // 仅当牛有奶（健康累积）时才可挤出奶碗；每次挤奶消耗 25 点奶量
        int milk = livestock.getMilk();
        if (milk <= 0) {
            return;
        }
        player.playSound(SoundEvents.COW_MILK, 1.0f, 1.0f);
        if (!player.level().isClientSide()) {
            ItemStack result = new ItemStack(ICPMItems.MILK_BOWL);
            ItemStack transformed = ItemUtils.createFilledResult(stack, player, result);
            player.setItemInHand(hand, transformed);
            livestock.setMilk(milk - 25);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
