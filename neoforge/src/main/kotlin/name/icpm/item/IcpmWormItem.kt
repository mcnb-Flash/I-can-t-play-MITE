package name.icpm.item

import name.icpm.common.ICPMCompostHelper
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Blocks

/**
 * ICPM 活虫（1.6.4 ItemMeat wormRaw）
 *
 * 除了作为食物（生虫）和钓鱼饵，活虫还会**吃植物**：
 * - 手持虫子右键草/树叶/花/树苗/作物等植物方块 → 消耗该植物 → 就地产出 1 个粪便
 * - 右键草方块 → 草皮被吃掉，变成泥土，产出 1 个粪便
 *
 * 箱子里放活虫会自动堆肥产粪便（见 ICPMCompostHelper.tryCompostChest）。
 */
class IcpmWormItem(
    properties: Properties
) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val state = level.getBlockState(pos)

        if (!ICPMCompostHelper.isPlantBlockForWorm(state)) {
            return InteractionResult.PASS
        }

        if (!level.isClientSide) {
            // 1) 消耗植物：草方块被啃成泥土，其余植物直接消失（不掉落）
            if (state.`is`(Blocks.GRASS_BLOCK)) {
                level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3)
            } else {
                level.destroyBlock(pos, false)
            }
            // 2) 产出粪便（掉在植物位置）
            Containers.dropItemStack(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, ItemStack(ICPMItems.MANURE))
            // 3) 音效
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.6f, 1.0f)
        }
        return InteractionResult.SUCCESS
    }
}
