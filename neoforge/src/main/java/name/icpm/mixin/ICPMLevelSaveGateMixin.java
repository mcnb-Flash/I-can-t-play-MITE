package name.icpm.mixin;

import name.icpm.common.DataSeal;
import name.icpm.common.ICPMDevMode;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防改物：拆解破坏期（{@code .breached} 标记存在）封锁 level.dat 与区块落盘。
 *
 * <p>玩家数据由 {@link ICPMDataSealMixin} 在 {@code PlayerList.save} HEAD 拦截；而 level.dat 的写入
 * 走 {@code MinecraftServer.saveAllChunks} → {@code LevelStorageAccess.saveDataTag}，不经过 PlayerList：
 * <ul>
 *   <li>退出存档：{@code stopServer} → {@code saveAllChunks} → level.dat 被内存数据覆盖；</li>
 *   <li>自动存盘：{@code saveEverything} → {@code saveAllChunks} → 同上。</li>
 * </ul>
 * 若不拦截，篡改恢复流程中刚从备份还原的 level.dat 会在停机时被内存里的被篡改数据覆盖，
 * 造成「恢复成功后重新进入仍弹篡改窗」（或基准备份被篡改数据覆盖、作弊自愈）。
 *
 * <p>devMode 下跳过；非单人（专用服务器）不干预。
 */
@Mixin(MinecraftServer.class)
public class ICPMLevelSaveGateMixin {

    @Inject(method = "saveAllChunks", at = @At("HEAD"), cancellable = true)
    private void icpm$gateLevelSave(boolean suppressLogs, boolean flush, boolean force,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        MinecraftServer s = (MinecraftServer) (Object) this;
        if (s.isSingleplayer() && DataSeal.shouldSkipSave(s)) {
            // 拆解期：跳过 level.dat 与全部区块落盘（返回 true 表示「已保存」，避免调用方告警）
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
