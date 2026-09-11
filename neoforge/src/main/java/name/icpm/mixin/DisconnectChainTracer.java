package name.icpm.mixin;

import name.icpm.FreezeDetector;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 断线链 tracer：覆盖 PlayerList.remove 之后的剩余关机路径。
 *
 * 已知（1.21.11 反编译）：
 *   ServerGamePacketListenerImpl.onDisconnect:
 *     1) log "lost connection"
 *     2) removePlayerFromWorld(): broadcast player.left -> player.disconnect()
 *        -> PlayerList.remove(player) -> player.getTextFilter().leave()
 *     3) super.onDisconnect(details) [ServerCommonPacketListenerImpl]:
 *        if (isSingleplayerOwner()) { log "Stopping singleplayer server as player logged out";
 *                                      server.halt(false); }
 * 卡死日志中 PlayerList.remove 已 EXIT（PlayerListShutdownTracer 确认），但"Stopping
 *  singleplayer server"缺失 ⇒ 卡点在 remove EXIT 之后、halt 之前的这段链路上。
 * 本 tracer 在 onDisconnect HEAD/TAIL 打点，并起 5 秒看门狗：超时即 dump 全线程栈，
 * 精确定位这段链路上（含第三方 mod mixin）的卡点。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class DisconnectChainTracer {

    private static final Logger LOG = LoggerFactory.getLogger("ICPM-Shutdown");

    @Unique
    private static Thread icpm$watchdog;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void icpm$onDisconnectEnter(DisconnectionDetails details, CallbackInfo ci) {
        LOG.error("[SHUTDOWN] ServerGamePacketListenerImpl.onDisconnect ENTER  thread="
                + Thread.currentThread().getName());
        Thread w = new Thread(DisconnectChainTracer::icpm$watch, "ICPM-DisconnectStallWatcher");
        w.setDaemon(true);
        icpm$watchdog = w;
        w.start();
    }

    @Inject(method = "onDisconnect", at = @At("TAIL"))
    private void icpm$onDisconnectExit(DisconnectionDetails details, CallbackInfo ci) {
        Thread w = icpm$watchdog;
        icpm$watchdog = null;
        if (w != null) {
            w.interrupt();
        }
        LOG.error("[SHUTDOWN] ServerGamePacketListenerImpl.onDisconnect EXIT  thread="
                + Thread.currentThread().getName());
    }

    @Unique
    private static void icpm$watch() {
        try {
            Thread.sleep(5_000L);
        } catch (InterruptedException e) {
            return;
        }
        LOG.error("[SHUTDOWN] onDisconnect STALLED > 5s! Dumping all thread stacks to locate the hang...");
        FreezeDetector.dumpThreads(0L, 0L, "ServerGamePacketListenerImpl.onDisconnect stalled");
    }
}
