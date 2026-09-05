package name.icpm.block

import name.icpm.ICPM
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.level.block.SoundType
import net.minecraft.core.registries.Registries.BLOCK
import net.minecraft.sounds.SoundEvents

/**
 * ICPM 方块注册
 */
object ICPMBlocks {

    // 方块名称列表（用于注册）
    @JvmField
    val BLOCK_NAMES: List<String> = listOf(
        "underworld_portal", "return_portal", "hell_portal", "mantle",
        "silver_ore", "deepslate_silver_ore", "mithril_ore", "deepslate_mithril_ore",
        "adamantium_ore", "deepslate_adamantium_ore",
        "silver_block", "ancient_metal_block", "mithril_block", "adamantium_block",
        "copper_anvil", "silver_anvil", "gold_anvil",
        "ancient_metal_anvil", "mithril_anvil", "adamantium_anvil",
        "chipped_copper_anvil", "damaged_copper_anvil",
        "chipped_silver_anvil", "damaged_silver_anvil",
        "chipped_gold_anvil", "damaged_gold_anvil",
        "chipped_ancient_metal_anvil", "damaged_ancient_metal_anvil",
        "chipped_mithril_anvil", "damaged_mithril_anvil",
        "chipped_adamantium_anvil", "damaged_adamantium_anvil",
        "flint_workbench",
        "copper_workbench", "silver_workbench", "gold_workbench",
        "iron_workbench", "ancient_metal_workbench", "mithril_workbench", "adamantium_workbench",
        "clay_furnace", "hardened_clay_furnace", "sandstone_furnace", "obsidian_furnace", "netherrack_furnace",
        "silver_door", "gold_door", "ancient_metal_door", "mithril_door", "adamantium_door",
        "emerald_enchanting_table",
        "silver_strongbox", "gold_strongbox", "iron_strongbox",
        "ancient_metal_strongbox", "mithril_strongbox", "adamantium_strongbox",
        "mithril_runestone", "adamantium_runestone", "core"
    )

    /**
     * 地下世界传送门方块
     */
    @JvmField
    var UNDERWORLD_PORTAL: UnderworldPortalBlock? = null

    /**
     * 返回传送门方块
     */
    @JvmField
    var RETURN_PORTAL: ReturnPortalBlock? = null

    /**
     * 地狱传送门方块
     */
    @JvmField
    var HELL_PORTAL: HellPortalBlock? = null

    /**
     * 地幔方块（地下世界最底层基岩变种）
     */
    @JvmField
    var MANTLE: Block? = null

    /**
     * 银矿石
     * R196: y: 0-96, 硬度 2.5f, 挖掘等级 2
     */
    @JvmField
    var SILVER_ORE: Block? = null

    /**
     * 深板岩银矿石
     * y: -64~-1 (深板岩层，仅负层级), 硬度 4.5f, 挖掘等级 2
     */
    @JvmField
    var DEEPSLATE_SILVER_ORE: Block? = null

    /**
     * 秘银矿石
     * R196: y: 0-32, 硬度 3.5f, 挖掘等级 3（铁镐可挖）
     */
    @JvmField
    var MITHRIL_ORE: Block? = null

    /**
     * 深板岩秘银矿石
     * y: -64~-16 (深板岩层，仅负层级), 硬度 5.5f, 挖掘等级 3
     */
    @JvmField
    var DEEPSLATE_MITHRIL_ORE: Block? = null

    /**
     * 艾德曼矿石
     * y: 0-16 (石头层，仅正层级), 硬度 4.0f, 挖掘等级 4（秘银镐可挖）
     */
    @JvmField
    var ADAMANTIUM_ORE: Block? = null

    /**
     * 深板岩艾德曼矿石
     * y: -64~-32 (深板岩层，仅负层级), 硬度 6.0f, 挖掘等级 4
     */
    @JvmField
    var DEEPSLATE_ADAMANTIUM_ORE: Block? = null

    /**
     * 银块
     */
    @JvmField
    var SILVER_BLOCK: Block? = null

    /**
     * 远古金属块
     */
    @JvmField
    var ANCIENT_METAL_BLOCK: Block? = null

    /**
     * 秘银块
     */
    @JvmField
    var MITHRIL_BLOCK: Block? = null

    /**
     * 艾德曼块
     */
    @JvmField
    var ADAMANTIUM_BLOCK: Block? = null

    // ===== 金属砧方块 =====

    // 金属砧变体名称（每个金属3个阶段：正常、chipped、damaged）
    @JvmField
    val ANVIL_VARIANTS: Map<BlockMetalAnvil.MetalType, List<String>> = mapOf(
        BlockMetalAnvil.MetalType.COPPER to listOf("copper_anvil", "chipped_copper_anvil", "damaged_copper_anvil"),
        BlockMetalAnvil.MetalType.SILVER to listOf("silver_anvil", "chipped_silver_anvil", "damaged_silver_anvil"),
        BlockMetalAnvil.MetalType.GOLD to listOf("gold_anvil", "chipped_gold_anvil", "damaged_gold_anvil"),
        BlockMetalAnvil.MetalType.ANCIENT_METAL to listOf("ancient_metal_anvil", "chipped_ancient_metal_anvil", "damaged_ancient_metal_anvil"),
        BlockMetalAnvil.MetalType.MITHRIL to listOf("mithril_anvil", "chipped_mithril_anvil", "damaged_mithril_anvil"),
        BlockMetalAnvil.MetalType.ADAMANTIUM to listOf("adamantium_anvil", "chipped_adamantium_anvil", "damaged_adamantium_anvil")
    )

    /**
     * 获取指定金属和阶段的砧变体方块
     * stage: 0=完好, 1=chipped, 2=damaged
     */
    @JvmStatic
    fun getAnvilVariant(metalType: BlockMetalAnvil.MetalType, stage: Int): BlockMetalAnvil? {
        val names = ANVIL_VARIANTS[metalType] ?: return null
        if (stage < 0 || stage >= names.size) return null
        val block = BuiltInRegistries.BLOCK.getOptional(
            Identifier.fromNamespaceAndPath(ICPM.MOD_ID, names[stage])
        ).orElse(null)
        return block as? BlockMetalAnvil
    }

    /**
     * 铜砧
     * 挖掘等级要求: 1 (石镐)
     * 可以修复: 铜工具
     */
    @JvmField
    var COPPER_ANVIL: BlockMetalAnvil? = null

    /**
     * 银砧
     * 挖掘等级要求: 2 (石镐)
     * 可以修复: 铜、银工具
     */
    @JvmField
    var SILVER_ANVIL: BlockMetalAnvil? = null

    /**
     * 铁砧（已删除：原版 minecraft:anvil 即铁砧，经 VanillaAnvilMenuMixin 接入 ICPM 砧体系=IRON 等级）
     */

    /**
     * 金砧
     * 挖掘等级要求: 2 (石镐)
     * 可以修复: 铜、银、铁、金工具
     */
    @JvmField
    var GOLD_ANVIL: BlockMetalAnvil? = null

    /**
     * 远古金属砧
     * 挖掘等级要求: 3 (铁镐)
     * 可以修复: 铜、银、铁、金、远古金属工具
     */
    @JvmField
    var ANCIENT_METAL_ANVIL: BlockMetalAnvil? = null

    /**
     * 秘银砧
     * 挖掘等级要求: 4 (秘银镐)
     * 可以修复: 铜、银、铁、金、远古金属、秘银工具
     */
    @JvmField
    var MITHRIL_ANVIL: BlockMetalAnvil? = null

    /**
     * 艾德曼砧
     * 挖掘等级要求: 5 (艾德曼镐)
     * 可以修复: 所有金属工具
     */
    @JvmField
    var ADAMANTIUM_ANVIL: BlockMetalAnvil? = null

    // ===== 工作台方块 =====

    /** 燧石工作台（多原木衍生变体，见 BlockICPMFlintWorkbench.WoodType） */
    @JvmField var FLINT_WORKBENCH: BlockICPMFlintWorkbench? = null
    @JvmField var COPPER_WORKBENCH: BlockICPMWorkbench? = null
    @JvmField var SILVER_WORKBENCH: BlockICPMWorkbench? = null
    @JvmField var GOLD_WORKBENCH: BlockICPMWorkbench? = null
    @JvmField var IRON_WORKBENCH: BlockICPMWorkbench? = null
    @JvmField var ANCIENT_METAL_WORKBENCH: BlockICPMWorkbench? = null
    @JvmField var MITHRIL_WORKBENCH: BlockICPMWorkbench? = null
    @JvmField var ADAMANTIUM_WORKBENCH: BlockICPMWorkbench? = null

    /** 原石熔炉=原版熔炉（mixin 注入，maxHeatLevel=2），此处不再单独注册 */
    /** 粘土熔炉（maxHeatLevel=1，不能烧大物品） */
    @JvmField var CLAY_FURNACE: ICPMFurnaceBlock? = null

    /** 硬化粘土熔炉（maxHeatLevel=1） */
    @JvmField var HARDENED_CLAY_FURNACE: ICPMFurnaceBlock? = null

    /** 沙石熔炉（maxHeatLevel=1） */
    @JvmField var SANDSTONE_FURNACE: ICPMFurnaceBlock? = null

    /** 黑曜石熔炉（maxHeatLevel=3） */
    @JvmField var OBSIDIAN_FURNACE: ICPMFurnaceBlock? = null

    /** 地狱岩熔炉（maxHeatLevel=4，可烧艾德曼矿） */
    @JvmField var NETHERRACK_FURNACE: ICPMFurnaceBlock? = null

    // ===== 金属门（ICPM R196 BlockDoor + 金属 Material；红石开门）=====
    @JvmField var SILVER_DOOR: net.minecraft.world.level.block.DoorBlock? = null
    @JvmField var GOLD_DOOR: net.minecraft.world.level.block.DoorBlock? = null
    @JvmField var ANCIENT_METAL_DOOR: net.minecraft.world.level.block.DoorBlock? = null
    @JvmField var MITHRIL_DOOR: net.minecraft.world.level.block.DoorBlock? = null
    @JvmField var ADAMANTIUM_DOOR: net.minecraft.world.level.block.DoorBlock? = null

    /** 绿宝石附魔台（ICPM R196 BlockEnchantmentTable, Material.emerald） */
    @JvmField var EMERALD_ENCHANTING_TABLE: EmeraldEnchantingTableBlock? = null

    // ===== 金属箱/强箱（1.6.4 BlockStrongbox，仅所有者可开）=====
    @JvmField var SILVER_STRONGBOX: ICPMStrongboxBlock? = null
    @JvmField var GOLD_STRONGBOX: ICPMStrongboxBlock? = null
    @JvmField var IRON_STRONGBOX: ICPMStrongboxBlock? = null
    @JvmField var ANCIENT_METAL_STRONGBOX: ICPMStrongboxBlock? = null
    @JvmField var MITHRIL_STRONGBOX: ICPMStrongboxBlock? = null
    @JvmField var ADAMANTIUM_STRONGBOX: ICPMStrongboxBlock? = null

    /** 秘银符文石（1.6.4 BlockRunestone） */
    @JvmField var MITHRIL_RUNESTONE: BlockRunestone? = null

    /** 艾德曼符文石（1.6.4 BlockRunestone） */
    @JvmField var ADAMANTIUM_RUNESTONE: BlockRunestone? = null

    /** 地核（1.6.4 BlockMantleOrCore metadata=1，地下世界最底层，不可破坏） */
    @JvmField var CORE: Block? = null

    /**
     * 创建并注册方块
     */
    @JvmStatic
    fun createAndRegister(name: String): Block {
        // 先创建ResourceKey
        val id = Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name)
        val blockKey = ResourceKey.create(Registries.BLOCK, id)

        val block = when (name) {
            "underworld_portal" -> {
                // 地下世界传送门：使用专门的UnderworldPortalBlock类
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f)
                    .lightLevel { 11 }
                    .pushReaction(PushReaction.BLOCK)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey) // 设置ResourceKey避免NullPointerException
                UnderworldPortalBlock(properties)
            }
            "return_portal" -> {
                // 返回传送门：使用专门的ReturnPortalBlock类
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f)
                    .lightLevel { 11 }
                    .pushReaction(PushReaction.BLOCK)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey) // 设置ResourceKey避免NullPointerException
                ReturnPortalBlock(properties)
            }
            "hell_portal" -> {
                // 地狱传送门：使用专门的HellPortalBlock类
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(-1.0f)
                    .lightLevel { 11 }
                    .pushReaction(PushReaction.BLOCK)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey) // 设置ResourceKey避免NullPointerException
                HellPortalBlock(properties)
            }
            "mantle" -> {
                // 地幔：地下世界最底层的基岩变种，不可破坏
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0f, 3600000.0f) // 完全不可破坏
                    .pushReaction(PushReaction.BLOCK)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey) // 设置ResourceKey避免NullPointerException
                Block(properties)
            }
            "silver_ore" -> {
                // 银矿：R196 y:0-96, 硬度 2.5f, 挖掘等级 2（石镐）
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "deepslate_silver_ore" -> {
                // 深板岩银矿：y:0-64, 硬度 4.5f, 挖掘等级 2
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5f, 3.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "mithril_ore" -> {
                // 秘银矿：R196 y:0-32, 硬度 3.5f, 挖掘等级 3（铁镐）
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "deepslate_mithril_ore" -> {
                // 深板岩秘银矿：y:0-16, 硬度 5.5f, 挖掘等级 3
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(5.5f, 3.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "adamantium_ore" -> {
                // 艾德曼矿：R196 y:0-24, 硬度 4.0f, 挖掘等级 4（秘银镐）
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(4.0f, 3600000.0f)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "deepslate_adamantium_ore" -> {
                // 深板岩艾德曼矿：y:0-16, 硬度 6.0f, 挖掘等级 4
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(6.0f, 3600000.0f)
                    .sound(SoundType.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "silver_block" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 3600000.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "ancient_metal_block" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f, 3600000.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "mithril_block" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 3600000.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "adamantium_block" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(6.0f, 1200.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                Block(properties)
            }
            "copper_anvil" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .setId(blockKey)
                BlockMetalAnvil(BlockMetalAnvil.MetalType.COPPER, 0, properties)
            }
            "silver_anvil" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .setId(blockKey)
                BlockMetalAnvil(BlockMetalAnvil.MetalType.SILVER, 0, properties)
            }
            "gold_anvil" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .setId(blockKey)
                BlockMetalAnvil(BlockMetalAnvil.MetalType.GOLD, 0, properties)
            }
            "ancient_metal_anvil" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .setId(blockKey)
                BlockMetalAnvil(BlockMetalAnvil.MetalType.ANCIENT_METAL, 0, properties)
            }
            "mithril_anvil" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(6.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .setId(blockKey)
                BlockMetalAnvil(BlockMetalAnvil.MetalType.MITHRIL, 0, properties)
            }
            "adamantium_anvil" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(8.0f, 1200.0f)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .setId(blockKey)
                BlockMetalAnvil(BlockMetalAnvil.MetalType.ADAMANTIUM, 0, properties)
            }
            // ===== 金属砧变体（chipped/damaged）=====
            "chipped_copper_anvil", "damaged_copper_anvil",
            "chipped_silver_anvil", "damaged_silver_anvil",
            "chipped_gold_anvil", "damaged_gold_anvil",
            "chipped_ancient_metal_anvil", "damaged_ancient_metal_anvil",
            "chipped_mithril_anvil", "damaged_mithril_anvil",
            "chipped_adamantium_anvil", "damaged_adamantium_anvil" -> {
                val metalName = name.removePrefix("chipped_").removePrefix("damaged_").removeSuffix("_anvil")
                val stage = if (name.startsWith("chipped_")) 1 else 2
                val metalType = when (metalName) {
                    "copper" -> BlockMetalAnvil.MetalType.COPPER
                    "silver" -> BlockMetalAnvil.MetalType.SILVER
                    "iron" -> BlockMetalAnvil.MetalType.IRON
                    "gold" -> BlockMetalAnvil.MetalType.GOLD
                    "ancient_metal" -> BlockMetalAnvil.MetalType.ANCIENT_METAL
                    "mithril" -> BlockMetalAnvil.MetalType.MITHRIL
                    "adamantium" -> BlockMetalAnvil.MetalType.ADAMANTIUM
                    else -> throw IllegalArgumentException("Unknown anvil variant: $name")
                }
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(if (metalName == "gold") MapColor.GOLD else MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(if (metalName == "adamantium") SoundType.NETHERITE_BLOCK else SoundType.METAL)
                    .setId(blockKey)
                BlockMetalAnvil(metalType, stage, properties)
            }
            // 工作台：燧石工作台为多态方块，不同原木仅是 wood 状态属性的衍生外观
            "flint_workbench" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .setId(blockKey)
                BlockICPMFlintWorkbench(properties)
            }
            "copper_workbench" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .setId(blockKey)
                BlockICPMWorkbench(1, "铜工作台", properties)
            }
            "silver_workbench" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .setId(blockKey)
                BlockICPMWorkbench(1, "银工作台", properties)
            }
            "gold_workbench" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(2.5f, 3.0f)
                    .sound(SoundType.WOOD)
                    .setId(blockKey)
                BlockICPMWorkbench(1, "金工作台", properties)
            }
            "iron_workbench" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.WOOD)
                    .setId(blockKey)
                BlockICPMWorkbench(3, "铁工作台", properties)
            }
            "ancient_metal_workbench" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 6.0f)
                    .sound(SoundType.WOOD)
                    .setId(blockKey)
                BlockICPMWorkbench(4, "远古金属工作台", properties)
            }
            "mithril_workbench" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(4.0f, 6.0f)
                    .sound(SoundType.WOOD)
                    .setId(blockKey)
                BlockICPMWorkbench(5, "秘银工作台", properties)
            }
            "adamantium_workbench" -> {
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.WOOD)
                    .setId(blockKey)
                BlockICPMWorkbench(6, "艾德曼工作台", properties)
            }
            "clay_furnace" -> {
                // 粘土熔炉：ICPM R196 BlockFurnaceClay，maxHeatLevel=1，不能烧大物品
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                    .strength(2.5f)
                    .lightLevel { state -> if (state.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT)) 13 else 0 }
                    .sound(SoundType.GRAVEL)
                    .setId(blockKey)
                ICPMFurnaceBlock(maxHeatLevel = 1, acceptsLargeItems = false, properties = properties)
            }
            "hardened_clay_furnace" -> {
                // 硬化粘土熔炉：ICPM R196 BlockFurnaceHardenedClay，maxHeatLevel=1（1.6.4 硬度 1.0）
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                    .strength(1.0f)
                    .lightLevel { state -> if (state.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT)) 13 else 0 }
                    .sound(SoundType.STONE)
                    .setId(blockKey)
                ICPMFurnaceBlock(1, properties)
            }
            "sandstone_furnace" -> {
                // 沙石熔炉：ICPM R196 BlockFurnaceSandstone，maxHeatLevel=1
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                    .strength(2.0f)
                    .lightLevel { state -> if (state.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT)) 13 else 0 }
                    .sound(SoundType.STONE)
                    .setId(blockKey)
                ICPMFurnaceBlock(1, properties)
            }
            "obsidian_furnace" -> {
                // 黑曜石熔炉：ICPM R196 BlockFurnaceObsidian，maxHeatLevel=3（可烧岩浆、冶炼秘银矿）
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(50.0f, 1200.0f)
                    .lightLevel { state -> if (state.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT)) 13 else 0 }
                    .sound(SoundType.STONE)
                    .setId(blockKey)
                ICPMFurnaceBlock(3, properties)
            }
            "netherrack_furnace" -> {
                // 地狱岩熔炉：ICPM R196 BlockFurnaceNetherrack，maxHeatLevel=4（烈焰棒级，可烧艾德曼矿）
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                    .strength(2.0f)
                    .lightLevel { state -> if (state.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT)) 13 else 0 }
                    .sound(SoundType.NETHERRACK)
                    .setId(blockKey)
                ICPMFurnaceBlock(4, properties)
            }
            "silver_door", "gold_door", "ancient_metal_door", "mithril_door", "adamantium_door" -> {
                // 金属门：ICPM R196 BlockDoor（Material 金属），红石开门（canOpenByHand=false）
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f)
                    .noOcclusion()
                    .isValidSpawn { _, _, _, _ -> false }
                    .setId(blockKey)
                DoorBlock(createMetalBlockSetType(name), properties)
            }
            "emerald_enchanting_table" -> {
                // 绿宝石附魔台：ICPM R196 BlockEnchantmentTable(Material.emerald)，
                // 硬度 2.4、抗爆 20（与原版附魔台一致），外观用 emerald_enchanting_table 贴图
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(2.4f, 20.0f)
                    .lightLevel { 7 }
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                EmeraldEnchantingTableBlock(properties)
            }
            "silver_strongbox", "gold_strongbox", "iron_strongbox",
            "ancient_metal_strongbox", "mithril_strongbox", "adamantium_strongbox" -> {
                // 金属箱（强箱）：1.6.4 BlockStrongbox，仅所有者可开，金属块贴图
                val metalName = name.removeSuffix("_strongbox")
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                ICPMStrongboxBlock(metalName, properties)
            }
            "mithril_runestone", "adamantium_runestone" -> {
                // 符文石：1.6.4 BlockRunestone（黑曜石强度，16 变体符文，符文门框架 4 角）
                val metalName = name.removeSuffix("_runestone")
                val metal = if (metalName == "mithril") BlockRunestone.MetalType.MITHRIL else BlockRunestone.MetalType.ADAMANTIUM
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.4f, 20.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .setId(blockKey)
                BlockRunestone(metal, properties)
            }
            "core" -> {
                // 地核：地下世界最底层方块（mantle 之下），不可破坏（1.6.4 BlockMantleOrCore core）
                val properties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0f, 3600000.0f)
                    .pushReaction(PushReaction.BLOCK)
                    .setId(blockKey)
                Block(properties)
            }
            else -> throw IllegalArgumentException("Unknown block: $name")
        }

        // 注册方块
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block)

        // 添加 MINEABLE_WITH_PICKAXE 标签（砧为0级，手可破坏，不需要此标签）
        if (name.endsWith("_ore") || name.endsWith("_block") || name == "mantle") {
            ICPMTagRegistry.addToPickaxeMineable(block)
        }

        // 更新静态字段
        when (name) {
             "underworld_portal" -> UNDERWORLD_PORTAL = block as UnderworldPortalBlock
             "return_portal" -> RETURN_PORTAL = block as ReturnPortalBlock
             "hell_portal" -> HELL_PORTAL = block as HellPortalBlock
            "mantle" -> MANTLE = block
            "silver_ore" -> SILVER_ORE = block
            "deepslate_silver_ore" -> DEEPSLATE_SILVER_ORE = block
            "mithril_ore" -> MITHRIL_ORE = block
            "deepslate_mithril_ore" -> DEEPSLATE_MITHRIL_ORE = block
            "adamantium_ore" -> ADAMANTIUM_ORE = block
            "deepslate_adamantium_ore" -> DEEPSLATE_ADAMANTIUM_ORE = block
            "silver_block" -> SILVER_BLOCK = block
            "ancient_metal_block" -> ANCIENT_METAL_BLOCK = block
            "mithril_block" -> MITHRIL_BLOCK = block
            "adamantium_block" -> ADAMANTIUM_BLOCK = block
            "copper_anvil" -> COPPER_ANVIL = block as BlockMetalAnvil
            "silver_anvil" -> SILVER_ANVIL = block as BlockMetalAnvil
            "gold_anvil" -> GOLD_ANVIL = block as BlockMetalAnvil
            "ancient_metal_anvil" -> ANCIENT_METAL_ANVIL = block as BlockMetalAnvil
            "mithril_anvil" -> MITHRIL_ANVIL = block as BlockMetalAnvil
            "adamantium_anvil" -> ADAMANTIUM_ANVIL = block as BlockMetalAnvil
            "flint_workbench" -> FLINT_WORKBENCH = block as BlockICPMFlintWorkbench
            "copper_workbench" -> COPPER_WORKBENCH = block as BlockICPMWorkbench
            "silver_workbench" -> SILVER_WORKBENCH = block as BlockICPMWorkbench
            "gold_workbench" -> GOLD_WORKBENCH = block as BlockICPMWorkbench
            "iron_workbench" -> IRON_WORKBENCH = block as BlockICPMWorkbench
            "ancient_metal_workbench" -> ANCIENT_METAL_WORKBENCH = block as BlockICPMWorkbench
            "mithril_workbench" -> MITHRIL_WORKBENCH = block as BlockICPMWorkbench
            "adamantium_workbench" -> ADAMANTIUM_WORKBENCH = block as BlockICPMWorkbench
            "clay_furnace" -> CLAY_FURNACE = block as ICPMFurnaceBlock
            "hardened_clay_furnace" -> HARDENED_CLAY_FURNACE = block as ICPMFurnaceBlock
            "sandstone_furnace" -> SANDSTONE_FURNACE = block as ICPMFurnaceBlock
            "obsidian_furnace" -> OBSIDIAN_FURNACE = block as ICPMFurnaceBlock
            "netherrack_furnace" -> NETHERRACK_FURNACE = block as ICPMFurnaceBlock
            "silver_door" -> SILVER_DOOR = block as DoorBlock
            "gold_door" -> GOLD_DOOR = block as DoorBlock
            "ancient_metal_door" -> ANCIENT_METAL_DOOR = block as DoorBlock
            "mithril_door" -> MITHRIL_DOOR = block as DoorBlock
            "adamantium_door" -> ADAMANTIUM_DOOR = block as DoorBlock
            "emerald_enchanting_table" -> EMERALD_ENCHANTING_TABLE = block as EmeraldEnchantingTableBlock
            "silver_strongbox" -> SILVER_STRONGBOX = block as ICPMStrongboxBlock
            "gold_strongbox" -> GOLD_STRONGBOX = block as ICPMStrongboxBlock
            "iron_strongbox" -> IRON_STRONGBOX = block as ICPMStrongboxBlock
            "ancient_metal_strongbox" -> ANCIENT_METAL_STRONGBOX = block as ICPMStrongboxBlock
            "mithril_strongbox" -> MITHRIL_STRONGBOX = block as ICPMStrongboxBlock
            "adamantium_strongbox" -> ADAMANTIUM_STRONGBOX = block as ICPMStrongboxBlock
            "mithril_runestone" -> MITHRIL_RUNESTONE = block as BlockRunestone
            "adamantium_runestone" -> ADAMANTIUM_RUNESTONE = block as BlockRunestone
            "core" -> CORE = block
        }

        return block
    }

    /**
     * 金属门 BlockSetType：红石开门（canOpenByHand=false），金属音效（仿 ICPM R196 金属门）
     */
    @JvmStatic
    private fun createMetalBlockSetType(name: String): BlockSetType {
        return BlockSetType(
            name,
            false, // canOpenByHand：金属门需红石
            false, // canOpenByWindCharge
            false, // canButtonBeActivatedByArrows
            BlockSetType.PressurePlateSensitivity.EVERYTHING,
            SoundType.METAL,
            SoundEvents.IRON_DOOR_CLOSE,
            SoundEvents.IRON_DOOR_OPEN,
            SoundEvents.IRON_TRAPDOOR_CLOSE,
            SoundEvents.IRON_TRAPDOOR_OPEN,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF,
            SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON,
            SoundEvents.STONE_BUTTON_CLICK_OFF,
            SoundEvents.STONE_BUTTON_CLICK_ON
        )
    }

    @JvmStatic
    fun init() {
        // 初始化时注册所有方块（由ICPM.java调用）
    }
}