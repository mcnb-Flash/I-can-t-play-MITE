package name.icpm.common

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object ICPMDissolveHelper {
    private val INSTANT_DISSOLVE_BLOCKS = setOf(
        Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
        Blocks.DEAD_BUSH, Blocks.VINE, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT,
        Blocks.COBWEB, Blocks.SUGAR_CANE, Blocks.BAMBOO, Blocks.LILY_PAD,
        Blocks.MOSS_CARPET, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS, Blocks.KELP,
        Blocks.SHORT_DRY_GRASS, Blocks.TALL_DRY_GRASS, Blocks.SPORE_BLOSSOM,
        Blocks.BIG_DRIPLEAF, Blocks.SMALL_DRIPLEAF, Blocks.HANGING_ROOTS,
        Blocks.PITCHER_PLANT, Blocks.TORCHFLOWER
    )

    /**
     * 返回方块被溶解所需的 tick 数。
     * 0 = 立即溶解；>0 = 需累积进度；-1 = 不可溶解。
     * 对应 R196 aqz.getDissolvePeriod（简化：按标签分类）。
     */
    fun getDissolvePeriod(level: Level, pos: BlockPos): Int {
        val state: BlockState = level.getBlockState(pos)
        if (state.isAir) return -1
        val block = state.block
        if (state.`is`(BlockTags.CROPS) || state.`is`(BlockTags.FLOWERS) || block in INSTANT_DISSOLVE_BLOCKS) return 0
        return when {
            state.`is`(BlockTags.LEAVES) -> 40
            state.`is`(BlockTags.WOOL) -> 60
            state.`is`(BlockTags.SAND) -> 60
            state.`is`(BlockTags.DIRT) -> 80
            state.`is`(BlockTags.PLANKS) -> 120
            state.`is`(BlockTags.LOGS) -> 160
            else -> -1
        }
    }

    /** 溶解（移除）一个方块，不掉落物品，并生成蒸汽/烟粒子。 */
    fun dissolveBlock(level: Level, pos: BlockPos) {
        if (level is ServerLevel) {
            level.destroyBlock(pos, false)
            val cx = pos.x + 0.5
            val cy = pos.y + 0.5
            val cz = pos.z + 0.5
            level.addParticle(ParticleTypes.SMOKE, cx, cy, cz, 0.0, 0.1, 0.0)
            level.addParticle(ParticleTypes.SMOKE, cx + 0.2, cy, cz, 0.0, 0.05, 0.0)
            level.addParticle(ParticleTypes.LARGE_SMOKE, cx, cy, cz + 0.2, 0.0, 0.08, 0.0)
        }
    }
}
