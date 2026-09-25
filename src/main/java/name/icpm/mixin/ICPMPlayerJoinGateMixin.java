package name.icpm.mixin;

import name.icpm.common.ICPMDevMode;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 登录期游戏模式归一（防外部改档的第二道保险）。
 *
 * <p>存档里的 {@code playerGameType} 若被 HMCL 等外部工具改成创造，玩家进世界时实际上已经被
 * {@link ICPMGameModeNormalizeMixin} 在收敛点归一；此处再显式核对一次并给出提示，
 * 同时确保归一后的值随存档保存写回（原版写的是当前 {@code gameMode}）。
 *
 * <p>dev 模式开启时不干预玩家模式。
 */
@Mixin(PlayerList.class)
public class ICPMPlayerJoinGateMixin {

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void icpm$normalizeOnJoin(Connection connection, ServerPlayer player,
                                      CommonListenerCookie cookie, CallbackInfo ci) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        if (player.gameMode() != GameType.SURVIVAL) {
            player.setGameMode(GameType.SURVIVAL);
            player.sendSystemMessage(Component.literal(
                    "§c[ICPM] 检测到非生存模式，已按 R196 规则重置为生存。"));
        }
    }
}
