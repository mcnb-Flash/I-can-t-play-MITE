package name.icpm.entity.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * MITE 僵尸"寻食生肉"（R196 EntityAIMoveToFoodItem 的忠实移植）。
 *
 * <p>空闲僵尸会游荡到附近地上的生肉物品（腐肉/生牛肉/生猪排/生鸡肉/生羊肉/生兔肉/生鱼等）
 * 并吃掉（回复少量生命）。仅当无当前攻击目标时生效，避免干扰战斗。
 */
public class ZombieSeekFoodGoal extends Goal {

    /** R196 ItemMeat 对应的原版生肉集合（ICPM 未单独定义 ItemMeat 接口，这里显式枚举） */
    private static final java.util.Set<Item> RAW_MEATS = java.util.Set.of(
            Items.ROTTEN_FLESH,
            Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT,
            Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH
    );

    private final Mob mob;
    private ItemEntity target;
    private int recalcCooldown;

    public ZombieSeekFoodGoal(Mob mob) {
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
        target = findMeat();
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
            // 吃掉：回复少量生命并消耗该物品实体
            mob.heal(4.0f);
            target.discard();
            target = null;
            recalcCooldown = 20;
        }
    }

    private ItemEntity findMeat() {
        AABB box = mob.getBoundingBox().inflate(8.0);
        List<ItemEntity> items = mob.level().getEntitiesOfClass(ItemEntity.class, box);
        for (ItemEntity item : items) {
            if (isRawMeat(item.getItem())) {
                return item;
            }
        }
        return null;
    }

    private static boolean isRawMeat(ItemStack stack) {
        return !stack.isEmpty() && RAW_MEATS.contains(stack.getItem());
    }
}
