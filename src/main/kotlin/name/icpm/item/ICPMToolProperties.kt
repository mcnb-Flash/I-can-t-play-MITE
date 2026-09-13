package name.icpm.item

import name.icpm.common.ICPMDurability
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

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
        LEATHER, WOOD, FLINT, OBSIDIAN, RUSTED_IRON, COPPER, SILVER, GOLD, IRON,
        ANCIENT_METAL, MITHRIL, ADAMANTIUM, DIAMOND, NETHERITE
    }

    /**
     * R196 工具基础攻击伤害表。
     *
     * 判决源：R196 各 ItemTool 子类 `getBaseDamageVsEntity()`：
     * ItemSword 4.0 / ItemDagger(super-2) 2.0 / ItemKnife(super-1) 1.0 /
     * ItemAxe 3.0 / ItemHatchet(super-1) 2.0 / ItemBattleAxe(super+1) 4.0 /
     * ItemPickaxe 2.0 / ItemWarHammer(继承镐) 2.0 / ItemShovel 1.0 /
     * ItemMattock(继承铲) 1.0 / ItemHoe 1.0 / ItemCudgel 1.0 /
     * ItemClub(super+1) 2.0 / ItemScythe 1.0。
     *
     * 语义说明：R196 `ItemTool.getCombinedDamageVsEntity() = getBaseDamageVsEntity() + 材质伤害`，
     * 再叠加玩家基础攻击 1.0（`EntityPlayer.java:210` setEntityAttribute(attackDamage, 1.0)）。
     * 1.21.11 的 `.sword(tier, x, spd)` / `XxxItem(tier, x, spd)` 语义为「x + tier.attackDamageBonus」，
     * 故这里的 x 必须直接取 R196 的 `getBaseDamageVsEntity()` 才能总量对齐。
     */
    @JvmStatic
    fun getR196BaseDamage(category: ToolCategory): Float = when (category) {
        ToolCategory.SWORD -> 4.0f
        ToolCategory.DAGGER -> 2.0f
        ToolCategory.KNIFE -> 1.0f
        ToolCategory.AXE -> 3.0f
        ToolCategory.HATCHET -> 2.0f
        ToolCategory.BATTLE_AXE -> 4.0f
        ToolCategory.PICKAXE -> 2.0f
        ToolCategory.WAR_HAMMER -> 2.0f
        ToolCategory.SHOVEL -> 1.0f
        ToolCategory.MATTOCK -> 1.0f
        ToolCategory.HOE -> 1.0f
        ToolCategory.CUDGEL -> 1.0f
        ToolCategory.CLUB -> 2.0f
        ToolCategory.SCYTHE -> 1.0f
        ToolCategory.SPEAR -> 0.85f
    }

    /**
     * R196 材质攻击加值（`Material.getDamageVsEntity()`，Material.java:370-409）。
     * wood0 / flint1 / obsidian2 / rusted_iron2 / copper3 / silver3 / gold2 /
     * iron4 / ancient_metal4 / mithril5 / adamantium6 / diamond4。
     */
    @JvmStatic
    fun getR196MaterialDamage(material: ToolMaterial): Float = when (material) {
        ToolMaterial.LEATHER -> 0.0f
        ToolMaterial.WOOD -> 0.0f
        ToolMaterial.FLINT -> 1.0f
        ToolMaterial.OBSIDIAN -> 2.0f
        ToolMaterial.RUSTED_IRON -> 2.0f
        ToolMaterial.COPPER -> 3.0f
        ToolMaterial.SILVER -> 3.0f
        ToolMaterial.GOLD -> 2.0f
        ToolMaterial.IRON -> 4.0f
        ToolMaterial.ANCIENT_METAL -> 4.0f
        ToolMaterial.MITHRIL -> 5.0f
        ToolMaterial.ADAMANTIUM -> 6.0f
        ToolMaterial.DIAMOND -> 4.0f
        ToolMaterial.NETHERITE -> 7.0f
    }

    /**
     * 手持工具的 R196 攻击伤害修饰总量（不含玩家基础 1.0）。
     * = getBaseDamageVsEntity() + 材质伤害。
     */
    @JvmStatic
    fun getR196AttackDamageModifier(stack: ItemStack): Float {
        val category = getToolCategory(stack) ?: return 0f
        val material = getToolMaterial(stack) ?: return 0f
        return getR196BaseDamage(category) + getR196MaterialDamage(material)
    }

    /**
     * 读取 ItemStack 当前 ATTACK_DAMAGE 属性修饰之和。
     */
    @JvmStatic
    fun getCurrentAttackDamageModifier(stack: ItemStack): Float {
        val mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS) ?: return 0f
        var total = 0f
        for (entry in mods.modifiers()) {
            if (entry.attribute() != net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) continue
            val mod = entry.modifier()
            if (mod.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                total += mod.amount().toFloat()
            }
        }
        return total
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
    fun getBlockDecayRate(stack: ItemStack, state: BlockState?): Float {
        val category = getToolCategory(stack) ?: return 0f
        val type = TOOL_TYPE_MAP[category] ?: return 0f
        if (state != null) {
            // R196 ItemScythe.getBaseDecayRateForBreakingBlock：高草/作物 0.5，其余 2.0
            if (type == ICPMDurability.ToolType.SCYTHE &&
                (state.`is`(BlockTags.CROPS) || state.`is`(Blocks.TALL_GRASS) || state.`is`(Blocks.FERN))
            ) return 0.5f
            // R196 ItemKnife.getBaseDecayRateForBreakingBlock：cloth/plants/vine ÷4（继承剑 2.0 → 0.5）
            // 其余方块 ÷2 → 1.0（即 ToolType.KNIFE.blockDecayRate 默认值）
            if (type == ICPMDurability.ToolType.KNIFE && isR196KnifeSoftBlock(state)) return 0.5f
            // R196 ItemAxe.getBaseDecayRateForBreakingBlock：砂岩 1.875
            if (type == ICPMDurability.ToolType.AXE &&
                (state.`is`(Blocks.SANDSTONE) || state.`is`(Blocks.RED_SANDSTONE))
            ) return 1.875f
        }
        return type.blockDecayRate
    }

    @JvmStatic
    fun getAttackDecayRate(stack: ItemStack): Float {
        val category = getToolCategory(stack) ?: return 0f
        val type = TOOL_TYPE_MAP[category] ?: return 0f
        // R196 ItemScythe.getBaseDecayRateForAttackingEntity：vampiric 1.0 / sharpness 2.0 / 默认 4.0
        if (type == ICPMDurability.ToolType.SCYTHE) {
            if (enchantLevel(stack, "icpm:vampiric") > 0) return 1.0f
            if (enchantLevel(stack, "minecraft:sharpness") > 0) return 2.0f
        }
        return type.attackDecayRate
    }

    /**
     * R196 `ItemKnife.getBaseDecayRateForBreakingBlock` 的 ÷4 集合：
     * `Material.cloth`（布/羊毛）、`Material.plants`（作物/草/花/树苗）、`Material.vine`（藤蔓）。
     * 不含 `Material.tree_leaves`（树叶走 ÷2）。
     */
    @JvmStatic
    private fun isR196KnifeSoftBlock(state: BlockState): Boolean {
        return state.`is`(BlockTags.WOOL)
                || state.`is`(BlockTags.CROPS)
                || state.`is`(BlockTags.FLOWERS)
                || state.`is`(BlockTags.SAPLINGS)
                || state.`is`(Blocks.VINE)
                || state.`is`(Blocks.SHORT_GRASS)
                || state.`is`(Blocks.TALL_GRASS)
                || state.`is`(Blocks.FERN)
                || state.`is`(Blocks.LARGE_FERN)
                || state.`is`(Blocks.SWEET_BERRY_BUSH)
                || state.`is`(Blocks.NETHER_SPROUTS)
                || state.`is`(Blocks.CRIMSON_ROOTS)
                || state.`is`(Blocks.WARPED_ROOTS)
    }

    /** 读取物品附魔等级（按注册 id 全名匹配，如 "icpm:vampiric"） */
    @JvmStatic
    private fun enchantLevel(stack: ItemStack, enchantmentId: String): Int {
        val enchantments = stack.get(DataComponents.ENCHANTMENTS) ?: return 0
        for (holder in enchantments.keySet()) {
            val key = holder.unwrapKey().orElse(null) ?: continue
            if (key.identifier().toString() == enchantmentId) return enchantments.getLevel(holder)
        }
        return 0
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
            ToolMaterial.OBSIDIAN -> ICPMDurability.Material.OBSIDIAN
            ToolMaterial.RUSTED_IRON -> ICPMDurability.Material.RUSTED_IRON
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