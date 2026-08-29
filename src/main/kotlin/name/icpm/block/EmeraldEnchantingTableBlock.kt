package name.icpm.block

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import name.icpm.inventory.ICPMEnchantmentMenu

/**
 * ICPM 绿宝石附魔台方块（1.6.4 BlockEnchantmentTable, Material.emerald）
 *
 * 关键设计：继承普通 [Block] 而非原版 [net.minecraft.world.level.block.EnchantingTableBlock]。
 *
 * 原版 EnchantingTableBlock 是 EntityBlock，其方块实体类型 BlockEntityType.ENCHANTING_TABLE
 * 在构造时硬编码并会校验"所处方块是否属于该类型"，icpm 方块不在其中，右键放置即触发
 * "Invalid block entity" 崩溃。故这里彻底不进入该体系。
 *
 * 1.21.11 的 EnchantmentMenu 改用 ContainerLevelAccess 自行扫描周围书架，不再依赖方块实体，
 * 因此用普通 Block + openMenu 即可获得与原版完全一致的附魔功能（消耗青金石/经验、读取书架能力）。
 * 唯一缺失的是浮动书本的客户端动画（纯视觉，无功能影响）。
 * 如需补回书本动画，需另建自定义 BlockEntity + 渲染器，后续可扩展。
 */
class EmeraldEnchantingTableBlock(
    properties: Properties
) : Block(properties) {

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            player.openMenu(getMenuProvider(state, level, pos))
        }
        return InteractionResult.SUCCESS
    }

    override fun getMenuProvider(state: BlockState, level: Level, pos: BlockPos): MenuProvider {
        val access = ContainerLevelAccess.create(level, pos)
        return SimpleMenuProvider(
            { id, inv, _ -> ICPMEnchantmentMenu(id, inv, access) },
            Component.translatable("container.icpm.emerald_enchantment")
        )
    }
}
