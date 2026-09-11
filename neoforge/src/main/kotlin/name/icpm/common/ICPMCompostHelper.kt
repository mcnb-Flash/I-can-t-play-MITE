package name.icpm.common

import name.icpm.item.ICPMItems
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.BlockTags
import net.minecraft.util.RandomSource
import net.minecraft.world.Containers
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * ICPM 虫堆肥助手（1.6.4 TileEntityChest.checkForWormComposting / Item.getCompostingValue 移植）
 *
 * 机制：
 * 1. 箱子堆肥：箱子里有活虫（worm）时，开/关箱子会触发堆肥——虫子随机消耗箱内
 *    可堆肥的植物类物品（堆肥值>0），compost 累计满 1.0 时产出 1 个粪便（manure）。
 * 2. 植物判定：手持虫子右键草/树叶/花/树苗/作物/草方块等植物方块 → 消耗植物产出粪便
 *    （见 IcpmWormItem）。
 */
object ICPMCompostHelper {

    /** 显式堆肥值（1.6.4 Item.getCompostingValue 特殊值；值越高，每 tick 被虫吃的概率越高） */
    private val SPECIAL_COMPOST_VALUES = mapOf(
        "icpm:flour" to 0.8f,
        "minecraft:wheat" to 0.5f,
        "minecraft:paper" to 0.1f,
        // 1.6.4：活虫不吃自己（wormRaw=0）；烤虫可堆肥（wormCooked=0.1）
        "icpm:worm" to 0.0f,
        "icpm:cooked_worm" to 0.1f,
        // 1.6.4：鸡蛋/牛奶碗/金苹果不可堆肥
        "minecraft:egg" to 0.0f,
        "minecraft:golden_apple" to 0.0f,
        "minecraft:enchanted_golden_apple" to 0.0f,
        "icpm:milk_bowl" to 0.0f
    )

    /** 明确不可堆肥的物品（1.6.4 ItemSeeds = 0：各类种子不吃） */
    private val NON_COMPOSTABLE = setOf(
        "minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:melon_seeds",
        "minecraft:pumpkin_seeds", "minecraft:torchflower_seeds", "minecraft:pitcher_pod",
        "minecraft:dead_bush"
    )

    /** 虫子可吃的植物方块（手持虫右键） */
    fun isPlantBlockForWorm(state: BlockState): Boolean {
        if (state.`is`(Blocks.GRASS_BLOCK)) return true
        return state.`is`(BlockTags.SAPLINGS) || state.`is`(BlockTags.LEAVES)
                || state.`is`(BlockTags.FLOWERS) || state.`is`(BlockTags.CROPS)
                || state.`is`(BlockTags.REPLACEABLE_BY_TREES)
    }

    /** 物品堆肥值（0.0 = 不可堆肥；>0 可被虫子吃） */
    fun getCompostingValue(stack: ItemStack): Float {
        if (stack.isEmpty) return 0f
        val id = BuiltInRegistries.ITEM.getKey(stack.item)?.toString() ?: return 0f
        SPECIAL_COMPOST_VALUES[id]?.let { return it }
        if (id in NON_COMPOSTABLE) return 0f
        // 默认：可食用物品 → nutrition * 0.1（1.6.4 默认 (satiation + nutrition) * 0.1 的近似）
        val foodProperties = stack.get(DataComponents.FOOD) ?: return 0f
        return foodProperties.nutrition() * 0.1f
    }

    /**
     * 箱子堆肥（1.6.4 TileEntityChest.checkForWormComposting）
     * 开/关箱子时调用：每只活虫尝试吃一份随机可堆肥物品。
     *
     * @param compost 当前堆肥进度（0.0 - 0.99，由调用方持久化）
     * @return 处理后的新堆肥进度
     */
    @JvmStatic
    fun tryCompostChest(chest: ChestBlockEntity, compost: Float): Float {
        val level = chest.level ?: return compost
        if (level.isClientSide) return compost

        var worms = 0
        for (i in 0 until chest.containerSize) {
            val s = chest.getItem(i)
            if (s.item == ICPMItems.WORM) worms += s.count
        }
        if (worms < 1) return compost

        var c = compost
        var attempts = worms
        while (attempts-- > 0) {
            val idx = indexOfRandomWormFood(chest, level.random)
            if (idx < 0) break
            val food = chest.getItem(idx)
            val value = getCompostingValue(food)
            if (value <= 0f) break
            val chanceIn = (100f * value).toInt()
            if (chanceIn <= 0) break
            // 概率吃（1.6.4：nextInt(chance_in) == 0 才成功）
            if (level.random.nextInt(chanceIn) != 0) continue
            // 空间检查：粪便必须能放下（无空槽则失败）
            if (!canAcceptManure(chest)) continue
            chest.removeItem(idx, 1)
            c += value
            if (c >= 1f) {
                c = convertCompost(level, chest, c)
            }
        }
        return c
    }

    /** 找一个随机可堆肥物品的槽位（1.6.4 getIndexOfRandomWormFood） */
    private fun indexOfRandomWormFood(chest: ChestBlockEntity, random: RandomSource): Int {
        val candidates = ArrayList<Int>()
        for (i in 0 until chest.containerSize) {
            val s = chest.getItem(i)
            if (!s.isEmpty && getCompostingValue(s) > 0f) candidates.add(i)
        }
        if (candidates.isEmpty()) return -1
        return candidates[random.nextInt(candidates.size)]
    }

    /** 是否有槽位可放入粪便（有粪堆叠或空槽） */
    private fun canAcceptManure(chest: ChestBlockEntity): Boolean {
        for (i in 0 until chest.containerSize) {
            val s = chest.getItem(i)
            if (s.isEmpty) return true
            if (s.item == ICPMItems.MANURE && s.count < s.maxStackSize) return true
        }
        return false
    }

    /** 把满 1.0 的 compost 转成粪便（1.6.4 convertAsMuchCompostAsPossible） */
    private fun convertCompost(level: net.minecraft.world.level.Level, chest: ChestBlockEntity, compost: Float): Float {
        var c = compost
        while (c >= 1f) {
            val manure = ItemStack(ICPMItems.MANURE)
            var placed = false
            for (i in 0 until chest.containerSize) {
                val s = chest.getItem(i)
                if (s.isEmpty) {
                    chest.setItem(i, manure)
                    placed = true
                    break
                }
                if (s.item == ICPMItems.MANURE && s.count < s.maxStackSize) {
                    s.grow(1)
                    placed = true
                    break
                }
            }
            if (!placed) {
                val p = chest.getBlockPos()
                Containers.dropItemStack(level, p.x + 0.5, p.y + 0.5, p.z + 0.5, manure)
            }
            c -= 1f
        }
        return c
    }
}
