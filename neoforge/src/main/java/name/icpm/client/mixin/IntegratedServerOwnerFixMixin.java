package name.icpm.client.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复「保存并退出」永久卡在保存界面（根因级修复）。
 *
 * 已实锤的机制（2026-09-01 第三轮复现 + 全线程栈）：
 *   1. 渲染线程在 Minecraft.disconnect(Screen,ZZ) 里 `while(!isShutdown()) runTick(false)`
 *      画保存界面，等服务端线程死；isShutdown() == !serverThread.isAlive()。
 *   2. 服务端断线链完整走完（onDisconnect ENTER→EXIT、PlayerList.remove ENTER→EXIT），
 *      但【没有】"Stopping singleplayer server as player logged out"、【没有】halt ENTER。
 *   3. 线程栈：Server thread state=TIMED_WAITING，parked 在 waitUntilNextTick —— 服务端
 *      线程健康活着、running 仍为 true ⇒ halt 从未被调用 ⇒ isSingleplayerOwner() 返回 false。
 *   4. 根因：IntegratedServer.<init> 用 Minecraft.getGameProfile() 设置 singleplayerProfile，
 *      而 getGameProfile() 优先返回启动器异步 profile 查询(profileFuture)的 ProfileResult——
 *      其 name 未必等于游戏内玩家名（本启动器 UUID 为随机 v4、非名字派生，账号名与游戏内名
 *      "mcnb" 不一致）。于是原版按名字比对 singleplayerProfile 的 isSingleplayerOwner 恒 false
 *      → 房主玩家断线时服务器不会自行停机 → 保存界面永转。新档/旧档都复现，与存档无关。
 *
 * 修复：isSingleplayerOwner 仅在「singleplayerProfile 名字与断线玩家一致」时放行原版逻辑；
 * 否则回退为「与客户端本地用户名（Minecraft.getUser().getName()）比对」——本地玩家就是
 * 房主，这正是原版该判定的本意。LAN 开放时其它玩家的名字 ≠ 本地用户名，回退仍返回 false，
 * 不影响他人断线。附带打点输出两边的实际值，便于后续确认。
 */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerOwnerFixMixin {

    private static final Logger LOG = LoggerFactory.getLogger("ICPM-Shutdown");

    @Inject(method = "isSingleplayerOwner", at = @At("HEAD"), cancellable = true)
    private void icpm$fixSingleplayerOwner(NameAndId nameAndId, CallbackInfoReturnable<Boolean> cir) {
        try {
            MinecraftServer self = (MinecraftServer) (Object) this;
            GameProfile sp = self.getSingleplayerProfile();
            String spName = sp == null ? null : sp.name();
            String playerName = nameAndId == null ? null : nameAndId.name();

            // 原版判定能通过（名字一致）→ 放行原版逻辑
            if (spName != null && spName.equalsIgnoreCase(playerName)) {
                return;
            }

            // 回退：断线玩家是本地房主 → 视为 owner，令服务器自行停机
            String localName = Minecraft.getInstance().getUser().getName();
            boolean isLocal = localName != null && localName.equalsIgnoreCase(playerName);
            LOG.error("[SHUTDOWN] isSingleplayerOwner FALLBACK: singleplayerProfile="
                    + (sp == null ? "null" : spName + "/" + sp.id())
                    + " playerName=" + playerName
                    + " localUserName=" + localName
                    + " -> force " + isLocal);
            if (isLocal) {
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            LOG.error("[SHUTDOWN] isSingleplayerOwner fallback error", t);
        }
    }
}
