package name.icpm.network;

import name.icpm.ICPM;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 背包合成进度同步网络包（S2C）
 *
 * 服务端在开始/取消背包合成时发送给客户端，客户端据此绘制进度条：
 * - active=true  : 合成进行中，durationTick 为总时长，startTick 为开始游戏刻
 * - active=false : 合成已结束/取消，客户端清除进度条
 */
public record InventoryCraftSyncPacket(boolean active, int durationTick, long startTick) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InventoryCraftSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "inventory_craft_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryCraftSyncPacket> CODEC =
        StreamCodec.of(InventoryCraftSyncPacket::write, InventoryCraftSyncPacket::read);

    public static InventoryCraftSyncPacket read(RegistryFriendlyByteBuf buf) {
        return new InventoryCraftSyncPacket(buf.readBoolean(), buf.readVarInt(), buf.readVarLong());
    }

    public static void write(RegistryFriendlyByteBuf buf, InventoryCraftSyncPacket packet) {
        buf.writeBoolean(packet.active());
        buf.writeVarInt(packet.durationTick());
        buf.writeVarLong(packet.startTick());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
