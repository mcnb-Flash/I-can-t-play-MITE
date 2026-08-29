package name.icpm.client.mixin;

import name.icpm.FreezeDetector;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 每帧（{@code Minecraft.runTick}）向 {@link FreezeDetector} 打存活时间戳，
 * 并惰性启动监控守护线程。
 *
 * 选 {@code runTick} 而非 {@code run}：前者每帧调用（标题界面 / 游戏中 / 暂停均如此），
 * 后者只在启动时调用一次，无法反映运行期卡死。
 *
 * require = -1：该探针是诊断辅助，若方法名因版本差异无法解析，静默跳过而非导致启动崩溃。
 */
@Mixin(Minecraft.class)
public class ClientTickMarkerMixin {

    @Inject(method = "runTick", at = @At("HEAD"), require = -1)
    private void icpm$markTick(CallbackInfo ci) {
        FreezeDetector.markClientTick();
        FreezeDetector.ensureStarted();
    }
}
