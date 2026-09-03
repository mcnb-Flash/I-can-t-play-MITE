package name.icpm.network;

import name.icpm.ICPM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/**
 * R196 "Ctrl+右键 消耗 100 经验放置液体源头"（C2S）。
 *
 * <p>客户端按住 Ctrl 使用水/岩浆桶时发送；服务端在标准 use 管道之外直接
 * 放置源头液块、扣 100 经验并同步手持物品为对应空桶。语义见
 * ItemBucket.shouldContainedLiquidBePlacedAsSourceBlock + tryPlaceContainedLiquid。
 */
public record BucketSourcePacket(InteractionHand hand, BlockPos pos, Direction face)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BucketSourcePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "bucket_source"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BucketSourcePacket> CODEC =
            StreamCodec.of(BucketSourcePacket::write, BucketSourcePacket::read);

    public static BucketSourcePacket read(RegistryFriendlyByteBuf buf) {
        return new BucketSourcePacket(buf.readEnum(InteractionHand.class), buf.readBlockPos(),
                buf.readEnum(Direction.class));
    }

    public static void write(RegistryFriendlyByteBuf buf, BucketSourcePacket packet) {
        buf.writeEnum(packet.hand());
        buf.writeBlockPos(packet.pos());
        buf.writeEnum(packet.face());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
