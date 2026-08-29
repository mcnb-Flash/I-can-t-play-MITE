package name.icpm.common;

import net.minecraft.world.level.Level;

/**
 * ICPM 季节系统。
 *
 * 周期与 R196 月相周期对齐：一年 = 128 天（蓝月周期 day/128==0），
 * 一季 = 32 天（血月周期 day/32==0），四季 = 春 / 夏 / 秋 / 冬。
 *
 * 效果：
 * - 春季：作物生长小幅加速（春雨滋润）
 * - 夏季：无加成（炎热干燥）
 * - 秋季：作物生长大幅加速（丰收季节）
 * - 冬季：作物几乎停止生长（严寒）
 * 季节变化时在 ICPM.java 中广播消息。
 */
public final class ICPMSeason {

    /** 一季天数 = 32（与血月周期一致） */
    public static final long DAYS_PER_SEASON = 32L;

    /** 一年天数 = 128（与蓝月周期一致） */
    public static final long DAYS_PER_YEAR = 128L;

    public enum Season {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER
    }

    private ICPMSeason() {
    }

    /** 当前季节（按游戏日计算：day = dayTime/24000 + 1） */
    public static Season getSeason(long dayTime) {
        long day = dayTime / 24000L + 1L;
        int index = (int) (((day - 1L) / DAYS_PER_SEASON) % 4L);
        return switch (index) {
            case 0 -> Season.SPRING;
            case 1 -> Season.SUMMER;
            case 2 -> Season.AUTUMN;
            default -> Season.WINTER;
        };
    }

    public static Season getSeason(Level level) {
        return getSeason(level.getDayTime());
    }

    public static boolean isSpring(long dayTime) {
        return getSeason(dayTime) == Season.SPRING;
    }

    public static boolean isSummer(long dayTime) {
        return getSeason(dayTime) == Season.SUMMER;
    }

    public static boolean isAutumn(long dayTime) {
        return getSeason(dayTime) == Season.AUTUMN;
    }

    public static boolean isWinter(long dayTime) {
        return getSeason(dayTime) == Season.WINTER;
    }

    /** 季节语言键（message.icpm.season.spring 等） */
    public static String messageKey(Season season) {
        return "message.icpm.season." + season.name().toLowerCase();
    }

    /** 季节对作物生长的速度加成（叠加到 getGrowthSpeed） */
    public static float growthBonus(Season season) {
        return switch (season) {
            case SPRING -> 1.0f;   // 春雨滋润
            case SUMMER -> 0.0f;   // 炎热干燥
            case AUTUMN -> 2.0f;   // 丰收季节
            case WINTER -> -2.0f;  // 严寒，几乎不生长
        };
    }
}
