package name.icpm.common

import net.minecraft.world.item.ItemStack

/**
 * ICPM 耐久管理器
 *
 * 工具耐久是"点数制"，不是"次数制"
 * 平均品质时：工具总耐久 = 4 × 部件数 × 材质耐久系数 × 100
 */
object ICPMDurabilityManager {

    /**
     * 材质耐久系数（基于ICPM R196 EnumEquipmentMaterial.durability）
     */
    object MaterialDurability {
        const val FLINT = 1.0f
        const val COPPER = 4.0f
        const val SILVER = 4.0f
        const val IRON = 8.0f
        const val GOLD = 4.0f
        const val ANCIENT_METAL = 16.0f
        const val MITHRIL = 64.0f
        const val ADAMANTIUM = 256.0f
        const val DIAMOND = 16.0f
        const val NETHERITE = 256.0f
    }

    /**
     * 工具部件数（基于ICPM R196 yj.getNumComponentsForDurability()）
     */
    object ToolParts {
        const val PICKAXE = 3
        const val AXE = 3
        const val SHOVEL = 1
        const val HOE = 2
        const val SWORD = 2
        const val HATCHET = 1
        const val BATTLE_AXE = 4
        const val WAR_HAMMER = 5
        const val MATTOCK = 4
        const val SCYTHE = 2
        const val DAGGER = 1
        const val KNIFE = 1
        const val CUDGEL = 1
    }

    /**
     * 护甲部件数（基于ICPM R196）
     * 
     * 验证公式：护甲总耐久 = 部件数 × 材质耐久系数 × 2
     * 铁材质系数 = 8.0
     * 
     * 铁套实际耐久：
     * - 头盔 80 = 5 × 8 × 2
     * - 胸甲 128 = 8 × 8 × 2
     * - 护腿 112 = 7 × 8 × 2
     * - 靴子 64 = 4 × 8 × 2
     * 
     * 铁锁甲对应减半：40/64/56/32
     */
    object ArmorParts {
        const val HELMET = 5
        const val CHESTPLATE = 8
        const val LEGGINGS = 7
        const val BOOTS = 4
    }

    /**
     * 工具攻击衰减率
     */
    object AttackDecayRate {
        const val SWORD = 0.5f
        const val DAGGER = 0.5f
        const val KNIFE = 0.5f
        const val PICKAXE = 1.0f
        const val AXE = 1.0f
        const val SHOVEL = 1.0f
        const val HOE = 2.0f
        const val BATTLE_AXE = 0.75f
        const val WAR_HAMMER = 0.66f
        const val HATCHET = 1.33f
        const val SHEARS = 2.0f
        const val CUDGEL = 0.25f
        const val SCYTHE = 4.0f
    }

    /**
     * 计算工具总耐久（平均品质）
     * 公式：4 × 部件数 × 材质耐久系数 × 100
     */
    @JvmStatic
    fun calculateToolDurability(parts: Int, materialDurability: Float): Int {
        return (4 * parts * materialDurability * 100).toInt()
    }

    /**
     * 计算护甲总耐久（平均品质）
     * 公式：部件数 × 材质耐久系数 × 2（锁甲不乘2）
     */
    @JvmStatic
    fun calculateArmorDurability(parts: Int, materialDurability: Float, isChainmail: Boolean = false): Int {
        val multiplier = if (isChainmail) 1 else 2
        return (parts * materialDurability * multiplier).toInt()
    }

    /**
     * 计算挖方块耐久消耗
     * 公式：max(ceil(方块硬度 × 100 × 工具衰减率), int((100×衰减率)/20), 1)
     * 与 ICPMDurability.calculateBlockDecay 一致：fromHardness 向上取整，避免低耐久工具残留 1 点耐久。
     */
    @JvmStatic
    fun calculateBlockBreakDamage(blockHardness: Float, toolDecayRate: Float): Int {
        val damage1 = kotlin.math.ceil(blockHardness * 100 * toolDecayRate).toInt()
        val damage2 = ((100 * toolDecayRate) / 20).toInt()
        return maxOf(damage1, damage2, 1)
    }

    /**
     * 计算打实体耐久消耗
     * 公式：max(int(100 × 攻击衰减率), 1)
     */
    @JvmStatic
    fun calculateEntityAttackDamage(attackDecayRate: Float): Int {
        return maxOf((100 * attackDecayRate).toInt(), 1)
    }

    /**
     * 计算修复量（使用金属粒）
     * 工具：200 × 材质系数
     * 护甲：材质系数
     */
    @JvmStatic
    fun calculateRepairAmount(materialDurability: Float, isArmor: Boolean): Int {
        return if (isArmor) {
            materialDurability.toInt()
        } else {
            (200 * materialDurability).toInt()
        }
    }

    /**
     * 计算品质倍率
     * 劣质0.5、差0.75、普通1.0、精良1.5、优秀2.0、卓越2.5、大师3.0、传奇3.5
     */
    @JvmStatic
    fun getQualityMultiplier(quality: Int): Float {
        return when (quality) {
            0 -> 0.5f   // 劣质
            1 -> 0.75f  // 差
            2 -> 1.0f   // 普通
            3 -> 1.5f   // 精良
            4 -> 2.0f   // 优秀
            5 -> 2.5f   // 卓越
            6 -> 3.0f   // 大师
            7 -> 3.5f   // 传奇
            else -> 1.0f
        }
    }

    /**
     * 计算耐久附魔减免概率
     * 每级额外免损15%，5级是75%免损
     */
    @JvmStatic
    fun getUnbreakingReduction(unbreakingLevel: Int): Float {
        return minOf(unbreakingLevel * 0.15f, 0.75f)
    }
}