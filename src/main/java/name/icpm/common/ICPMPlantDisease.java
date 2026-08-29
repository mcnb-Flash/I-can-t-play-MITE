package name.icpm.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ICPM 植物病害管理器（1.6.4 作物病害机制移植）
 *
 * 作物在随机刻中有概率患病（1.6.4 Plant.getRandomDisease / 病害随机传播）：
 * - 患病作物停止生长（CropBlockMixin 在 randomTick 里跳过生长）
 * - 骨粉右键患病作物可治疗（BoneMealMixin 只允许治疗，不允许催熟）
 *
 * 病害状态仅存于服务端内存（单机 mod 可接受），按维度分桶存储，
 * 维度卸载时由调用方清理（见 WorldEvents 注册）。
 */
public final class ICPMPlantDisease {

    private ICPMPlantDisease() {}

    /** 维度 -> 患病作物位置集合 */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> DISEASED = new ConcurrentHashMap<>();

    public static boolean isDiseased(ResourceKey<Level> dim, BlockPos pos) {
        Set<BlockPos> set = DISEASED.get(dim);
        return set != null && set.contains(pos);
    }

    public static void infect(ResourceKey<Level> dim, BlockPos pos) {
        DISEASED.computeIfAbsent(dim, k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    public static void cure(ResourceKey<Level> dim, BlockPos pos) {
        Set<BlockPos> set = DISEASED.get(dim);
        if (set != null) {
            set.remove(pos);
        }
    }

    /** 方块被破坏/替换时清理，避免内存泄漏 */
    public static void onBlockRemoved(ResourceKey<Level> dim, BlockPos pos) {
        Set<BlockPos> set = DISEASED.get(dim);
        if (set != null) {
            set.remove(pos);
        }
    }

    /** 维度卸载时清理整桶数据 */
    public static void clearDimension(ResourceKey<Level> dim) {
        DISEASED.remove(dim);
    }
}
