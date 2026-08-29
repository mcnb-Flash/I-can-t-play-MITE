package name.icpm.mixin;

import name.icpm.ICPM;
import name.icpm.common.ICPMInventoryCraftingState;
import name.icpm.network.InventoryCraftSyncPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 背包 2x2 合成栏时间机制 —— 服务端时间门控与状态维护。
 *
 * 背包界面没有独立菜单，合成状态按玩家 UUID 存于 ICPMInventoryCraftingState。
 * 背包菜单（InventoryMenu / PlayerScreenHandler）继承自 AbstractContainerMenu，
 * 而 clicked（1.21.11 中 renamed 为 onSlotClick）方法声明在基类 AbstractContainerMenu 中，
 * 背包菜单自身字节码并不包含该方法。本模组 mixin 配置未生成 refMap，Mixin 不会跨父类查找注入目标，
 * 因此这里直接 mixin 基类 AbstractContainerMenu，再用 instanceof InventoryMenu 守卫，
 * 只对玩家的背包菜单做时间门控（其它菜单原样放行）。
 *
 * 结果槽（slot 0）门控：
 *   - 未开始  -> 开始合成（记录状态、下发进度同步），取消原版即时取走
 *   - 进行中  -> 取消点击，防止提前取走（白嫖）
 *   - 已完成  -> 取走成品（消耗网格、发放物品），取消原版取走由本逻辑完成
 * 改动网格槽（slot 1~4）则视为配方被改动，清空进行中的状态。
 *
 * 仅服务端执行（player 为 ServerPlayer 时）。
 */
@Mixin(AbstractContainerMenu.class)
public class ICPMInventoryCraftingMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void icpm$gateBackpackCraft(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        // 仅服务端处理（客户端预测点击的玩家不是 ServerPlayer）
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        // 只对玩家背包菜单（InventoryMenu）做门控，其它菜单原样放行
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (!(self instanceof InventoryMenu)) {
            return;
        }
        UUID uuid = player.getUUID();

        // 结果槽（slot 0）：时间门控
        if (slotId == 0) {
            if (!ICPMInventoryCraftingState.isActive(uuid)) {
                // 开始合成（内部会校验配方、记录状态、下发同步包）
                ICPM.startBackpackCraft(serverPlayer, self);
            } else {
                long currentTick = ((ServerLevel) serverPlayer.level()).getGameTime();
                if (ICPMInventoryCraftingState.isComplete(uuid, currentTick)) {
                    // 合成完成：取走成品（内部消耗网格、发放物品、清空状态、下发同步包）
                    ICPM.takeBackpackCraft(serverPlayer, self);
                }
                // 进行中但未完成：什么都不做，仅拦截原版取走
            }
            // 始终取消原版结果槽取走：开始/取走由上述逻辑处理，
            // 进行中则直接拦截，避免客户端预测取走造成白嫖
            ci.cancel();
            return;
        }

        // 网格槽 1~4 改动：若进行中则重置（配方被改动）
        if (slotId >= 1 && slotId <= 4) {
            if (ICPMInventoryCraftingState.isActive(uuid)) {
                ICPMInventoryCraftingState.clear(uuid);
                ServerPlayNetworking.send(serverPlayer, new InventoryCraftSyncPacket(false, 0, 0));
            }
        }
    }
}
