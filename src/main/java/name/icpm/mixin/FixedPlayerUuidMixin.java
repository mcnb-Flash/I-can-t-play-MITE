package name.icpm.mixin;

import net.minecraft.core.UUIDUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.UUID;

/**
 * 让新 jar 用老 MITE 写死的自定义 UUID（00000000-0000-3004-998f-501a96e2ae48）替代标准离线 UUID
 * （OfflinePlayer:mcnb），从而直接读取旧存档里的 playerdata/00000000-...dat 与 level.dat 的
 * Data.Player，而不新建空白玩家、与旧数据互相覆盖。
 *
 * 为什么 override 这里而不是 Entity.getUUID()：
 *   单人（集成）服务器下，服务端玩家 GameProfile 的 id、玩家实体 uuid 字段、getUUID()、
 *   getStringUUID()、playerdata 文件名、以及客户端 LocalPlayer 的 UUID 全部由
 *   UUIDUtil.createOfflinePlayerUUID(name) 派生。只改 getUUID() 会让服务端实体 UUID
 *   与客户端 GameProfile/本地玩家 UUID 不一致，导致客户端认不出自己、镜头与操控失效。
 *   改这个派生源，上面一切都自动一致，一处搞定。
 *
 * 副作用说明：本 mod 面向单人游戏。专用多人（含 LAN 开放）下所有离线玩家会共用此 UUID，
 * 因此不要用于多人服务器。
 */
@Mixin(UUIDUtil.class)
public abstract class FixedPlayerUuidMixin {

    private static final UUID MITE_UUID = UUID.fromString("00000000-0000-3004-998f-501a96e2ae48");

    /**
     * @author ICPM
     * @reason 使用老 MITE 写死的固定玩家 UUID，以承接旧存档玩家数据。
     */
    @Overwrite
    public static UUID createOfflinePlayerUUID(String string) {
        return MITE_UUID;
    }
}
