package name.icpm.client.mixin;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端关闭流程里程碑 tracer。
 * 「保存并退出」走 disconnectWithSavingScreen（显示保存界面）。记录其进出，
 * 配合服务端 ServerShutdownTracer，即可判断卡死在客户端（界面已显示但 disconnect 未完成）
 * 还是服务端（stopServer 未返回）。
 */
@Mixin(Minecraft.class)
public class ClientShutdownTracer {

    private static final Logger LOG = LoggerFactory.getLogger("ICPM-Shutdown");

    @Inject(method = "disconnectWithSavingScreen", at = @At("HEAD"))
    private void icpm$saveQuitEnter(CallbackInfo ci) {
        LOG.error("[SHUTDOWN] disconnectWithSavingScreen ENTER  (Save&Quit 点击，准备显示保存界面)");
    }

    @Inject(method = "disconnectWithSavingScreen", at = @At("TAIL"))
    private void icpm$saveQuitExit(CallbackInfo ci) {
        LOG.error("[SHUTDOWN] disconnectWithSavingScreen EXIT   (保存界面已显示，等待底层关闭任务完成)");
    }
}
