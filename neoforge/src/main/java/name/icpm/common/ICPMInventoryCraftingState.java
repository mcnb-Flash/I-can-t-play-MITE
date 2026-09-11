package name.icpm.common;

import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 背包 2x2 合成栏的每玩家合成状态（服务端）。
 *
 * 复刻 R196 的背包合成时间机制所需的最小状态集合：
 * - 记录一次合成的开始游戏刻 (startTick) 与总时长 (durationTick)
 * - 记录开始时的配方结果 (expectedResult)，用于取走时校验网格未被改动（防止白嫖）
 *
 * 与 ICPM 工作台不同，背包没有独立菜单，状态按玩家 UUID 维护在静态表中。
 */
public final class ICPMInventoryCraftingState {

    private static final ConcurrentHashMap<UUID, Entry> STATES = new ConcurrentHashMap<>();

    private static final class Entry {
        final long startTick;
        final long durationTick;
        final ItemStack expectedResult;

        Entry(long startTick, long durationTick, ItemStack expectedResult) {
            this.startTick = startTick;
            this.durationTick = durationTick;
            this.expectedResult = expectedResult;
        }
    }

    private ICPMInventoryCraftingState() {
    }

    /** 开始一次背包合成 */
    public static void start(UUID uuid, long startTick, long durationTick, ItemStack expectedResult) {
        STATES.put(uuid, new Entry(startTick, durationTick, expectedResult.copy()));
    }

    public static boolean isActive(UUID uuid) {
        return STATES.containsKey(uuid);
    }

    /** 是否已到可取出时间 */
    public static boolean isComplete(UUID uuid, long currentTick) {
        Entry e = STATES.get(uuid);
        if (e == null) return false;
        return (currentTick - e.startTick) >= e.durationTick;
    }

    /** 当前进度 0.0 ~ 1.0 */
    public static float getProgress(UUID uuid, long currentTick) {
        Entry e = STATES.get(uuid);
        if (e == null || e.durationTick <= 0) return 0f;
        return Math.min(1.0f, (float) (currentTick - e.startTick) / (float) e.durationTick);
    }

    public static long getDurationTick(UUID uuid) {
        Entry e = STATES.get(uuid);
        return e == null ? 0 : e.durationTick;
    }

    public static long getStartTick(UUID uuid) {
        Entry e = STATES.get(uuid);
        return e == null ? 0 : e.startTick;
    }

    /** 开始时的配方结果（取走校验用） */
    public static ItemStack getExpectedResult(UUID uuid) {
        Entry e = STATES.get(uuid);
        return e == null ? ItemStack.EMPTY : e.expectedResult;
    }

    public static void clear(UUID uuid) {
        STATES.remove(uuid);
    }
}
