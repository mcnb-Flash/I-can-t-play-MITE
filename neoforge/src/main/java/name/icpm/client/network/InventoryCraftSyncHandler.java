package name.icpm.client.network;

import name.icpm.client.InventoryCraftClientState;
import name.icpm.network.InventoryCraftSyncPacket;
import net.minecraft.client.Minecraft;

/**
 * 背包合成进度同步处理器（客户端）
 *
 * 接收服务端发来的 InventoryCraftSyncPacket，更新客户端背包合成状态，
 * 供 InventoryScreenCraftMixin 绘制进度条。
 *
 * 进度条完全基于客户端游戏刻计算：收到 active 包时以客户端当前游戏刻作为起点，
 * 避免直接复用服务端 startTick（客户端与服务端游戏刻不同步会导致进度条错乱）。
 *
 * NeoForge：由 ICPMNeoForge 的 RegisterPayloadHandlersEvent 中 playToClient lambda 调用。
 */
public class InventoryCraftSyncHandler {

    private InventoryCraftSyncHandler() {
    }

    /** 客户端主线程处理（NeoForge payload handler 已包 enqueueWork，此处直接执行）。 */
    public static void handle(InventoryCraftSyncPacket payload) {
        if (payload.active()) {
            long clientStartTick = Minecraft.getInstance().level.getGameTime();
            InventoryCraftClientState.set(true, payload.durationTick(), clientStartTick);
        } else {
            InventoryCraftClientState.clear();
        }
    }
}
