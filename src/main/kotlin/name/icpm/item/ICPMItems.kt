package name.icpm.item

import name.icpm.ICPM
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.ShovelItem
import net.minecraft.world.item.HoeItem
import net.minecraft.world.item.ToolMaterial
import net.minecraft.world.item.ShearsItem
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.equipment.ArmorType
import net.minecraft.core.Holder
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorMaterials
import net.minecraft.world.item.equipment.EquipmentAsset
import net.minecraft.world.item.equipment.EquipmentAssets
import net.minecraft.world.item.equipment.Equippable
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Items
import name.icpm.common.ICPMFoodProperties
import net.minecraft.world.level.material.Fluids
import name.icpm.block.ICPMBlocks

/**
 * ICPM 物品注册
 *
 * 数值严格基于 ICPM R196 反编译源文件:
 *   - akc.getDamageVsEntity() — 材质伤害加成
 *   - xj.getBaseDamageVsEntity() — 工具基础伤害
 *   - xj.getMultipliedDurability() — 工具耐久公式 4 × 部件数 × 材质耐久 × 100
 *
 * 1.21.11 中 Item.Properties 必须通过 setId() 设置 id 才能正确注册
 */
object ICPMItems {

    // ==================== ICPM 自定义护甲材质 ====================
    // 基于 R196 源码的护甲防御值（单位：半心）
    private val SILVER_ARMOR_MAT = createArmorMaterial(
        "silver", mapOf(
            ArmorType.HELMET to 2,
            ArmorType.CHESTPLATE to 3,
            ArmorType.LEGGINGS to 2,
            ArmorType.BOOTS to 1
        ), 30, SoundEvents.ARMOR_EQUIP_GOLD, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_silver"))
    )
    private val ANCIENT_METAL_ARMOR_MAT = createArmorMaterial(
        "ancient_metal", mapOf(
            ArmorType.HELMET to 2,
            ArmorType.CHESTPLATE to 4,
            ArmorType.LEGGINGS to 3,
            ArmorType.BOOTS to 1
        ), 40, SoundEvents.ARMOR_EQUIP_IRON, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_ancient_metal"))
    )
    private val MITHRIL_ARMOR_MAT = createArmorMaterial(
        "mithril", mapOf(
            ArmorType.HELMET to 3,
            ArmorType.CHESTPLATE to 4,
            ArmorType.LEGGINGS to 3,
            ArmorType.BOOTS to 2
        ), 100, SoundEvents.ARMOR_EQUIP_DIAMOND, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_mithril"))
    )
    private val ADAMANTIUM_ARMOR_MAT = createArmorMaterial(
        "adamantium", mapOf(
            ArmorType.HELMET to 3,
            ArmorType.CHESTPLATE to 5,
            ArmorType.LEGGINGS to 4,
            ArmorType.BOOTS to 3
        ), 40, SoundEvents.ARMOR_EQUIP_NETHERITE, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_adamantium"))
    )

    // 锁链甲材质（银/远古金属/秘银/精金）— 防御值与板甲相同，但 assetName 指向锁链甲穿戴贴图
    private val SILVER_CHAINMAIL_MAT = createArmorMaterial(
        "silver_chainmail", mapOf(
            ArmorType.HELMET to 2, ArmorType.CHESTPLATE to 3, ArmorType.LEGGINGS to 2, ArmorType.BOOTS to 1
        ), 30, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_silver"))
    )
    private val ANCIENT_METAL_CHAINMAIL_MAT = createArmorMaterial(
        "ancient_metal_chainmail", mapOf(
            ArmorType.HELMET to 2, ArmorType.CHESTPLATE to 4, ArmorType.LEGGINGS to 3, ArmorType.BOOTS to 1
        ), 40, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_ancient_metal"))
    )
    private val MITHRIL_CHAINMAIL_MAT = createArmorMaterial(
        "mithril_chainmail", mapOf(
            ArmorType.HELMET to 3, ArmorType.CHESTPLATE to 4, ArmorType.LEGGINGS to 3, ArmorType.BOOTS to 2
        ), 100, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_mithril"))
    )
    private val ADAMANTIUM_CHAINMAIL_MAT = createArmorMaterial(
        "adamantium_chainmail", mapOf(
            ArmorType.HELMET to 3, ArmorType.CHESTPLATE to 5, ArmorType.LEGGINGS to 4, ArmorType.BOOTS to 3
        ), 40, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_adamantium"))
    )

    // ==================== R196 源文件数值表 ====================
    // 来自 EnumEquipmentMaterial.durability (R196 源)
    private const val FLINT_MAT_DUR = 1.0f
    private const val COPPER_MAT_DUR = 4.0f
    private const val GOLD_MAT_DUR = 4.0f
    private const val SILVER_MAT_DUR = 4.0f
    private const val IRON_MAT_DUR = 8.0f
    private const val ANCIENT_METAL_MAT_DUR = 16.0f
    private const val MITHRIL_MAT_DUR = 64.0f
    private const val ADAMANTIUM_MAT_DUR = 256.0f
    private const val NETHERITE_MAT_DUR = 256.0f

    // 来自 yj.getNumComponentsForDurability() 等 (R196 源)
    private const val PICKAXE_COMPONENTS = 3
    private const val AXE_COMPONENTS = 3
    private const val SHOVEL_COMPONENTS = 1
    private const val HOE_COMPONENTS = 2
    private const val SWORD_COMPONENTS = 2
    private const val HATCHET_COMPONENTS = 1
    private const val WAR_HAMMER_COMPONENTS = 5
    private const val BATTLE_AXE_COMPONENTS = 4
    private const val DAGGER_COMPONENTS = 1
    private const val KNIFE_COMPONENTS = 1
    private const val CUDGEL_COMPONENTS = 1
    private const val SCYTHE_COMPONENTS = 2
    private const val MATTOCK_COMPONENTS = 4
    private const val SPEAR_COMPONENTS = 3
    private const val SHEARS_COMPONENTS = 2

    // 护甲部件数 (R196 源)
    private const val HELMET_COMPONENTS = 5
    private const val CHESTPLATE_COMPONENTS = 8
    private const val LEGGINGS_COMPONENTS = 7
    private const val BOOTS_COMPONENTS = 4
    private const val HORSE_COMPONENTS = 8

    /**
     * R196 源文件工具耐久公式: 4.0 × 部件数 × 材质耐久系数 × 100
     * 来源: xj.getMultipliedDurability()
     */
    private fun miteDurability(materialDurability: Float, components: Int): Int {
        return (4.0f * components * materialDurability * 100.0f).toInt()
    }

    /**
     * R196 源文件护甲耐久公式: 部件数 × 材质耐久系数 × 2
     * 锁甲不乘2（本mod暂无锁甲）
     */
    private fun miteArmorDurability(materialDurability: Float, components: Int): Int {
        return (components * materialDurability * 2).toInt()
    }

    /**
     * 创建带 id 的 Item.Properties
     */
    private fun makeProperties(name: String, maxStack: Int = 64): Item.Properties {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name))
        var props = Item.Properties().setId(key).stacksTo(maxStack)
        // 挂原版铁砧修复材料 tag（repairable 组件）：原版铁砧即可用对应金属粒/锭修复 ICPM 工具/护甲
        val repairTag = icpmRepairTagFor(name)
        if (repairTag != null) {
            props = props.repairable(repairTag)
        }
        return props
    }

    /**
     * 按物品 id 前缀返回对应金属的修复 tag（R196 语义：原版铁砧用金属粒/锭修复）。
     * 铜/银/铁/金(gold|golden)/远古金属/秘银/艾德曼；flint/木等无对应金属粒 → null 不可修复。
     */
    private fun icpmRepairTagFor(name: String): TagKey<Item>? = when {
        name.startsWith("copper_") -> TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_copper"))
        name.startsWith("silver_") -> TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_silver"))
        name.startsWith("iron_") -> TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_iron"))
        name.startsWith("gold_") || name.startsWith("golden_") -> TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_gold"))
        name.startsWith("ancient_metal_") -> TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_ancient_metal"))
        name.startsWith("mithril_") -> TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_mithril"))
        name.startsWith("adamantium_") -> TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_adamantium"))
        else -> null
    }

    // ========== 燧石系物品 ==========
    @JvmField val FLINT_FRAGMENT: Item = register("flint_fragment", Item(makeProperties("flint_fragment", 64)))
    @JvmField val OBSIDIAN_SHARD: Item = register("obsidian_shard", Item(makeProperties("obsidian_shard", 64)))
    @JvmField val EMERALD_SHARD: Item = register("emerald_shard", Item(makeProperties("emerald_shard", 64)))
    @JvmField val DIAMOND_SHARD: Item = register("diamond_shard", Item(makeProperties("diamond_shard", 64)))
    // 注意：燧石碎片在本 mod 中已实现为 FLINT_FRAGMENT，不再另加 flint_shard
    @JvmField val GLASS_SHARD: Item = register("glass_shard", Item(makeProperties("glass_shard", 64)))
    @JvmField val QUARTZ_SHARD: Item = register("quartz_shard", Item(makeProperties("quartz_shard", 64)))
    @JvmField val LEATHER_CORD: Item = register("leather_cord", Item(makeProperties("leather_cord", 64)))

    // ========== 怪物掉落碎片（R196 Item.fragsInfernalCreeper 等） ==========
    // 地狱苦力怕掉落地狱碎片（见 InfernalCreeperEntity）
    @JvmField val INFERNAL_CREEPER_FRAG: Item = register("infernal_creeper_frag", Item(makeProperties("infernal_creeper_frag", 64)))

    // ========== 链条（ICPM R196 ItemChain：4 粒合成 1 链，链可拆回 4 粒） ==========
    @JvmField val COPPER_CHAIN: Item = register("copper_chain", Item(makeProperties("copper_chain", 64)))
    @JvmField val SILVER_CHAIN: Item = register("silver_chain", Item(makeProperties("silver_chain", 64)))
    @JvmField val GOLD_CHAIN: Item = register("gold_chain", Item(makeProperties("gold_chain", 64)))
    @JvmField val IRON_CHAIN: Item = register("iron_chain", Item(makeProperties("iron_chain", 64)))
    @JvmField val MITHRIL_CHAIN: Item = register("mithril_chain", Item(makeProperties("mithril_chain", 64)))
    @JvmField val ADAMANTIUM_CHAIN: Item = register("adamantium_chain", Item(makeProperties("adamantium_chain", 64)))
    @JvmField val ANCIENT_METAL_CHAIN: Item = register("ancient_metal_chain", Item(makeProperties("ancient_metal_chain", 64)))

    // ========== 钓鱼竿（ICPM R196 ItemFishingRod：2 木棍 + 线 + 碎片/粒，材料决定耐久） ==========
    // 用 ICPMFishingRodItem：清理可能残留的失效鱼钩引用，避免右键永远只能收线而无法抛竿
    @JvmField val FLINT_FISHING_ROD: Item = register("flint_fishing_rod", ICPMFishingRodItem(makeProperties("flint_fishing_rod", 1).durability(40).enchantable(1)))
    @JvmField val OBSIDIAN_FISHING_ROD: Item = register("obsidian_fishing_rod", ICPMFishingRodItem(makeProperties("obsidian_fishing_rod", 1).durability(100).enchantable(1)))
    @JvmField val COPPER_FISHING_ROD: Item = register("copper_fishing_rod", ICPMFishingRodItem(makeProperties("copper_fishing_rod", 1).durability(120).enchantable(1)))
    @JvmField val SILVER_FISHING_ROD: Item = register("silver_fishing_rod", ICPMFishingRodItem(makeProperties("silver_fishing_rod", 1).durability(150).enchantable(1)))
    @JvmField val GOLD_FISHING_ROD: Item = register("gold_fishing_rod", ICPMFishingRodItem(makeProperties("gold_fishing_rod", 1).durability(80).enchantable(1)))
    @JvmField val IRON_FISHING_ROD: Item = register("iron_fishing_rod", ICPMFishingRodItem(makeProperties("iron_fishing_rod", 1).durability(200).enchantable(1)))
    @JvmField val MITHRIL_FISHING_ROD: Item = register("mithril_fishing_rod", ICPMFishingRodItem(makeProperties("mithril_fishing_rod", 1).durability(400).enchantable(1)))
    @JvmField val ADAMANTIUM_FISHING_ROD: Item = register("adamantium_fishing_rod", ICPMFishingRodItem(makeProperties("adamantium_fishing_rod", 1).durability(800).enchantable(1)))
    @JvmField val ANCIENT_METAL_FISHING_ROD: Item = register("ancient_metal_fishing_rod", ICPMFishingRodItem(makeProperties("ancient_metal_fishing_rod", 1).durability(350).enchantable(1)))

    // ========== 马铠 (ICPM 多级金属马铠, 1.21.11 用 EQUIPPABLE 组件实现) ==========
    // 1.21.11 无 AnimalArmorItem：马铠是普通 Item + EQUIPPABLE 组件（slot=BODY, asset=装备资源, allowedEntities=马）。
    // 马能否穿戴由 minecraft:can_wear_horse_armor 实体标签决定（马/僵尸马已在内），无需物品标签。
    private fun horseEquippable(metal: String): Equippable =
        Equippable.builder(EquipmentSlot.BODY)
            .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "${metal}_horse_armor")))
            .setAllowedEntities(EntityType.HORSE)
            .build()

    @JvmField val COPPER_HORSE_ARMOR: Item = register("copper_horse_armor",
        Item(makeProperties("copper_horse_armor", 1)
            .enchantable(30)
            .durability(miteArmorDurability(COPPER_MAT_DUR, HORSE_COMPONENTS))
            .component(DataComponents.EQUIPPABLE, horseEquippable("copper"))))
    @JvmField val SILVER_HORSE_ARMOR: Item = register("silver_horse_armor",
        Item(makeProperties("silver_horse_armor", 1)
            .enchantable(30)
            .durability(miteArmorDurability(SILVER_MAT_DUR, HORSE_COMPONENTS))
            .component(DataComponents.EQUIPPABLE, horseEquippable("silver"))))
    @JvmField val ANCIENT_METAL_HORSE_ARMOR: Item = register("ancient_metal_horse_armor",
        Item(makeProperties("ancient_metal_horse_armor", 1)
            .enchantable(40)
            .durability(miteArmorDurability(ANCIENT_METAL_MAT_DUR, HORSE_COMPONENTS))
            .component(DataComponents.EQUIPPABLE, horseEquippable("ancient_metal"))))
    @JvmField val MITHRIL_HORSE_ARMOR: Item = register("mithril_horse_armor",
        Item(makeProperties("mithril_horse_armor", 1)
            .enchantable(100)
            .durability(miteArmorDurability(MITHRIL_MAT_DUR, HORSE_COMPONENTS))
            .component(DataComponents.EQUIPPABLE, horseEquippable("mithril"))))
    @JvmField val ADAMANTIUM_HORSE_ARMOR: Item = register("adamantium_horse_armor",
        Item(makeProperties("adamantium_horse_armor", 1)
            .enchantable(40)
            .durability(miteArmorDurability(ADAMANTIUM_MAT_DUR, HORSE_COMPONENTS))
            .component(DataComponents.EQUIPPABLE, horseEquippable("adamantium"))))

    // 燧石工具 (基于 R196 原版：仅铲子、短斧、斧子)
    @JvmField val FLINT_SHOVEL: Item = register("flint_shovel",
        ShovelItem(FLINT_TIER, 0.0f, -3.0f, makeProperties("flint_shovel", 1)
            .durability(miteDurability(FLINT_MAT_DUR, SHOVEL_COMPONENTS))))
    @JvmField val FLINT_HATCHET: Item = register("flint_hatchet",
        AxeItem(FLINT_TIER, 1.0f, -3.2f, makeProperties("flint_hatchet", 1)
            .durability(miteDurability(FLINT_MAT_DUR, HATCHET_COMPONENTS))))
    @JvmField val FLINT_AXE: Item = register("flint_axe",
        AxeItem(FLINT_TIER, 2.0f, -3.0f, makeProperties("flint_axe", 1)
            .durability(miteDurability(FLINT_MAT_DUR, AXE_COMPONENTS))))

    // 小刀 (Knife) - 1.18.2: Icpm_fjv (dagger子类，伤害再-2)，flint_knife(400, FLINT, 2.5F, 0.0F)
    // 最终伤害 = (2.5-2.0)+0.5 = 1.0，攻速 = 4.0+0.0 = 4.0
    @JvmField val FLINT_KNIFE: Item = register("flint_knife",
        Item(makeProperties("flint_knife", 1).sword(FLINT_TIER, -1.0f, 0.0f)
            .durability(miteDurability(FLINT_MAT_DUR, KNIFE_COMPONENTS))))

    // 黑曜石小刀 (Obsidian Knife) - 1.18.2: obsidian_knife(800, FLINT, 3.5F, 0.0F)
    // 最终伤害 = (3.5-2.0)+0.5 = 2.0，攻速 = 4.0
    @JvmField val OBSIDIAN_KNIFE: Item = register("obsidian_knife",
        Item(makeProperties("obsidian_knife", 1).sword(FLINT_TIER, 0.0f, 0.0f)
            .durability(800)))

    // 矿石物品由 ICPM.java 的 registerAllBlocks() 自动创建，无需手动注册

    // ========== 铜系工具 ==========
    // 原版 1.21.11 已有 copper_pickaxe/axe/shovel/hoe/sword 及铜甲，ICPM 不再重复注册
    // 其 ICPM 属性（耐久/伤害）通过 VanillaToolDurabilityMixin + ICPMToolProperties 注入

    // ========== 铜制特殊工具 ==========
    @JvmField val COPPER_HATCHET: Item = register("copper_hatchet",
        AxeItem(COPPER_TIER, 1.0f, -3.2f, makeProperties("copper_hatchet", 1)
            .durability(miteDurability(COPPER_MAT_DUR, HATCHET_COMPONENTS))))
    @JvmField val COPPER_DAGGER: Item = register("copper_dagger",
        Item(makeProperties("copper_dagger", 1).sword(COPPER_TIER, 1.0f, -1.8f)
            .durability(miteDurability(COPPER_MAT_DUR, DAGGER_COMPONENTS))))
    @JvmField val COPPER_SCYTHE: Item = register("copper_scythe",
        HoeItem(COPPER_TIER, 0.0f, -2.0f, makeProperties("copper_scythe", 1)
            .durability(miteDurability(COPPER_MAT_DUR, SCYTHE_COMPONENTS))))
    @JvmField val WOOD_CUDGEL: Item = register("wood_cudgel", Item(makeProperties("wood_cudgel", 1)))
    @JvmField val COPPER_WAR_HAMMER: Item = register("copper_war_hammer",
        Item(makeProperties("copper_war_hammer", 1).pickaxe(COPPER_TIER, 2.0f, -3.5f)
            .durability(miteDurability(COPPER_MAT_DUR, WAR_HAMMER_COMPONENTS))))
    @JvmField val COPPER_BATTLE_AXE: Item = register("copper_battle_axe",
        AxeItem(COPPER_TIER, 4.0f, -3.2f, makeProperties("copper_battle_axe", 1)
            .durability(miteDurability(COPPER_MAT_DUR, BATTLE_AXE_COMPONENTS))))
    @JvmField val COPPER_MATTOCK: Item = register("copper_mattock",
        ShovelItem(COPPER_TIER, 1.0f, -3.0f, makeProperties("copper_mattock", 1)
            .durability(miteDurability(COPPER_MAT_DUR, MATTOCK_COMPONENTS))))
    @JvmField val COPPER_SHEARS: Item = register("copper_shears",
        ShearsItem(makeProperties("copper_shears", 1)
            .enchantable(30)
            .durability(miteDurability(COPPER_MAT_DUR, SHEARS_COMPONENTS))))

    // ========== 金系工具 ==========
    // 原版 1.21.11 已有 golden_pickaxe/axe/shovel/hoe/sword 及金甲，ICPM 不再重复注册
    // 其 ICPM 属性（耐久/伤害）通过 VanillaToolDurabilityMixin + ICPMToolProperties 注入

    // ========== 金制特殊工具 ==========
    @JvmField val GOLD_HATCHET: Item = register("gold_hatchet",
        AxeItem(GOLD_TIER, 1.0f, -3.2f, makeProperties("gold_hatchet", 1)
            .durability(miteDurability(GOLD_MAT_DUR, HATCHET_COMPONENTS))))
    @JvmField val GOLD_DAGGER: Item = register("gold_dagger",
        Item(makeProperties("gold_dagger", 1).sword(GOLD_TIER, 1.0f, -1.8f)
            .durability(miteDurability(GOLD_MAT_DUR, DAGGER_COMPONENTS))))
    @JvmField val GOLD_SCYTHE: Item = register("gold_scythe",
        HoeItem(GOLD_TIER, 0.0f, -2.0f, makeProperties("gold_scythe", 1)
            .durability(miteDurability(GOLD_MAT_DUR, SCYTHE_COMPONENTS))))
    @JvmField val GOLD_WAR_HAMMER: Item = register("gold_war_hammer",
        Item(makeProperties("gold_war_hammer", 1).pickaxe(GOLD_TIER, 2.0f, -3.5f)
            .durability(miteDurability(GOLD_MAT_DUR, WAR_HAMMER_COMPONENTS))))
    @JvmField val GOLD_BATTLE_AXE: Item = register("gold_battle_axe",
        AxeItem(GOLD_TIER, 4.0f, -3.2f, makeProperties("gold_battle_axe", 1)
            .durability(miteDurability(GOLD_MAT_DUR, BATTLE_AXE_COMPONENTS))))
    @JvmField val GOLD_MATTOCK: Item = register("gold_mattock",
        ShovelItem(GOLD_TIER, 1.0f, -3.0f, makeProperties("gold_mattock", 1)
            .durability(miteDurability(GOLD_MAT_DUR, MATTOCK_COMPONENTS))))
    @JvmField val GOLD_SHEARS: Item = register("gold_shears",
        ShearsItem(makeProperties("gold_shears", 1)
            .enchantable(50)
            .durability(miteDurability(GOLD_MAT_DUR, SHEARS_COMPONENTS))))

    // ========== 银系物品 ==========
    // 铜粒使用原版 minecraft:copper_nugget，不再注册 ICPM 自定义铜粒
    @JvmField val SILVER_NUGGET: Item = register("silver_nugget", Item(makeProperties("silver_nugget", 64)))
    @JvmField val SILVER_INGOT: Item = register("silver_ingot", Item(makeProperties("silver_ingot", 64)))

    @JvmField val SILVER_SHOVEL: Item = register("silver_shovel",
        ShovelItem(SILVER_TIER, 1.0f, -3.0f, makeProperties("silver_shovel", 1)
            .durability(miteDurability(SILVER_MAT_DUR, SHOVEL_COMPONENTS))))
    @JvmField val SILVER_AXE: Item = register("silver_axe",
        AxeItem(SILVER_TIER, 2.0f, -3.0f, makeProperties("silver_axe", 1)
            .durability(miteDurability(SILVER_MAT_DUR, AXE_COMPONENTS))))
    @JvmField val SILVER_HOE: Item = register("silver_hoe",
        HoeItem(SILVER_TIER, 0.0f, -2.0f, makeProperties("silver_hoe", 1)
            .durability(miteDurability(SILVER_MAT_DUR, HOE_COMPONENTS))))
    @JvmField val SILVER_PICKAXE: Item = register("silver_pickaxe",
        Item(makeProperties("silver_pickaxe", 1).pickaxe(SILVER_TIER, 1.0f, -2.8f)
            .durability(miteDurability(SILVER_MAT_DUR, PICKAXE_COMPONENTS))))
    @JvmField val SILVER_SWORD: Item = register("silver_sword",
        Item(makeProperties("silver_sword", 1).sword(SILVER_TIER, 3.0f, -2.4f)
            .durability(miteDurability(SILVER_MAT_DUR, SWORD_COMPONENTS))))

    // ========== 银制特殊工具 (R196 原版) ==========
    @JvmField val SILVER_HATCHET: Item = register("silver_hatchet",
        AxeItem(SILVER_TIER, 1.0f, -3.2f, makeProperties("silver_hatchet", 1)
            .durability(miteDurability(SILVER_MAT_DUR, HATCHET_COMPONENTS))))
    @JvmField val SILVER_DAGGER: Item = register("silver_dagger",
        Item(makeProperties("silver_dagger", 1).sword(SILVER_TIER, 1.0f, -1.8f)
            .durability(miteDurability(SILVER_MAT_DUR, DAGGER_COMPONENTS))))
    @JvmField val SILVER_WAR_HAMMER: Item = register("silver_war_hammer",
        Item(makeProperties("silver_war_hammer", 1).pickaxe(SILVER_TIER, 2.0f, -3.5f)
            .durability(miteDurability(SILVER_MAT_DUR, WAR_HAMMER_COMPONENTS))))
    @JvmField val SILVER_BATTLE_AXE: Item = register("silver_battle_axe",
        AxeItem(SILVER_TIER, 4.0f, -3.2f, makeProperties("silver_battle_axe", 1)
            .durability(miteDurability(SILVER_MAT_DUR, BATTLE_AXE_COMPONENTS))))
    @JvmField val SILVER_SCYTHE: Item = register("silver_scythe",
        HoeItem(SILVER_TIER, 0.0f, -2.0f, makeProperties("silver_scythe", 1)
            .durability(miteDurability(SILVER_MAT_DUR, SCYTHE_COMPONENTS))))
    @JvmField val SILVER_MATTOCK: Item = register("silver_mattock",
        ShovelItem(SILVER_TIER, 1.0f, -3.0f, makeProperties("silver_mattock", 1)
            .durability(miteDurability(SILVER_MAT_DUR, MATTOCK_COMPONENTS))))
    @JvmField val SILVER_SHEARS: Item = register("silver_shears",
        ShearsItem(makeProperties("silver_shears", 1)
            .enchantable(30)
            .durability(miteDurability(SILVER_MAT_DUR, SHEARS_COMPONENTS))))
    @JvmField val SILVER_SPEAR: Item = register("silver_spear",
        Item(makeProperties("silver_spear", 1).spear(SILVER_TIER, 0.85f, 0.82f, 0.65f, 4.0f, 9.0f, 8.25f, 5.1f, 12.5f, 4.6f)
            .durability(miteDurability(SILVER_MAT_DUR, SPEAR_COMPONENTS))))

    // ========== 银制盔甲 (使用自定义材质，防御值基于 R196 源码) ==========
    // R196 银盔甲耐久: 部件数 × 材质耐久 × 2
    // 银材质系数 = 4.0: 头盔 5×4×2=40, 胸甲 8×4×2=64, 护腿 7×4×2=56, 靴子 4×4×2=32
    @JvmField val SILVER_HELMET: Item = register("silver_helmet",
        Item(makeProperties("silver_helmet", 1).humanoidArmor(SILVER_ARMOR_MAT, ArmorType.HELMET)
            .durability(miteArmorDurability(SILVER_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val SILVER_CHESTPLATE: Item = register("silver_chestplate",
        Item(makeProperties("silver_chestplate", 1).humanoidArmor(SILVER_ARMOR_MAT, ArmorType.CHESTPLATE)
            .durability(miteArmorDurability(SILVER_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val SILVER_LEGGINGS: Item = register("silver_leggings",
        Item(makeProperties("silver_leggings", 1).humanoidArmor(SILVER_ARMOR_MAT, ArmorType.LEGGINGS)
            .durability(miteArmorDurability(SILVER_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val SILVER_BOOTS: Item = register("silver_boots",
        Item(makeProperties("silver_boots", 1).humanoidArmor(SILVER_ARMOR_MAT, ArmorType.BOOTS)
            .durability(miteArmorDurability(SILVER_MAT_DUR, BOOTS_COMPONENTS))))

    // ========== 远古金属系物品 ==========
    @JvmField val ANCIENT_METAL_NUGGET: Item = register("ancient_metal_nugget", Item(makeProperties("ancient_metal_nugget", 64)))
    @JvmField val ANCIENT_METAL_INGOT: Item = register("ancient_metal_ingot", Item(makeProperties("ancient_metal_ingot", 64)))

    // ========== 远古金属制盔甲 (使用自定义材质，防御值基于 R196 源码) ==========
    // R196 远古金属盔甲耐久: 部件数 × 材质耐久 × 2
    // 远古金属材质系数 = 16.0: 头盔 5×16×2=160, 胸甲 8×16×2=256, 护腿 7×16×2=224, 靴子 4×16×2=128
    @JvmField val ANCIENT_METAL_HELMET: Item = register("ancient_metal_helmet",
        Item(makeProperties("ancient_metal_helmet", 1).humanoidArmor(ANCIENT_METAL_ARMOR_MAT, ArmorType.HELMET)
            .durability(miteArmorDurability(ANCIENT_METAL_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val ANCIENT_METAL_CHESTPLATE: Item = register("ancient_metal_chestplate",
        Item(makeProperties("ancient_metal_chestplate", 1).humanoidArmor(ANCIENT_METAL_ARMOR_MAT, ArmorType.CHESTPLATE)
            .durability(miteArmorDurability(ANCIENT_METAL_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val ANCIENT_METAL_LEGGINGS: Item = register("ancient_metal_leggings",
        Item(makeProperties("ancient_metal_leggings", 1).humanoidArmor(ANCIENT_METAL_ARMOR_MAT, ArmorType.LEGGINGS)
            .durability(miteArmorDurability(ANCIENT_METAL_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val ANCIENT_METAL_BOOTS: Item = register("ancient_metal_boots",
        Item(makeProperties("ancient_metal_boots", 1).humanoidArmor(ANCIENT_METAL_ARMOR_MAT, ArmorType.BOOTS)
            .durability(miteArmorDurability(ANCIENT_METAL_MAT_DUR, BOOTS_COMPONENTS))))

    @JvmField val ANCIENT_METAL_SHOVEL: Item = register("ancient_metal_shovel",
        ShovelItem(ANCIENT_METAL_TIER, 1.0f, -3.0f, makeProperties("ancient_metal_shovel", 1)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, SHOVEL_COMPONENTS))))
    @JvmField val ANCIENT_METAL_AXE: Item = register("ancient_metal_axe",
        AxeItem(ANCIENT_METAL_TIER, 2.0f, -3.0f, makeProperties("ancient_metal_axe", 1)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, AXE_COMPONENTS))))
    @JvmField val ANCIENT_METAL_HOE: Item = register("ancient_metal_hoe",
        HoeItem(ANCIENT_METAL_TIER, 0.0f, -2.0f, makeProperties("ancient_metal_hoe", 1)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, HOE_COMPONENTS))))
    @JvmField val ANCIENT_METAL_PICKAXE: Item = register("ancient_metal_pickaxe",
        Item(makeProperties("ancient_metal_pickaxe", 1).pickaxe(ANCIENT_METAL_TIER, 1.0f, -2.8f)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, PICKAXE_COMPONENTS))))
    @JvmField val ANCIENT_METAL_SWORD: Item = register("ancient_metal_sword",
        Item(makeProperties("ancient_metal_sword", 1).sword(ANCIENT_METAL_TIER, 3.0f, -2.4f)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, SWORD_COMPONENTS))))

    // ========== 远古金属特殊工具 (R196 原版) ==========
    @JvmField val ANCIENT_METAL_HATCHET: Item = register("ancient_metal_hatchet",
        AxeItem(ANCIENT_METAL_TIER, 1.0f, -3.0f, makeProperties("ancient_metal_hatchet", 1)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, HATCHET_COMPONENTS))))
    @JvmField val ANCIENT_METAL_DAGGER: Item = register("ancient_metal_dagger",
        Item(makeProperties("ancient_metal_dagger", 1).sword(ANCIENT_METAL_TIER, 1.0f, -1.8f)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, DAGGER_COMPONENTS))))
    @JvmField val ANCIENT_METAL_WAR_HAMMER: Item = register("ancient_metal_war_hammer",
        Item(makeProperties("ancient_metal_war_hammer", 1).pickaxe(ANCIENT_METAL_TIER, 2.0f, -3.5f)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, WAR_HAMMER_COMPONENTS))))
    @JvmField val ANCIENT_METAL_BATTLE_AXE: Item = register("ancient_metal_battle_axe",
        AxeItem(ANCIENT_METAL_TIER, 4.0f, -3.2f, makeProperties("ancient_metal_battle_axe", 1)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, BATTLE_AXE_COMPONENTS))))
    @JvmField val ANCIENT_METAL_SCYTHE: Item = register("ancient_metal_scythe",
        HoeItem(ANCIENT_METAL_TIER, 0.0f, -2.0f, makeProperties("ancient_metal_scythe", 1)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, SCYTHE_COMPONENTS))))
    @JvmField val ANCIENT_METAL_MATTOCK: Item = register("ancient_metal_mattock",
        ShovelItem(ANCIENT_METAL_TIER, 1.0f, -3.0f, makeProperties("ancient_metal_mattock", 1)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, MATTOCK_COMPONENTS))))
    @JvmField val ANCIENT_METAL_SHEARS: Item = register("ancient_metal_shears",
        ShearsItem(makeProperties("ancient_metal_shears", 1)
            .enchantable(40)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, SHEARS_COMPONENTS))))
    @JvmField val ANCIENT_METAL_SPEAR: Item = register("ancient_metal_spear",
        Item(makeProperties("ancient_metal_spear", 1).spear(ANCIENT_METAL_TIER, 0.95f, 0.95f, 0.60f, 2.5f, 8.0f, 6.75f, 5.1f, 11.25f, 4.6f)
            .durability(miteDurability(ANCIENT_METAL_MAT_DUR, SPEAR_COMPONENTS))))

    // ========== 秘银系物品 ==========
    @JvmField val MITHRIL_NUGGET: Item = register("mithril_nugget", Item(makeProperties("mithril_nugget", 64)))
    @JvmField val MITHRIL_INGOT: Item = register("mithril_ingot", Item(makeProperties("mithril_ingot", 64)))

    // ========== 秘银制盔甲 (使用自定义材质，防御值基于 R196 源码) ==========
    // R196 秘银盔甲耐久: 部件数 × 材质耐久 × 2
    // 秘银材质系数 = 64.0: 头盔 5×64×2=640, 胸甲 8×64×2=1024, 护腿 7×64×2=896, 靴子 4×64×2=512
    @JvmField val MITHRIL_HELMET: Item = register("mithril_helmet",
        Item(makeProperties("mithril_helmet", 1).humanoidArmor(MITHRIL_ARMOR_MAT, ArmorType.HELMET)
            .durability(miteArmorDurability(MITHRIL_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val MITHRIL_CHESTPLATE: Item = register("mithril_chestplate",
        Item(makeProperties("mithril_chestplate", 1).humanoidArmor(MITHRIL_ARMOR_MAT, ArmorType.CHESTPLATE)
            .durability(miteArmorDurability(MITHRIL_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val MITHRIL_LEGGINGS: Item = register("mithril_leggings",
        Item(makeProperties("mithril_leggings", 1).humanoidArmor(MITHRIL_ARMOR_MAT, ArmorType.LEGGINGS)
            .durability(miteArmorDurability(MITHRIL_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val MITHRIL_BOOTS: Item = register("mithril_boots",
        Item(makeProperties("mithril_boots", 1).humanoidArmor(MITHRIL_ARMOR_MAT, ArmorType.BOOTS)
            .durability(miteArmorDurability(MITHRIL_MAT_DUR, BOOTS_COMPONENTS))))

    @JvmField val MITHRIL_SHOVEL: Item = register("mithril_shovel",
        ShovelItem(MITHRIL_TIER, 1.0f, -3.0f, makeProperties("mithril_shovel", 1)
            .durability(miteDurability(MITHRIL_MAT_DUR, SHOVEL_COMPONENTS))))
    @JvmField val MITHRIL_AXE: Item = register("mithril_axe",
        AxeItem(MITHRIL_TIER, 2.0f, -3.0f, makeProperties("mithril_axe", 1)
            .durability(miteDurability(MITHRIL_MAT_DUR, AXE_COMPONENTS))))
    @JvmField val MITHRIL_HOE: Item = register("mithril_hoe",
        HoeItem(MITHRIL_TIER, 0.0f, -2.0f, makeProperties("mithril_hoe", 1)
            .durability(miteDurability(MITHRIL_MAT_DUR, HOE_COMPONENTS))))
    @JvmField val MITHRIL_PICKAXE: Item = register("mithril_pickaxe",
        Item(makeProperties("mithril_pickaxe", 1).pickaxe(MITHRIL_TIER, 1.0f, -2.8f)
            .durability(miteDurability(MITHRIL_MAT_DUR, PICKAXE_COMPONENTS))))
    @JvmField val MITHRIL_SWORD: Item = register("mithril_sword",
        Item(makeProperties("mithril_sword", 1).sword(MITHRIL_TIER, 3.0f, -2.4f)
            .durability(miteDurability(MITHRIL_MAT_DUR, SWORD_COMPONENTS))))

    // ========== 秘银特殊工具 (R196 原版) ==========
    @JvmField val MITHRIL_HATCHET: Item = register("mithril_hatchet",
        AxeItem(MITHRIL_TIER, 1.0f, -3.0f, makeProperties("mithril_hatchet", 1)
            .durability(miteDurability(MITHRIL_MAT_DUR, HATCHET_COMPONENTS))))
    @JvmField val MITHRIL_DAGGER: Item = register("mithril_dagger",
        Item(makeProperties("mithril_dagger", 1).sword(MITHRIL_TIER, 1.0f, -1.8f)
            .durability(miteDurability(MITHRIL_MAT_DUR, DAGGER_COMPONENTS))))
    @JvmField val MITHRIL_WAR_HAMMER: Item = register("mithril_war_hammer",
        Item(makeProperties("mithril_war_hammer", 1).pickaxe(MITHRIL_TIER, 2.0f, -3.5f)
            .durability(miteDurability(MITHRIL_MAT_DUR, WAR_HAMMER_COMPONENTS))))
    @JvmField val MITHRIL_BATTLE_AXE: Item = register("mithril_battle_axe",
        AxeItem(MITHRIL_TIER, 4.0f, -3.2f, makeProperties("mithril_battle_axe", 1)
            .durability(miteDurability(MITHRIL_MAT_DUR, BATTLE_AXE_COMPONENTS))))
    @JvmField val MITHRIL_SCYTHE: Item = register("mithril_scythe",
        HoeItem(MITHRIL_TIER, 0.0f, -2.0f, makeProperties("mithril_scythe", 1)
            .durability(miteDurability(MITHRIL_MAT_DUR, SCYTHE_COMPONENTS))))
    @JvmField val MITHRIL_MATTOCK: Item = register("mithril_mattock",
        ShovelItem(MITHRIL_TIER, 1.0f, -3.0f, makeProperties("mithril_mattock", 1)
            .durability(miteDurability(MITHRIL_MAT_DUR, MATTOCK_COMPONENTS))))
    @JvmField val MITHRIL_SHEARS: Item = register("mithril_shears",
        ShearsItem(makeProperties("mithril_shears", 1)
            .enchantable(100)
            .durability(miteDurability(MITHRIL_MAT_DUR, SHEARS_COMPONENTS))))
    @JvmField val MITHRIL_SPEAR: Item = register("mithril_spear",
        Item(makeProperties("mithril_spear", 1).spear(MITHRIL_TIER, 1.05f, 1.075f, 0.50f, 3.0f, 7.5f, 6.5f, 5.1f, 10.0f, 4.6f)
            .durability(miteDurability(MITHRIL_MAT_DUR, SPEAR_COMPONENTS))))

    // ========== 艾德曼系物品 ==========
    @JvmField val ADAMANTIUM_NUGGET: Item = register("adamantium_nugget", Item(makeProperties("adamantium_nugget", 64)))
    @JvmField val ADAMANTIUM_INGOT: Item = register("adamantium_ingot", Item(makeProperties("adamantium_ingot", 64)))

    // ========== 艾德曼制盔甲 (使用自定义材质，防御值基于 R196 源码) ==========
    // R196 艾德曼盔甲耐久: 部件数 × 材质耐久 × 2
    // 艾德曼材质系数 = 256.0: 头盔 5×256×2=2560, 胸甲 8×256×2=4096, 护腿 7×256×2=3584, 靴子 4×256×2=2048
    @JvmField val ADAMANTIUM_HELMET: Item = register("adamantium_helmet",
        Item(makeProperties("adamantium_helmet", 1).humanoidArmor(ADAMANTIUM_ARMOR_MAT, ArmorType.HELMET)
            .durability(miteArmorDurability(ADAMANTIUM_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val ADAMANTIUM_CHESTPLATE: Item = register("adamantium_chestplate",
        Item(makeProperties("adamantium_chestplate", 1).humanoidArmor(ADAMANTIUM_ARMOR_MAT, ArmorType.CHESTPLATE)
            .durability(miteArmorDurability(ADAMANTIUM_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val ADAMANTIUM_LEGGINGS: Item = register("adamantium_leggings",
        Item(makeProperties("adamantium_leggings", 1).humanoidArmor(ADAMANTIUM_ARMOR_MAT, ArmorType.LEGGINGS)
            .durability(miteArmorDurability(ADAMANTIUM_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val ADAMANTIUM_BOOTS: Item = register("adamantium_boots",
        Item(makeProperties("adamantium_boots", 1).humanoidArmor(ADAMANTIUM_ARMOR_MAT, ArmorType.BOOTS)
            .durability(miteArmorDurability(ADAMANTIUM_MAT_DUR, BOOTS_COMPONENTS))))

    @JvmField val ADAMANTIUM_SHOVEL: Item = register("adamantium_shovel",
        ShovelItem(ADAMANTIUM_TIER, 1.0f, -3.0f, makeProperties("adamantium_shovel", 1)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, SHOVEL_COMPONENTS))))
    @JvmField val ADAMANTIUM_AXE: Item = register("adamantium_axe",
        AxeItem(ADAMANTIUM_TIER, 2.0f, -3.0f, makeProperties("adamantium_axe", 1)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, AXE_COMPONENTS))))
    @JvmField val ADAMANTIUM_HOE: Item = register("adamantium_hoe",
        HoeItem(ADAMANTIUM_TIER, 0.0f, -2.0f, makeProperties("adamantium_hoe", 1)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, HOE_COMPONENTS))))
    @JvmField val ADAMANTIUM_PICKAXE: Item = register("adamantium_pickaxe",
        Item(makeProperties("adamantium_pickaxe", 1).pickaxe(ADAMANTIUM_TIER, 1.0f, -2.8f)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, PICKAXE_COMPONENTS))))
    @JvmField val ADAMANTIUM_SWORD: Item = register("adamantium_sword",
        Item(makeProperties("adamantium_sword", 1).sword(ADAMANTIUM_TIER, 3.0f, -2.4f)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, SWORD_COMPONENTS))))

    // ========== 艾德曼特殊工具 (R196 原版) ==========
    @JvmField val ADAMANTIUM_HATCHET: Item = register("adamantium_hatchet",
        AxeItem(ADAMANTIUM_TIER, 1.0f, -3.0f, makeProperties("adamantium_hatchet", 1)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, HATCHET_COMPONENTS))))
    @JvmField val ADAMANTIUM_DAGGER: Item = register("adamantium_dagger",
        Item(makeProperties("adamantium_dagger", 1).sword(ADAMANTIUM_TIER, 1.0f, -1.8f)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, DAGGER_COMPONENTS))))
    @JvmField val ADAMANTIUM_WAR_HAMMER: Item = register("adamantium_war_hammer",
        Item(makeProperties("adamantium_war_hammer", 1).pickaxe(ADAMANTIUM_TIER, 2.0f, -3.5f)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, WAR_HAMMER_COMPONENTS))))
    @JvmField val ADAMANTIUM_BATTLE_AXE: Item = register("adamantium_battle_axe",
        AxeItem(ADAMANTIUM_TIER, 4.0f, -3.2f, makeProperties("adamantium_battle_axe", 1)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, BATTLE_AXE_COMPONENTS))))
    @JvmField val ADAMANTIUM_SCYTHE: Item = register("adamantium_scythe",
        HoeItem(ADAMANTIUM_TIER, 0.0f, -2.0f, makeProperties("adamantium_scythe", 1)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, SCYTHE_COMPONENTS))))
    @JvmField val ADAMANTIUM_MATTOCK: Item = register("adamantium_mattock",
        ShovelItem(ADAMANTIUM_TIER, 1.0f, -3.0f, makeProperties("adamantium_mattock", 1)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, MATTOCK_COMPONENTS))))
    @JvmField val ADAMANTIUM_SHEARS: Item = register("adamantium_shears",
        ShearsItem(makeProperties("adamantium_shears", 1)
            .enchantable(40)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, SHEARS_COMPONENTS))))
    @JvmField val ADAMANTIUM_SPEAR: Item = register("adamantium_spear",
        Item(makeProperties("adamantium_spear", 1).spear(ADAMANTIUM_TIER, 1.15f, 1.20f, 0.40f, 2.5f, 7.0f, 5.5f, 5.1f, 8.75f, 4.6f)
            .durability(miteDurability(ADAMANTIUM_MAT_DUR, SPEAR_COMPONENTS))))

    // ========== 锁链甲 (R196 锁甲耐久 = 部件数 × 材质系数 × 1，不乘2) ==========
    // 7 种材质 × 头盔/胸甲/护腿/靴子 = 28 件
    // 防御值基于 R196 源码，与对应金属板甲一致（锁甲防御不衰减）
    private val COPPER_CHAINMAIL_MAT = createArmorMaterial(
        "copper_chainmail", mapOf(
            ArmorType.HELMET to 2,
            ArmorType.CHESTPLATE to 3,
            ArmorType.LEGGINGS to 2,
            ArmorType.BOOTS to 1
        ), 30, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_copper"))
    )
    private val GOLD_CHAINMAIL_MAT = createArmorMaterial(
        "gold_chainmail", mapOf(
            ArmorType.HELMET to 2,
            ArmorType.CHESTPLATE to 3,
            ArmorType.LEGGINGS to 2,
            ArmorType.BOOTS to 1
        ), 50, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_gold"))
    )
    private val IRON_CHAINMAIL_MAT = createArmorMaterial(
        "iron_chainmail", mapOf(
            ArmorType.HELMET to 2,
            ArmorType.CHESTPLATE to 4,
            ArmorType.LEGGINGS to 3,
            ArmorType.BOOTS to 1
        ), 30, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0f, 0.0f,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "repair_iron"))
    )

    private fun chainmailDurability(materialDurability: Float, components: Int): Int {
        return (components * materialDurability * 1).toInt()
    }

    // 铜锁链甲 (铜材质系数 4.0: 头盔 20, 胸甲 32, 护腿 28, 靴子 16)
    @JvmField val COPPER_CHAINMAIL_HELMET: Item = register("copper_chainmail_helmet",
        Item(makeProperties("copper_chainmail_helmet", 1).humanoidArmor(COPPER_CHAINMAIL_MAT, ArmorType.HELMET)
            .durability(chainmailDurability(COPPER_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val COPPER_CHAINMAIL_CHESTPLATE: Item = register("copper_chainmail_chestplate",
        Item(makeProperties("copper_chainmail_chestplate", 1).humanoidArmor(COPPER_CHAINMAIL_MAT, ArmorType.CHESTPLATE)
            .durability(chainmailDurability(COPPER_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val COPPER_CHAINMAIL_LEGGINGS: Item = register("copper_chainmail_leggings",
        Item(makeProperties("copper_chainmail_leggings", 1).humanoidArmor(COPPER_CHAINMAIL_MAT, ArmorType.LEGGINGS)
            .durability(chainmailDurability(COPPER_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val COPPER_CHAINMAIL_BOOTS: Item = register("copper_chainmail_boots",
        Item(makeProperties("copper_chainmail_boots", 1).humanoidArmor(COPPER_CHAINMAIL_MAT, ArmorType.BOOTS)
            .durability(chainmailDurability(COPPER_MAT_DUR, BOOTS_COMPONENTS))))

    // 金锁链甲 (金材质系数 4.0)
    @JvmField val GOLD_CHAINMAIL_HELMET: Item = register("gold_chainmail_helmet",
        Item(makeProperties("gold_chainmail_helmet", 1).humanoidArmor(GOLD_CHAINMAIL_MAT, ArmorType.HELMET)
            .durability(chainmailDurability(GOLD_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val GOLD_CHAINMAIL_CHESTPLATE: Item = register("gold_chainmail_chestplate",
        Item(makeProperties("gold_chainmail_chestplate", 1).humanoidArmor(GOLD_CHAINMAIL_MAT, ArmorType.CHESTPLATE)
            .durability(chainmailDurability(GOLD_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val GOLD_CHAINMAIL_LEGGINGS: Item = register("gold_chainmail_leggings",
        Item(makeProperties("gold_chainmail_leggings", 1).humanoidArmor(GOLD_CHAINMAIL_MAT, ArmorType.LEGGINGS)
            .durability(chainmailDurability(GOLD_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val GOLD_CHAINMAIL_BOOTS: Item = register("gold_chainmail_boots",
        Item(makeProperties("gold_chainmail_boots", 1).humanoidArmor(GOLD_CHAINMAIL_MAT, ArmorType.BOOTS)
            .durability(chainmailDurability(GOLD_MAT_DUR, BOOTS_COMPONENTS))))

    // 铁锁链甲 (铁材质系数 8.0: 头盔 40, 胸甲 64, 护腿 56, 靴子 32)
    @JvmField val IRON_CHAINMAIL_HELMET: Item = register("iron_chainmail_helmet",
        Item(makeProperties("iron_chainmail_helmet", 1).humanoidArmor(IRON_CHAINMAIL_MAT, ArmorType.HELMET)
            .durability(chainmailDurability(IRON_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val IRON_CHAINMAIL_CHESTPLATE: Item = register("iron_chainmail_chestplate",
        Item(makeProperties("iron_chainmail_chestplate", 1).humanoidArmor(IRON_CHAINMAIL_MAT, ArmorType.CHESTPLATE)
            .durability(chainmailDurability(IRON_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val IRON_CHAINMAIL_LEGGINGS: Item = register("iron_chainmail_leggings",
        Item(makeProperties("iron_chainmail_leggings", 1).humanoidArmor(IRON_CHAINMAIL_MAT, ArmorType.LEGGINGS)
            .durability(chainmailDurability(IRON_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val IRON_CHAINMAIL_BOOTS: Item = register("iron_chainmail_boots",
        Item(makeProperties("iron_chainmail_boots", 1).humanoidArmor(IRON_CHAINMAIL_MAT, ArmorType.BOOTS)
            .durability(chainmailDurability(IRON_MAT_DUR, BOOTS_COMPONENTS))))

    // 银锁链甲 (银材质系数 4.0)
    @JvmField val SILVER_CHAINMAIL_HELMET: Item = register("silver_chainmail_helmet",
        Item(makeProperties("silver_chainmail_helmet", 1).humanoidArmor(SILVER_CHAINMAIL_MAT, ArmorType.HELMET)
            .durability(chainmailDurability(SILVER_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val SILVER_CHAINMAIL_CHESTPLATE: Item = register("silver_chainmail_chestplate",
        Item(makeProperties("silver_chainmail_chestplate", 1).humanoidArmor(SILVER_CHAINMAIL_MAT, ArmorType.CHESTPLATE)
            .durability(chainmailDurability(SILVER_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val SILVER_CHAINMAIL_LEGGINGS: Item = register("silver_chainmail_leggings",
        Item(makeProperties("silver_chainmail_leggings", 1).humanoidArmor(SILVER_CHAINMAIL_MAT, ArmorType.LEGGINGS)
            .durability(chainmailDurability(SILVER_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val SILVER_CHAINMAIL_BOOTS: Item = register("silver_chainmail_boots",
        Item(makeProperties("silver_chainmail_boots", 1).humanoidArmor(SILVER_CHAINMAIL_MAT, ArmorType.BOOTS)
            .durability(chainmailDurability(SILVER_MAT_DUR, BOOTS_COMPONENTS))))

    // 远古金属锁链甲 (远古金属材质系数 16.0: 头盔 80, 胸甲 128, 护腿 112, 靴子 64)
    @JvmField val ANCIENT_METAL_CHAINMAIL_HELMET: Item = register("ancient_metal_chainmail_helmet",
        Item(makeProperties("ancient_metal_chainmail_helmet", 1).humanoidArmor(ANCIENT_METAL_CHAINMAIL_MAT, ArmorType.HELMET)
            .durability(chainmailDurability(ANCIENT_METAL_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val ANCIENT_METAL_CHAINMAIL_CHESTPLATE: Item = register("ancient_metal_chainmail_chestplate",
        Item(makeProperties("ancient_metal_chainmail_chestplate", 1).humanoidArmor(ANCIENT_METAL_CHAINMAIL_MAT, ArmorType.CHESTPLATE)
            .durability(chainmailDurability(ANCIENT_METAL_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val ANCIENT_METAL_CHAINMAIL_LEGGINGS: Item = register("ancient_metal_chainmail_leggings",
        Item(makeProperties("ancient_metal_chainmail_leggings", 1).humanoidArmor(ANCIENT_METAL_CHAINMAIL_MAT, ArmorType.LEGGINGS)
            .durability(chainmailDurability(ANCIENT_METAL_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val ANCIENT_METAL_CHAINMAIL_BOOTS: Item = register("ancient_metal_chainmail_boots",
        Item(makeProperties("ancient_metal_chainmail_boots", 1).humanoidArmor(ANCIENT_METAL_CHAINMAIL_MAT, ArmorType.BOOTS)
            .durability(chainmailDurability(ANCIENT_METAL_MAT_DUR, BOOTS_COMPONENTS))))

    // 秘银锁链甲 (秘银材质系数 64.0: 头盔 320, 胸甲 512, 护腿 448, 靴子 256)
    @JvmField val MITHRIL_CHAINMAIL_HELMET: Item = register("mithril_chainmail_helmet",
        Item(makeProperties("mithril_chainmail_helmet", 1).humanoidArmor(MITHRIL_CHAINMAIL_MAT, ArmorType.HELMET)
            .durability(chainmailDurability(MITHRIL_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val MITHRIL_CHAINMAIL_CHESTPLATE: Item = register("mithril_chainmail_chestplate",
        Item(makeProperties("mithril_chainmail_chestplate", 1).humanoidArmor(MITHRIL_CHAINMAIL_MAT, ArmorType.CHESTPLATE)
            .durability(chainmailDurability(MITHRIL_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val MITHRIL_CHAINMAIL_LEGGINGS: Item = register("mithril_chainmail_leggings",
        Item(makeProperties("mithril_chainmail_leggings", 1).humanoidArmor(MITHRIL_CHAINMAIL_MAT, ArmorType.LEGGINGS)
            .durability(chainmailDurability(MITHRIL_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val MITHRIL_CHAINMAIL_BOOTS: Item = register("mithril_chainmail_boots",
        Item(makeProperties("mithril_chainmail_boots", 1).humanoidArmor(MITHRIL_CHAINMAIL_MAT, ArmorType.BOOTS)
            .durability(chainmailDurability(MITHRIL_MAT_DUR, BOOTS_COMPONENTS))))

    // 艾德曼锁链甲 (艾德曼材质系数 256.0: 头盔 1280, 胸甲 2048, 护腿 1792, 靴子 1024)
    @JvmField val ADAMANTIUM_CHAINMAIL_HELMET: Item = register("adamantium_chainmail_helmet",
        Item(makeProperties("adamantium_chainmail_helmet", 1).humanoidArmor(ADAMANTIUM_CHAINMAIL_MAT, ArmorType.HELMET)
            .durability(chainmailDurability(ADAMANTIUM_MAT_DUR, HELMET_COMPONENTS))))
    @JvmField val ADAMANTIUM_CHAINMAIL_CHESTPLATE: Item = register("adamantium_chainmail_chestplate",
        Item(makeProperties("adamantium_chainmail_chestplate", 1).humanoidArmor(ADAMANTIUM_CHAINMAIL_MAT, ArmorType.CHESTPLATE)
            .durability(chainmailDurability(ADAMANTIUM_MAT_DUR, CHESTPLATE_COMPONENTS))))
    @JvmField val ADAMANTIUM_CHAINMAIL_LEGGINGS: Item = register("adamantium_chainmail_leggings",
        Item(makeProperties("adamantium_chainmail_leggings", 1).humanoidArmor(ADAMANTIUM_CHAINMAIL_MAT, ArmorType.LEGGINGS)
            .durability(chainmailDurability(ADAMANTIUM_MAT_DUR, LEGGINGS_COMPONENTS))))
    @JvmField val ADAMANTIUM_CHAINMAIL_BOOTS: Item = register("adamantium_chainmail_boots",
        Item(makeProperties("adamantium_chainmail_boots", 1).humanoidArmor(ADAMANTIUM_CHAINMAIL_MAT, ArmorType.BOOTS)
            .durability(chainmailDurability(ADAMANTIUM_MAT_DUR, BOOTS_COMPONENTS))))

    // ========== 铁制特殊工具 (R196 铁: 耐久8.0, 附魔30, 伤害4.0) ==========
    // R196 iron: getDamageVsEntity() = 4.0f
    // 战锤伤害 = 稿子 + 1 = 5.0 + 1.0 = 6.0, 战斧伤害 = 剑 + 1 = 7.0 + 1.0 = 8.0
    @JvmField val IRON_HATCHET: Item = register("iron_hatchet",
        AxeItem(IRON_TIER, 1.0f, -3.2f, makeProperties("iron_hatchet", 1)
            .durability(miteDurability(IRON_MAT_DUR, HATCHET_COMPONENTS))))
    @JvmField val IRON_DAGGER: Item = register("iron_dagger",
        Item(makeProperties("iron_dagger", 1).sword(IRON_TIER, 1.0f, -1.8f)
            .durability(miteDurability(IRON_MAT_DUR, DAGGER_COMPONENTS))))
    @JvmField val IRON_WAR_HAMMER: Item = register("iron_war_hammer",
        Item(makeProperties("iron_war_hammer", 1).pickaxe(IRON_TIER, 2.0f, -3.5f)
            .durability(miteDurability(IRON_MAT_DUR, WAR_HAMMER_COMPONENTS))))
    @JvmField val IRON_BATTLE_AXE: Item = register("iron_battle_axe",
        AxeItem(IRON_TIER, 4.0f, -3.2f, makeProperties("iron_battle_axe", 1)
            .durability(miteDurability(IRON_MAT_DUR, BATTLE_AXE_COMPONENTS))))
    @JvmField val IRON_SCYTHE: Item = register("iron_scythe",
        HoeItem(IRON_TIER, 0.0f, -2.0f, makeProperties("iron_scythe", 1)
            .durability(miteDurability(IRON_MAT_DUR, SCYTHE_COMPONENTS))))
    @JvmField val IRON_MATTOCK: Item = register("iron_mattock",
        ShovelItem(IRON_TIER, 1.0f, -3.0f, makeProperties("iron_mattock", 1)
            .durability(miteDurability(IRON_MAT_DUR, MATTOCK_COMPONENTS))))

    // ========== 下界合金特殊工具 (ICPM 扩展) ==========
    // 通过艾德曼工具 + 下界合金升级模板 + 下界合金锭 在锻造台升级获得
    // 耐久使用 NETHERITE_MAT_DUR = 256.0 (同艾德曼)

    // 匕首 (Dagger) - damage = 1.0 (sword 基础 1.0 + 材质 7.0 + P 1.0 = 9.0)
    @JvmField val NETHERITE_DAGGER: Item = register("netherite_dagger",
        Item(makeProperties("netherite_dagger", 1).sword(NETHERITE_TIER, 1.0f, -1.8f)
            .durability(miteDurability(NETHERITE_MAT_DUR, DAGGER_COMPONENTS))))

    // 短斧 (Hatchet) - damage = 材质 7.0 + P 1.0 = 8.0 (斧类 attackDamage = 1.0 附加)
    @JvmField val NETHERITE_HATCHET: Item = register("netherite_hatchet",
        AxeItem(NETHERITE_TIER, 1.0f, -3.0f, makeProperties("netherite_hatchet", 1)
            .durability(miteDurability(NETHERITE_MAT_DUR, HATCHET_COMPONENTS))))

    // 战锤 (WarHammer) - damage = 材质 7.0 + P 2.0 = 10.0 (镐类, 可挖石头)
    @JvmField val NETHERITE_WAR_HAMMER: Item = register("netherite_war_hammer",
        Item(makeProperties("netherite_war_hammer", 1).pickaxe(NETHERITE_TIER, 2.0f, -3.5f)
            .durability(miteDurability(NETHERITE_MAT_DUR, WAR_HAMMER_COMPONENTS))))

    // 战斧 (BattleAxe) - damage = 材质 7.0 + P 4.0 = 12.0
    @JvmField val NETHERITE_BATTLE_AXE: Item = register("netherite_battle_axe",
        AxeItem(NETHERITE_TIER, 4.0f, -3.2f, makeProperties("netherite_battle_axe", 1)
            .durability(miteDurability(NETHERITE_MAT_DUR, BATTLE_AXE_COMPONENTS))))

    // 镰刀 (Scythe) - damage = 材质 7.0 + P 0.0 = 8.0
    @JvmField val NETHERITE_SCYTHE: Item = register("netherite_scythe",
        HoeItem(NETHERITE_TIER, 0.0f, -2.0f, makeProperties("netherite_scythe", 1)
            .durability(miteDurability(NETHERITE_MAT_DUR, SCYTHE_COMPONENTS))))

    // 鸭嘴锄 (Mattock) - damage = 材质 7.0 + P 1.0 = 9.0
    @JvmField val NETHERITE_MATTOCK: Item = register("netherite_mattock",
        ShovelItem(NETHERITE_TIER, 1.0f, -3.0f, makeProperties("netherite_mattock", 1)
            .durability(miteDurability(NETHERITE_MAT_DUR, MATTOCK_COMPONENTS))))

    // 下界合金长矛为原版 Minecraft 物品(minecraft:netherite_spear)，此处不重复注册。
    // 延续"艾德曼长矛 → 下界合金长矛"：通过锻造台升级配方(见 netherite_spear_from_adamantium.json)，
    // 将 ICPM 的 adamantium_spear 升级为原版 netherite_spear。

    // ========== ICPM 特有食物 ==========
    // 数值基于 1.18.2-ICPM 移植(IFW)与 ICPM 1.6.4 合成表。
    // 原版已有的食物不在此注册，而是通过 ICPMFoodInjectionMixin 注入 ICPM 数值。
    // 注意：史莱姆球（slime_ball / slime_sphere）原版与本 mod 均已存在，绝不重复注册。
    @JvmField val FLOUR: Item = register("flour", Item(makeProperties("flour", 64))) // 面粉：不可直接食用，仅合成材料
    @JvmField val DOUGH: Item = register("dough", Item(makeProperties("dough", 64).food(ICPMFoodProperties.DOUGH)))
    @JvmField val CHEESE: Item = register("cheese", Item(makeProperties("cheese", 64).food(ICPMFoodProperties.CHEESE)))
    @JvmField val CHOCOLATE: Item = register("chocolate", Item(makeProperties("chocolate", 64).food(ICPMFoodProperties.CHOCOLATE)))
    @JvmField val ICE_CREAM: Item = register("ice_cream", Item(makeProperties("ice_cream", 4).food(ICPMFoodProperties.ICE_CREAM)))
    @JvmField val SORBET: Item = register("sorbet", Item(makeProperties("sorbet", 4).food(ICPMFoodProperties.SORBET)))
    @JvmField val MASHED_POTATO: Item = register("mashed_potato", Item(makeProperties("mashed_potato", 4).food(ICPMFoodProperties.MASHED_POTATO)))
    @JvmField val BEEF_STEW: Item = register("beef_stew", Item(makeProperties("beef_stew", 4).food(ICPMFoodProperties.BEEF_STEW).usingConvertsTo(Items.BOWL)))
    @JvmField val CHICKEN_SOUP: Item = register("chicken_soup", Item(makeProperties("chicken_soup", 4).food(ICPMFoodProperties.CHICKEN_SOUP).usingConvertsTo(Items.BOWL)))
    @JvmField val VEGETABLE_SOUP: Item = register("vegetable_soup", Item(makeProperties("vegetable_soup", 4).food(ICPMFoodProperties.VEGETABLE_SOUP).usingConvertsTo(Items.BOWL)))
    @JvmField val VEGETABLE_SOUP_CREAM: Item = register("vegetable_soup_cream", Item(makeProperties("vegetable_soup_cream", 4).food(ICPMFoodProperties.VEGETABLE_SOUP_CREAM).usingConvertsTo(Items.BOWL)))
    @JvmField val MUSHROOM_SOUP_CREAM: Item = register("mushroom_soup_cream", Item(makeProperties("mushroom_soup_cream", 4).food(ICPMFoodProperties.MUSHROOM_SOUP_CREAM).usingConvertsTo(Items.BOWL)))
    @JvmField val PUMPKIN_SOUP: Item = register("pumpkin_soup", Item(makeProperties("pumpkin_soup", 4).food(ICPMFoodProperties.PUMPKIN_SOUP).usingConvertsTo(Items.BOWL)))
    @JvmField val SALAD: Item = register("salad", Item(makeProperties("salad", 4).food(ICPMFoodProperties.SALAD).usingConvertsTo(Items.BOWL)))
    @JvmField val PORRIDGE: Item = register("porridge", Item(makeProperties("porridge", 4).food(ICPMFoodProperties.PORRIDGE)))
    @JvmField val CEREAL: Item = register("cereal", Item(makeProperties("cereal", 4).food(ICPMFoodProperties.CEREAL).usingConvertsTo(Items.BOWL)))
    @JvmField val ORANGE: Item = register("orange", Item(makeProperties("orange", 16).food(ICPMFoodProperties.ORANGE)))
    @JvmField val BANANA: Item = register("banana", Item(makeProperties("banana", 16).food(ICPMFoodProperties.BANANA)))
    @JvmField val BLUEBERRY: Item = register("blueberry", Item(makeProperties("blueberry", 16).food(ICPMFoodProperties.BLUEBERRY)))
    @JvmField val ONION: Item = register("onion", Item(makeProperties("onion", 16).food(ICPMFoodProperties.ONION)))
    @JvmField val WORM: Item = register("worm", IcpmWormItem(makeProperties("worm", 16).food(ICPMFoodProperties.WORM)))
    @JvmField val COOKED_WORM: Item = register("cooked_worm", Item(makeProperties("cooked_worm", 16).food(ICPMFoodProperties.COOKED_WORM)))
    @JvmField val MILK_BOWL: Item = register("milk_bowl", Item(makeProperties("milk_bowl", 4).food(ICPMFoodProperties.MILK_BOWL).usingConvertsTo(Items.BOWL).craftRemainder(Items.BOWL)))
    @JvmField val WATER_BOWL: Item = register("water_bowl", Item(makeProperties("water_bowl", 4).food(ICPMFoodProperties.WATER_BOWL).usingConvertsTo(Items.BOWL).craftRemainder(Items.BOWL)))

    // ========== 去咒药水（R196 ItemBottleOfDisenchanting：饮用解咒，豁免禁饮诅咒） ==========
    @JvmField val BOTTLE_OF_DISENCHANTING: Item = register("bottle_of_disenchanting", CurseCureItem(makeProperties("bottle_of_disenchanting", 1)))

    // ========== 粪便（ICPM 1.6.4 ItemManure：燃料 + 施肥） ==========
    @JvmField val MANURE: Item = register("manure", IcpmManureItem(makeProperties("manure", 64)))

    // ========== ICPM 特有材料物品 ==========
    // 宝石/矿石碎片
    @JvmField val RUBY: Item = register("ruby", Item(makeProperties("ruby", 64)))
    @JvmField val SAPPHIRE: Item = register("sapphire", Item(makeProperties("sapphire", 64)))
    @JvmField val PERIDOT: Item = register("peridot", Item(makeProperties("peridot", 64)))
    @JvmField val TOPAZ: Item = register("topaz", Item(makeProperties("topaz", 64)))
    @JvmField val AMETHYST: Item = register("amethyst", Item(makeProperties("amethyst", 64)))
    @JvmField val OPAL: Item = register("opal", Item(makeProperties("opal", 64)))

    // 矿石粒
    @JvmField val COPPER_ORE_CHUNK: Item = register("copper_ore_chunk", Item(makeProperties("copper_ore_chunk", 64)))
    @JvmField val TIN_ORE_CHUNK: Item = register("tin_ore_chunk", Item(makeProperties("tin_ore_chunk", 64)))
    @JvmField val LEAD_ORE_CHUNK: Item = register("lead_ore_chunk", Item(makeProperties("lead_ore_chunk", 64)))
    @JvmField val SILVER_ORE_CHUNK: Item = register("silver_ore_chunk", Item(makeProperties("silver_ore_chunk", 64)))
    @JvmField val GOLD_ORE_CHUNK: Item = register("gold_ore_chunk", Item(makeProperties("gold_ore_chunk", 64)))
    @JvmField val IRON_ORE_CHUNK: Item = register("iron_ore_chunk", Item(makeProperties("iron_ore_chunk", 64)))
    @JvmField val MITHRIL_ORE_CHUNK: Item = register("mithril_ore_chunk", Item(makeProperties("mithril_ore_chunk", 64)))
    @JvmField val ADAMANTIUM_ORE_CHUNK: Item = register("adamantium_ore_chunk", Item(makeProperties("adamantium_ore_chunk", 64)))

    // 金属锭/粒 (已有的保留，这里补充可能缺失的)
    @JvmField val TIN_INGOT: Item = register("tin_ingot", Item(makeProperties("tin_ingot", 64)))
    @JvmField val LEAD_INGOT: Item = register("lead_ingot", Item(makeProperties("lead_ingot", 64)))
    @JvmField val BRONZE_INGOT: Item = register("bronze_ingot", Item(makeProperties("bronze_ingot", 64)))
    @JvmField val STEEL_INGOT: Item = register("steel_ingot", Item(makeProperties("steel_ingot", 64)))

    // ========== 硬币 (ICPM 货币) ==========
    @JvmField val COPPER_COIN: Item = register("copper_coin", Item(makeProperties("copper_coin", 64)))
    @JvmField val SILVER_COIN: Item = register("silver_coin", Item(makeProperties("silver_coin", 64)))
    @JvmField val GOLD_COIN: Item = register("gold_coin", Item(makeProperties("gold_coin", 64)))
    @JvmField val ANCIENT_METAL_COIN: Item = register("ancient_metal_coin", Item(makeProperties("ancient_metal_coin", 64)))
    @JvmField val MITHRIL_COIN: Item = register("mithril_coin", Item(makeProperties("mithril_coin", 64)))
    @JvmField val ADAMANTIUM_COIN: Item = register("adamantium_coin", Item(makeProperties("adamantium_coin", 64)))

    // ========== 多级桶 (空/水/岩浆/牛奶/石头) ==========
    @JvmField val COPPER_BUCKET: Item = registerBucket("copper_bucket", ICPMBucketItem(Fluids.EMPTY, "copper", makeProperties("copper_bucket", 1)))
    @JvmField val COPPER_WATER_BUCKET: Item = registerBucket("copper_water_bucket", ICPMBucketItem(Fluids.WATER, "copper", makeProperties("copper_water_bucket", 1).craftRemainder(COPPER_BUCKET)))
    @JvmField val COPPER_LAVA_BUCKET: Item = registerBucket("copper_lava_bucket", ICPMBucketItem(Fluids.LAVA, "copper", makeProperties("copper_lava_bucket", 1).craftRemainder(COPPER_BUCKET)))
    @JvmField val COPPER_MILK_BUCKET: Item = registerBucket("copper_milk_bucket", ICPMMilkBucketItem("copper", makeProperties("copper_milk_bucket", 1).craftRemainder(COPPER_BUCKET)))
    @JvmField val COPPER_STONE_BUCKET: Item = registerBucket("copper_stone_bucket", ICPMStoneBucketItem("copper", makeProperties("copper_stone_bucket", 1).craftRemainder(COPPER_BUCKET)))

    @JvmField val SILVER_BUCKET: Item = registerBucket("silver_bucket", ICPMBucketItem(Fluids.EMPTY, "silver", makeProperties("silver_bucket", 1)))
    @JvmField val SILVER_WATER_BUCKET: Item = registerBucket("silver_water_bucket", ICPMBucketItem(Fluids.WATER, "silver", makeProperties("silver_water_bucket", 1).craftRemainder(SILVER_BUCKET)))
    @JvmField val SILVER_LAVA_BUCKET: Item = registerBucket("silver_lava_bucket", ICPMBucketItem(Fluids.LAVA, "silver", makeProperties("silver_lava_bucket", 1).craftRemainder(SILVER_BUCKET)))
    @JvmField val SILVER_MILK_BUCKET: Item = registerBucket("silver_milk_bucket", ICPMMilkBucketItem("silver", makeProperties("silver_milk_bucket", 1).craftRemainder(SILVER_BUCKET)))
    @JvmField val SILVER_STONE_BUCKET: Item = registerBucket("silver_stone_bucket", ICPMStoneBucketItem("silver", makeProperties("silver_stone_bucket", 1).craftRemainder(SILVER_BUCKET)))

    @JvmField val GOLD_BUCKET: Item = registerBucket("gold_bucket", ICPMBucketItem(Fluids.EMPTY, "gold", makeProperties("gold_bucket", 1)))
    @JvmField val GOLD_WATER_BUCKET: Item = registerBucket("gold_water_bucket", ICPMBucketItem(Fluids.WATER, "gold", makeProperties("gold_water_bucket", 1).craftRemainder(GOLD_BUCKET)))
    @JvmField val GOLD_LAVA_BUCKET: Item = registerBucket("gold_lava_bucket", ICPMBucketItem(Fluids.LAVA, "gold", makeProperties("gold_lava_bucket", 1).craftRemainder(GOLD_BUCKET)))
    @JvmField val GOLD_MILK_BUCKET: Item = registerBucket("gold_milk_bucket", ICPMMilkBucketItem("gold", makeProperties("gold_milk_bucket", 1).craftRemainder(GOLD_BUCKET)))
    @JvmField val GOLD_STONE_BUCKET: Item = registerBucket("gold_stone_bucket", ICPMStoneBucketItem("gold", makeProperties("gold_stone_bucket", 1).craftRemainder(GOLD_BUCKET)))

    // 铁桶已删除：原版 minecraft:bucket/water_bucket/lava_bucket/milk_bucket 即铁桶（行为一致），
    // 避免与原版重复注册。铁石桶为 ICPM 特有（搬运圆石），保留；
    // 其用完返还的"铁空桶"映射到原版 bucket（icpm:iron_bucket 已删除，emptyOf("iron") 需指向原版桶）。
    @JvmField val IRON_STONE_BUCKET: Item = registerBucket("iron_stone_bucket", ICPMStoneBucketItem("iron", makeProperties("iron_stone_bucket", 1).craftRemainder(net.minecraft.world.item.Items.BUCKET)))
    init {
        name.icpm.item.ICPMBuckets.register("iron_bucket", net.minecraft.world.item.Items.BUCKET)
    }

    @JvmField val ANCIENT_METAL_BUCKET: Item = registerBucket("ancient_metal_bucket", ICPMBucketItem(Fluids.EMPTY, "ancient_metal", makeProperties("ancient_metal_bucket", 1)))
    @JvmField val ANCIENT_METAL_WATER_BUCKET: Item = registerBucket("ancient_metal_water_bucket", ICPMBucketItem(Fluids.WATER, "ancient_metal", makeProperties("ancient_metal_water_bucket", 1).craftRemainder(ANCIENT_METAL_BUCKET)))
    @JvmField val ANCIENT_METAL_LAVA_BUCKET: Item = registerBucket("ancient_metal_lava_bucket", ICPMBucketItem(Fluids.LAVA, "ancient_metal", makeProperties("ancient_metal_lava_bucket", 1).craftRemainder(ANCIENT_METAL_BUCKET)))
    @JvmField val ANCIENT_METAL_MILK_BUCKET: Item = registerBucket("ancient_metal_milk_bucket", ICPMMilkBucketItem("ancient_metal", makeProperties("ancient_metal_milk_bucket", 1).craftRemainder(ANCIENT_METAL_BUCKET)))
    @JvmField val ANCIENT_METAL_STONE_BUCKET: Item = registerBucket("ancient_metal_stone_bucket", ICPMStoneBucketItem("ancient_metal", makeProperties("ancient_metal_stone_bucket", 1).craftRemainder(ANCIENT_METAL_BUCKET)))

    @JvmField val MITHRIL_BUCKET: Item = registerBucket("mithril_bucket", ICPMBucketItem(Fluids.EMPTY, "mithril", makeProperties("mithril_bucket", 1)))
    @JvmField val MITHRIL_WATER_BUCKET: Item = registerBucket("mithril_water_bucket", ICPMBucketItem(Fluids.WATER, "mithril", makeProperties("mithril_water_bucket", 1).craftRemainder(MITHRIL_BUCKET)))
    @JvmField val MITHRIL_LAVA_BUCKET: Item = registerBucket("mithril_lava_bucket", ICPMBucketItem(Fluids.LAVA, "mithril", makeProperties("mithril_lava_bucket", 1).craftRemainder(MITHRIL_BUCKET)))
    @JvmField val MITHRIL_MILK_BUCKET: Item = registerBucket("mithril_milk_bucket", ICPMMilkBucketItem("mithril", makeProperties("mithril_milk_bucket", 1).craftRemainder(MITHRIL_BUCKET)))
    @JvmField val MITHRIL_STONE_BUCKET: Item = registerBucket("mithril_stone_bucket", ICPMStoneBucketItem("mithril", makeProperties("mithril_stone_bucket", 1).craftRemainder(MITHRIL_BUCKET)))

    @JvmField val ADAMANTIUM_BUCKET: Item = registerBucket("adamantium_bucket", ICPMBucketItem(Fluids.EMPTY, "adamantium", makeProperties("adamantium_bucket", 1)))
    @JvmField val ADAMANTIUM_WATER_BUCKET: Item = registerBucket("adamantium_water_bucket", ICPMBucketItem(Fluids.WATER, "adamantium", makeProperties("adamantium_water_bucket", 1).craftRemainder(ADAMANTIUM_BUCKET)))
    @JvmField val ADAMANTIUM_LAVA_BUCKET: Item = registerBucket("adamantium_lava_bucket", ICPMBucketItem(Fluids.LAVA, "adamantium", makeProperties("adamantium_lava_bucket", 1).craftRemainder(ADAMANTIUM_BUCKET)))
    @JvmField val ADAMANTIUM_MILK_BUCKET: Item = registerBucket("adamantium_milk_bucket", ICPMMilkBucketItem("adamantium", makeProperties("adamantium_milk_bucket", 1).craftRemainder(ADAMANTIUM_BUCKET)))
    @JvmField val ADAMANTIUM_STONE_BUCKET: Item = registerBucket("adamantium_stone_bucket", ICPMStoneBucketItem("adamantium", makeProperties("adamantium_stone_bucket", 1).craftRemainder(ADAMANTIUM_BUCKET)))

    // ========== 其他杂项 ==========
    // 以下物品原版 1.21.11 已有，ICPM 不再重复注册，直接引用原版:
    // minecraft:feather, minecraft:leather, minecraft:string, minecraft:gunpowder,
    // minecraft:blaze_powder, minecraft:blaze_rod, minecraft:ender_pearl,
    // minecraft:ghast_tear, minecraft:magma_cream, minecraft:slime_ball,
    // minecraft:experience_bottle

    // ========== 箭矢 ==========
    // 数值来自 1.18.2-ICPM Items.java (damage, recoverChance):
    //   flint=1/0.3, obsidian=2/0.4, copper=3/0.6, silver=3/0.6, gold=2/0.5,
    //   iron=4/0.7, ancient_metal=4/0.8, mithril=5/0.8, adamantium=6/0.9
    // 用户确认阶段1排除 rusted_iron_arrow / tungsten_arrow（材料未实现）
    @JvmField val FLINT_ARROW: Item = register("flint_arrow",
        ICPMArrowItem(1, 0.3f, makeProperties("flint_arrow", 64)))
    @JvmField val OBSIDIAN_ARROW: Item = register("obsidian_arrow",
        ICPMArrowItem(2, 0.4f, makeProperties("obsidian_arrow", 64)))
    @JvmField val COPPER_ARROW: Item = register("copper_arrow",
        ICPMArrowItem(3, 0.6f, makeProperties("copper_arrow", 64)))
    @JvmField val SILVER_ARROW: Item = register("silver_arrow",
        ICPMArrowItem(3, 0.6f, makeProperties("silver_arrow", 64)))
    @JvmField val GOLD_ARROW: Item = register("gold_arrow",
        ICPMArrowItem(2, 0.5f, makeProperties("gold_arrow", 64)))
    @JvmField val IRON_ARROW: Item = register("iron_arrow",
        ICPMArrowItem(4, 0.7f, makeProperties("iron_arrow", 64)))
    @JvmField val ANCIENT_METAL_ARROW: Item = register("ancient_metal_arrow",
        ICPMArrowItem(4, 0.8f, makeProperties("ancient_metal_arrow", 64)))
    @JvmField val MITHRIL_ARROW: Item = register("mithril_arrow",
        ICPMArrowItem(5, 0.8f, makeProperties("mithril_arrow", 64)))
    @JvmField val ADAMANTIUM_ARROW: Item = register("adamantium_arrow",
        ICPMArrowItem(6, 0.9f, makeProperties("adamantium_arrow", 64)))

    // ========== 弓 ==========
    // 1.18.2-ICPM: bow(32耐久), ancient_metal_bow(64耐久, 速度x1.1), mithril_bow(128耐久, 速度x1.25)
    // R196 ItemBow 附魔 = 材质最高 enchantability：wood=10, ancient_metal=40, mithril=100
    @JvmField val BOW: Item = register("bow",
        BowItem(makeProperties("bow", 1).enchantable(10).durability(32)))
    @JvmField val ANCIENT_METAL_BOW: Item = register("ancient_metal_bow",
        ICPMBowItem(1.1f, makeProperties("ancient_metal_bow", 1).enchantable(40).durability(64)))
    @JvmField val MITHRIL_BOW: Item = register("mithril_bow",
        ICPMBowItem(1.25f, makeProperties("mithril_bow", 1).enchantable(100).durability(128)))

    /**
     * 创建 ICPM 自定义护甲材质
     */
    private fun createArmorMaterial(
        name: String,
        defense: Map<ArmorType, Int>,
        enchantmentValue: Int,
        equipSound: Holder<SoundEvent>,
        toughness: Float,
        knockbackResistance: Float,
        repairIngredient: TagKey<Item>
    ): ArmorMaterial {
        return ArmorMaterial(
            15, // durability multiplier，具体耐久由 Item.Properties.durability() 覆盖
            defense,
            enchantmentValue,
            equipSound,
            toughness,
            knockbackResistance,
            repairIngredient,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name))
        )
    }

    /**
     * 注册物品。
     *
     * 同时注册 mite: 命名空间别名：原 MITE 模组（及早期 ICPM 存档）的物品 ID 使用 mite: 前缀，
     * 而本模组改为 icpm: 前缀。旧存档里以 mite:* 存储的物品在读取时会被判为「未知类型」而跳过，
     * 进而触发玩家数据被打断/重新生成等问题。注册 mite: 别名后，旧档的 mite:* 物品可正常解析加载，
     * 重新保存时会被规范化回 icpm:*（同一 Item 实例，无数据丢失）。
     */
    private fun <T : Item> register(name: String, item: T): T {
        val id = Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name)
        val key = ResourceKey.create(Registries.ITEM, id)
        Registry.register(BuiltInRegistries.ITEM, key, item)
        return item
    }

    private fun <T : Item> registerBucket(name: String, item: T): T {
        val registered = register(name, item)
        ICPMBuckets.register(name, registered)
        return registered
    }

    fun init() {
        // 初始化时注册所有物品
    }
}
