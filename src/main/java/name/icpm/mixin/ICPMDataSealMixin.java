package name.icpm.mixin;

import name.icpm.common.DataSeal;
import name.icpm.common.ICPMDevMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 防改物：在 PlayerList 存盘（退出/自动存盘）前后做签名写入与 level.dat 备份。
 *
 * <p>HEAD：若处于「拆解破坏期」({@link DataSeal#shouldSkipSave}) 则跳过整个存盘，防止被篡改的
 * 内存数据把 {@code level.dat} 自愈写回；否则在落盘前给玩家背包写入数据封印。
 * TAIL：落盘完成后把 {@code level.dat} 备份到 {@code <存档>/icpmback/level.dat.bak}。
 *
 * <p>devMode 下跳过。非单人（专用服务器）不触碰 level.dat。
 */
@Mixin(PlayerList.class)
public abstract class ICPMDataSealMixin {

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void icpm$saveHead(ServerPlayer player, CallbackInfo ci) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        net.minecraft.server.MinecraftServer s = player.level().getServer();
        if (s != null && s.isSingleplayer() && DataSeal.shouldSkipSave(s)) {
            // 篡改拆解期：跳过保存，避免 level.dat 被自愈
            ci.cancel();
        }
    }

    @Inject(method = "save", at = @At("TAIL"))
    private void icpm$saveTail(ServerPlayer player, CallbackInfo ci) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        net.minecraft.server.MinecraftServer s = player.level().getServer();
        if (s != null && s.isSingleplayer() && !DataSeal.shouldSkipSave(s)) {
            DataSeal.backupLevelDat(s, player);
        }
    }
}
