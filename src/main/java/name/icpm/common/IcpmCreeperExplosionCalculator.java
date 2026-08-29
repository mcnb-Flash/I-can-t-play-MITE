package name.icpm.common;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;

/**
 * MITE 忠实移植：爬行者爆炸使用两个独立半径。
 * <p>
 * R196 中爬行者爆炸：方块破坏半径 = explosionRadius × 0.715，实体伤害半径 = explosionRadius × 1.1。
 * 1.21.11 的爆炸只有单一 radius，因此这里：
 * <ul>
 *   <li>调用方传入 实体半径 = 基础半径 × 1.1（作为爆炸的 radius），保证实体伤害范围符合 R196；</li>
 *   <li>方块破坏在 {@link #shouldBlockExplode} 中按 0.715/1.1 的比例裁剪上限，使方块破坏半径 = 基础 × 0.715。</li>
 * </ul>
 */
public class IcpmCreeperExplosionCalculator extends SimpleExplosionDamageCalculator {
    // 方块半径相对爆炸 radius（=基础×1.1）的比例 = 0.715 / 1.1
    private static final double BLOCK_RADIUS_FACTOR = 0.715 / 1.1;

    /**
     * 与原版 MOB 爆炸一致：会破坏方块、会伤害实体，无额外击退倍率、无免疫方块。
     * 方块破坏半径由 {@link #shouldBlockExplode} 单独裁剪到 0.715 倍。
     */
    public IcpmCreeperExplosionCalculator() {
        super(true, true, Optional.empty(), Optional.empty());
    }

    @Override
    public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
        // 爆炸源（爬行者自身）不承受自己的爆炸伤害
        return entity != explosion.getDirectSourceEntity() && super.shouldDamageEntity(explosion, entity);
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState blockState, float radius) {
        if (!super.shouldBlockExplode(explosion, level, pos, blockState, radius)) {
            return false;
        }
        double limit = explosion.radius() * BLOCK_RADIUS_FACTOR;
        return pos.getCenter().distanceToSqr(explosion.center()) <= limit * limit;
    }
}
