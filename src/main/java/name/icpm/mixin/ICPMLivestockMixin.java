package name.icpm.mixin;

import name.icpm.entity.ICPMLivestock;
import name.icpm.entity.LivestockState;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.cow.Cow;

import org.spongepowered.asm.mixin.Mixin;

/**
 * MITE EntityLivestock（R196）核心移植 —— 仅做接口接线。
 *
 * 挂到 Cow / Pig / Sheep / Chicken 四个具体类上，使它们实现 ICPMLivestock 接口
 * （薄委托到 {@link LivestockState} 静态表）。
 *
 * 注意：
 * - 生理 tick / 交配门槛 / 成长门槛 / 受惊传染 / NBT 持久化等"继承方法"逻辑，
 *   放在以声明类为目标的 mixin（见 ICPMLivestockBehaviorMixin @Mixin(Animal.class)、
 *   ICPMLivestockTickMixin @Mixin(LivingEntity.class)、
 *   ICPMLivestockGrowthMixin @Mixin(AgeableMob.class)）。
 * - 六个生理/受惊 AI Goal 的注册放在 ICPMLivestockGoalsMixin（@Mixin(Mob.class)），
 *   因为 registerGoals() 声明于 Mob，四个具体动物类并不重写它（强行在多目标 mixin
 *   里按 method_5959 注入会因目标类无此方法而启动崩溃）。
 */
@Mixin({Cow.class, Pig.class, Sheep.class, Chicken.class})
public abstract class ICPMLivestockMixin extends Animal implements ICPMLivestock {

    // Mixin 不会被实例化（运行时由 Mixin 把方法织入真正的 Cow/Pig/Sheep/Chicken），
    // 提供显式构造器以满足 javac 对 Animal(EntityType, Level) 构造器的要求。
    public ICPMLivestockMixin() {
        super(null, null);
    }

    private Entity self() {
        return (Entity) (Object) this;
    }

    private Animal selfAnimal() {
        return (Animal) (Object) this;
    }

    private LivestockState st() {
        return LivestockState.get(self());
    }

    // ===================== 接口实现：健康判定 =====================

    @Override public boolean isWell() { return st().isWell(); }
    @Override public boolean isHungry() { return st().isHungry(); }
    @Override public boolean isVeryHungry() { return st().isVeryHungry(); }
    @Override public boolean isDesperateForFood() { return st().isDesperateForFood(); }
    @Override public boolean isThirsty() { return st().isThirsty(); }
    @Override public boolean isVeryThirsty() { return st().isVeryThirsty(); }
    @Override public boolean isDesperateForWater() { return st().isDesperateForWater(); }

    @Override public boolean isCrowded(int x, int y, int z) {
        return LivestockState.isCrowded(selfAnimal(), x, y, z);
    }

    // ===================== 接口实现：状态存取 =====================

    @Override public float getFood() { return st().food; }
    @Override public void setFood(float food) { st().food = food; }
    @Override public float getWater() { return st().water; }
    @Override public void setWater(float water) { st().water = water; }
    @Override public float getFreedom() { return st().freedom; }
    @Override public void setFreedom(float freedom) { st().freedom = freedom; }

    // ===================== 接口实现：食物 / 水源判定 =====================

    @Override public boolean isFoodBlock(net.minecraft.world.level.block.state.BlockState state) {
        return LivestockState.isFoodBlock(state);
    }

    @Override public boolean isWaterSource(BlockPos pos) {
        return LivestockState.isWaterSource(self().level(), pos);
    }

    // ===================== 接口实现：奶量 =====================

    @Override public int getMilk() {
        Animal self = selfAnimal();
        return self.isBaby() ? 0 : st().milk;
    }

    @Override public void setMilk(int milk) {
        st().milk = Math.max(0, Math.min(100, milk));
    }

    // ===================== 接口实现：受惊传染 =====================

    @Override public boolean isSpooked() { return st().isSpooked(self()); }
    @Override public void spook(long until) { st().spook(until); }
}
