package name.icpm.common;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import java.lang.reflect.Field;

/**
 * 跨类安全访问 {@link Mob#goalSelector}（protected 字段）。
 *
 * <p>为什么不用 {@code @Shadow}：Mixin 的 @Shadow 字段只在【目标类自身】查找，
 * {@code goalSelector} 声明于父类 {@code Mob}，在 @Mixin(AbstractSkeleton)/@Mixin(Zombie) 中
 * @Shadow 它会运行期抛 {@code InvalidMixinException: @Shadow field ... was not located in the target class}
 * （编译期不校验，2026-08-19 游戏实测崩溃）。且 {@code Mob} 无公共 {@code getGoalSelector()}。
 *
 * <p>实现：静态反射缓存 {@code Mob} 类中类型为 {@code GoalSelector} 的字段。
 * 注意不能按名字字符串反射——Loom 不重映射字符串字面量，运行时字段名是中间名（field_6201），
 * 但 {@code f.getType() == GoalSelector.class} 的类型引用会被重映射，可稳定匹配。
 */
public final class MobGoalSelectorAccess {

    private static final Field GOAL_SELECTOR_FIELD;

    static {
        Field found = null;
        for (Field f : Mob.class.getDeclaredFields()) {
            if (f.getType() == GoalSelector.class) {
                f.setAccessible(true);
                found = f;
                break;
            }
        }
        GOAL_SELECTOR_FIELD = found;
    }

    private MobGoalSelectorAccess() {
    }

    /** 返回目标 {@link GoalSelector}；异常或未找到时返回 null（调用方需防空）。 */
    public static GoalSelector get(Mob mob) {
        if (GOAL_SELECTOR_FIELD == null || mob == null) {
            return null;
        }
        try {
            return (GoalSelector) GOAL_SELECTOR_FIELD.get(mob);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
