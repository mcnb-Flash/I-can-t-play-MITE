package name.icpm.world;

import name.icpm.block.ICPMBlocks;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceBlockConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * ICPM 矿石生成器
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
 */
public class ICPMOreGenerator {

    /**
     * 注册 ICPM 矿石生成（data-driven via BiomeModifications）。
     * 将 ore_silver / ore_mithril / ore_adamantium 的 placed_feature 绑定到主世界所有群系的 UNDERGROUND_ORES 阶段。
     * placed_feature 已就绪于 data/icpm/worldgen/placed_feature/ore_{silver,mithril,adamantium}.json，
     * 仅曾被 underworld 群系引用；在此为主世界群系补充注册。
     */
    public static void register() {
        ResourceKey<PlacedFeature> silver = ResourceKey.create(Registries.PLACED_FEATURE,
                name.icpm.ICPM.id("ore_silver"));
        ResourceKey<PlacedFeature> mithril = ResourceKey.create(Registries.PLACED_FEATURE,
                name.icpm.ICPM.id("ore_mithril"));
        ResourceKey<PlacedFeature> adamantium = ResourceKey.create(Registries.PLACED_FEATURE,
                name.icpm.ICPM.id("ore_adamantium"));

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                silver
        );
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                mithril
        );
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                adamantium
        );
        name.icpm.ICPM.LOGGER.info("ICPM OreGenerator registered overworld ICPM ores (silver/mithril/adamantium)");
    }

    /**
     * 在指定区块生成 ICPM 矿石
     *
     * @param level 世界
     * @param chunk 区块
     * @param random 随机源
     */
    private static void generateOres(ServerLevel level, LevelChunk chunk, RandomSource random) {
        ChunkPos chunkPos = chunk.getPos();
        int worldX = chunkPos.getMinBlockX();
        int worldZ = chunkPos.getMinBlockZ();

        // ========== 普通矿石（石头层，仅 y >= 0）==========
        // 生成银矿 (y: 0-96)
        if (ICPMBlocks.SILVER_ORE != null && random.nextFloat() < 0.5f) {
            generateOreVein(level, chunk, random, ICPMBlocks.SILVER_ORE, Blocks.STONE,
                    worldX + random.nextInt(16), 0 + random.nextInt(97), worldZ + random.nextInt(16), 8);
        }

        // 生成秘银矿 (y: 0-32)
        if (ICPMBlocks.MITHRIL_ORE != null && random.nextFloat() < 0.3f) {
            generateOreVein(level, chunk, random, ICPMBlocks.MITHRIL_ORE, Blocks.STONE,
                    worldX + random.nextInt(16), 0 + random.nextInt(33), worldZ + random.nextInt(16), 6);
        }

        // 生成艾德曼矿 (y: 0-16)
        if (ICPMBlocks.ADAMANTIUM_ORE != null && random.nextFloat() < 0.2f) {
            generateOreVein(level, chunk, random, ICPMBlocks.ADAMANTIUM_ORE, Blocks.STONE,
                    worldX + random.nextInt(16), 0 + random.nextInt(17), worldZ + random.nextInt(16), 4);
        }

        // ========== 深板岩矿石（深板岩层，仅 y < 0）==========
        // 生成深板岩银矿 (y: -64~-1)
        if (ICPMBlocks.DEEPSLATE_SILVER_ORE != null && random.nextFloat() < 0.4f) {
            generateOreVein(level, chunk, random, ICPMBlocks.DEEPSLATE_SILVER_ORE, Blocks.DEEPSLATE,
                    worldX + random.nextInt(16), -64 + random.nextInt(64), worldZ + random.nextInt(16), 6);
        }

        // 生成深板岩秘银矿 (y: -64~-16)
        if (ICPMBlocks.DEEPSLATE_MITHRIL_ORE != null && random.nextFloat() < 0.25f) {
            generateOreVein(level, chunk, random, ICPMBlocks.DEEPSLATE_MITHRIL_ORE, Blocks.DEEPSLATE,
                    worldX + random.nextInt(16), -64 + random.nextInt(49), worldZ + random.nextInt(16), 4);
        }

        // 生成深板岩艾德曼矿 (y: -64~-32)
        if (ICPMBlocks.DEEPSLATE_ADAMANTIUM_ORE != null && random.nextFloat() < 0.15f) {
            generateOreVein(level, chunk, random, ICPMBlocks.DEEPSLATE_ADAMANTIUM_ORE, Blocks.DEEPSLATE,
                    worldX + random.nextInt(16), -64 + random.nextInt(33), worldZ + random.nextInt(16), 3);
        }
    }

    /**
     * 生成单个矿脉
     *
     * @param level 世界
     * @param chunk 区块
     * @param random 随机源
     * @param oreBlock 矿石方块
     * @param replaceBlock 要替换的方块
     * @param startX 起始X
     * @param startY 起始Y
     * @param startZ 起始Z
     * @param veinSize 矿脉大小
     */
    private static void generateOreVein(ServerLevel level, LevelChunk chunk, RandomSource random,
                                        Block oreBlock, Block replaceBlock,
                                        int startX, int startY, int startZ, int veinSize) {
        // 简单的矿脉生成算法（基于 R196 的 growVein 方法）
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < veinSize; i++) {
            int dx = startX + random.nextInt(3) - 1;
            int dy = startY + random.nextInt(3) - 1;
            int dz = startZ + random.nextInt(3) - 1;

            pos.set(dx, dy, dz);

            if (chunk.getBlockState(pos).is(replaceBlock)) {
                level.setBlock(pos, oreBlock.defaultBlockState(), 2);
            }
        }
    }
}