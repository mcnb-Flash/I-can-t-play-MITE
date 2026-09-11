package name.icpm.entity.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import name.icpm.entity.ICPMLivestock;

/**
 * MITE EntityAISeekWaterIfThirsty（R196）移植
 * 动物口渴时前往附近水源（水 / 雪 / 炼药锅）饮水；靠近后由 updateWellness 补充水分。
 */
public class ICPMSeekWaterIfThirsty extends MoveToBlockGoal {

    private final ICPMLivestock livestock;

    public ICPMSeekWaterIfThirsty(PathfinderMob mob, double speedModifier, boolean swimIfNecessary) {
        super(mob, speedModifier, 16);
        this.livestock = (ICPMLivestock) (Object) mob;
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().isClientSide() || this.livestock.isThirsty()) {
            return super.canUse();
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.livestock.isThirsty() && super.canContinueToUse();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return this.livestock.isWaterSource(pos);
    }
}
