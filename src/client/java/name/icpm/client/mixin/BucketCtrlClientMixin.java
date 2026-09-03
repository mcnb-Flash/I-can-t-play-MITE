package name.icpm.client.mixin;

import name.icpm.item.ICPMBucketItem;
import name.icpm.network.BucketSourcePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 Ctrl+右键 放液体源头（客户端拦截）。
 *
 * <p>仅在按住 Ctrl、手持水/岩浆桶且（创造或经验≥100）时接管：发送
 * {@link BucketSourcePacket} 并取消本次普通 use（普通 use 会放"流动"而非源头）。
 * 经验不足或未按 Ctrl 时不拦截，走正常流动放置。
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class BucketCtrlClientMixin {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void icpm$ctrlPlaceSource(Player player, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof LocalPlayer local)) {
            return;
        }
        if (!Minecraft.getInstance().hasControlDown()) {
            return;
        }
        ItemStack stack = local.getItemInHand(hand);
        if (!(stack.getItem() instanceof BucketItem bucket)) {
            return;
        }
        // 仅水/岩浆桶（原版铁桶三件套或 ICPM 桶）
        if (!(bucket.getClass() == BucketItem.class || bucket instanceof ICPMBucketItem)) {
            return;
        }
        Fluid content = bucket.getContent();
        if (content != Fluids.WATER && content != Fluids.LAVA) {
            return;
        }
        boolean creative = local.getAbilities().instabuild;
        if (!creative && local.totalExperience < 100) {
            return; // 经验不足：让普通 use 放"流动"（R196 shouldContainedLiquidBePlacedAsSourceBlock=false）
        }
        if (local.level() == null) {
            return;
        }
        // 射线取目标（与服务端 use 同款 NONE clip、blockInteractionRange 长度）
        net.minecraft.world.phys.Vec3 eye = local.getEyePosition(1.0f);
        net.minecraft.world.phys.Vec3 look = local.getViewVector(1.0f);
        double reach = local.blockInteractionRange();
        BlockHitResult hit = local.level().clip(new net.minecraft.world.level.ClipContext(eye,
                eye.add(look.x * reach, look.y * reach, look.z * reach),
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, local));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos hitPos = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockPos adj = hitPos.relative(face);
        if (!local.level().mayInteract(local, hitPos) || !local.mayUseItemAt(adj, face, stack)) {
            return;
        }
        ClientPlayNetworking.send(new BucketSourcePacket(hand, hitPos, face));
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
