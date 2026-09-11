package name.icpm.mixin;

import name.icpm.common.ICPMConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 创造模式限制（config/icpm.json → enableCreativeMode）。
 *
 * enableCreativeMode=false（默认）时拦截所有把玩家变更为创造模式的路径
 * （/gamemode creative、作弊切换、第三方 mod 调 setGameMode 等统一收敛点 = ServerPlayer.setGameMode）。
 * 已是创造模式的玩家不受影响；已开启的玩家可自由切换/退出。
 */
@Mixin(ServerPlayer.class)
public abstract class ICPMCreativeModeGateMixin {

    @Unique
    private static final String CONFIG_HINT = "creative mode is disabled (config/icpm.json enableCreativeMode=false)";

    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void icpm$blockCreativeSwitch(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        if (gameType != GameType.CREATIVE) {
            return; // 只限制「变更为创造」；生存/冒险/旁观不受影响
        }
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.gameMode().equals(GameType.CREATIVE)) {
            return; // 已是创造，重复设置放行
        }
        if (!ICPMConfig.isCreativeEnabled()) {
            self.sendSystemMessage(Component.literal("§c[ICPM] 无法切换到创造模式：" + CONFIG_HINT));
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
