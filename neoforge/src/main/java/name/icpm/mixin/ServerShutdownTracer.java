package name.icpm.mixin;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 服务端关闭流程里程碑 tracer。
 * 目的：定位「保存并退出」时保存界面不消失的根因 —— 是服务端 stopServer 没完成，
 * 还是客户端 disconnect 没推进。用 [SHUTDOWN] 前缀输出，便于在 latest.log 中检索。
 */
@Mixin(MinecraftServer.class)
public class ServerShutdownTracer {

    private static final Logger LOG = LoggerFactory.getLogger("ICPM-Shutdown");

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void icpm$stopServerEnter(CallbackInfo ci) {
        LOG.error("[SHUTDOWN] stopServer ENTER  thread=" + Thread.currentThread().getName());
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void icpm$stopServerExit(CallbackInfo ci) {
        LOG.error("[SHUTDOWN] stopServer EXIT   thread=" + Thread.currentThread().getName());
    }

    @Inject(method = "saveAllChunks", at = @At("HEAD"))
    private void icpm$saveAllChunksEnter(CallbackInfoReturnable<Boolean> ci) {
        LOG.error("[SHUTDOWN] saveAllChunks ENTER  thread=" + Thread.currentThread().getName());
    }

    @Inject(method = "saveAllChunks", at = @At("TAIL"))
    private void icpm$saveAllChunksExit(CallbackInfoReturnable<Boolean> ci) {
        LOG.error("[SHUTDOWN] saveAllChunks EXIT   thread=" + Thread.currentThread().getName());
    }

    /**
     * halt 是单机「停止服务端」的实际入口（ServerCommonPacketListenerImpl.onDisconnect 在
     * 玩家断开后调用）：halt ENTER 出现但没有后续服务端日志 ⇒ 卡在 halt 之前的链路。
     */
    @Inject(method = "halt", at = @At("HEAD"))
    private void icpm$haltEnter(boolean waitForShutdown, CallbackInfo ci) {
        LOG.error("[SHUTDOWN] MinecraftServer.halt ENTER  waitForShutdown=" + waitForShutdown
                + "  thread=" + Thread.currentThread().getName());
    }

    @Inject(method = "halt", at = @At("TAIL"))
    private void icpm$haltExit(boolean waitForShutdown, CallbackInfo ci) {
        LOG.error("[SHUTDOWN] MinecraftServer.halt EXIT   thread=" + Thread.currentThread().getName());
    }
}
