package name.icpm.mixin;

import name.icpm.common.ICPMFarmlandFertility;
import name.icpm.common.ICPMEnchantEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 肥沃附魔（R196 EnchantmentFertility 移植）：锄/战锄/镰刀锄地成功后给耕地增加肥力。
 */
@Mixin(HoeItem.class)
public abstract class ICPMFertilityMixin {

    @Inject(method = "useOn", at = @At("TAIL"))
    private void icpm$fertility(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction() || context.getLevel().isClientSide()) {
            return;
        }
        ItemStack hoe = context.getItemInHand();
        int lvl = ICPMEnchantEffects.level(context.getLevel(), hoe, "fertility");
        if (lvl <= 0) {
            return;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // 锄地成功后点击位置变成耕地，肥力 +附魔等级（上限 3）
        ICPMFarmlandFertility.add(level.dimension(), pos, lvl);
    }
}
