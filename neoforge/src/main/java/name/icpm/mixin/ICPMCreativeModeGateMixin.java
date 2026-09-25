package name.icpm.mixin;

import name.icpm.common.ICPMDevMode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 非生存模式变更的拦截与提示（R196 {@code CommandGameMode} 的现代等价）。
 *
 * <p>R196 的 {@code /gamemode} 只允许切 {@code SURVIVAL}（{@code CommandGameMode.java:40}）。
 * 本类对**变更为非生存**的请求给出明确反馈；真正的模式归一由
 * {@link ICPMGameModeNormalizeMixin} 在收敛点（{@code ServerPlayerGameMode.setGameModeForPlayer}）
 * 完成，因此存档恢复、其它 mod 等旁路同样被覆盖。
 *
 * <p>开关：{@code config/icpm.json} 的 {@code enableCreativeMode}，或 JVM 参数
 * {@code -Dicpm.devMode=true}（见 {@link ICPMDevMode}）。
 *
 * <p>dev 模式开启时：不拦截、不提示、**不修改玩家模式**。
 */
@Mixin(ServerPlayer.class)
public abstract class ICPMCreativeModeGateMixin {

    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void icpm$blockNonSurvival(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        if (ICPMDevMode.isEnabled()) {
            return; // dev 模式：放行，且不改动玩家模式
        }
        if (gameType == GameType.SURVIVAL) {
            return;
        }
        ServerPlayer self = (ServerPlayer) (Object) this;
        self.sendSystemMessage(Component.literal(
                "§c[ICPM] 本模组禁用非生存模式（R196：/gamemode 仅允许 survival）。"
                        + "开发者可用 -Dicpm.devMode=true 开启调试。"));
        // 当前已是非生存（例如存档被外部工具改过）⇒ 立即归一回生存
        if (self.gameMode() != GameType.SURVIVAL) {
            self.setGameMode(GameType.SURVIVAL);
        }
        cir.setReturnValue(Boolean.FALSE);
    }
}
