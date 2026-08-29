package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 迅捷附魔（R196 EnchantmentQuickness 移植）：弓拉弓时间每级 -2 tick（最少 5 tick）。
 */
@Mixin(BowItem.class)
public abstract class ICPMBowQuicknessMixin {

    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true)
    private void icpm$quickness(ItemStack itemStack, LivingEntity livingEntity, CallbackInfoReturnable<Integer> cir) {
        int lvl = ICPMEnchantEffects.level(livingEntity.level(), itemStack, "quickness");
        if (lvl > 0) {
            cir.setReturnValue(Math.max(5, cir.getReturnValue() - lvl * 2));
        }
    }
}
