package name.icpm.mixin;

import name.icpm.block.BlockMetalAnvil;
import name.icpm.blockentity.TileEntityMetalAnvil;
import name.icpm.inventory.MetalAnvilMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版铁砧 → ICPM 金属砧容器（R196 修复机制）
 *
 * R196 的修复（ContainerRepair）挂在原版铁砧（BlockAnvil）上。1.21.11 原版铁砧走原版
 * AnvilMenu（锭修复/经验成本），无法用金属粒按 MITE 规则修复 ICPM 工具。
 * 此 mixin 拦截原版铁砧的 useWithoutItem，改为打开 MetalAnvilMenu（anvilEntity=null，
 * fallbackMetalType=IRON）——原版铁砧视作铁砧等级，走 R196 公式：金属粒修复、
 * 砧等级检查（铁砧可修铜/银/金/铁，高级材料需 ICPM 高级砧）、无经验成本、无砧损耗。
 *
 * 注：1.21.11 原版 AnvilBlock 不实现 EntityBlock、也没有 newBlockEntity（原版铁砧改用
 * BlockState 属性表示损坏等级、GUI 走 getMenuProvider，不再有方块实体）。因此原版铁砧
 * 不会拥有持久化的 TileEntityMetalAnvil——anvilEntity 恒为 null，由 MetalAnvilMenu 的
 * fallbackMetalType=IRON 兜底（无砧损耗）。ICPM 的金属砧（BlockMetalAnvil）自身仍完整
 * 具备耐久与损耗机制，不受此限制影响。
 */
@Mixin(AnvilBlock.class)
public class VanillaAnvilMenuMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void icpm$openMetalAnvil(BlockState state, Level level, BlockPos pos, Player player,
                                     BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) {
            return;
        }
        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.repair");
            }

            @Override
            public BlockPos getScreenOpeningData(ServerPlayer serverPlayer) {
                return pos;
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                // 1.21.11 原版铁砧无方块实体（AnvilBlock 不实现 EntityBlock），
                // 故 anvilEntity 恒为 null，由 MetalAnvilMenu 的 fallbackMetalType=IRON 兜底。
                TileEntityMetalAnvil anvilEntity = level.getBlockEntity(pos) instanceof TileEntityMetalAnvil te ? te : null;
                return new MetalAnvilMenu(
                        syncId,
                        inv,
                        ContainerLevelAccess.create(level, pos),
                        anvilEntity,
                        BlockMetalAnvil.MetalType.IRON
                );
            }
        });
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
