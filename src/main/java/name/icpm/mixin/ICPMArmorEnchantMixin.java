package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 自由移动附魔（R196）：护腿带附魔则完全免疫蛛网减速。
 */
@Mixin(WebBlock.class)
public abstract class ICPMArmorEnchantMixin {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void icpm$freeAction(BlockState blockState, Level level, BlockPos blockPos, Entity entity,
                                 InsideBlockEffectApplier insideBlockEffectApplier, boolean bl, CallbackInfo ci) {
        if (entity instanceof Player player && ICPMEnchantEffects.armorLevel(player, "free_action") > 0) {
            ci.cancel();
        }
    }
}
