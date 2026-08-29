package name.icpm.item

import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.ToolMaterial
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.block.Block

/**
 * ICPM 材质系统
 * 
 * 耐久计算公式：
 * 工具总耐久 = 4 × 部件数 × 材质耐久系数 × 100
 * 护甲总耐久 = 部件数 × 材质耐久系数 × 2（锁甲不乘2）
 * 
 * 材质耐久系数：
 * - 燧石：1
 * - 铜：2
 * - 银：4
 * - 铁：8
 * - 远古金属：16
 * - 秘银：64
 * - 艾德曼：256
 */
object ICPMTiers {
    
    // ========== 材质耐久系数 ==========
    const val FLINT_DURABILITY_FACTOR: Int = 1
    const val COPPER_DURABILITY_FACTOR: Int = 2
    const val SILVER_DURABILITY_FACTOR: Int = 4
    const val IRON_DURABILITY_FACTOR: Int = 8
    const val ANCIENT_METAL_DURABILITY_FACTOR: Int = 16
    const val MITHRIL_DURABILITY_FACTOR: Int = 64
    const val ADAMANTIUM_DURABILITY_FACTOR: Int = 256
    
    // ========== 工具部件数 (基于R196 yj.getNumComponentsForDurability()) ==========
    const val PARTS_PICKAXE: Int = 3
    const val PARTS_SHOVEL: Int = 1
    const val PARTS_AXE: Int = 3
    const val PARTS_HOE: Int = 2
    const val PARTS_SWORD: Int = 2
    
    // ========== 护甲部件数 ==========
    const val PARTS_HELMET: Int = 5
    const val PARTS_CHESTPLATE: Int = 8
    const val PARTS_LEGGINGS: Int = 7
    const val PARTS_BOOTS: Int = 4
    
    /**
     * 计算工具耐久
     */
    fun calculateToolDurability(parts: Int, durabilityFactor: Int): Int {
        return 4 * parts * durabilityFactor * 100
    }
    
    /**
     * 计算护甲耐久
     */
    fun calculateArmorDurability(parts: Int, durabilityFactor: Int, isChainmail: Boolean = false): Int {
        val base = parts * durabilityFactor * 2
        return if (isChainmail) base / 2 else base
    }
}