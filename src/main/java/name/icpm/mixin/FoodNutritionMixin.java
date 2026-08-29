package name.icpm.mixin;

import name.icpm.common.ICPMFoodStats;
import name.icpm.common.PlayerNutritionManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 进食拦截
 *
 * 玩家吃食物时：
 * 1. PlayerNutritionManager.onFoodEaten —— 蛋白质/必需脂肪/植物营养素
 * 2. ICPMFoodStats.onFoodEaten —— R196 satiation/nutrition 双槽数值
 *    （原版 foodLevel/saturation 数值已由 FoodDataMixin 禁用的 eat(FoodProperties) 拦截）
 *
 * 注入点：Item.finishUsingItem（1.21.11 所有进食的真正入口，参数直接携带被吃的物品）。
 * 不能注入 LivingEntity.completeUsingItem 的 TAIL —— 该方法末尾 stopUsingItem() 会把
 * useItem 置为 EMPTY，TAIL 时 getUseItem() 恒为空，营养永远不会加上。
 */
@Mixin(Item.class)
public class FoodNutritionMixin {

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void icpm$onFinishUsingItem(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        if (stack.has(DataComponents.FOOD)) {
            PlayerNutritionManager.onFoodEaten(player, stack);
            ICPMFoodStats.onFoodEaten(player, stack);
        }
    }
}
