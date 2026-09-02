package name.icpm.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ICPM 剪取动物交互（R196 ItemShears 剪羊毛/剪蘑菇牛 50 点耐久 + 右键去抖）。
 *
 * <p>⚠️ 1.21.11 剪羊毛/剪蘑菇牛并非走 {@code Item.interactLivingEntity}——原版
 * {@link Sheep}/{@link MushroomCow} 的 {@code mobInteract} 才是唯一入口（此前挂在
 * Item.interactLivingEntity 上的旧版 mixin 完全空转：ShearsItem 不覆写该方法，返回
 * PASS 后直接落入实体的 mobInteract，耐久/冷却从未生效）。
 *
 * <p>本 mixin 直接注入两个实体的 {@code mobInteract}：
 * <ul>
 *   <li>{@code @Redirect(ItemStack.is(Item))}：把 {@code stack.is(Items.SHEARS)} 判定
 *       放宽为「任意 ShearsItem 子类」（ICPM 铜/金/银/远古金属/秘银/艾德曼剪刀都是
 *       独立的 ShearsItem 注册，不等于原版 Items.SHEARS → 原判定下剪不了羊/蘑菇牛）；</li>
 *   <li>{@code @Inject(RETURN)}：剪取成功后按 R196 规则补扣耐久到合计 50 点
 *       （原版 mobInteract 内已扣 1，此处补 49），并记录右键去抖时刻。</li>
 *   <li>{@code @Inject(HEAD, cancellable)}：去抖冷却窗口内拦截再次剪取。</li>
 * </ul>
 *
 * <p>原版物品判定被 {@code ItemStack.is} 常量比对实现；蘑菇牛里还有碗/蘑菇煲等其它
 * {@code is} 比对，Redirect 处理器必须只对 {@code Items.SHEARS} 目标放宽、其余走原逻辑。
 */
@Mixin({Sheep.class, MushroomCow.class})
public abstract class ShearsInteractMixin {

    /** 玩家 UUID → 上次剪取类右键动作发生时的服务端游戏刻。仅服务端写入/读取。 */
    private static final Map<UUID, Long> LAST_SHEAR_USE = new ConcurrentHashMap<>();
    private static final long SHEAR_USE_DELAY_TICKS = 10L;

    /** 距上次剪取类右键动作是否仍在去抖冷却窗口内（服务端游戏刻）。 */
    private static boolean onShearCooldown(UUID id, long now) {
        Long last = LAST_SHEAR_USE.get(id);
        return last != null && (now - last) < SHEAR_USE_DELAY_TICKS;
    }

    /** 记录一次剪取类右键动作发生时刻（服务端）。 */
    private static void markShearUse(UUID id, long now) {
        LAST_SHEAR_USE.put(id, now);
    }

    /**
     * 放宽 {@code Sheep.mobInteract}/{@code MushroomCow.mobInteract} 里的
     * {@code stack.is(Items.SHEARS)}：ICPM 自定义剪刀（ShearsItem 子类但非原版物品）
     * 同样视为"剪刀"。
     */
    @Redirect(method = "mobInteract",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean icpm$acceptIcpmShears(ItemStack stack, Item item) {
        if (item == Items.SHEARS && stack.getItem() instanceof ShearsItem) {
            return true;
        }
        // 蘑菇牛 mobInteract 里还有 Items.BOWL / Items.MUSHROOM_STEW / Items.SUSPICIOUS_STEW 等比对
        return stack.is(item);
    }

    /** 去抖：冷却窗口内禁止剪取（仅对剪刀类物品生效，喂食/挤奶等不受影响）。 */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void icpm$shearCooldownHead(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof ShearsItem)) {
            return;
        }
        if (onShearCooldown(player.getUUID(), player.level().getGameTime())) {
            // 冷却中：直接取消本次交互（不剪取、不消耗耐久）
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    /** 剪取成功（羊剪毛/蘑菇牛剪蘑菇）后：原版已扣 1，补扣 49 → 合计 50，并刷新去抖时刻。 */
    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void icpm$shearEntityDurability(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof ShearsItem)) {
            return;
        }
        stack.hurtAndBreak(49, player, hand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        markShearUse(player.getUUID(), player.level().getGameTime());
    }
}
