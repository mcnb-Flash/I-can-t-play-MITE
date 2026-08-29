package name.icpm.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * MITE 僵尸"烧树"（R196 EntityAIMoveToTree 的忠实移植，继承 MoveToBlockGoal）。
 *
 * <p>空闲僵尸会游荡到附近的树叶/原木方块并破坏之（MITE 中僵尸烧毁植被）。
 * 用 {@link BlockTags#LEAVES} 与 {@link BlockTags#LOGS} 泛化判定所有树种。
 * 仅当僵尸无当前攻击目标（未追猎）时 {@code canUse} 成立，避免干扰战斗。
 */
public class ZombieBurnTreeGoal extends MoveToBlockGoal {

    private final Zombie zombie;

    public ZombieBurnTreeGoal(Zombie zombie, double speedModifier) {
        super(zombie, speedModifier, 16);
        this.zombie = zombie;
    }

    @Override
    public boolean canUse() {
        // 追猎玩家/村民时不烧树，专注战斗
        return zombie.getTarget() == null && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return zombie.getTarget() == null && super.canContinueToUse();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS);
    }

    @Override
    public void tick() {
        super.tick();
        if (isReachedTarget() && zombie.level() instanceof ServerLevel sl) {
            BlockPos target = getMoveToTarget();
            BlockState state = sl.getBlockState(target);
            // "烧" = 直接破坏树叶/原木（MITE 中僵尸清掉植被）
            sl.levelEvent(2001, target, Block.getId(state));
            sl.destroyBlock(target, true, zombie, 0);
        }
    }
}
