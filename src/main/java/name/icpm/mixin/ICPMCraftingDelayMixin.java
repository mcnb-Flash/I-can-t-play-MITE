package name.icpm.mixin;

import name.icpm.common.ICPMCraftCooldowns;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 合成延迟 Mixin
 *
 * 当玩家从合成台取出成品后，记录合成冷却
 */
@Mixin(ResultSlot.class)
public class ICPMCraftingDelayMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void icpm$onCraftTaken(Player player, ItemStack stack, CallbackInfo ci) {
        if (!player.level().isClientSide()) {
            ICPMCraftCooldowns.markCrafted(player);
        }
    }
}
