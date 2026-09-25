package name.icpm.client.mixin;

import name.icpm.common.ICPMDevMode;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创建世界界面：禁用「允许作弊」与「创造 / 调试」游戏模式。
 *
 * <p>MITE R196 里创建世界时“允许作弊”和创造模式都无法选择；此处对齐该行为：
 * 非 dev 模式下 {@code setAllowCommands(true)} 与 {@code setGameMode(CREATIVE|DEBUG)} 一律被拒
 * （撤销设置，保持默认的“不允许作弊 + 生存”）。
 *
 * <p>功能层的兜底在服务端：即便外部直接构造带作弊的世界，
 * {@code ICPMGameModeNormalizeMixin} 仍会把玩家的游戏模式归一为生存。
 *
 * <p>dev 模式开启时不做任何限制。
 */
@Mixin(WorldCreationUiState.class)
public class ICPMWorldCreationGateMixin {

    @Inject(method = "setAllowCommands", at = @At("HEAD"), cancellable = true)
    private void icpm$blockAllowCommands(boolean value, CallbackInfo ci) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        if (value) {
            ci.cancel(); // 只能关，不能开
        }
    }

    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void icpm$blockNonSurvivalMode(WorldCreationUiState.SelectedGameMode mode, CallbackInfo ci) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        if (mode == WorldCreationUiState.SelectedGameMode.CREATIVE
                || mode == WorldCreationUiState.SelectedGameMode.DEBUG) {
            ci.cancel();
        }
    }
}
