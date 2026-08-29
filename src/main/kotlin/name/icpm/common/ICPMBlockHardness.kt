package name.icpm.common

import net.minecraft.tags.BlockTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries

/**
 * ICPM 方块硬度（基于 1.6.4-ICPM R196 BlockHardness 表）
 *
 * ICPM 的方块硬度与原版 1.21.11 不同：木制方块通过 Material.wood.durability(=0.5)
 * 缩放，例如：
 *   - 原木 log = Material.wood.getFullBlockHardness() = 1.0（原版为 2.0）
 *   - 木板 planks = 1.0 × 2/3 ≈ 0.667
 *   - 其余木制家族（栏/门/活板门/压力板/按钮/楼梯/ slabs 等）≈ 0.5
 *
 * 耐久衰减公式（ItemTool.getToolDecayFromBreakingBlock）依赖此硬度：
 *   cost = max(hardness × 100 × baseDecayRate, 100×baseDecayRate/20, 1)
 * 若喂入原版硬度(2.0)而非 ICPM 硬度(1.0)，原木每次损耗会被放大一倍，
 * 导致"燧石短斧挖一块原木掉 200 耐久、只能挖 2 块"而非 r196 的 3 块。
 *
 * 注：R196(1.6.4) 没有 stripped logs；现代 MC 的 WOODEN_FENCE_GATES 标签已移除
 * （改用通用 FENCE_GATES），故木制家族用仍可解析的 WOODEN_* 标签 + FENCE_GATES 判断，
 * 其余方块回落到原版方块硬度（ICPM 多数石质方块硬度与原版接近，此为已知近似）。
 */
object ICPMBlockHardness {

    @JvmStatic
    fun get(level: Level, pos: BlockPos, state: BlockState): Float {
        // 原木：ICPM 硬度 1.0（原版为 2.0）
        if (state.`is`(BlockTags.LOGS)) {
            return 1.0f
        }
        // 木板：ICPM 硬度 ≈ 0.667
        if (state.`is`(BlockTags.PLANKS)) {
            return 0.667f
        }
        // 其余木制家族：ICPM 代表值 0.5
        if (isWoodenFamily(state)) {
            return 0.5f
        }
        // 草本/植被：ICPM 中割草（草/高草丛/蕨/花/树苗/蘑菇/灌木/藤蔓等）硬度≈0。
        // 用镰刀/剑割草应几乎不耗耐久；不再回退到原版破坏速度（草块 0.6、高草丛 0.0），
        // 否则会被后续 mixin 的 bump 提到 1.0，导致"割草耐久消耗等同于原木"，过于昂贵。
        if (isFoliage(state)) {
            return 0.0f
        }
        // R196 BlockHardness 与原版 1.21.11 差异显著的方块（按注册表 path 匹配）
        // 例：黑曜石 R196=8.0（原版 50.0！），若不覆盖则挖 1 块黑曜石耗 5000 耐久（铁镐总
        // 耐久仅 9600，挖一块掉一半多）——正是"挖黑曜石大半耐久消失"bug 的根因。
        val path = BuiltInRegistries.BLOCK.getKey(state.block)?.path
        if (path != null) {
            R196_HARDNESS[path]?.let { return it }
        }
        // 回落到原版方块破坏速度（无工具上下文下的基础硬度）
        return state.getDestroySpeed(level, pos)
    }

    /**
     * R196 BlockHardness 表中与原版 1.21.11 差异显著的核心方块（path → R196 硬度）。
     * 数值来源：BlockHardness.java（obsidian=8.0/netherrack=4.0/glowStone=3.0/web=0.1/
     * glass=0.1/netherBrick=4.0/sandStone=1.0/furnace=1.5/brick=1.5/stoneBrick=2.0/
     * blockClay=1.0/blockIce=log(1.0)/oreCoal=1.5/oreGold·Diamond·Emerald=2.5/
     * oreLapis·Redstone=2.0/oreIron=3.0/oreCopper·Silver=2.5）。
     * 原版矿石（1.21.11）统一 3.0，R196 更低——影响耐久消耗但幅度有限，一并覆盖以忠实 R196。
     */
    private val R196_HARDNESS: Map<String, Float> = mapOf(
        "obsidian" to 8.0f,
        "netherrack" to 4.0f,
        "glowstone" to 3.0f,
        "nether_bricks" to 4.0f,
        "red_nether_bricks" to 4.0f,
        "cobweb" to 0.1f,
        "glass" to 0.1f,
        "glass_pane" to 0.02f,
        "sandstone" to 1.0f,
        "red_sandstone" to 1.0f,
        "furnace" to 1.5f,
        "bricks" to 1.5f,
        "stone_bricks" to 2.0f,
        "clay" to 1.0f,
        "ice" to 1.0f,
        "coal_ore" to 1.5f,
        "deepslate_coal_ore" to 1.5f,
        "iron_ore" to 3.0f,
        "deepslate_iron_ore" to 3.0f,
        "gold_ore" to 2.5f,
        "deepslate_gold_ore" to 2.5f,
        "diamond_ore" to 2.5f,
        "deepslate_diamond_ore" to 2.5f,
        "emerald_ore" to 2.5f,
        "deepslate_emerald_ore" to 2.5f,
        "lapis_ore" to 2.0f,
        "deepslate_lapis_ore" to 2.0f,
        "redstone_ore" to 2.0f,
        "deepslate_redstone_ore" to 2.0f,
        "copper_ore" to 2.5f,
        "deepslate_copper_ore" to 2.5f
    )

    private fun isWoodenFamily(state: BlockState): Boolean {
        return state.`is`(BlockTags.WOODEN_FENCES) ||
                state.`is`(BlockTags.FENCE_GATES) ||
                state.`is`(BlockTags.WOODEN_DOORS) ||
                state.`is`(BlockTags.WOODEN_TRAPDOORS) ||
                state.`is`(BlockTags.WOODEN_PRESSURE_PLATES) ||
                state.`is`(BlockTags.WOODEN_BUTTONS) ||
                state.`is`(BlockTags.WOODEN_STAIRS) ||
                state.`is`(BlockTags.WOODEN_SLABS)
    }

    /**
     * 草本/植被判定：草块、高草丛、蕨、大蕨、灌木、死灌木、花（含高花）、树苗、
     * 蘑菇、甜浆果丛、下界菌/ sprouts、垂泪藤/扭藤/垂根等。
     * 这些方块在 ICPM 中硬度≈0，用镰刀/剑割除几乎不耗耐久。
     * 用注册表路径匹配（避免依赖 Blocks.* 静态字段在部分映射下的解析问题）。
     */
    private val FOLIAGE_PATHS = setOf(
        "grass", "tall_grass", "fern", "large_fern", "bush", "dead_bush",
        "red_mushroom", "brown_mushroom", "sweet_berry_bush",
        "warped_fungus", "crimson_fungus", "nether_sprouts",
        "weeping_vines", "twisting_vines", "hanging_roots"
    )

    private fun isFoliage(state: BlockState): Boolean {
        val path = BuiltInRegistries.BLOCK.getKey(state.block).path
        return path in FOLIAGE_PATHS ||
                state.`is`(BlockTags.SAPLINGS) ||
                state.`is`(BlockTags.FLOWERS) ||
                state.`is`(BlockTags.SMALL_FLOWERS)
    }
}
