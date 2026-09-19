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
 * ICPM 肥沃附魔（R196 `EnchantmentFertility` 移植）。
 *
 * <p>R196 判决源 `ItemHoe.java:88-91`（{@code ItemMattock} / {@code ItemScythe} 同款）：
 * <pre>
 * world.setBlock(x, y, z, Block.tilledField.blockID);          // 锄地成耕
 * if (Math.random() &lt; EnchantmentHelper.getEnchantmentLevelFraction(Enchantment.fertility, item_stack)) {
 *     BlockFarmland.setFertilized(world, x, y, z, true);        // 概率直接施好肥
 * }
 * </pre>
 * 其中 {@code getEnchantmentLevelFraction = level / getNumLevels()}
 * （`EnchantmentHelper.java:51-56`），fertility 的 {@code numLevels = 5}
 * （`Enchantment.java:107-109` 默认值，`EnchantmentFertility` 未覆写）⇒ **概率 = 附魔等级 / 5**。
 */
@Mixin(HoeItem.class)
public abstract class ICPMFertilityMixin {

    @Inject(method = "useOn", at = @At("TAIL"))
    private void icpm$fertility(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction() || context.getLevel().isClientSide()) {
            return;
        }
        Level level = context.getLevel();
        ItemStack hoe = context.getItemInHand();
        int lvl = ICPMEnchantEffects.level(level, hoe, "fertility");
        if (lvl <= 0) {
            return;
        }
        // R196：概率 = 附魔等级 / 5，命中则把刚翻好的耕地直接设为已施肥
        if (level.random.nextFloat() < (float) lvl / 5.0f) {
            ICPMFarmlandFertility.add(level.dimension(), context.getClickedPos(), 1);
        }
    }
}
