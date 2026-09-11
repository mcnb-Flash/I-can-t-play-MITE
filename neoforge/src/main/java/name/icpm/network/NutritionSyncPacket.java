package name.icpm.network;

import name.icpm.ICPM;
import name.icpm.component.NutritionComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 营养值同步网络包
 * 用于将服务端的营养值数据同步到客户端
 */
public record NutritionSyncPacket(int protein, int essentialFats, int phytonutrients) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NutritionSyncPacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "nutrition_sync"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, NutritionSyncPacket> CODEC = 
        StreamCodec.of(NutritionSyncPacket::write, NutritionSyncPacket::read);
    
    public static NutritionSyncPacket read(RegistryFriendlyByteBuf buf) {
        return new NutritionSyncPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }
    
    public static void write(RegistryFriendlyByteBuf buf, NutritionSyncPacket packet) {
        buf.writeVarInt(packet.protein);
        buf.writeVarInt(packet.essentialFats);
        buf.writeVarInt(packet.phytonutrients);
    }
    
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static NutritionSyncPacket fromComponent(NutritionComponent component) {
        return new NutritionSyncPacket(component.protein(), component.essentialFats(), component.phytonutrients());
    }
}
