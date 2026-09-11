package name.icpm.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * 修复「单人本地玩家 UUID 与旧存档不一致」导致的玩家数据错乱。
 *
 * 背景：老 MITE 里单人玩家 UUID 由 UUIDUtil.createOfflinePlayerUUID(name) 派生，
 * FixedPlayerUuidMixin 将其固定为 00000000-0000-3004-998f-501a96e2ae48，从而直接承接
 * 旧存档 playerdata/00000000-...dat。
 *
 * 但在 1.21.11 中，本地玩家的 UUID 由【启动器】计算并随 auth 会话传入（Minecraft.user
 * 字段），游戏内根本不会调用 UUIDUtil.createOfflinePlayerUUID → FixedPlayerUuidMixin 对
 * 本地玩家完全无效 → 每次进入旧存档都会用新 UUID 新建 playerdata 文件、旧 MITE 进度被
 * 孤立（表现即「玩家数据错乱/重置」）。日志佐证：
 *   "Local player id 26977e4c-a09d-4fe2-ae82-bdb5168dc209 was not found in the known
 *    players list [00000000-0000-3004-998f-501a96e2ae48, ...]"
 *
 * 修复：注入 Minecraft.getUser() 返回值的 RETURN，把返回的 User 替换为携带固定 MITE
 * UUID 的新 User（保留原名/令牌/xuid/clientId）。getUser() 是本地玩家登录握手
 * （ClientHandshakePacketListenerImpl）与 FTB Teams 等读取本地 UUID 的唯一来源，改这里
 * 一处即可让：登录握手发包的 UUID、服务端 ServerPlayer 的 GameProfile/UUID、playerdata
 * 文件名、客户端 LocalPlayer 全部回到固定 UUID，与旧存档一致。
 *
 * 副作用：本 mod 面向单人/离线游戏。在线模式（真实 accessToken）下不生效、保持真实会话
 * UUID，不影响正版/第三方服务器；离线多人（LAN 开放）下所有离线玩家会共用该 UUID（与
 * FixedPlayerUuidMixin 相同的限制）。
 */
@Mixin(Minecraft.class)
public abstract class FixedLocalPlayerUuidMixin {

    /** 老 MITE 写死的固定玩家 UUID（与 FixedPlayerUuidMixin 一致）。 */
    private static final UUID ICpmMiteUuid = UUID.fromString("00000000-0000-3004-998f-501a96e2ae48");

    /** 懒构建一次，避免每帧分配新 User。 */
    @Unique
    private static User icpm$fixedUser;

    @Inject(method = "getUser", at = @At("RETURN"), cancellable = true)
    private void icpm$fixLocalPlayerUuid(CallbackInfoReturnable<User> cir) {
        User real = cir.getReturnValue();
        if (real == null || ICpmMiteUuid.equals(real.getProfileId())) {
            return;
        }
        // 仅离线模式才固定 UUID；在线模式保持真实会话 UUID（避免破坏正版/第三方服务器校验）。
        // 离线判定：正版/在线会话的 accessToken 是 JWT（含 '.'）；离线启动器的 token 是
        // "0"、空串或启动器伪 token（本机启动器实测为"去连字符的 UUID 字符串"，即
        // Realms 报错 "Failed to parse into SignedJWT: 26977e4c...9" 的来源），均不含 '.'。
        String token = real.getAccessToken();
        boolean offline = token == null || token.isEmpty() || !token.contains(".");
        if (!offline) {
            return;
        }
        if (icpm$fixedUser == null) {
            icpm$fixedUser = new User(
                    real.getName(),
                    ICpmMiteUuid,
                    real.getAccessToken(),
                    real.getXuid(),
                    real.getClientId());
        }
        cir.setReturnValue(icpm$fixedUser);
    }
}
