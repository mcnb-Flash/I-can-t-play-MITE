package name.icpm.network;

import name.icpm.ICPM;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 背包合成操作网络包（C2S）
 *
 * 客户端在背包界面点击 2x2 结果槽时发送，通知服务端执行：
 * - START: 开始合成（首次点击，进入等待）
 * - TAKE : 取走合成成品（合成完成后点击）
 *
 * 为避免原版容器点击预测导致的不同步，背包界面直接拦截结果槽点击并改发此包，
 * 不再走原版 SlotResult 的即时取走逻辑。
 */
public record InventoryCraftPacket(Action action) implements CustomPacketPayload {

    public enum Action {
        START,
        TAKE
    }

    public static final CustomPacketPayload.Type<InventoryCraftPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "inventory_craft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InventoryCraftPacket> CODEC =
        StreamCodec.of(InventoryCraftPacket::write, InventoryCraftPacket::read);

    public static InventoryCraftPacket read(RegistryFriendlyByteBuf buf) {
        return new InventoryCraftPacket(Action.values()[buf.readByte()]);
    }

    public static void write(RegistryFriendlyByteBuf buf, InventoryCraftPacket packet) {
        buf.writeByte(packet.action().ordinal());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
