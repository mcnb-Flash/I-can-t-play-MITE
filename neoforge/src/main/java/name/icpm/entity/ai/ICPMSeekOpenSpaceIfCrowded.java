package name.icpm.entity.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import name.icpm.entity.ICPMLivestock;

/**
 * MITE EntityAISeekOpenSpaceIfCrowded（R196）移植
 * 动物所处位置过于拥挤（非户外或附近生物过多）时，前往开阔处。
 */
public class ICPMSeekOpenSpaceIfCrowded extends MoveToBlockGoal {

    private final ICPMLivestock livestock;

    public ICPMSeekOpenSpaceIfCrowded(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier, 16);
        this.livestock = (ICPMLivestock) (Object) mob;
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().isClientSide()) {
            return false;
        }
        BlockPos p = this.mob.blockPosition();
        return this.livestock.isCrowded(p.getX(), p.getY(), p.getZ()) && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos p = this.mob.blockPosition();
        return this.livestock.isCrowded(p.getX(), p.getY(), p.getZ()) && super.canContinueToUse();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        return !this.livestock.isCrowded(pos.getX(), pos.getY(), pos.getZ());
    }
}
