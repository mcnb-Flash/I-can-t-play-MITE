package name.icpm.item

import name.icpm.common.ICPMFarmlandFertility
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * ICPM 粪便（1.6.4 ItemManure）
 *
 * - 燃料（burnTime 100，见 FurnaceFuelMixin）
 * - **只能给耕地增肥**：右键耕地（FarmBlock）提升土壤肥力（0-3 级），
 *   肥力越高作物生长越快（见 CropBlockMixin.getGrowthSpeed 注入）。
 * - 不能催熟作物——催熟已被移除，骨粉只能治疗患病作物（见 BoneMealMixin）。
 */
class IcpmManureItem(
    properties: Properties
) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val state = level.getBlockState(pos)

        // 只对耕地生效
        if (state.`is`(Blocks.FARMLAND)) {
            if (!level.isClientSide) {
                val dim: ResourceKey<Level> = level.dimension()
                val newLevel = ICPMFarmlandFertility.add(dim, pos, 1)
                context.itemInHand.shrink(1)
                // 施肥粒子（绿色十字）
                level.levelEvent(2005, pos, 0)
                if (newLevel >= ICPMFarmlandFertility.MAX_FERTILITY && level is ServerLevel) {
                    // 已满级，提示
                    val player = context.player
                    if (player != null) {
                        player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("土壤肥力已达上限！"),
                            true
                        )
                    }
                }
            }
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }
}
