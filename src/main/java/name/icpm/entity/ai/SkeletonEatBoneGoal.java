package name.icpm.entity.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * MITE 骷髅"吃骨头回血"（R196 EntityAIMoveToRepairItem + isRepairItem(Item.bone) + getHealFX=repair 的忠实移植）。
 *
 * <p>空闲骷髅会游荡到附近地上的骨头物品并吃掉（回复少量生命）。仅当无当前攻击目标时生效，
 * 避免干扰战斗。覆盖原版 Skeleton 系与 ICPM 全部骷髅变体。
 */
public class SkeletonEatBoneGoal extends Goal {

    private final Mob mob;
    private ItemEntity target;
    private int recalcCooldown;

    public SkeletonEatBoneGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) {
            return false;
        }
        if (recalcCooldown > 0) {
            recalcCooldown--;
            return target != null && !target.isRemoved();
        }
        recalcCooldown = 20;
        target = findBone();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && !target.isRemoved() && mob.getTarget() == null;
    }

    @Override
    public void stop() {
        target = null;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        mob.getNavigation().moveTo(target, 1.0);
        if (mob.distanceToSqr((Entity) target) < 2.25) {
            // 吃掉骨头：回复生命（R196 的 repair 回血），并消耗该物品实体
            mob.heal(3.0f);
            target.discard();
            target = null;
            recalcCooldown = 20;
        }
    }

    private ItemEntity findBone() {
        AABB box = mob.getBoundingBox().inflate(8.0);
        List<ItemEntity> items = mob.level().getEntitiesOfClass(ItemEntity.class, box);
        for (ItemEntity item : items) {
            if (isBone(item.getItem())) {
                return item;
            }
        }
        return null;
    }

    private static boolean isBone(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.BONE;
    }
}
