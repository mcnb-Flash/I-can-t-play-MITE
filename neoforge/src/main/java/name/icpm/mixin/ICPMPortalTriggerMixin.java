package name.icpm.mixin;

import name.icpm.block.HellPortalBlock;
import name.icpm.block.ReturnPortalBlock;
import name.icpm.block.UnderworldPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 传送门触发 Mixin
 * 拦截实体 tick 方法，检测实体是否在 ICPM 传送门方块内并触发 PortalProcessor
 *
 * 1.21.11 中 entityInside 依赖 InsideBlockEffectApplier，空碰撞形状可能导致检测失败。
 * 通过 tick 检测作为备用机制，调用 entity.setAsInsidePortal() 启动原版传送流程。
 */
@Mixin(Entity.class)
public abstract class ICPMPortalTriggerMixin {

    @Unique
    private int icpm$portalCheckCounter = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void icpm$checkPortalOnTick(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if (entity.level().isClientSide()) {
            return;
        }

        icpm$portalCheckCounter++;
        if (icpm$portalCheckCounter < 4) {
            return;
        }
        icpm$portalCheckCounter = 0;

        if (!entity.isAlive()) {
            return;
        }

        if (entity.isOnPortalCooldown()) {
            return;
        }

        if (!entity.canUsePortal(false)) {
            return;
        }

        // 如果已经在传送门处理中，跳过
        if (entity.portalProcess != null) {
            return;
        }

        AABB aabb = entity.getBoundingBox();
        BlockPos minPos = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ);
        BlockPos maxPos = BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = entity.level().getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof UnderworldPortalBlock || block instanceof ReturnPortalBlock || block instanceof HellPortalBlock) {
                entity.setAsInsidePortal((net.minecraft.world.level.block.Portal) block, pos);
                return;
            }
        }
    }
}
