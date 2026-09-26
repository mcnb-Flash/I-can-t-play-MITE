package name.icpm.entity.ai;

import name.icpm.entity.LivestockState;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * R196 EntityCreature 的"定向逃跑"分支（findPathAwayFromXYZ + considerFleeing/considerStopFleeing）。
 *
 * <p>与 {@link ICPMFleeWhenSpooked}（被同伴惊吓后的随机乱跑）不同，本目标在动物
 * <b>重伤且攻击者临近</b>时（{@code LivestockState.considerFleeing} 已置 {@code hasDecided_to_flee}），
 * 沿"远离攻击者"的方向持续寻路逃窜，直到不再重伤或攻击者远离（{@code considerStopFleeing}）。
 * 逃跑期间附加移动速度加成（R196 的 field_110181_i "Fleeing speed bonus"）。
 *
 * <p>仅由 {@code ICPMLivestockGoalsMixin} 在确认是牲畜（Cow/Pig/Sheep/Chicken）后才注册，
 * 且优先级（2）高于随机惊慌（3），故同时被惊吓与重伤逃跑时优先定向逃离攻击者。
 */
public class ICPMAvoidAttackerFrightGoal extends Goal {

    private static final AttributeModifier FLEE_SPEED = new AttributeModifier(
            Identifier.fromNamespaceAndPath("icpm", "flee_speed"), 1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private final PathfinderMob mob;
    private final double speed;
    private int repathTimer;

    public ICPMAvoidAttackerFrightGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private LivestockState st() {
        return LivestockState.get((Entity) this.mob);
    }

    private Animal asAnimal() {
        return (Animal) (Object) this.mob;
    }

    private LivingEntity attacker() {
        return st().getFleeAttacker(asAnimal());
    }

    @Override
    public boolean canUse() {
        LivingEntity a = attacker();
        if (a == null) {
            st().hasDecidedToFlee = false;
            return false;
        }
        if (!st().hasDecidedToFlee) {
            return false;
        }
        if (st().considerStopFleeing(asAnimal(), a)) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.repathTimer = 0;
        AttributeInstance ai = this.mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (ai != null) {
            ai.addTransientModifier(FLEE_SPEED);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        AttributeInstance ai = this.mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (ai != null) {
            ai.removeModifier(FLEE_SPEED);
        }
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity a = attacker();
        if (a == null) {
            return;
        }
        if (--this.repathTimer <= 0 || this.mob.getNavigation().isDone()) {
            Vec3 from = a.position();
            Vec3 self = this.mob.position();
            // 远离攻击者的方向（R196 findPathAwayFromXYZ 的语义）
            Vec3 dir = new Vec3(self.x - from.x, 0.0, self.z - from.z);
            double len = dir.length();
            if (len < 1.0E-4) {
                double ang = this.mob.getRandom().nextDouble() * Math.PI * 2.0;
                dir = new Vec3(Math.cos(ang), 0.0, Math.sin(ang));
                len = 1.0;
            }
            dir = dir.scale((14.0 + this.mob.getRandom().nextDouble() * 8.0) / len);
            Vec3 target = self.add(dir);
            this.mob.getNavigation().moveTo(target.x, target.y, target.z, this.speed);
            this.repathTimer = 10 + this.mob.getRandom().nextInt(10);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
