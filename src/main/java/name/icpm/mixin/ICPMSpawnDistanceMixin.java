package name.icpm.mixin;

import name.icpm.common.ICPMPortalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * R196 近距刷怪放宽（判决源 {@code SpawnerAnimals.java:192-206}）。
 *
 * <pre>
 * can_spawn_close_to_player = (isOverworld || isUnderworld)
 *     &amp;&amp; getClosestPlayer(pos, 24.0, true) != null        // 24 格内有玩家
 *     &amp;&amp; getBlockLightValue(x, y,     z) == 0            // 生成格全黑
 *     &amp;&amp; getBlockLightValue(x, y + 1, z) == 0;           // 上方一格全黑
 * if (can_spawn_close_to_player) {
 *     if (getClosestPlayer(pos, 8.0, false) != null) 拒绝; // 8 格内仍禁止
 * } else if (getClosestPlayer(pos, 24.0, false) != null) 拒绝; // 否则 24 格内禁止
 * </pre>
 *
 * <p>即：**主世界 / 地下世界的全黑处，怪物可在 8～24 格内生成**（1.6.4 的"贴近玩家刷怪"）。
 * 1.21.11 的 {@code NaturalSpawner.isRightDistanceToPlayerAndSpawnPoint} 首行是
 * {@code if (distSq < 576.0) return false;}（24 格硬下限，无黑夜放宽）。
 *
 * <p>本 mixin 只在满足 R196 放宽条件时把该阈值降为 {@code 64.0}（8 格）；
 * 其余判定（世界出生点 24 格保护、区块可生成检查等）**完全由 vanilla 原逻辑执行**。
 */
@Mixin(NaturalSpawner.class)
public abstract class ICPMSpawnDistanceMixin {

    @ModifyConstant(method = "isRightDistanceToPlayerAndSpawnPoint",
            constant = @Constant(doubleValue = 576.0))
    private static double icpm$relaxDarkSpawnDistance(double original,
                                                      ServerLevel level,
                                                      ChunkAccess chunk,
                                                      BlockPos.MutableBlockPos pos,
                                                      double distanceToPlayerSqr) {
        // R196：仅主世界 / 地下世界享有该放宽
        if (!level.dimension().equals(Level.OVERWORLD)
                && !level.dimension().equals(ICPMPortalHandler.UNDERWORLD_KEY)) {
            return original;
        }
        // distSq ∈ [64, 576)：24 格内有玩家、8 格内无玩家（无玩家时 distSq 会远大于 576）
        if (distanceToPlayerSqr < 64.0 || distanceToPlayerSqr >= 576.0) {
            return original;
        }
        // R196：生成格与其上方一格在综合光照下必须全黑（getBlockLightValue == 0）
        if (level.getMaxLocalRawBrightness(pos) != 0) {
            return original;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) != 0) {
            return original;
        }
        return 64.0;
    }
}
