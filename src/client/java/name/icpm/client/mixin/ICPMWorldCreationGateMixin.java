package name.icpm.client.mixin;

import name.icpm.common.ICPMDevMode;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 创建世界界面：禁用「允许作弊」与「创造 / 调试」游戏模式。
 *
 * <p>MITE R196 里创建世界时“允许作弊”和创造模式都无法选择；此处对齐该行为。
 *
 * <p><b>做法是「把参数归一」而不是「取消调用」</b>：{@code setGameMode} 与
 * {@code setAllowCommands} 内部都会执行 {@code onChanged()} 通知监听器，而
 * {@code CreateWorldScreen$GameTab} 注册了同步监听（把 CycleButton 的值重新拉回 uiState）。
 * 归一参数 ⇒ 状态被写回合法值 ⇒ 监听器把界面上的按钮/开关也一并拉回（点“创造”会自己弹回“生存”、
 * 拨开“允许作弊”会自己弹回关闭），行为与显示一致；若用 cancellable 取消，uiState 不变、
 * 但 CycleButton 自己的值已经变成新的，界面会显示成“创造”而实际是生存——反而误导。
 *
 * <p>功能层的兜底在服务端：即便外部直接构造带作弊的世界，
 * {@code ICPMGameModeNormalizeMixin} 仍会把玩家的游戏模式归一为生存。
 *
 * <p>dev 模式开启时不做任何限制。
 */
@Mixin(WorldCreationUiState.class)
public class ICPMWorldCreationGateMixin {

    /** 「允许作弊」：只能关，不能开。 */
    @ModifyVariable(method = "setAllowCommands", at = @At("HEAD"), argsOnly = true)
    private boolean icpm$blockAllowCommands(boolean value) {
        if (ICPMDevMode.isEnabled()) {
            return value;
        }
        return false;
    }

    /**
     * 游戏模式：创造 / 调试一律归一为生存。
     *
     * <p>调试模式（DEBUG）由「世界类型 = 调试」经 {@code WorldDimensions.isDebug()} 决定，
     * {@code WorldCreationUiState.getGameMode()} 会在 isDebug 时直接返回 DEBUG，
     * 因此把存下来的值归一为 SURVIVAL 不会影响真正的调试世界。
     */
    @ModifyVariable(method = "setGameMode", at = @At("HEAD"), argsOnly = true)
    private WorldCreationUiState.SelectedGameMode icpm$blockNonSurvivalMode(
            WorldCreationUiState.SelectedGameMode mode) {
        if (ICPMDevMode.isEnabled()) {
            return mode;
        }
        if (mode == WorldCreationUiState.SelectedGameMode.CREATIVE
                || mode == WorldCreationUiState.SelectedGameMode.DEBUG) {
            return WorldCreationUiState.SelectedGameMode.SURVIVAL;
        }
        return mode;
    }
}
