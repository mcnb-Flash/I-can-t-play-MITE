package name.icpm.common;

import name.icpm.entity.monster.BoneLordEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * 骷髅 frenzy 跨类共享状态（WeakHashMap 范式，仿 {@code ZombieMiteState}/{@code LivestockState}）。
 *
 * <p>为什么需要静态表：SkeletonFrenzyMixin 目标 {@link AbstractSkeleton}，而 {@code tick()} 声明于
 * LivingEntity（AbstractSkeleton 不重写），Mixin 的 @Inject 无法在目标类中注入父类方法（运行期
 * "could not find any targets matching 'tick'" 崩溃，2026-08-19 实测）。故每 tick 逻辑迁到
 * {@code @Mixin(LivingEntity)} 的 {@code SkeletonFrenzyTickMixin}，状态经本表跨 mixin 共享。
 *
 * <p>逻辑（R196 骨王灵感 + 血月叠加 忠实移植）：
 * <ul>
 *   <li>骨王灵感：附近 16 格内有 {@link BoneLordEntity} → 移速 ×1.2 + 100% 近战 + 破门 ×2；</li>
 *   <li>血月叠加：{@link ICPMMoonPhase#isBloodMoonNight} 当夜 → 100% 近战 + 破门 ×2
 *       （速度由全局 ICPMMoonFrenzyMixin 的 Speed I 提供，不重复叠加）；</li>
 *   <li>远程 CD ×0.67 为基线 buff（见 SkeletonFrenzyMixin 的 getAttackInterval 注入）。</li>
 * </ul>
 */
public final class SkeletonFrenzyState {

    /** 单只骷髅的 frenzy 状态 */
    public static final class Entry {
        public boolean boneLordNear;
        public boolean wasFrenzied;
        public boolean wasBoneLord;
        public int scanTimer;
        public BreakDoorGoal breakDoorGoal;
        public AttributeModifier speedModifier;
    }

    private static final Identifier FRENZY_SPEED_ID = Identifier.parse("icpm:frenzy.speed");
    private static final double BONE_LORD_RADIUS = 16.0;
    private static final int DEFAULT_DOOR_BREAK_TIME = readDefaultDoorBreakTime();

    private static final Map<AbstractSkeleton, Entry> MAP =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SkeletonFrenzyState() {
    }

    public static Entry get(AbstractSkeleton sk) {
        return MAP.computeIfAbsent(sk, k -> new Entry());
    }

    /** 每 tick 更新 frenzy 状态（由 SkeletonFrenzyTickMixin 在 LivingEntity.tick 调用）。 */
    public static void tick(AbstractSkeleton self) {
        Entry e = get(self);

        if (e.scanTimer > 0) {
            e.scanTimer--;
        } else {
            e.boneLordNear = scanBoneLord(self);
            e.scanTimer = 20;
        }

        boolean boneLord = e.boneLordNear;
        boolean frenzied = ICPMMoonPhase.isBloodMoonNight(self.level()) || boneLord;

        // 移速 ×1.2：仅"骨王灵感"触发。血月速度由全局 Speed I 提供，此处不重复叠加。
        if (boneLord != e.wasBoneLord) {
            AttributeInstance speed = self.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                if (e.speedModifier != null) {
                    speed.removeModifier(e.speedModifier);
                    e.speedModifier = null;
                }
                if (boneLord) {
                    e.speedModifier = new AttributeModifier(
                            FRENZY_SPEED_ID, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                    speed.addTransientModifier(e.speedModifier);
                }
            }
            e.wasBoneLord = boneLord;
        }

        // 破门 ×2（破门耗时减半即速度 ×2）：骨王或血月均触发。
        if (frenzied != e.wasFrenzied) {
            var gs = MobGoalSelectorAccess.get(self);
            if (frenzied && e.breakDoorGoal == null) {
                e.breakDoorGoal = new BreakDoorGoal(self, Math.max(1, DEFAULT_DOOR_BREAK_TIME / 2),
                        (Predicate<net.minecraft.world.Difficulty>) d -> true);
                if (gs != null) {
                    gs.addGoal(0, e.breakDoorGoal);
                }
            } else if (!frenzied && e.breakDoorGoal != null) {
                if (gs != null) {
                    gs.removeGoal(e.breakDoorGoal);
                }
                e.breakDoorGoal = null;
                // 恢复按手持物决定的弓 / 近战目标。
                self.reassessWeaponGoal();
            }
            e.wasFrenzied = frenzied;
        }
    }

    private static boolean scanBoneLord(AbstractSkeleton self) {
        List<BoneLordEntity> list = self.level()
                .getEntitiesOfClass(BoneLordEntity.class, self.getBoundingBox().inflate(BONE_LORD_RADIUS));
        return !list.isEmpty();
    }

    private static int readDefaultDoorBreakTime() {
        try {
            Field f = BreakDoorGoal.class.getDeclaredField("DEFAULT_DOOR_BREAK_TIME");
            f.setAccessible(true);
            return (int) f.get(null);
        } catch (Exception e) {
            return 240;
        }
    }
}
