package name.icpm.mixin;

import name.icpm.FreezeDetector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 定位「保存并退出」卡死在保存界面：逐玩家移除步的精准 tracer。
 *
 * 已确认的证据链（1.21.11 反编译）：
 *   1. 渲染线程在 Minecraft.disconnect(Screen,ZZ) 里 `while (!isShutdown()) runTick(false)`
 *      循环绘制保存界面；isShutdown() == !serverThread.isAlive()。
 *   2. 服务端玩家断线走 ServerGamePacketListenerImpl.onDisconnect → removePlayerFromWorld
 *      → PlayerList.remove(player) → （save / removePlayerImmediately / broadcast…）。
 *   3. removePlayerFromWorld 返回后，ServerCommonPacketListenerImpl.onDisconnect 才会执行
 *      "Stopping singleplayer server as player logged out" → server.halt(false)。
 *   4. 卡死日志中【没有】"Stopping singleplayer server" 且服务端线程在 "退出游戏" 后静默
 *      ⇒ PlayerList.remove 一直没返回 ⇒ 服务端线程活着 ⇒ isShutdown() 永假 ⇒ 保存界面永转。
 *
 * 本 tracer 在 PlayerList.remove 的 HEAD/TAIL 打点；并起一个 4 秒看门狗：remove 超过 4 秒
 * 未退出时立即 dump 全线程栈（含 "Server thread" 的精确卡点栈），无需用户等待看门狗阈值。
 * 用 [SHUTDOWN] 前缀输出，便于在 latest.log 中检索。
 */
@Mixin(PlayerList.class)
public class PlayerListShutdownTracer {

    private static final Logger LOG = LoggerFactory.getLogger("ICPM-Shutdown");

    /** remove 进入时启动的卡死看门狗线程。 */
    @Unique
    private static Thread icpm$stallWatcher;

    @Inject(method = "remove", at = @At("HEAD"))
    private void icpm$removeEnter(ServerPlayer player, CallbackInfo ci) {
        LOG.error("[SHUTDOWN] PlayerList.remove ENTER player=" + player.getUUID()
                + " name=" + player.getScoreboardName()
                + " thread=" + Thread.currentThread().getName());
        Thread w = new Thread(PlayerListShutdownTracer::icpm$watch, "ICPM-RemoveStallWatcher");
        w.setDaemon(true);
        icpm$stallWatcher = w;
        w.start();
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void icpm$removeExit(ServerPlayer player, CallbackInfo ci) {
        Thread w = icpm$stallWatcher;
        icpm$stallWatcher = null;
        if (w != null) {
            w.interrupt();
        }
        LOG.error("[SHUTDOWN] PlayerList.remove EXIT player=" + player.getUUID());
    }

    /**
     * 看门狗：remove 超 4 秒未退出 → dump 全线程栈。
     */
    @Unique
    private static void icpm$watch() {
        try {
            Thread.sleep(4_000L);
        } catch (InterruptedException e) {
            return; // remove 正常返回，看门狗被撤销
        }
        LOG.error("[SHUTDOWN] PlayerList.remove STALLED > 4s! Dumping all thread stacks to locate the hang...");
        FreezeDetector.dumpThreads(0L, 0L, "PlayerList.remove stalled");
    }
}
