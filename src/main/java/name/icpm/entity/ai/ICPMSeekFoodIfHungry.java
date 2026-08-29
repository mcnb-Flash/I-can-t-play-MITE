package name.icpm.entity.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import name.icpm.entity.ICPMLivestock;

/**
 * MITE EntityAISeekFoodIfHungry（R196）移植
 * 动物饥饿时前往附近的草 / 高草觅食；靠近后由生理 tick 的 updateWellness 补充水分般补充食物。
 */
public class ICPMSeekFoodIfHungry extends MoveToBlockGoal {

    private final ICPMLivestock livestock;

    public ICPMSeekFoodIfHungry(PathfinderMob mob, double speedModifier, boolean swimIfNecessary) {
        super(mob, speedModifier, 16);
        this.livestock = (ICPMLivestock) (Object) mob;
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().isClientSide() || this.livestock.isHungry()) {
            return super.canUse();
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.livestock.isHungry() && super.canContinueToUse();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return this.livestock.isFoodBlock(level.getBlockState(pos));
    }
}
