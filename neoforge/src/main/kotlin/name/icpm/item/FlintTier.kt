package name.icpm.item

import net.minecraft.world.item.ToolMaterial
import net.minecraft.tags.BlockTags

/**
 * ICPM 工具材质定义
 *
 * 数值严格基于 ICPM R196 反编译源文件:
 *   - akc.getDamageVsEntity() — 材质基础伤害
 *   - EnumEquipmentMaterial.durability — 材质耐久系数
 *   - xj.getMultipliedDurability() — 工具总耐久公式: 4 × 部件数 × 材质耐久系数 × 100
 *
 * 在 1.21.11 中，ToolMaterial.attackDamageBonus 对应 R196 材质的伤害加成
 * 实际工具的最大耐久则在 ICPMItems.kt 中通过 .durability() 覆盖
 */

// ========== 燧石材质 ==========
// R196 EnumEquipmentMaterial.flint: durability=1.0, enchantability=0
// R196 akc.getDamageVsEntity() = 1.0f
val FLINT_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_WOODEN_TOOL,
    100,  // 默认耐久（实际值由 .durability() 覆盖）
    5.0f, // R196: getBaseHarvestEfficiency(4.0f) * getMaterialHarvestEfficiency(1.25f) = 5.0f
    1.0f,  // R196 flint: getDamageVsEntity() = 1.0f
    5,     // 附魔能力
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "flint"))
)

// ========== 铜材质 ==========
// R196 EnumEquipmentMaterial.copper: durability=4.0, enchantability=30
// R196 akc.getDamageVsEntity() = 3.0f（与银完全一致）
// R196 ToolMaterialHarvestEfficiency.copper = 1.75 → 4.0 × 1.75 = 7.0f
val COPPER_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_IRON_TOOL,
    760,    // 默认耐久
    7.0f,   // R196: 4.0f * getMaterialHarvestEfficiency(1.75f) = 7.0f
    3.0f,   // R196 copper: getDamageVsEntity() = 3.0f
    30,     // R196 copper: enchantability = 30
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "copper_tool_materials"))
)

// ========== 金材质 ==========
// R196 EnumEquipmentMaterial.gold: durability=4.0, enchantability=50
// R196 akc.getDamageVsEntity() = 2.0f
// R196 ToolMaterialHarvestEfficiency.gold = 1.75 → 4.0 × 1.75 = 7.0f
// 最小采集等级 2（同银/铜），使用原版金锭修复（minecraft:gold_tool_materials）
val GOLD_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_IRON_TOOL,
    760,    // 默认耐久
    7.0f,   // R196: 4.0f * getMaterialHarvestEfficiency(1.75f) = 7.0f
    2.0f,   // R196 gold: getDamageVsEntity() = 2.0f
    50,     // R196 gold: enchantability = 50
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "gold_tool_materials"))
)

// ========== 银材质 ==========
// R196 EnumEquipmentMaterial.silver: durability=4.0, enchantability=30
// R196 akc.getDamageVsEntity() = 3.0f
val SILVER_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_IRON_TOOL,
    1600,   // 默认耐久
    7.0f,   // R196: 4.0f * getMaterialHarvestEfficiency(1.75f) = 7.0f
    3.0f,   // R196 silver: getDamageVsEntity() = 3.0f
    30,     // R196 silver: enchantability = 30
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("icpm", "silver_ingot"))
)

// ========== 铁材质 ==========
// R196 EnumEquipmentMaterial.iron: durability=8.0, enchantability=30
// R196 akc.getDamageVsEntity() = 4.0f
// R196 ToolMaterialHarvestEfficiency.iron = 2.0 → 4.0 × 2.0 = 8.0f
val IRON_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_IRON_TOOL,
    3200,   // 默认耐久
    8.0f,   // R196: 4.0f * getMaterialHarvestEfficiency(2.0f) = 8.0f
    4.0f,   // R196 iron: getDamageVsEntity() = 4.0f
    30,     // R196 iron: enchantability = 30
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "iron_tool_materials"))
)

// ========== 远古金属材质 ==========
// R196 EnumEquipmentMaterial.ancient_metal: durability=16.0, enchantability=40
// R196 akc.getDamageVsEntity() = 4.0f
val ANCIENT_METAL_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_IRON_TOOL,
    6400,   // 默认耐久
    8.0f,   // R196: 4.0f * getMaterialHarvestEfficiency(2.0f) = 8.0f
    4.0f,   // R196 ancient_metal: getDamageVsEntity() = 4.0f
    40,     // R196 ancient_metal: enchantability = 40
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("icpm", "ancient_metal_ingot"))
)

// ========== 秘银材质 ==========
// R196 EnumEquipmentMaterial.mithril: durability=64.0, enchantability=100
// R196 akc.getDamageVsEntity() = 5.0f
val MITHRIL_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
    25600,  // 默认耐久
    10.0f,  // R196: 4.0f * getMaterialHarvestEfficiency(2.5f) = 10.0f
    5.0f,   // R196 mithril: getDamageVsEntity() = 5.0f
    100,    // R196 mithril: enchantability = 100
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("icpm", "mithril_ingot"))
)

// ========== 艾德曼材质 ==========
// R196 EnumEquipmentMaterial.adamantium: durability=256.0, enchantability=40
// R196 akc.getDamageVsEntity() = 6.0f
val ADAMANTIUM_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
    102400, // 默认耐久
    12.0f,  // R196: 4.0f * getMaterialHarvestEfficiency(3.0f) = 12.0f
    6.0f,   // R196 adamantium: getDamageVsEntity() = 6.0f
    40,     // R196 adamantium: enchantability = 40
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("icpm", "adamantium_ingot"))
)

// ========== 下界合金材质 ==========
// 艾德曼 6.0 -> 下界合金 7.0，延续材质伤害阶梯 (flint1/copper3/gold2/silver3/ancient4/mithril5/adamantium6)
// 挖掘等级 6 (ICPM 顶级，可挖下界合金块)，strVsBlock 10.0 (mixin 中)
// 修复用下界合金锭 (minecraft:netherite_tool_materials)
val NETHERITE_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
    2031,   // 默认耐久（实际值由 .durability() 覆盖）
    12.0f,  // 效率
    7.0f,   // R196 风格: 材质伤害加成，比艾德曼 6.0 高一级
    40,     // 附魔能力（同艾德曼）
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "netherite_tool_materials"))
)

// ========== 特殊工具材质 (无材质伤害加成) ==========
// 用于 Cudgel/Scythe 等 R196 源文件中不应用材质伤害的工具
// R196 xj.getMaterialDamageVsEntity() 对部分工具不会被使用
val CUDGEL_TIER: ToolMaterial = ToolMaterial(
    BlockTags.INCORRECT_FOR_WOODEN_TOOL,
    100,
    1.0f,
    0.0f,   // 无材质伤害加成
    5,
    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "stick"))
)
