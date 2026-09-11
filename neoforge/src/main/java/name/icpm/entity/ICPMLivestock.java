package name.icpm.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * MITE 牲畜接口（R196 EntityLivestock 移植）
 *
 * 通过该接口，Goal 与挤奶 mixin 可访问混入到 Cow/Pig/Sheep/Chicken 上的
 * 生理状态（饥饿 / 口渴 / 拥挤 / 健康 / 奶量）。由各动物类经 ICPMLivestockMixin
 * 直接 implements 实现（委托到 icpm_xxx 逻辑，避免 @Interface 前缀校验对自定义接口失效）。
 */
public interface ICPMLivestock {

    // ===== 健康（wellness）判定 =====
    boolean isWell();

    boolean isHungry();

    boolean isVeryHungry();

    boolean isDesperateForFood();

    boolean isThirsty();

    boolean isVeryThirsty();

    boolean isDesperateForWater();

    boolean isCrowded(int x, int y, int z);

    // ===== 状态存取 =====
    float getFood();

    void setFood(float food);

    float getWater();

    void setWater(float water);

    float getFreedom();

    void setFreedom(float freedom);

    // ===== 食物 / 水源判定（供 Goal 的 isValidTarget 使用） =====
    boolean isFoodBlock(BlockState state);

    boolean isWaterSource(BlockPos pos);

    // ===== 牛产奶 =====
    int getMilk();

    void setMilk(int milk);

    // ===== R196 受惊传染（spook） =====
    /** 当前是否处于被惊吓状态（spooked_until 之前）。被攻击动物的邻居会被传染此状态。 */
    boolean isSpooked();

    /** 被同伴惊吓：把 spooked_until 设为给定世界时间（取较大值）并标记 has_been_spooked_by_other_animal。 */
    void spook(long until);
}
