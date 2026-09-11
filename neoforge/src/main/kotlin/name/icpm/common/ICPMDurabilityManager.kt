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

    // 注：本对象原有的 ToolParts / ArmorParts / AttackDecayRate 嵌套对象及
    // calculateBlockBreakDamage / calculateEntityAttackDamage / getQualityMultiplier /
    // getUnbreakingReduction 方法为早期自创实现，与 name.icpm.common.ICPMDurability 重复
    // 且数值与 R196 不符，已于 2026-09-11 v6 审计后删除（全项目零引用）。
    // 运行时耐久与衰减一律走 ICPMDurability。

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
}