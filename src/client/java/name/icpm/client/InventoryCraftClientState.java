package name.icpm.client;

/**
 * 背包合成客户端状态（单例）
 *
 * 由 InventoryCraftSyncHandler 在服务端同步包到达时更新，
 * 由 InventoryScreenCraftMixin 在渲染时读取以绘制进度条。
 * 进度计算使用客户端世界游戏刻（与服务端共享同一世界时间，存在极小网络延迟，对进度条无影响）。
 */
public final class InventoryCraftClientState {

    public static boolean active = false;
    public static int durationTick = 0;
    public static long startTick = 0;

    private InventoryCraftClientState() {
    }

    public static void set(boolean active, int durationTick, long startTick) {
        InventoryCraftClientState.active = active;
        InventoryCraftClientState.durationTick = durationTick;
        InventoryCraftClientState.startTick = startTick;
    }

    public static void clear() {
        active = false;
        durationTick = 0;
        startTick = 0;
    }

    public static float getProgress(long currentTick) {
        if (!active || durationTick <= 0) return 0f;
        return Math.min(1.0f, (float) (currentTick - startTick) / (float) durationTick);
    }

    public static boolean isComplete(long currentTick) {
        return active && (currentTick - startTick) >= durationTick;
    }
}
