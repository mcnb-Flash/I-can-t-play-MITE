package name.icpm.common;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * ICPM 月相机制（R196 World.isBloodMoon/isBlueMoon/isHarvestMoon 移植）。
 * 月相周期 8 天；血月每 32 天、蓝月每 128 天、丰收之月 = 血月后 8 天。
 */
public final class ICPMMoonPhase {

    /** 月相编号 0-7（0=满月，4=新月，R196 getMoonPhase） */
    public static int phase(long dayTime) {
        return (int) ((dayTime / 24000L + 1L) % 8L);
    }

    /** 血月（R196：day/32 == 0 且非蓝月） */
    public static boolean isBloodMoon(long dayTime) {
        return (dayTime / 24000L + 1L) % 32L == 0L && !isBlueMoon(dayTime);
    }

    /** 蓝月（R196：day/128 == 0） */
    public static boolean isBlueMoon(long dayTime) {
        return (dayTime / 24000L + 1L) % 128L == 0L;
    }

    /** 丰收之月（R196：血月 + 8 天） */
    public static boolean isHarvestMoon(long dayTime) {
        return isBloodMoon(dayTime + 192000L);
    }

    public static boolean isBloodMoonDay(Level level) {
        return isBloodMoon(level.getDayTime());
    }

    public static boolean isBlueMoonDay(Level level) {
        return isBlueMoon(level.getDayTime());
    }

    public static boolean isHarvestMoonDay(Level level) {
        return isHarvestMoon(level.getDayTime());
    }

    /** 血月之夜：血月日（day%32==0 且非蓝月）当晚 20:00 起，至次日 6:00 前（整夜） */
    public static boolean isBloodMoonNight(Level level) {
        return isBloodMoonNight(level.getDayTime());
    }

    public static boolean isBloodMoonNight(long dayTime) {
        long day = dayTime / 24000L + 1L;
        long t = dayTime % 24000L;
        long d = day % 32L;
        long m = day % 128L;
        // 血月日当晚 20:00（16000 tick）起
        if (d == 0L && m != 0L) {
            return t >= 16000L;
        }
        // 血月次日凌晨（0:00 ~ 6:00 前，即睡到第二天早上仍算血月夜）
        if (d == 1L && m != 1L) {
            return t < 6000L;
        }
        return false;
    }

    /** 蓝月之夜 */
    public static boolean isBlueMoonNight(Level level) {
        return isNight(level) && isBlueMoonDay(level);
    }

    /** 丰收月之夜 */
    public static boolean isHarvestMoonNight(Level level) {
        return isNight(level) && isHarvestMoonDay(level);
    }

    private static boolean isNight(Level level) {
        return level.getDayTime() % 24000L >= 13000L;
    }

    /**
     * MITE 定制月光亮度表（覆盖原版相位表），供 ServerLevel.getMoonBrightness 注入使用。
     * 血月 0.6 / 丰收月 1.0 / 蓝月 1.1 / 其余 月相因子×0.5+0.75（满月 1.25 / 新月 0.75）。
     */
    public static float miteMoonBrightness(Level level) {
        if (isBloodMoonDay(level)) {
            return 0.6f;
        }
        if (isHarvestMoonDay(level)) {
            return 1.0f;
        }
        if (isBlueMoonDay(level)) {
            return 1.1f;
        }
        int phase = phase(level.getDayTime());
        float[] arr = DimensionType.MOON_BRIGHTNESS_PER_PHASE;
        float factor = (phase >= 0 && phase < arr.length) ? arr[phase] : 1.0f;
        return factor * 0.5f + 0.75f;
    }

    private ICPMMoonPhase() {
    }
}
