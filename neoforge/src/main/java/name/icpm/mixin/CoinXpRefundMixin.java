package name.icpm.mixin;

import name.icpm.ICPM;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 金属币分解返还经验
 *
 * CoinUncraftRecipe.assemble 会给分解产物（金属粒）附加 COIN_XP 组件（值 = 应返还经验）。
 * 玩家从结果槽取走时（ResultSlot.onTake），读取组件返还经验并移除标记。
 * 这样背包 2x2 合成栏中任意数量金属币分解为粒时都能正确返还经验。
 */
@Mixin(ResultSlot.class)
public abstract class CoinXpRefundMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void icpm$refundCoinXp(Player player, ItemStack stack, CallbackInfo ci) {
        if (stack == null || stack.isEmpty() || player.level().isClientSide()) {
            return;
        }
        Integer xp = stack.get(ICPM.COIN_XP_COMPONENT);
        if (xp != null && xp > 0) {
            player.giveExperiencePoints(xp);
            stack.remove(ICPM.COIN_XP_COMPONENT);
        }
    }
}
