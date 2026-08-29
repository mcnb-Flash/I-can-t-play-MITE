package name.icpm.common;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ICPM 合成冷却跟踪工具类（非 Mixin）
 *
 * 当玩家从合成台取出成品后，有短暂冷却期（10 tick = 0.5秒）
 * 防止快速连续合成
 */
public final class ICPMCraftCooldowns {

    private static final ConcurrentHashMap<UUID, Long> CRAFT_COOLDOWNS = new ConcurrentHashMap<>();

    public static final int CRAFT_COOLDOWN_TICKS = 10; // 0.5秒冷却

    private ICPMCraftCooldowns() {
    }

    /** 设置合成冷却（从当前 tick 起 +10 tick） */
    public static void markCrafted(Player player) {
        CRAFT_COOLDOWNS.put(player.getUUID(), player.level().getGameTime() + CRAFT_COOLDOWN_TICKS);
    }

    /** 是否处于冷却中 */
    public static boolean hasCraftCooldown(Player player) {
        Long cooldownEnd = CRAFT_COOLDOWNS.get(player.getUUID());
        if (cooldownEnd == null) {
            return false;
        }
        long currentTick = player.level().getGameTime();
        if (currentTick >= cooldownEnd) {
            CRAFT_COOLDOWNS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    /** 剩余冷却 tick 数 */
    public static int getCraftCooldownRemaining(Player player) {
        Long cooldownEnd = CRAFT_COOLDOWNS.get(player.getUUID());
        if (cooldownEnd == null) {
            return 0;
        }
        long currentTick = player.level().getGameTime();
        long remaining = cooldownEnd - currentTick;
        if (remaining <= 0) {
            CRAFT_COOLDOWNS.remove(player.getUUID());
            return 0;
        }
        return (int) remaining;
    }
}
