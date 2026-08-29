package name.icpm.mixin;

import name.icpm.ICPM;
import name.icpm.common.ICPMFoodProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 可食用原版非食物 - 统一进食触发。
 *
 * 1.21.11 进食由 CONSUMABLE 组件驱动（Item.use 基类在有 CONSUMABLE 时自动进食），
 * 但部分物品（如 EggItem）覆写 use 只扔不吃的，无法靠基类自动进食。本 mixin 在
 * Item.use 的 HEAD 统一处理 ICPM 可食用物品，保证进食必定触发：
 *
 * - 对 DEFAULT_CONSUMABLE_ITEMS 中的物品：若玩家可进食，则 startUsingItem + CONSUME。
 * - 鸡蛋：默认右键 = 吃；潜行右键 = 扔（放行原版 EggItem.use）。
 *   注：ICPM r196 ItemEgg.hasIngestionPriority = !ctrl_is_down（默认吃、ctrl 扔），
 *   这里以"潜行"作为扔的修饰键，符合现代 MC 习惯。
 * - 种子（wheat/pumpkin/melon）的种植走 useOnBlock（右键耕地），该路径在 use 之前调用，
 *   种植成功时 use 不会被调用，因此种植与进食互不冲突。
 *
 * 配合 ICPMFoodInjectionMixin 注入的 FOOD + CONSUMABLE，进食能正确回数值。
 */
@Mixin({Item.class, EggItem.class})
public class ICPMEdibleUseMixin {

    // use 声明于 Item；EggItem 重写了 use。直接用官方方法名 "use" 注入即可：
    // 每个目标（Item / EggItem）都自行声明/重写 use，owner = 各自目标，无 Multi-target conflict。
    // （此前错误地写成 class 限定选择器 Lnet/minecraft/class_1792;method_7836(...)，
    //  会令 Mixin 指定非目标类而报 "target class not supported" 启动崩溃。）
    @Inject(method = "method_7836", remap = false, at = @At("HEAD"), cancellable = true)
    private void icpm$triggerIcpmEdible(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }
        Identifier id = stack.getItem().builtInRegistryHolder().key().identifier();
        if (!ICPMFoodProperties.DEFAULT_CONSUMABLE_ITEMS.contains(id.getPath())) {
            return;
        }
        // 鸡蛋：潜行 = 扔（放行原版 EggItem.use），否则 = 吃
        if (stack.is(Items.EGG) && player.isShiftKeyDown()) {
            // 扔出鸡蛋 → 授予"滚蛋"成就（该成就用 minecraft:impossible 触发，由代码在此处判定）
            if (player instanceof ServerPlayer sp) {
                var server = ((ServerLevel) sp.level()).getServer();
                var holder = server.getAdvancements().get(ICPM.id("throw_egg"));
                if (holder != null) {
                    sp.getAdvancements().award(holder, "impossible");
                }
            }
            return;
        }
        // 强制进食：有 CONSUMABLE 才能走完进食动画并回 FOOD 数值
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (consumable != null && food != null && player.canEat(food.canAlwaysEat())) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
