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
     * R196 权威表（EnumEquipmentMaterial.java:9-25）：
     * leather(1.0,10) wood(0.5,10) flint(1.0,0) obsidian(2.0,0) glass(2.0,0)
     * copper(4,30) silver(4,30) gold(4,50) rusted_iron(4,0) netherrack(4,0) quartz(4,0)
     * iron(8,30) emerald(8,70) ancient_metal(16,40) diamond(16,100)
     * mithril(64,100) adamantium(256,40)
     */
    enum class Material(val durabilityFactor: Float, val enchantability: Int) {
        LEATHER(1.0f, 10),
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
     *
     * 衰减率逐项核对 R196 源码（getBaseDecayRateForBreakingBlock / getBaseDecayRateForAttackingEntity）：
     * - ItemPickaxe 1.0/1.0，ItemShovel 0.5/1.0，ItemAxe 1.0/1.0（砂岩特例 1.875 在 ToolProperties 按 state 处理）
     * - ItemHoe 2.0/2.0，ItemSword 2.0/0.5
     * - ItemDagger 继承剑 2.0/0.5；ItemKnife = 剑÷2（作物 ÷4，特例在 ToolProperties）→ 1.0/0.5
     * - ItemCudgel 0.25/0.25；ItemClub 继承短棒 0.25/0.25
     * - ItemWarHammer = 镐×2/3 ≈ 0.667/0.667；ItemBattleAxe = 斧×1.25/×0.75 → 1.25/0.75
     * - ItemHatchet = 斧×4/3 ≈ 1.333/1.333；ItemMattock = 铲×0.8 = 0.4/1.0
     * - ItemScythe 2.0/4.0（割草 0.5、附魔减免特例在 ToolProperties）
     */
    enum class ToolType(
        val components: Int,
        val blockDecayRate: Float,
        val attackDecayRate: Float
    ) {
        PICKAXE(3, 1.0f, 1.0f),
        SHOVEL(1, 0.5f, 1.0f),
        AXE(3, 1.0f, 1.0f),
        HOE(2, 2.0f, 2.0f),
        SWORD(2, 2.0f, 0.5f),
        DAGGER(1, 2.0f, 0.5f),
        KNIFE(1, 1.0f, 0.5f),
        HATCHET(1, 1.333f, 1.333f),
        CLUB(2, 0.25f, 0.25f),
        CUDGEL(1, 0.25f, 0.25f),
        WAR_HAMMER(5, 0.667f, 0.667f),
        BATTLE_AXE(4, 1.25f, 0.75f),
        SCYTHE(2, 2.0f, 4.0f),
        MATTOCK(4, 0.4f, 1.0f),
        SPEAR(3, 1.0f, 1.0f);
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
