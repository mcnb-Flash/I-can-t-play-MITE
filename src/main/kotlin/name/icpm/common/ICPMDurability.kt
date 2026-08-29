package name.icpm.common

import kotlin.math.ceil

/**
 * ICPM耐久系统（Kotlin版）
 * 基于 1.6.4-ICPM R196 反编译源码
 *
 * 核心公式：
 * - 工具总耐久 = 4 × 部件数 × 材质耐久系数 × 100（普通品质）
 * - 护甲总耐久 = 部件数 × 材质耐久系数 × 2（普通品质，锁甲不乘2）
 *
 * 衰减消耗：
 * - 挖方块: max(int(方块硬度 × 100 × 衰减率), int((100×衰减率)/20), 1)
 * - 攻击: max(int(100 × 衰减率), 1)
 */
object ICPMDurability {

    /**
     * 材质耐久系数（来自 EnumEquipmentMaterial）
     *
     * ICPM R196 中所有材质按耐久系数排序：
     * leather(0.4) < wood(0.5) < flint(1.0) < obsidian(2.0) < glass(2.0)
     * < copper(4) < silver(4) < gold(4) < rusted_iron(4) < netherrack(4) < quartz(4)
     * < iron(8) < emerald(8) < ancient_metal(16) < diamond(16)
     * < mithril(64) < adamantium(256)
     */
    enum class Material(val durabilityFactor: Float, val enchantability: Int) {
        LEATHER(0.4f, 15),
        WOOD(0.5f, 10),
        FLINT(1.0f, 0),
        OBSIDIAN(2.0f, 0),
        GLASS(2.0f, 0),
        COPPER(4.0f, 30),
        SILVER(4.0f, 30),
        GOLD(4.0f, 50),
        RUSTED_IRON(4.0f, 0),
        NETHERRACK(4.0f, 0),
        QUARTZ(4.0f, 40),
        IRON(8.0f, 30),
        EMERALD(8.0f, 70),
        ANCIENT_METAL(16.0f, 40),
        DIAMOND(16.0f, 100),
         MITHRIL(64.0f, 100),
        ADAMANTIUM(256.0f, 40),
        NETHERITE(256.0f, 15);

        fun getFactor(): Float = durabilityFactor
        fun getEnchant(): Int = enchantability
    }

    /**
     * 工具类型（部件数 + 衰减率）
     */
    enum class ToolType(
        val components: Int,
        val blockDecayRate: Float,
        val attackDecayRate: Float,
        val baseAttackDamage: Float
    ) {
        PICKAXE(3, 1.0f, 1.0f, 2.0f),
        SHOVEL(1, 0.5f, 1.0f, 0.0f),
        AXE(3, 1.0f, 1.0f, 0.0f),
        HOE(2, 1.0f, 1.0f, 0.0f),
        SWORD(2, 1.0f, 1.0f, 4.0f),
        DAGGER(1, 1.0f, 1.0f, 0.0f),
        KNIFE(1, 0.5f, 1.0f, 0.0f),
        HATCHET(1, 1.333f, 1.333f, 0.0f),
        CLUB(2, 1.0f, 1.0f, 0.0f),
        CUDGEL(1, 0.25f, 0.25f, 0.0f),
        WAR_HAMMER(5, 0.667f, 0.667f, 0.0f),
        BATTLE_AXE(4, 1.25f, 0.75f, 0.0f),
        SCYTHE(2, 2.0f, 4.0f, 1.0f),
        MATTOCK(4, 0.8f, 1.0f, 0.0f),
        SPEAR(3, 1.0f, 1.0f, 3.0f);
    }

    /**
     * 护甲部件数
     */
    enum class ArmorPart(val components: Int) {
        HELMET(5),
        CHESTPLATE(8),
        LEGGINGS(7),
        BOOTS(4);
    }

    /**
     * 计算工具耐久上限
     * 公式：4 × 部件数 × 材质耐久系数 × 100
     */
    @JvmStatic
    fun calculateToolDurability(type: ToolType, material: Material): Int {
        return (4 * type.components * material.durabilityFactor * 100).toInt()
    }

    /**
     * 计算护甲耐久上限
     * 公式：部件数 × 材质耐久系数 × 2
     */
    @JvmStatic
    fun calculateArmorDurability(part: ArmorPart, material: Material): Int {
        return (part.components * material.durabilityFactor * 2).toInt()
    }

    /**
     * 挖方块耐久消耗公式
     *
     * 注意：fromHardness 使用天花板取整（ceil）而非截断。
     * 理由：ICPM R196 原版用 (int) 截断，对低耐久工具（如燧石短斧：400 耐久、原木每
     * 块 133）会产生 "挖 3 块原木耗 399、残留 1 点耐久" 的失衡现象。改为 ceiling 后
     * 133.3 → 134，燧石短斧挖 3 块原木（402 > 400）后耐久恰好耗尽、工具损坏，消除残
     * 留。对所有工具也更精确（损耗按整数向上计入）。
     */
    @JvmStatic
    fun calculateBlockDecay(blockHardness: Float, decayRate: Float): Int {
        val fromHardness = ceil(blockHardness * 100 * decayRate).toInt()
        val baseline = ((100 * decayRate) / 20).toInt()
        return maxOf(maxOf(fromHardness, baseline), 1)
    }

    /**
     * 攻击耐久消耗公式
     */
    @JvmStatic
    fun calculateAttackDecay(decayRate: Float): Int {
        return maxOf((100 * decayRate).toInt(), 1)
    }
}
