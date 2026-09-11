package name.icpm.common;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * ICPM 方块材质工具
 * 根据方块ID返回对应的 ICPMMaterial
 *
 * 用于 ICPM 系统的耐久计算、护甲减免等
 */
public class ICPMMaterialHelper {

    /**
     * 根据方块获取其 ICPM 材质
     */
    public static ICPMMaterial getMaterialForBlock(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) return ICPMMaterials.stone;

        String name = id.getPath();

        // 矿石类
        if (name.contains("copper_ore")) return ICPMMaterials.copper;
        if (name.contains("silver_ore")) return ICPMMaterials.silver;
        if (name.contains("ancient_metal_ore")) return ICPMMaterials.ancient_metal;
        if (name.contains("mithril_ore")) return ICPMMaterials.mithril;
        if (name.contains("adamantium_ore")) return ICPMMaterials.adamantium;
        if (name.contains("iron_ore")) return ICPMMaterials.iron;
        if (name.contains("gold_ore")) return ICPMMaterials.gold;
        if (name.contains("diamond_ore")) return ICPMMaterials.diamond;
        if (name.contains("emerald_ore")) return ICPMMaterials.emerald;
        if (name.contains("quartz_ore") || name.contains("nether_quartz_ore")) return ICPMMaterials.quartz;
        if (name.contains("redstone_ore")) return ICPMMaterials.redstone;

        // 金属块
        if (name.contains("copper_block")) return ICPMMaterials.copper;
        if (name.contains("silver_block")) return ICPMMaterials.silver;
        if (name.contains("ancient_metal_block")) return ICPMMaterials.ancient_metal;
        if (name.contains("mithril_block")) return ICPMMaterials.mithril;
        if (name.contains("adamantium_block")) return ICPMMaterials.adamantium;
        if (name.contains("iron_block")) return ICPMMaterials.iron;
        if (name.contains("gold_block")) return ICPMMaterials.gold;
        if (name.contains("diamond_block")) return ICPMMaterials.diamond;
        if (name.contains("emerald_block")) return ICPMMaterials.emerald;

        // 黑曜石
        if (name.contains("obsidian")) return ICPMMaterials.obsidian;

        // 燧石
        if (name.contains("flint")) return ICPMMaterials.flint;

        // 玻璃
        if (name.contains("glass")) return ICPMMaterials.glass;

        // 下界岩
        if (name.contains("netherrack") || name.contains("nether_rack")) return ICPMMaterials.netherrack;

        // 铁砧
        if (name.contains("anvil")) return ICPMMaterials.anvil;

        // 原版石
        if (name.contains("stone") || name.contains("cobblestone") || name.contains("granite")
            || name.contains("diorite") || name.contains("andesite") || name.contains("deepslate")
            || name.contains("dirt") || name.contains("sand") || name.contains("gravel")) {
            return ICPMMaterials.stone;
        }

        return ICPMMaterials.stone; // 默认
    }

    /**
     * 检查方块是否需要工具挖掘（ICPM逻辑）
     */
    public static boolean blockRequiresTool(Block block) {
        ICPMMaterial material = getMaterialForBlock(block);
        return material.requiresTool();
    }

    /**
     * 获取方块最低挖掘等级
     */
    public static int getMinHarvestLevel(Block block) {
        ICPMMaterial material = getMaterialForBlock(block);
        return material.getMinHarvestLevel();
    }
}
