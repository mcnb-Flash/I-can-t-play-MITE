package name.icpm.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 剪刀剪取实体交互（R196 ItemShears 剪羊毛/蘑菇牛 50 点耐久 + 右键去抖）。
 *
 * <p>挂 {@code interactLivingEntity} 的声明类 {@link Item}（铁律 2026-08-19：1.21.11 中
 * ShearsItem 不重写 interactLivingEntity，@Mixin(ShearsItem) 会运行期崩 "could not find any
 * targets matching 'interactLivingEntity'"），用 instanceof ShearsItem 过滤。
 */
@Mixin(Item.class)
public abstract class ShearsInteractMixin {

    /** 玩家 UUID → 上次剪取类右键动作发生时的服务端游戏刻。仅服务端写入/读取。 */
    private static final Map<UUID, Long> LAST_SHEAR_USE = new ConcurrentHashMap<>();
    private static final long SHEAR_USE_DELAY_TICKS = 10L;

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void icpm$shearsEntityHead(ItemStack stack, Player player, LivingEntity entity,
                                       InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(stack.getItem() instanceof ShearsItem)) {
            return;
        }
        // R196 右键全局去抖：冷却中右键剪取实体（羊/蘑菇牛等）同样不生效，避免瞬间连剪
        if (player == null || player.level().isClientSide()) {
            return;
        }
        long now = player.level().getGameTime();
        if (onShearCooldown(player.getUUID(), now)) {
            cir.setReturnValue(InteractionResult.PASS);
            cir.cancel();
        }
    }

    @Inject(method = "interactLivingEntity", at = @At("RETURN"))
    private void icpm$shearsEntityDurability(ItemStack stack, Player player, LivingEntity entity,
                                             InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(stack.getItem() instanceof ShearsItem)) {
            return;
        }
        if (cir.getReturnValue() != InteractionResult.SUCCESS) {
            return;
        }
        if (player == null || stack.isEmpty()) {
            return;
        }
        // 剪羊 / 蘑菇牛等实体交互：额外再扣 49 点，合计 50 点
        stack.hurtAndBreak(49, player, EquipmentSlot.MAINHAND);
        if (!player.level().isClientSide()) {
            markShearUse(player.getUUID(), player.level().getGameTime());
        }
    }

    /** 距上次剪取类右键动作是否仍在去抖冷却窗口内（服务端游戏刻）。 */
    private static boolean onShearCooldown(UUID id, long now) {
        Long last = LAST_SHEAR_USE.get(id);
        return last != null && (now - last) < SHEAR_USE_DELAY_TICKS;
    }

    /** 记录一次剪取类右键动作发生时刻（服务端）。 */
    private static void markShearUse(UUID id, long now) {
        LAST_SHEAR_USE.put(id, now);
    }
}
