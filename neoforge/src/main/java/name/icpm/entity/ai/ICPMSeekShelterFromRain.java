package name.icpm.entity.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import name.icpm.entity.ICPMLivestock;

/**
 * MITE EntityAISeekShelterFromRain（R196）移植
 * 下雨且当前暴露在天空下时，前往有遮蔽（看不见天）的位置躲雨。
 */
public class ICPMSeekShelterFromRain extends MoveToBlockGoal {

    public ICPMSeekShelterFromRain(PathfinderMob mob, double speedModifier, boolean swimIfNecessary) {
        super(mob, speedModifier, 16);
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().isClientSide()) {
            return false;
        }
        Level level = this.mob.level();
        BlockPos p = this.mob.blockPosition();
        return level.isRaining() && level.canSeeSky(p) && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        Level level = this.mob.level();
        BlockPos p = this.mob.blockPosition();
        return level.isRaining() && level.canSeeSky(p) && super.canContinueToUse();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return !level.canSeeSky(pos);
    }
}
