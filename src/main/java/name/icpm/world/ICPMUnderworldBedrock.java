package name.icpm.world;

import name.icpm.block.ICPMBlocks;
import name.icpm.common.ICPMPortalHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 地下世界底层基岩山脉 / 地幔盆地生成器。
 *
 * 忠实移植 MITE R196（TesseractLHY/Underworld 的 {@code UnderworldHook.init(Chunk,...)}）底层基岩算法。
 * 原版 {@code surface_rule} 的 {@code vertical_gradient} 是“按 Y 平滑渐变”——每一列都必定在 y1 铺一层基岩，
 * 永远不会出现“整列无基岩、地幔直接裸露”的豁口，与 R196 的“按列二值噪声 + 概率豁口”不符，故改用逐列代码生成。
 *
 * R196 算法（每列，绝对坐标采样保证跨区块无缝）：
 *   num_bedrock_blocks = random.nextInt(3) + 1              // 地幔层数 1~3
 *   bedrock_noise = max(strata_1a, strata_1b)
 *                  + (bump1a>0 ? bump1a*0.25 : 0)
 *                  + (bump1b>0 ? bump1b*0.125 : 0)
 *                  + (bump1c>0 ? bump1c*0.125 : 0)
 *                  + (bump4 >0 ? bump4 *0.09375 + 0.125 : 0)
 *   仅当 bedrock_noise > 0.12 且 dy <= bedrock_noise*7 才放置基岩，否则留豁口（地幔裸露 / 其上方石头裸露）。
 *   阈值从 0 略提高到 0.12，让豁口更明显；旧世界可能因 -60/-64 基岩堆叠而看不到豁口，建议新建世界验证。
 *
 * 重构版（min_y=-64, height=192）：地幔位于 y=-64..(-64+num_bedrock_blocks-1)（世界最底层、不可破坏）；
 * 基岩地板紧接地幔之上、最多 3 层（“三层基岩”），顶部封顶于 y=-59，仅当 bedrock_noise>0 才铺——bedrock_noise<=0 即豁口（盆地处地幔浅露）。
 * 即地幔+基岩地板整体占据 y=-64~-59（6 层），随 min_y 一同下移到世界真正底部。
 * 顶层基岩天花板（y123-127）仍由 noise_settings 的 surface_rule 负责，本类只管底层。
 */
public final class ICPMUnderworldBedrock {

    private static final Map<ServerLevel, Generator> GENERATORS = new ConcurrentHashMap<>();

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world.dimension() != ICPMPortalHandler.UNDERWORLD_KEY) {
                return;
            }
            getGenerator(world).generate(chunk);
        });
    }

    private static Generator getGenerator(ServerLevel world) {
        return GENERATORS.computeIfAbsent(world, Generator::new);
    }

    private static final class Generator {
        private final long seed;
        private final SimplexNoise strata1a;
        private final SimplexNoise strata1b;
        private final SimplexNoise bump1a;
        private final SimplexNoise bump1b;
        private final SimplexNoise bump1c;
        private final SimplexNoise bump4;
        private final BlockState mantle;
        private final BlockState bedrock = Blocks.BEDROCK.defaultBlockState();

        Generator(ServerLevel world) {
            this.seed = world.getSeed();
            // 各噪声场用 worldSeed 异或不同常量派生，互相正交（等价 R196 多个独立 noise_gen 静态实例）。
            // 用 LegacyRandomSource 对齐 underworld.json 的 "legacy_random_source": true。
            this.strata1a = new SimplexNoise(new LegacyRandomSource(seed ^ 0x9E3779B1L));
            this.strata1b = new SimplexNoise(new LegacyRandomSource(seed ^ 0x85EBCA77L));
            this.bump1a   = new SimplexNoise(new LegacyRandomSource(seed ^ 0xC2B2AE35L));
            this.bump1b   = new SimplexNoise(new LegacyRandomSource(seed ^ 0x27D4EB2FL));
            this.bump1c   = new SimplexNoise(new LegacyRandomSource(seed ^ 0x165667B1L));
            this.bump4    = new SimplexNoise(new LegacyRandomSource(seed ^ 0xD3A2646CL));
            Block mantleBlock = ICPMBlocks.MANTLE;
            this.mantle = mantleBlock != null ? mantleBlock.defaultBlockState() : Blocks.BEDROCK.defaultBlockState();
        }

        void generate(LevelChunk chunk) {
            int cx = chunk.getPos().x;
            int cz = chunk.getPos().z;
            int baseX = cx << 4;
            int baseZ = cz << 4;
            // R196: random seeded by worldSeed * intPairHash(chunkX, chunkZ)
            long chunkSeed = seed * ((long) cx * 2653L + (long) cz * 6714631L);
            Random random = new Random(chunkSeed);

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int numBedrock = random.nextInt(3) + 1; // 地幔层数 1~3

                    double sx = baseX + x;
                    double sz = baseZ + z;
                    // R196 scale_xz*2 = 0.015625*2 = 0.03125（主 strata 噪声）
                    double n1a = strata1a.getValue(sx * 0.03125, sz * 0.03125);
                    double n1b = strata1b.getValue(sx * 0.03125, sz * 0.03125);
                    double bedrockNoise = Math.max(n1a, n1b);
                    double b;
                    // R196 bump 加权（仅正贡献）；scale 对应 0.125 / 0.25 / 0.5 / 1.0
                    if ((b = bump1a.getValue(sx * 0.125, sz * 0.125)) > 0.0) bedrockNoise += b * 0.25;
                    if ((b = bump1b.getValue(sx * 0.25, sz * 0.25)) > 0.0) bedrockNoise += b * 0.125;
                    if ((b = bump1c.getValue(sx * 0.5, sz * 0.5)) > 0.0) bedrockNoise += b * 0.125;
                    if ((b = bump4.getValue(sx * 1.0, sz * 1.0)) > 0.0) bedrockNoise += b * 0.09375 + 0.125;

                    // 地幔+基岩地板整体位于世界最底 y=-64~-59（随 min_y=-64 一同下移，世界真正底部）。
                    final int FLOOR_BASE = -64; // 地幔最底层 Y（世界真正底部，min_y=-64）
                    final int FLOOR_TOP = -59;  // 基岩地板顶部封顶 Y（地板占据 -64..-59 共 6 层）

                    // 地幔：最底层 numBedrock 层（y = FLOOR_BASE .. FLOOR_BASE+numBedrock-1），不可破坏，封底防穿
                    // 关键：LevelChunk.setBlockState 依赖引用相等判断是否弄脏区块，而从磁盘加载出来的
                    // BlockState 与 defaultBlockState() 常量并非同一实例 → 每次加载都会把区块标记为脏，
                    // 导致退出保存时所有地下世界区块被迫重写、保存超时触发服务端看门狗。
                    // 因此先用 equals() 判定方块是否确实不同，相同则跳过写入，避免反复弄脏。
                    BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
                    for (int y = 0; y < numBedrock; y++) {
                        mpos.set(baseX + x, FLOOR_BASE + y, baseZ + z);
                        if (!chunk.getBlockState(mpos).equals(mantle)) {
                            chunk.setBlockState(mpos, mantle);
                        }
                    }

                    // 基岩地板：仅当 bedrock_noise > 0.12（R196 按列二值豁口，阈值略提高使豁口更明显），
                    // 紧接地幔之上；层数 = floor(bedrock_noise*7)+1，封顶 3（“三层基岩”），顶部封顶于 FLOOR_TOP(-59)。
                    // 注意：若在旧世界（此前 min_y=-60 时已经生成过区块）测试，旧基岩层在 y=-60 附近、新基岩层在 y=-64..-59
                    // 会相互堆叠，导致看起来没有豁口；建议新建世界验证豁口效果。
                    if (bedrockNoise > 0.12) {
                        int layers = (int) (bedrockNoise * 7.0) + 1;
                        if (layers > 3) layers = 3;
                        int top = FLOOR_BASE + numBedrock + layers;
                        if (top > FLOOR_TOP) top = FLOOR_TOP;
                        for (int y = FLOOR_BASE + numBedrock; y < top; y++) {
                            mpos.set(baseX + x, y, baseZ + z);
                            if (!chunk.getBlockState(mpos).equals(bedrock)) {
                                chunk.setBlockState(mpos, bedrock);
                            }
                        }
                    }
                    // bedrock_noise <= 0：豁口 —— 不铺基岩，地幔（及其上方生成的石头/空洞）直接裸露（盆地）。
                }
            }
        }
    }
}
