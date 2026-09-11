package name.icpm.common;

import net.minecraft.world.level.material.MapColor;

/**
 * ICPM 材质定义
 * 基于 1.6.4-ICPM R196 反编译源码
 *
 * 严格按 Material.java 的 registerMaterials() 顺序定义所有材质
 */
public class ICPMMaterials {

    public static ICPMMaterial air;
    public static ICPMMaterial leather;
    public static ICPMMaterial wood;
    public static ICPMMaterial flint;
    public static ICPMMaterial stone;
    public static ICPMMaterial obsidian;
    public static ICPMMaterial rusted_iron;
    public static ICPMMaterial copper;
    public static ICPMMaterial silver;
    public static ICPMMaterial gold;
    public static ICPMMaterial iron;
    public static ICPMMaterial ancient_metal;
    public static ICPMMaterial mithril;
    public static ICPMMaterial adamantium;
    public static ICPMMaterial netherrack;
    public static ICPMMaterial glass;
    public static ICPMMaterial quartz;
    public static ICPMMaterial emerald;
    public static ICPMMaterial diamond;
    public static ICPMMaterial grass;
    public static ICPMMaterial dirt;
    public static ICPMMaterial redstone;
    public static ICPMMaterial anvil;
    public static ICPMMaterial water;
    public static ICPMMaterial lava;

    /**
     * 初始化所有 ICPM 材质定义
     * 严格按照 1.6.4-ICPM R196 的 registerMaterials() 顺序
     */
    public static void registerAll() {
        // air 是透明材质，跳过

        // 皮革 - 来自EnumEquipmentMaterial.leather
        leather = new ICPMMaterial(ICPMDurability.Material.LEATHER)
            .setName("leather")
            .setMapColor(MapColor.COLOR_ORANGE)
            .setFlammability(true, false, true)
            .setHarmedByPepsin();

        // 木 - 来自EnumEquipmentMaterial.wood
        wood = new ICPMMaterial(ICPMDurability.Material.WOOD)
            .setMapColor(MapColor.WOOD)
            .setFlammability(true, true, true)
            .setMinHarvestLevel(0);

        // 燧石 - 来自EnumEquipmentMaterial.flint
        flint = new ICPMMaterial(ICPMDurability.Material.FLINT)
            .setMapColor(MapColor.STONE)
            .setRockyMineral()
            .setRequiresTool()
            .setMinHarvestLevel(2);

        // 石 - 普通石材质（无材质耐久）
        stone = new ICPMMaterial("stone", MapColor.STONE)
            .setRockyMineral()
            .setRequiresTool()
            .setMinHarvestLevel(2);

        // 黑曜石 - 来自EnumEquipmentMaterial.obsidian
        obsidian = new ICPMMaterial(ICPMDurability.Material.OBSIDIAN)
            .setMapColor(MapColor.COLOR_BLACK)
            .setRockyMineral()
            .setRequiresTool()
            .setMinHarvestLevel(3);

        // 生锈铁 - 来自EnumEquipmentMaterial.rusted_iron
        rusted_iron = new ICPMMaterial(ICPMDurability.Material.RUSTED_IRON)
            .setMapColor(MapColor.COLOR_BROWN)
            .setMetal(true)
            .setRequiresTool()
            .setMinHarvestLevel(2);

        // 铜 - 来自EnumEquipmentMaterial.copper
        copper = new ICPMMaterial(ICPMDurability.Material.COPPER)
            .setMapColor(MapColor.COLOR_ORANGE)
            .setMetal(true)
            .setRequiresTool()
            .setMinHarvestLevel(2);

        // 银 - 来自EnumEquipmentMaterial.silver
        silver = new ICPMMaterial(ICPMDurability.Material.SILVER)
            .setMapColor(MapColor.COLOR_LIGHT_GRAY)
            .setMetal(true)
            .setRequiresTool()
            .setMinHarvestLevel(2);

        // 金 - 来自EnumEquipmentMaterial.gold（不耐酸）
        gold = new ICPMMaterial(ICPMDurability.Material.GOLD)
            .setMapColor(MapColor.GOLD)
            .setMetal(false)  // 金在ICPM中不算"金属"，因为不耐酸
            .setRequiresTool()
            .setMinHarvestLevel(2);

        // 铁 - 来自EnumEquipmentMaterial.iron
        iron = new ICPMMaterial(ICPMDurability.Material.IRON)
            .setMapColor(MapColor.METAL)
            .setMetal(true)
            .setRequiresTool()
            .setMinHarvestLevel(3);

        // 远古金属 - 来自EnumEquipmentMaterial.ancient_metal
        ancient_metal = new ICPMMaterial(ICPMDurability.Material.ANCIENT_METAL)
            .setMapColor(MapColor.COLOR_PURPLE)
            .setMetal(true)
            .setRequiresTool()
            .setMinHarvestLevel(3);

        // 秘银 - 来自EnumEquipmentMaterial.mithril（不算金属）
        mithril = new ICPMMaterial(ICPMDurability.Material.MITHRIL)
            .setMapColor(MapColor.COLOR_LIGHT_BLUE)
            .setMetal(false)  // 秘银不算金属
            .setRequiresTool()
            .setMinHarvestLevel(4);

        // 艾德曼 - 来自EnumEquipmentMaterial.adamantium（不算金属，不受熔岩伤害）
        adamantium = new ICPMMaterial(ICPMDurability.Material.ADAMANTIUM)
            .setMapColor(MapColor.COLOR_GREEN)
            .setMetal(false)  // 艾德曼不算金属
            .setHarmedByLava(false)  // 不受熔岩伤害
            .setRequiresTool()
            .setMinHarvestLevel(5);

        // 下界岩 - 来自EnumEquipmentMaterial.netherrack
        netherrack = new ICPMMaterial(ICPMDurability.Material.NETHERRACK)
            .setFlammability(true, false, false)
            .setHarmedByLava(false)
            .setMapColor(MapColor.COLOR_RED)
            .setRockyMineral()
            .setRequiresTool()
            .setMinHarvestLevel(2);

        // 玻璃 - 来自EnumEquipmentMaterial.glass
        glass = new ICPMMaterial(ICPMDurability.Material.GLASS)
            .setMapColor(MapColor.NONE)
            .setRockyMineral(true)  // 是水晶
            .setTranslucent();

        // 石英 - 来自EnumEquipmentMaterial.quartz
        quartz = new ICPMMaterial(ICPMDurability.Material.QUARTZ)
            .setMapColor(MapColor.QUARTZ)
            .setRockyMineral(true)
            .setRequiresTool()
            .setMinHarvestLevel(2);

        // 绿宝石 - 来自EnumEquipmentMaterial.emerald
        emerald = new ICPMMaterial(ICPMDurability.Material.EMERALD)
            .setMapColor(MapColor.EMERALD)
            .setRockyMineral(true)
            .setRequiresTool()
            .setMinHarvestLevel(3);

        // 钻石 - 来自EnumEquipmentMaterial.diamond
        diamond = new ICPMMaterial(ICPMDurability.Material.DIAMOND)
            .setMapColor(MapColor.DIAMOND)
            .setRockyMineral(true)
            .setRequiresTool()
            .setMinHarvestLevel(4);

        // 草 - 非工具材质
        grass = new ICPMMaterial("grass", MapColor.GRASS)
            .setFlammability(true, false, true);

        // 泥土 - 非工具材质
        dirt = new ICPMMaterial("dirt", MapColor.DIRT);

        // 红石 - 来自EnumEquipmentMaterial.rusted_iron属性
        redstone = new ICPMMaterial(ICPMDurability.Material.RUSTED_IRON)
            .setName("redstone")
            .setMapColor(MapColor.COLOR_RED)
            .setRockyMineral()
            .setRequiresTool()
            .setMinHarvestLevel(2);

        // 铁砧 - 使用铁材质
        anvil = new ICPMMaterial("anvil", MapColor.METAL);

        // 液体材质 - ICPM中用于特殊处理
        water = new ICPMMaterial("water", MapColor.WATER).setLiquid();
        lava = new ICPMMaterial("lava", MapColor.FIRE).setLiquid();
    }
}
