package name.icpm.block

import name.icpm.blockentity.ICPMStrongboxBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult

/**
 * ICPM 金属箱（强箱）方块（1.6.4 BlockStrongbox，Material 金属）
 *
 * - 仅放置者（或创造模式）能打开，其他人点击播放拒绝音效（1.6.4 私人箱）
 * - 27 槽容器，UI 用原版箱子界面
 * - 外观：金属块贴图的箱子形状方块模型（1.6.4 同款，非实体渲染箱子）
 */
class ICPMStrongboxBlock(
    /** 金属名（用于贴图/语言，如 silver/gold/iron/mithril/adamantium/ancient_metal） */
    val metalName: String,
    properties: Properties
) : BaseEntityBlock(properties) {

    override fun codec(): com.mojang.serialization.MapCodec<out BaseEntityBlock> =
        simpleCodec { props -> ICPMStrongboxBlock("iron", props) }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ICPMStrongboxBlockEntity(pos, state)
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is ICPMStrongboxBlockEntity) {
                if (!blockEntity.isOwner(player) && !player.isCreative) {
                    // 非所有者：播放锁定音效并拒绝（1.6.4 chest_locked）
                    level.playSound(
                        null, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                        SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.1f + 0.9f
                    )
                    return InteractionResult.SUCCESS
                }
                player.openMenu(blockEntity)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.horizontalDirection.opposite)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(HorizontalDirectionalBlock.FACING)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    /** 放置时记录所有者 */
    override fun setPlacedBy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        placer: net.minecraft.world.entity.LivingEntity?,
        itemStack: net.minecraft.world.item.ItemStack
    ) {
        super.setPlacedBy(level, pos, state, placer, itemStack)
        if (!level.isClientSide && placer is Player) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is ICPMStrongboxBlockEntity) {
                blockEntity.setOwner(placer)
            }
        }
    }

    /** 破坏时掉落内容物 */
    override fun playerDestroy(
        level: Level,
        player: Player,
        pos: BlockPos,
        state: BlockState,
        blockEntity: BlockEntity?,
        itemStack: net.minecraft.world.item.ItemStack
    ) {
        if (blockEntity is ICPMStrongboxBlockEntity) {
            blockEntity.dropContent(level, pos)
        }
        super.playerDestroy(level, player, pos, state, blockEntity, itemStack)
    }
}
