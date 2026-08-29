package name.icpm.mixin;

import name.icpm.common.ICPMFoodProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ICPM 原版食物注入 Mixin
 *
 * 所有物品注册都会汇聚到 Items.registerItem(ResourceKey, Function, Item.Properties)，
 * 该方法内执行 properties.setId(resourceKey)，这里通过 @Redirect 拦截该调用：
 * - 若物品是 ICPM 需要重定义的原版食物 → 替换/添加 FOOD 组件（ICPM 数值）
 * - 若该食物需要进食效果（生鸡肉中毒、河豚等）→ 同时替换 CONSUMABLE 组件
 * - 其余物品原样放行（保留原版数值/行为）
 *
 * 注意：本模组 mixin 未配置 refmap，不能使用 @Shadow/@Accessor/@Invoker，
 * 此方案仅用 @Redirect + 公开 API，符合约束。
 */
@Mixin(Items.class)
public class ICPMFoodInjectionMixin {

    @Redirect(
            method = "registerItem(Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Function;Lnet/minecraft/world/item/Item$Properties;)Lnet/minecraft/world/item/Item;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/Item$Properties;setId(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/item/Item$Properties;")
    )
    private static Item.Properties icpm$applyIcpmFoodProperties(Item.Properties properties, ResourceKey<Item> key) {
        String path = key.identifier().getPath();
        FoodProperties miteFood = ICPMFoodProperties.VANILLA_FOODS.get(path);
        if (miteFood != null) {
            // 仅替换 FOOD，保留原 CONSUMABLE（如蜂蜜瓶自带清除中毒效果）
            properties.component(DataComponents.FOOD, miteFood);
            Consumable miteConsumable = ICPMFoodProperties.VANILLA_CONSUMABLES.get(path);
            if (miteConsumable != null) {
                // 需要 ICPM 进食效果的食物：同时替换 CONSUMABLE
                properties.component(DataComponents.CONSUMABLE, miteConsumable);
            } else if (ICPMFoodProperties.DEFAULT_CONSUMABLE_ITEMS.contains(path)) {
                // 原版非食物（小麦种子等）：补默认进食行为使其可食用，保留其余原版 CONSUMABLE 不变
                properties.component(DataComponents.CONSUMABLE, ICPMFoodProperties.defaultConsumable());
            }
        }
        return properties.setId(key);
    }
}
