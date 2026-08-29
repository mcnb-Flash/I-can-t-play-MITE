package name.icpm.network;

import name.icpm.ICPM;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 金属砧命名框同步包（C2S）
 *
 * 客户端命名框内容变化时发送，服务端收到后调用 MetalAnvilMenu.setItemName(name)
 * 更新命名状态并重算结果（纯命名 / 修复+命名）。
 * 复刻 R196 的 Packet250CustomPayload("MC|ItemName") 行为。
 */
public record AnvilRenamePacket(String name) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AnvilRenamePacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "anvil_rename"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilRenamePacket> CODEC =
        StreamCodec.of(AnvilRenamePacket::write, AnvilRenamePacket::read);

    public static AnvilRenamePacket read(RegistryFriendlyByteBuf buf) {
        return new AnvilRenamePacket(buf.readUtf(40));
    }

    public static void write(RegistryFriendlyByteBuf buf, AnvilRenamePacket packet) {
        buf.writeUtf(packet.name(), 40);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
