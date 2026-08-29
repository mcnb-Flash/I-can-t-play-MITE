package name.icpm.entity.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;

/**
 * MITE EntityAIGetOutOfWater（R196）移植
 * 牲畜落水时前往附近的干燥陆地。
 */
public class ICPMGetOutOfWater extends MoveToBlockGoal {

    public ICPMGetOutOfWater(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier, 16);
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().isClientSide()) {
            return false;
        }
        return this.mob.isInWater() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.isInWater() && super.canContinueToUse();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        if (level.getFluidState(pos).is(Fluids.WATER)) {
            return false;
        }
        if (level.getFluidState(pos.below()).is(Fluids.WATER)) {
            return false;
        }
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid();
    }
}
