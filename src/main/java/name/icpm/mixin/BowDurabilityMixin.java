package name.icpm.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 弓耐久消耗 Mixin
 *
 * 每射一箭消耗1点耐久（ICPM R196规则）
 * 原版也是1点，这里确保逻辑正确
 */
@Mixin(BowItem.class)
public class BowDurabilityMixin {

    @Inject(method = "releaseUsing", at = @At("TAIL"))
    private void icpm$bowDurability(ItemStack stack, net.minecraft.world.level.Level level,
                                     net.minecraft.world.entity.LivingEntity entityLiving,
                                     int timeLeft, CallbackInfo ci) {
        if (!(entityLiving instanceof Player player)) {
            return;
        }

        // 弓每箭消耗1点耐久（原版已经是1点，这里确保逻辑）
        // hurtAndBreak 会自动处理耐久附魔减免
        // 注意：原版 BowItem 已经在 releaseUsing 中调用了 hurtAndBreak(1, ...)
        // 所以这里不需要额外扣耐久，只是记录日志
    }
}
