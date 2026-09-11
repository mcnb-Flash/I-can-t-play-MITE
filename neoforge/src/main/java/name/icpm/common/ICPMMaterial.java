package name.icpm.common;

import net.minecraft.world.level.material.MapColor;

/**
 * ICPM 方块材质系统
 * 基于 1.6.4-ICPM R196 反编译源码 (Material.java)
 *
 * 完整复现 ICPM 的方块材质属性：
 * - is_metal: 是否金属
 * - is_rocky_mineral: 是否岩石矿物
 * - is_crystal: 是否水晶
 * - requires_tool: 需要工具
 * - can_catch_fire: 可燃
 * - dissolves_in_water: 溶于水
 * - is_harmed_by_acid: 受酸腐蚀
 * - is_harmed_by_lava: 受熔岩伤害
 * - min_harvest_level: 最低挖掘等级
 * - full_block_hardness: 完整方块硬度
 * - max_quality: 最高品质
 */
public class ICPMMaterial {

    private static int numMaterials = 0;
    private static final ICPMMaterial[] materials = new ICPMMaterial[1024];

    // 基础属性
    private boolean isLiquid = false;
    private boolean isEdible = false;
    private boolean isDrinkable = false;
    private boolean canCatchFire = false;
    private boolean canBurnAsFuelSource = false;
    private boolean isHarmedByFire = false;
    private boolean canDouseFire = false;
    private boolean dissolvesInWater = false;
    private boolean isReplaceable = false;
    private boolean isTranslucent = false;
    private boolean isMetal = false;
    private boolean isRockyMineral = false;
    private boolean isCrystal = false;
    private boolean requiresTool = false;
    private boolean isHarmedByLava = true;
    private boolean isHarmedByPepsin = false;
    private boolean isHarmedByAcid = true;

    // 数值属性
    public MapColor mapColor;
    public int mobilityFlag;
    protected float durability;
    protected int enchantability;
    public String name;
    private float fullBlockHardness;
    protected int minHarvestLevel;
    private float maxQuality = 3.0f; // 默认到 "优秀" 品质

    public ICPMMaterial(String name) {
        this(name, null);
    }

    public ICPMMaterial(String name, MapColor mapColor) {
        this.setName(name);
        this.setMapColor(mapColor);
        materials[numMaterials++] = this;
    }

    public ICPMMaterial(ICPMDurability.Material enumMaterial) {
        this(enumMaterial.name().toLowerCase(), null);
        this.setDurability(enumMaterial.getFactor());
        this.setEnchantability(enumMaterial.getEnchant());
        this.setMaxQuality(getMaxQualityForMaterial(enumMaterial));
    }

    /**
     * 根据ICPM原版设置每个材质的最高品质
     */
    private float getMaxQualityForMaterial(ICPMDurability.Material material) {
        return switch (material) {
            case WOOD, FLINT, OBSIDIAN, NETHERRACK, QUARTZ, GLASS -> 1.0f;  // fine
            case COPPER, SILVER, GOLD, EMERALD -> 1.5f;  // excellent
            case IRON, ANCIENT_METAL, DIAMOND, RUSTED_IRON -> 2.5f;  // masterwork
             case MITHRIL, ADAMANTIUM, NETHERITE -> 3.0f;  // legendary
            case LEATHER -> 1.0f;
        };
    }

    // ==================== Setter方法 ====================

    public ICPMMaterial setMapColor(MapColor mapColor) {
        this.mapColor = mapColor;
        return this;
    }

    public ICPMMaterial setDurability(float durability) {
        this.durability = durability;
        return this;
    }

    public ICPMMaterial setEnchantability(int enchantability) {
        this.enchantability = enchantability;
        return this;
    }

    public ICPMMaterial setName(String name) {
        this.name = name;
        return this;
    }

    public ICPMMaterial setFullBlockHardness(float hardness) {
        this.fullBlockHardness = hardness;
        return this;
    }

    public ICPMMaterial setMinHarvestLevel(int level) {
        this.minHarvestLevel = level;
        return this;
    }

    public ICPMMaterial setLiquid() {
        this.isLiquid = true;
        this.isHarmedByAcid = false;
        return this;
    }

    public ICPMMaterial setEdible() {
        this.isEdible = true;
        return this;
    }

    public ICPMMaterial setDrinkable() {
        this.isDrinkable = true;
        return this;
    }

    public ICPMMaterial setTranslucent() {
        this.isTranslucent = true;
        return this;
    }

    public ICPMMaterial setRequiresTool() {
        this.requiresTool = true;
        return this;
    }

    public ICPMMaterial setFlammability(boolean canCatchFire, boolean canBurn, boolean harmedByFire) {
        this.canCatchFire = canCatchFire;
        this.canBurnAsFuelSource = canBurn;
        this.isHarmedByFire = harmedByFire;
        return this;
    }

    public ICPMMaterial setHarmedByLava(boolean harmed) {
        this.isHarmedByLava = harmed;
        return this;
    }

    public ICPMMaterial setHarmedByPepsin() {
        this.isHarmedByPepsin = true;
        return this;
    }

    public ICPMMaterial setHarmedByAcid(boolean harmed) {
        this.isHarmedByAcid = harmed;
        return this;
    }

    public ICPMMaterial setCanDouseFire() {
        this.setFlammability(false, false, true);
        this.canDouseFire = true;
        return this;
    }

    public ICPMMaterial setDissolvesInWater() {
        this.dissolvesInWater = true;
        return this;
    }

    public ICPMMaterial setReplaceable() {
        this.isReplaceable = true;
        return this;
    }

    public ICPMMaterial setMetal(boolean harmedByAcid) {
        this.isMetal = true;
        this.setHarmedByAcid(harmedByAcid);
        return this;
    }

    public ICPMMaterial setRockyMineral() {
        return this.setRockyMineral(false);
    }

    public ICPMMaterial setRockyMineral(boolean isCrystal) {
        this.isRockyMineral = true;
        this.isCrystal = isCrystal;
        this.isHarmedByAcid = false;
        return this;
    }

    public ICPMMaterial setMaxQuality(float maxQuality) {
        this.maxQuality = maxQuality;
        return this;
    }

    // ==================== Getter方法 ====================

    public MapColor getMapColor() { return this.mapColor; }
    public float getDurability() { return this.durability; }
    public int getEnchantability() { return this.enchantability; }
    public String getName() { return this.name; }
    public float getFullBlockHardness() { return this.fullBlockHardness; }
    public int getMinHarvestLevel() { return this.minHarvestLevel; }
    public float getMaxQuality() { return this.maxQuality; }

    public boolean isLiquid() { return this.isLiquid; }
    public boolean isEdible() { return this.isEdible; }
    public boolean isDrinkable() { return this.isDrinkable; }
    public boolean isTranslucent() { return this.isTranslucent; }
    public boolean requiresTool() { return this.requiresTool; }
    public boolean canCatchFire() { return this.canCatchFire; }
    public boolean canBurnAsFuelSource() { return this.canBurnAsFuelSource; }
    public boolean isHarmedByFire() { return this.isHarmedByFire; }
    public boolean isHarmedByLava() { return this.isHarmedByLava; }
    public boolean isHarmedByPepsin() { return this.isHarmedByPepsin; }
    public boolean isHarmedByAcid() { return this.isHarmedByAcid; }
    public boolean canDouseFire() { return this.canDouseFire; }
    public boolean dissolvesInWater() { return this.dissolvesInWater; }
    public boolean isReplaceable() { return this.isReplaceable; }
    public boolean isMetal() { return this.isMetal; }
    public boolean isRockyMineral() { return this.isRockyMineral; }
    public boolean isCrystal() { return this.isCrystal; }
    public boolean isSolid() { return true; }
}
