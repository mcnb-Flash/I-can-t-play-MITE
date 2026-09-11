package name.icpm.block

import name.icpm.blockentity.ICPMFurnaceBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.FurnaceBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * ICPM 熔炉方块（粘土/硬化粘土/沙石/黑曜石/地狱岩）
 *
 * 继承原版 FurnaceBlock（复用 FACING/LIT 状态、粒子特效、比较器输出等），
 * 仅替换方块实体与热量上限：
 * - 粘土熔炉（clay_furnace）：maxHeat=1，不能烧大物品
 * - 硬化粘土熔炉（hardened_clay_furnace）：maxHeat=1
 * - 沙石熔炉（sandstone_furnace）：maxHeat=1
 * - 黑曜石熔炉（obsidian_furnace）：maxHeat=3，可烧岩浆，冶炼秘银矿
 * - 地狱岩熔炉（netherrack_furnace）：maxHeat=4，烈焰棒级，冶炼艾德曼矿
 * - 原石熔炉 = 原版熔炉（FurnaceBlockMixin 注入，maxHeat=2）
 *
 * 采用原版熔炉 UI（FurnaceMenu + GuiFurnace），见 ICPMFurnaceBlockEntity。
 */
class ICPMFurnaceBlock(
    /** 熔炉承受热量上限（ICPM R196 BlockFurnace.getMaxHeatLevel） */
    val maxHeatLevel: Int,
    properties: Properties,
    /** 是否可烧大物品（ICPM R196 BlockFurnace.acceptsLargeItems，粘土熔炉=false） */
    val acceptsLargeItems: Boolean = true
) : FurnaceBlock(properties) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ICPMFurnaceBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level !is ServerLevel) return null
        return createTickerHelper(
            blockEntityType,
            ICPMFurnaceBlockEntity.TYPE
        ) { _, pos, _, be -> be.miteTick() }
    }

    override fun openContainer(level: Level, pos: BlockPos, player: Player) {
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity is ICPMFurnaceBlockEntity) {
            player.openMenu(blockEntity)
        }
    }

    /**
     * 玩家破坏方块：掉落内容物（ICPM 原版 breakBlock 无条件掉落），
     * 爆炸破坏不掉内容物（与 ICPM 1.6.4 行为一致）。
     */
    override fun playerDestroy(
        level: Level,
        player: Player,
        pos: BlockPos,
        state: BlockState,
        blockEntity: BlockEntity?,
        itemStack: ItemStack
    ) {
        if (blockEntity is ICPMFurnaceBlockEntity) {
            blockEntity.dropContent(level, pos)
        }
        super.playerDestroy(level, player, pos, state, blockEntity, itemStack)
    }
}
