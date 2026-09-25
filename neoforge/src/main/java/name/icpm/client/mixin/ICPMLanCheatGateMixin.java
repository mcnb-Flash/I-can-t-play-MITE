package name.icpm.client.mixin;

import name.icpm.common.ICPMDevMode;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 局域网开放：强制生存 + 不允许作弊。
 *
 * <p>R196 {@code CommandServerPublishLocal.java:30} 的 {@code /publish} 就是
 * {@code shareToLAN(EnumGameType.SURVIVAL, false)} —— 强制生存且关闭作弊。
 *
 * <p>1.21.11 里局域网发布统一走
 * {@code IntegratedServer.publishServer(GameType gameType, boolean allowCheats, int port)}，
 * 因此在此把两个参数归一：界面上“允许作弊”开关即便被拨动也不会生效，游戏模式也只会是生存。
 *
 * <p>dev 模式开启时不做任何限制。
 */
@Mixin(IntegratedServer.class)
public class ICPMLanCheatGateMixin {

    /** 参数 1：游戏模式 → 强制 SURVIVAL。 */
    @ModifyVariable(method = "publishServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private GameType icpm$forceSurvivalLan(GameType gameType) {
        if (ICPMDevMode.isEnabled()) {
            return gameType;
        }
        return GameType.SURVIVAL;
    }

    /** 参数 2：允许作弊 → 强制 false（该参数在 publishServer 里是唯一的 boolean）。 */
    @ModifyVariable(method = "publishServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean icpm$blockLanCheats(boolean allowCheats) {
        if (ICPMDevMode.isEnabled()) {
            return allowCheats;
        }
        return false;
    }
}
