package name.icpm.network;

import name.icpm.ICPM;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 工作台合成操作网络包（C2S）
 *
 * 客户端发送此包通知服务端执行合成相关操作：
 * - START_CRAFT: 开始合成（左键结果槽）
 * - CYCLE_QUALITY: 切换品质等级（右键结果槽）
 * - TAKE_RESULT: 取走合成成品（合成完成后左键结果槽）
 *
 * 复刻 R196 的 SlotCrafting.onSlotClicked 行为：
 *   button 0 → 开始合成（crafting_proceed = true）
 *   button 1 → 切换品质（tryIncrementCraftingResultIndex）
 */
public record WorkbenchCraftPacket(Action action) implements CustomPacketPayload {

    public enum Action {
        START_CRAFT,
        CYCLE_QUALITY,
        TAKE_RESULT
    }

    public static final CustomPacketPayload.Type<WorkbenchCraftPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "workbench_craft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkbenchCraftPacket> CODEC =
        StreamCodec.of(WorkbenchCraftPacket::write, WorkbenchCraftPacket::read);

    public static WorkbenchCraftPacket read(RegistryFriendlyByteBuf buf) {
        return new WorkbenchCraftPacket(Action.values()[buf.readByte()]);
    }

    public static void write(RegistryFriendlyByteBuf buf, WorkbenchCraftPacket packet) {
        buf.writeByte(packet.action().ordinal());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
