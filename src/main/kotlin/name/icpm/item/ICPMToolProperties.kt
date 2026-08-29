package name.icpm.item

import name.icpm.common.ICPMDurability
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * ICPM工具属性管理
 * 通过 Item 类的 instance key 缓存衰减率信息
 */
object ICPMToolProperties {

    /**
     * 工具类型枚举（对应 ICPMDurability.ToolType）
     */
    enum class ToolCategory {
        PICKAXE, SHOVEL, AXE, HOE, SWORD,
        DAGGER, KNIFE, HATCHET, CLUB, CUDGEL,
        WAR_HAMMER, BATTLE_AXE, SCYTHE, MATTOCK, SPEAR
    }

    /**
     * 材质枚举（对应 ICPMDurability.Material）
     */
    enum class ToolMaterial {
        LEATHER, WOOD, FLINT, COPPER, SILVER, GOLD, IRON,
        ANCIENT_METAL, MITHRIL, ADAMANTIUM, DIAMOND, NETHERITE
    }

    private val TOOL_TYPE_MAP: Map<ToolCategory, ICPMDurability.ToolType> = mapOf(
        ToolCategory.PICKAXE to ICPMDurability.ToolType.PICKAXE,
        ToolCategory.SHOVEL to ICPMDurability.ToolType.SHOVEL,
        ToolCategory.AXE to ICPMDurability.ToolType.AXE,
        ToolCategory.HOE to ICPMDurability.ToolType.HOE,
        ToolCategory.SWORD to ICPMDurability.ToolType.SWORD,
        ToolCategory.DAGGER to ICPMDurability.ToolType.DAGGER,
        ToolCategory.KNIFE to ICPMDurability.ToolType.KNIFE,
        ToolCategory.HATCHET to ICPMDurability.ToolType.HATCHET,
        ToolCategory.CLUB to ICPMDurability.ToolType.CLUB,
        ToolCategory.CUDGEL to ICPMDurability.ToolType.CUDGEL,
        ToolCategory.WAR_HAMMER to ICPMDurability.ToolType.WAR_HAMMER,
        ToolCategory.BATTLE_AXE to ICPMDurability.ToolType.BATTLE_AXE,
        ToolCategory.SCYTHE to ICPMDurability.ToolType.SCYTHE,
        ToolCategory.MATTOCK to ICPMDurability.ToolType.MATTOCK,
        ToolCategory.SPEAR to ICPMDurability.ToolType.SPEAR
    )

    // 所有工具类型名称（用于遍历）
    // 小刀族仅燧石/黑曜石，短棍族仅木质，故从遍历列表中移除 knife/cudgel
    private val TOOL_NAMES = listOf(
        "pickaxe", "shovel", "axe", "hoe", "sword",
        "dagger", "hatchet", "war_hammer", "battle_axe", "scythe", "mattock", "spear"
    )

    // 工具类型映射（物品ID -> ToolCategory）
    private val ITEM_TOOL_TYPE: MutableMap<String, ToolCategory> = mutableMapOf()

    // 材质映射（物品ID -> ToolMaterial）
    private val ITEM_MATERIAL: MutableMap<String, ToolMaterial> = mutableMapOf()

    init {
        // ========== 燧石系工具 ==========
        ITEM_TOOL_TYPE["flint_knife"] = ToolCategory.KNIFE
        ITEM_TOOL_TYPE["obsidian_knife"] = ToolCategory.KNIFE
        ITEM_TOOL_TYPE["flint_shovel"] = ToolCategory.SHOVEL
        ITEM_TOOL_TYPE["flint_hatchet"] = ToolCategory.HATCHET
        ITEM_TOOL_TYPE["flint_axe"] = ToolCategory.AXE

        ITEM_MATERIAL["flint_knife"] = ToolMaterial.FLINT
        ITEM_MATERIAL["obsidian_knife"] = ToolMaterial.FLINT
        ITEM_MATERIAL["flint_shovel"] = ToolMaterial.FLINT
        ITEM_MATERIAL["flint_hatchet"] = ToolMaterial.FLINT
        ITEM_MATERIAL["flint_axe"] = ToolMaterial.FLINT

        // ========== 原版木制工具 (Minecraft原版) ==========
        // 必须注册进 ICPM 耐久体系：否则挖掘衰减会因未命中材质映射退避 blockDecay=1.0、
        // 而 getMaxDamage 仍用原版 59 → 一挖即坏。注册后按 WOOD 材质公式赋予耐久并走 ICPM 衰减。
        // 公式：4 × 部件数 × WOOD(0.5) × 100 → 木铲=200、木镐/斧=600、木锄/剑=400。
        ITEM_TOOL_TYPE["wooden_pickaxe"] = ToolCategory.PICKAXE
        ITEM_TOOL_TYPE["wooden_axe"] = ToolCategory.AXE
        ITEM_TOOL_TYPE["wooden_shovel"] = ToolCategory.SHOVEL
        ITEM_TOOL_TYPE["wooden_hoe"] = ToolCategory.HOE
        ITEM_TOOL_TYPE["wooden_sword"] = ToolCategory.SWORD

        ITEM_MATERIAL["wooden_pickaxe"] = ToolMaterial.WOOD
        ITEM_MATERIAL["wooden_axe"] = ToolMaterial.WOOD
        ITEM_MATERIAL["wooden_shovel"] = ToolMaterial.WOOD
        ITEM_MATERIAL["wooden_hoe"] = ToolMaterial.WOOD
        ITEM_MATERIAL["wooden_sword"] = ToolMaterial.WOOD

        // ========== 原版铜工具 (Minecraft原版) ==========
        ITEM_TOOL_TYPE["copper_pickaxe"] = ToolCategory.PICKAXE
        ITEM_TOOL_TYPE["copper_axe"] = ToolCategory.AXE
        ITEM_TOOL_TYPE["copper_shovel"] = ToolCategory.SHOVEL
        ITEM_TOOL_TYPE["copper_hoe"] = ToolCategory.HOE
        ITEM_TOOL_TYPE["copper_sword"] = ToolCategory.SWORD

        ITEM_MATERIAL["copper_pickaxe"] = ToolMaterial.COPPER
        ITEM_MATERIAL["copper_axe"] = ToolMaterial.COPPER
        ITEM_MATERIAL["copper_shovel"] = ToolMaterial.COPPER
        ITEM_MATERIAL["copper_hoe"] = ToolMaterial.COPPER
        ITEM_MATERIAL["copper_sword"] = ToolMaterial.COPPER

        // ========== 铜制特殊工具 (战斧/战锤/鸭嘴锄/短斧/匕首/镰刀) ==========
        ITEM_TOOL_TYPE["copper_battle_axe"] = ToolCategory.BATTLE_AXE
        ITEM_TOOL_TYPE["copper_war_hammer"] = ToolCategory.WAR_HAMMER
        ITEM_TOOL_TYPE["copper_mattock"] = ToolCategory.MATTOCK
        ITEM_TOOL_TYPE["copper_hatchet"] = ToolCategory.HATCHET
        ITEM_TOOL_TYPE["copper_dagger"] = ToolCategory.DAGGER
        ITEM_TOOL_TYPE["copper_scythe"] = ToolCategory.SCYTHE

        ITEM_MATERIAL["copper_battle_axe"] = ToolMaterial.COPPER
        ITEM_MATERIAL["copper_war_hammer"] = ToolMaterial.COPPER
        ITEM_MATERIAL["copper_mattock"] = ToolMaterial.COPPER
        ITEM_MATERIAL["copper_hatchet"] = ToolMaterial.COPPER
        ITEM_MATERIAL["copper_dagger"] = ToolMaterial.COPPER
        ITEM_MATERIAL["copper_scythe"] = ToolMaterial.COPPER

        // ========== 原版铁工具 (Minecraft原版) ==========
        ITEM_TOOL_TYPE["iron_pickaxe"] = ToolCategory.PICKAXE
        ITEM_TOOL_TYPE["iron_axe"] = ToolCategory.AXE
        ITEM_TOOL_TYPE["iron_shovel"] = ToolCategory.SHOVEL
        ITEM_TOOL_TYPE["iron_hoe"] = ToolCategory.HOE
        ITEM_TOOL_TYPE["iron_sword"] = ToolCategory.SWORD

        ITEM_MATERIAL["iron_pickaxe"] = ToolMaterial.IRON
        ITEM_MATERIAL["iron_axe"] = ToolMaterial.IRON
        ITEM_MATERIAL["iron_shovel"] = ToolMaterial.IRON
        ITEM_MATERIAL["iron_hoe"] = ToolMaterial.IRON
        ITEM_MATERIAL["iron_sword"] = ToolMaterial.IRON

        // ========== 铁制特殊工具 (战斧/战锤/鸭嘴锄/镰刀/短斧/匕首) ==========
        ITEM_TOOL_TYPE["iron_hatchet"] = ToolCategory.HATCHET
        ITEM_TOOL_TYPE["iron_dagger"] = ToolCategory.DAGGER
        ITEM_TOOL_TYPE["iron_war_hammer"] = ToolCategory.WAR_HAMMER
        ITEM_TOOL_TYPE["iron_battle_axe"] = ToolCategory.BATTLE_AXE
        ITEM_TOOL_TYPE["iron_scythe"] = ToolCategory.SCYTHE
        ITEM_TOOL_TYPE["iron_mattock"] = ToolCategory.MATTOCK

        ITEM_MATERIAL["iron_hatchet"] = ToolMaterial.IRON
        ITEM_MATERIAL["iron_dagger"] = ToolMaterial.IRON
        ITEM_MATERIAL["iron_war_hammer"] = ToolMaterial.IRON
        ITEM_MATERIAL["iron_battle_axe"] = ToolMaterial.IRON
        ITEM_MATERIAL["iron_scythe"] = ToolMaterial.IRON
        ITEM_MATERIAL["iron_mattock"] = ToolMaterial.IRON

        // ========== 原版金工具 (Minecraft原版) ==========
        ITEM_TOOL_TYPE["golden_pickaxe"] = ToolCategory.PICKAXE
        ITEM_TOOL_TYPE["golden_axe"] = ToolCategory.AXE
        ITEM_TOOL_TYPE["golden_shovel"] = ToolCategory.SHOVEL
        ITEM_TOOL_TYPE["golden_hoe"] = ToolCategory.HOE
        ITEM_TOOL_TYPE["golden_sword"] = ToolCategory.SWORD

        ITEM_MATERIAL["golden_pickaxe"] = ToolMaterial.GOLD
        ITEM_MATERIAL["golden_axe"] = ToolMaterial.GOLD
        ITEM_MATERIAL["golden_shovel"] = ToolMaterial.GOLD
        ITEM_MATERIAL["golden_hoe"] = ToolMaterial.GOLD
        ITEM_MATERIAL["golden_sword"] = ToolMaterial.GOLD

        // ========== 金制特殊工具 (战斧/战锤/鸭嘴锄/镰刀/短斧/匕首) ==========
        ITEM_TOOL_TYPE["gold_battle_axe"] = ToolCategory.BATTLE_AXE
        ITEM_TOOL_TYPE["gold_war_hammer"] = ToolCategory.WAR_HAMMER
        ITEM_TOOL_TYPE["gold_mattock"] = ToolCategory.MATTOCK
        ITEM_TOOL_TYPE["gold_scythe"] = ToolCategory.SCYTHE
        ITEM_TOOL_TYPE["gold_hatchet"] = ToolCategory.HATCHET
        ITEM_TOOL_TYPE["gold_dagger"] = ToolCategory.DAGGER

        ITEM_MATERIAL["gold_battle_axe"] = ToolMaterial.GOLD
        ITEM_MATERIAL["gold_war_hammer"] = ToolMaterial.GOLD
        ITEM_MATERIAL["gold_mattock"] = ToolMaterial.GOLD
        ITEM_MATERIAL["gold_scythe"] = ToolMaterial.GOLD
        ITEM_MATERIAL["gold_hatchet"] = ToolMaterial.GOLD
        ITEM_MATERIAL["gold_dagger"] = ToolMaterial.GOLD

        // ========== 原版钻石工具 (Minecraft原版) ==========
        ITEM_TOOL_TYPE["diamond_pickaxe"] = ToolCategory.PICKAXE
        ITEM_TOOL_TYPE["diamond_axe"] = ToolCategory.AXE
        ITEM_TOOL_TYPE["diamond_shovel"] = ToolCategory.SHOVEL
        ITEM_TOOL_TYPE["diamond_hoe"] = ToolCategory.HOE
        ITEM_TOOL_TYPE["diamond_sword"] = ToolCategory.SWORD

        ITEM_MATERIAL["diamond_pickaxe"] = ToolMaterial.DIAMOND
        ITEM_MATERIAL["diamond_axe"] = ToolMaterial.DIAMOND
        ITEM_MATERIAL["diamond_shovel"] = ToolMaterial.DIAMOND
        ITEM_MATERIAL["diamond_hoe"] = ToolMaterial.DIAMOND
        ITEM_MATERIAL["diamond_sword"] = ToolMaterial.DIAMOND

        // ========== 银系工具 ==========
        for (tool in TOOL_NAMES) {
            ITEM_TOOL_TYPE["silver_${tool}"] = ToolCategory.valueOf(tool.uppercase())
            ITEM_MATERIAL["silver_${tool}"] = ToolMaterial.SILVER
        }

        // ========== 远古金属系工具 ==========
        for (tool in TOOL_NAMES) {
            ITEM_TOOL_TYPE["ancient_metal_${tool}"] = ToolCategory.valueOf(tool.uppercase())
            ITEM_MATERIAL["ancient_metal_${tool}"] = ToolMaterial.ANCIENT_METAL
        }

        // ========== 秘银系工具 ==========
        for (tool in TOOL_NAMES) {
            ITEM_TOOL_TYPE["mithril_${tool}"] = ToolCategory.valueOf(tool.uppercase())
            ITEM_MATERIAL["mithril_${tool}"] = ToolMaterial.MITHRIL
        }

        // ========== 艾德曼系工具 ==========
        for (tool in TOOL_NAMES) {
            ITEM_TOOL_TYPE["adamantium_${tool}"] = ToolCategory.valueOf(tool.uppercase())
            ITEM_MATERIAL["adamantium_${tool}"] = ToolMaterial.ADAMANTIUM
        }

        // ========== 下界合金系工具 ==========
        // 5 个原版下界合金工具 (minecraft:netherite_*) + 8 个 ICPM 自定义下界合金特殊工具 (icpm:netherite_*)
        for (tool in TOOL_NAMES) {
            ITEM_TOOL_TYPE["netherite_${tool}"] = ToolCategory.valueOf(tool.uppercase())
            ITEM_MATERIAL["netherite_${tool}"] = ToolMaterial.NETHERITE
        }
    }

    @JvmStatic
    fun isICPMTool(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return ITEM_TOOL_TYPE.containsKey(getItemId(stack))
    }

    @JvmStatic
    fun getBlockDecayRate(stack: ItemStack): Float {
        val category = getToolCategory(stack) ?: return 0f
        val type = TOOL_TYPE_MAP[category] ?: return 0f
        return type.blockDecayRate
    }

    @JvmStatic
    fun getAttackDecayRate(stack: ItemStack): Float {
        val category = getToolCategory(stack) ?: return 0f
        val type = TOOL_TYPE_MAP[category] ?: return 0f
        return type.attackDecayRate
    }

    @JvmStatic
    fun getToolCategory(stack: ItemStack): ToolCategory? {
        return ITEM_TOOL_TYPE[getItemId(stack)]
    }

    /**
     * 直接通过 Item 查询工具类型（用于 Mixin 仅持有 Item 时的判断）
     */
    @JvmStatic
    fun getToolCategoryByItem(item: Item): ToolCategory? {
        val path = BuiltInRegistries.ITEM.getKey(item)?.path ?: return null
        return ITEM_TOOL_TYPE[path]
    }

    /**
     * 直接通过 Item 查询材质（用于 Mixin 挖掘等级判断）
     */
    @JvmStatic
    fun getToolMaterialByItem(item: Item): ToolMaterial? {
        val path = BuiltInRegistries.ITEM.getKey(item)?.path ?: return null
        return ITEM_MATERIAL[path]
    }

    @JvmStatic
    fun getToolMaterial(stack: ItemStack): ToolMaterial? {
        return ITEM_MATERIAL[getItemId(stack)]
    }

    @JvmStatic
    fun getMaxDurability(stack: ItemStack): Int {
        val category = getToolCategory(stack) ?: return 0
        val material = getToolMaterial(stack) ?: return 0

        val type = TOOL_TYPE_MAP[category] ?: return 0
        val mat = convertMaterial(material) ?: return 0

        return ICPMDurability.calculateToolDurability(type, mat)
    }

    fun convertMaterial(mat: ToolMaterial): ICPMDurability.Material? {
        return when (mat) {
            ToolMaterial.LEATHER -> ICPMDurability.Material.LEATHER
            ToolMaterial.WOOD -> ICPMDurability.Material.WOOD
            ToolMaterial.FLINT -> ICPMDurability.Material.FLINT
            ToolMaterial.COPPER -> ICPMDurability.Material.COPPER
            ToolMaterial.SILVER -> ICPMDurability.Material.SILVER
            ToolMaterial.GOLD -> ICPMDurability.Material.GOLD
            ToolMaterial.IRON -> ICPMDurability.Material.IRON
            ToolMaterial.ANCIENT_METAL -> ICPMDurability.Material.ANCIENT_METAL
            ToolMaterial.MITHRIL -> ICPMDurability.Material.MITHRIL
             ToolMaterial.ADAMANTIUM -> ICPMDurability.Material.ADAMANTIUM
            ToolMaterial.DIAMOND -> ICPMDurability.Material.DIAMOND
            ToolMaterial.NETHERITE -> ICPMDurability.Material.NETHERITE
        }
    }

    /**
     * 从ItemStack获取物品的注册ID
     */
    private fun getItemId(stack: ItemStack): String {
        val item: Item = stack.item
        val id: Identifier? = BuiltInRegistries.ITEM.getKey(item)
        return id?.path ?: ""
    }
}