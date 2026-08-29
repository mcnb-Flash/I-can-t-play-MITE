package name.icpm.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * R196 风格的可燃方块点火状态机。
 *
 * <p>R196 规则（忠实移植）：
 * - 非可燃方块无法被点燃；
 * - 可燃方块每次点燃只燃烧短短一段时间（本实现约 3 tick），植物（草/树叶等）除外——植物按正常火处理；
 * - 第 5 次及之后点燃按正常火时长燃烧（不短燃、不烧毁）；
 * - 累计被点燃满 8 次且该次火熄灭时，烧毁该可燃方块。</p>
 *
 * <p>逐方块的点燃次数保存在内存映射中（维度+坐标 -> 次数）。注意：该计数在服务器重启后清零，
 * 属于已知限制；如需跨重启持久化可后续接入 WorldSavedData。</p>
 */
public final class CombustionHandler {

    private static final TagKey<net.minecraft.world.level.block.Block> COMBUSTIBLE =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("icpm", "combustible"));
    private static final TagKey<net.minecraft.world.level.block.Block> PLANT =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("icpm", "plant"));

    /** 可燃方块累计点燃次数：dim + "_" + BlockPos.asLong() -> count */
    private static final Map<String, Integer> IGNITE = new ConcurrentHashMap<>();
    /** 短燃（非植物，前 4 次）：火坐标 -> 剩余 tick（烧够 SHORT_TICKS 后熄灭，不毁方块） */
    private static final Map<String, Integer> SHORT = new ConcurrentHashMap<>();
    /** 满 8 次后点燃：火坐标 -> 剩余 tick（烧够 DESTROY_TICKS 后熄灭并烧毁可燃方块） */
    private static final Map<String, Integer> DESTROY = new ConcurrentHashMap<>();

    /** 第 5 次起按正常火燃烧；达到此阈值即进入"可烧毁"判定。 */
    public static final int NORMAL_FROM = 5;
    public static final int BURN_UP_AT = 8;

    /** 短燃时长（tick）：非植物前 4 次点燃的火焰持续 3 tick 即熄灭。 */
    public static final int SHORT_TICKS = 3;
    /** 烧毁时长（tick）：第 8 次点燃的火焰持续到此时长后熄灭（≈正常火时长），并烧毁方块。 */
    public static final int DESTROY_TICKS = 15;

    public static boolean isCombustible(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(COMBUSTIBLE);
    }

    public static boolean isPlant(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(PLANT);
    }

    private static String key(ResourceKey<Level> dim, BlockPos pos) {
        return dim + "_" + pos.asLong();
    }

    /** 记录一次点燃并返回该方块当前累计点燃次数。 */
    public static int registerIgnition(Level level, BlockPos combustiblePos) {
        String k = key(level.dimension(), combustiblePos);
        int n = IGNITE.merge(k, 1, Integer::sum);
        return n;
    }

    public static Integer getIgniteCount(Level level, BlockPos combustiblePos) {
        return IGNITE.getOrDefault(key(level.dimension(), combustiblePos), 0);
    }

    /** 登记一次短燃：火燃烧 SHORT_TICKS 后熄灭（不烧毁方块）。 */
    public static void markShort(Level level, BlockPos firePos) {
        SHORT.put(key(level.dimension(), firePos), SHORT_TICKS);
    }

    /** 登记一次"满 8 次"点燃：火燃烧 DESTROY_TICKS 后熄灭并烧毁可燃方块。 */
    public static void markDestroy(Level level, BlockPos firePos) {
        DESTROY.put(key(level.dimension(), firePos), DESTROY_TICKS);
    }

    /** 火熄灭后的收尾（灭火声效等）。 */
    public static void afterBurn(Level level, BlockPos combustiblePos) {
        // 预留：可在此播放燃烧/噼啪声，目前无额外逻辑。
    }

    /**
     * 由 FireBlockMixin 调用：若此火是我们点燃的受控火，按剩余 tick 处理熄灭并返回 true（取消原版 tick）。
     * 返回 false 表示与受控火无关，交由原版逻辑处理。
     */
    public static boolean handleFireTick(Level level, BlockPos firePos) {
        String k = key(level.dimension(), firePos);

        Integer destroyLeft = DESTROY.get(k);
        if (destroyLeft != null) {
            destroyLeft -= 1;
            if (destroyLeft <= 0) {
                DESTROY.remove(k);
                BlockPos combustiblePos = firePos; // 火位于可燃方块表面，烧毁其支撑的可燃方块由下方/相邻推导
                // 烧毁：寻找相邻的可燃方块（点亮的那个）并置空
                BlockPos target = findCombustibleNeighbor(level, firePos);
                level.setBlock(firePos, Blocks.AIR.defaultBlockState(), 3);
                if (target != null) {
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                }
                afterBurn(level, target);
                return true;
            }
            DESTROY.put(k, destroyLeft);
            return true; // 仍在燃烧，打断原版 tick（阻止蔓延）
        }

        Integer shortLeft = SHORT.get(k);
        if (  shortLeft != null) {
            shortLeft -= 1;
            if (shortLeft <= 0) {
                SHORT.remove(k);
                level.setBlock(firePos, Blocks.AIR.defaultBlockState(), 3);
                afterBurn(level, firePos);
                return true;
            }
            SHORT.put(k, shortLeft);
            return true; // 仍在短燃，打断原版 tick（阻止蔓延）
        }

        return false;
    }

    /** 在火方块周围（含自身）找可燃方块，用于烧毁时定位目标。 */
    private static BlockPos findCombustibleNeighbor(Level level, BlockPos firePos) {
        if (isCombustible(level, firePos)) {
            return firePos;
        }
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            BlockPos p = firePos.relative(d);
            if (isCombustible(level, p)) {
                return p;
            }
        }
        return null;
    }
}
