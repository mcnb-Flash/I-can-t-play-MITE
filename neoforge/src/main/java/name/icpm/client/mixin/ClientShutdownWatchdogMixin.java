package name.icpm.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端退出看门狗开关。
 *
 * 默认【不禁用】原版 ClientShutdownWatchdog：若退出流程（保存/渲染收尾）真卡住，
 * 看门狗会在超时后抛出 WatchdogException 并打印客户端主线程栈 dump，从而暴露真正的卡顿点
 * （这正是定位"保存世界中"永久冻结所需的实证）。
 *
 * 仅当显式设置 JVM 参数 `-Dicpm.debug.keepClientWatchdogDisabled=true` 时才重新禁用，
 * 用于确认看门狗本身误报、而非真实卡顿的场景。
 */
@Mixin(targets = "com/mojang/blaze3d/platform/ClientShutdownWatchdog")
public class ClientShutdownWatchdogMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("ICPM-ShutdownWatchdog");

    @Inject(method = "startShutdownWatchdog", at = @At("HEAD"), cancellable = true, require = 0)
    private static void icpm$maybeDisableShutdownWatchdog(CallbackInfo ci) {
        boolean keepDisabled = Boolean.getBoolean("icpm.debug.keepClientWatchdogDisabled");
        if (keepDisabled) {
            LOGGER.info("ICPM: 按 -Dicpm.debug.keepClientWatchdogDisabled 禁用 Client shutdown watchdog");
            ci.cancel();
        }
    }
}
