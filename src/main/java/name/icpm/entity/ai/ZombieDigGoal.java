package name.icpm.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * MITE 僵尸"挖开路上的方块"（R196 EntityZombie.isDiggingEnabled 的忠实移植）。
 *
 * <p>聪明僵尸（{@link ZombieMiteState} 标记）会挖开正前方身体/头部高度的阻挡方块，
 * 从而穿过墙壁追敌。逻辑脱胎于 {@code MinerZombieEntity.tryMine}：带破坏进度、裂纹动画、
 * 满进度后破坏并播放破坏音效。禁止挖基岩/屏障等不可破坏方块。
 *
 * <p>仅当僵尸"聪明"且正前方有可挖方块时 {@code canUse} 成立，避免空闲时乱挖地面。
 */
public class ZombieDigGoal extends Goal {

    private final Mob mob;
    private BlockPos diggingPos;
    private float progress;

    public ZombieDigGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(mob instanceof net.minecraft.world.entity.monster.zombie.Zombie zombie)) {
            return false;
        }
        if (!ZombieMiteState.get(zombie).smart) {
            return false;
        }
        return findDigTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return diggingPos != null && canDig(diggingPos);
    }

    @Override
    public void start() {
        this.progress = 0f;
    }

    @Override
    public void stop() {
        if (diggingPos != null && mob.level() instanceof ServerLevel sl) {
            sl.destroyBlockProgress(mob.getId(), diggingPos, -1);
        }
        this.diggingPos = null;
        this.progress = 0f;
    }

    @Override
    public void tick() {
        if (diggingPos == null) {
            diggingPos = findDigTarget();
            if (diggingPos == null) {
                return;
            }
            progress = 0f;
        }
        ServerLevel level = (ServerLevel) mob.level();
        BlockState state = level.getBlockState(diggingPos);
        float hardness = state.getDestroySpeed(level, diggingPos);
        if (hardness < 0f) {
            diggingPos = null;
            return;
        }
        float toolSpeed = mob.getMainHandItem().getDestroySpeed(state);
        if (toolSpeed <= 0f) {
            toolSpeed = 1.0f;
        }
        // 比矿工僵尸慢（0.5 系数）：僵尸徒手/持工具挖墙更费力
        progress += (toolSpeed / (hardness * 30f)) * 0.5f;
        level.destroyBlockProgress(mob.getId(), diggingPos, (int) (progress * 10f));
        if (progress >= 1f) {
            level.levelEvent(2001, diggingPos, Block.getId(state));
            level.destroyBlock(diggingPos, true, mob, 0);
            diggingPos = null;
            progress = 0f;
        }
    }

    /** 寻找正前方身体/头部高度的第一个可挖方块 */
    private BlockPos findDigTarget() {
        BlockPos base = mob.blockPosition();
        Direction dir = mob.getDirection();
        BlockPos front = base.relative(dir);
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = front.above(dy);
            if (canDig(p)) {
                return p;
            }
        }
        return null;
    }

    private boolean canDig(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        ServerLevel level = (ServerLevel) mob.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0f;
    }
}
