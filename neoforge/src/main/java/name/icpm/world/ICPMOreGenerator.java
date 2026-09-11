package name.icpm.world;

/**
 * ICPM 矿石生成器（NeoForge）
 *
 * 基于 ICPM R196 源码 afq.java (WorldGenMinable) 的矿石分布：
 *
 * 普通矿石（石头层，仅 y >= 0）：
 * - 银矿 (silver_ore): y: 0-96, 矿脉大小 ~8
 * - 秘银矿 (mithril_ore): y: 0-32, 矿脉大小 ~6
 * - 艾德曼矿 (adamantium_ore): y: 0-16, 矿脉大小 ~4
 *
 * 深板岩矿石（深板岩层，仅 y < 0）：
 * - 深板岩银矿 (deepslate_silver_ore): y: -64~-1, 矿脉大小 ~6
 * - 深板岩秘银矿 (deepslate_mithril_ore): y: -64~-16, 矿脉大小 ~4
 * - 深板岩艾德曼矿 (deepslate_adamantium_ore): y: -64~-32, 矿脉大小 ~3
 *
 * 群系挂载：NeoForge 不再有 BiomeLoadingEvent / BiomeModifications，统一由数据驱动完成：
 *   data/icpm/neoforge/biome_modifier/overworld_icpm_ores.json
 *   （type: "neoforge:add_features"，step: "underground_ores"，
 *     features: ore_silver / ore_mithril / ore_adamantium）
 * 上述 placed_feature 亦被 underworld 群系引用（ore_*_underworld 变体），与主世界互不冲突。
 */
public final class ICPMOreGenerator {

    private ICPMOreGenerator() {
    }

    /**
     * NeoForge：实际挂载见 biome_modifier datapack；此方法保留仅为兼容旧调用点（空操作）。
     */
    public static void register() {
        name.icpm.ICPM.LOGGER.info("ICPM ores attached to overworld biomes via neoforge biome_modifier datapack");
    }
}
