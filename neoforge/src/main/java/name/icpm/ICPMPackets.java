package name.icpm;

import name.icpm.inventory.ICPMWorkbenchMenu;
import name.icpm.inventory.MetalAnvilMenu;
import name.icpm.item.ICPMBucketRules;
import name.icpm.network.AnvilRenamePacket;
import name.icpm.network.BucketSourcePacket;
import name.icpm.network.WorkbenchCraftPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * ICPM C2S 网络包处理器（NeoForge）。
 *
 * 内容对齐原 Fabric ICPM.onInitialize 中的三个 ServerPlayNetworking 接收器：
 * - 工作台合成操作（START_CRAFT / CYCLE_QUALITY / TAKE_RESULT）
 * - 金属砧命名框同步（MC|ItemName）
 * - R196 桶 Ctrl+右键 消耗 100 经验 放置液体源头
 *
 * 由 ICPMNeoForge 的 RegisterPayloadHandlersEvent playToServer 方法引用；
 * 各方法内部自行 enqueueWork 切回主线程。
 */
public final class ICPMPackets {

    private ICPMPackets() {
    }

    /** 工作台合成操作（R196 SlotCrafting.onSlotClicked）。 */
    public static void handleWorkbenchCraft(WorkbenchCraftPacket payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player.containerMenu instanceof ICPMWorkbenchMenu workbenchMenu) {
                switch (payload.action()) {
                    case START_CRAFT -> workbenchMenu.startCrafting(player);
                    case CYCLE_QUALITY -> workbenchMenu.cycleQuality(player);
                    case TAKE_RESULT -> workbenchMenu.takeResult(player);
                }
            }
        });
    }

    /** 金属砧命名框同步（R196 MC|ItemName）。 */
    public static void handleAnvilRename(AnvilRenamePacket payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player.containerMenu instanceof MetalAnvilMenu menu) {
                menu.setItemName(payload.name());
            }
        });
    }

    /** R196 桶 Ctrl+右键 消耗 100 经验 放置液体源头。 */
    public static void handleBucketSource(BucketSourcePacket payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                return;
            }
            ItemStack held = sp.getItemInHand(payload.hand());
            if (!(held.getItem() instanceof BucketItem bucket)) {
                return;
            }
            Fluid content = bucket.getContent();
            if (content != Fluids.WATER && content != Fluids.LAVA) {
                return;
            }
            if (!ICPMBucketRules.isR196Bucket(held)) {
                return;
            }
            // 距离/交互权限校验（客户端射线同源：blockInteractionRange）
            BlockPos pos = payload.pos();
            double d = sp.getEyePosition().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            double reach = sp.blockInteractionRange();
            if (d > (reach + 2.0) * (reach + 2.0)) {
                return;
            }
            if (!sp.level().mayInteract(sp, pos) || !sp.mayUseItemAt(pos, payload.face(), held)) {
                return;
            }
            // 目标：命中块可被替换则放命中块，否则放面相邻块
            BlockState st = sp.level().getBlockState(pos);
            BlockPos target = st.canBeReplaced(content) ? pos : pos.relative(payload.face());
            ICPMBucketRules.placeSourceAt(sp, held, payload.hand(), target);
        });
    }
}
