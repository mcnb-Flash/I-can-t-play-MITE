package name.icpm.entity.ai;

import name.icpm.entity.ICPMLivestock;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * R196 EntityAIFleeAttackerOrPanic 的"被同伴惊吓"分支（isSpooked 触发的随机乱跑）。
 *
 * 直接被攻击的个体由原版 PanicGoal 接管（优先度更高），本目标只负责"被同伴传染惊吓、
 * 但自身并未受伤"的同类——使其在 spooked_until 期间随机四散奔逃，并向外继续传染，
 * 形成整群连锁惊吓。复刻 R196：随机方向快跑，疑似路径完成时重新选点。
 */
public class ICPMFleeWhenSpooked extends Goal {

    private final PathfinderMob mob;
    private final double speed;
    private int repathTimer;

    public ICPMFleeWhenSpooked(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private boolean isSpooked() {
        return ((ICPMLivestock) (Object) this.mob).isSpooked();
    }

    @Override
    public boolean canUse() {
        return isSpooked();
    }

    @Override
    public void start() {
        this.repathTimer = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return isSpooked();
    }

    @Override
    public void tick() {
        if (--this.repathTimer <= 0 || this.mob.getNavigation().isDone()) {
            Vec3 target = DefaultRandomPos.getPos(this.mob, 8, 4);
            if (target != null) {
                this.mob.getNavigation().moveTo(target.x, target.y, target.z, this.speed);
            }
            this.repathTimer = 10 + this.mob.getRandom().nextInt(10);
        }
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
