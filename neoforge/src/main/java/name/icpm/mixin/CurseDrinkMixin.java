package name.icpm.mixin;

import name.icpm.curse.CurseFoods;
import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 诅咒：禁饮（药水级）—— R196 isIngestionForbiddenByCurse（isDrinkable，且去咒药水豁免
 * 由 R196 语义实现，此处药水饮用被禁）。喷溅/滞留药水是投掷物（不饮用）不受影响；
 * 食物级饮品（奶桶/汤）已在 CursePassiveMixin.canEat 拦截。
 */
@Mixin(Item.class)
public abstract class CurseDrinkMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void icpm$blockPotionDrink(Level level, Player player, InteractionHand hand,
                                       CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) {
            return; // 客户端仅做表现，服务端权威拦截
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.PotionItem
                && !(stack.getItem() instanceof net.minecraft.world.item.SplashPotionItem)
                && !(stack.getItem() instanceof net.minecraft.world.item.LingeringPotionItem)
                && ICPMCurseManager.isCursed(player, ICPMCurse.CANNOT_DRINK, true)
                && CurseFoods.isDrink(stack)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
