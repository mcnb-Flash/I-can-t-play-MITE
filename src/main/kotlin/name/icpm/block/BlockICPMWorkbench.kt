package name.icpm.block

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import name.icpm.inventory.ICPMWorkbenchMenu

/**
 * ICPM 工作台方块
 *
 * 等级制度（tier）：
 *  - 0: 燧石工作台 - 可合成锭/粒(tier0)及燧石级物品；铜/银/金工作台也可在此合成
 *  - 1: 铜/银/金工作台 - 三者同级(tier1)可互用，可合成铜/银/金制品、铁锭、铁工作台
 *  - 3: 铁工作台 - 可合成铁制品及远古金属工作台
 *  - 4: 远古金属工作台 - 可合成远古金属制品及秘银工作台
 *  - 5: 秘银工作台 - 可合成秘银制品及艾德曼工作台
 *  - 6: 艾德曼工作台 - 可合成所有物品
 *  (tier 2 已弃用：银/金工作台与铜工作台统一为 tier 1)
 *  递进链（R196 RecipeHelper.next_strongest_material）：铜/银/金→铁→远古金属→秘银→艾德曼
 */
open class BlockICPMWorkbench(
    val tier: Int,
    val displayName: String,
    properties: Properties
) : Block(properties) {

    companion object {
        val SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = SHAPE

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            player.openMenu(getMenuProvider(state, level, pos))
        }
        return InteractionResult.SUCCESS
    }

    open override fun getMenuProvider(state: BlockState, level: Level, pos: BlockPos): ExtendedScreenHandlerFactory<BlockPos> {
        val title = displayName
        return object : ExtendedScreenHandlerFactory<BlockPos> {
            override fun getDisplayName(): Component {
                return Component.literal(title)
            }

            override fun getScreenOpeningData(player: net.minecraft.server.level.ServerPlayer): BlockPos {
                return pos
            }

            override fun createMenu(syncId: Int, playerInventory: Inventory, player: Player): net.minecraft.world.inventory.AbstractContainerMenu {
                return ICPMWorkbenchMenu(syncId, playerInventory, ContainerLevelAccess.create(level, pos), tier)
            }
        }
    }

    override fun getRenderShape(state: BlockState): net.minecraft.world.level.block.RenderShape {
        return net.minecraft.world.level.block.RenderShape.MODEL
    }
}
