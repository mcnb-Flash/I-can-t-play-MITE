package name.icpm.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ICPM 传送门位置记忆
 * 记录玩家在维度间的最后位置，用于传送门返回时定位。
 *
 * - Overworld→Underworld 时保存 Overworld 坐标
 * - Underworld→Overworld 时使用保存的 Overworld 坐标
 * - Underworld→Nether 时保存 Underworld 坐标
 * - Nether→Underworld 时使用保存的 Underworld 坐标
 */
public class PortalPositionStorage {

    private static final Map<UUID, int[]> LAST_OVERWORLD_POS = new HashMap<>();
    private static final Map<UUID, int[]> LAST_UNDERWORLD_POS = new HashMap<>();

    /**
     * 保存 Overworld 位置（进入地下世界时调用）
     */
    public static void saveOverworldPosition(ServerPlayer player, int x, int y, int z) {
        LAST_OVERWORLD_POS.put(player.getUUID(), new int[]{x, y, z});
    }

    /**
     * 获取保存的 Overworld 位置（从地下世界返回时调用）
     * 返回 null 表示没有记录
     */
    public static int[] getOverworldPosition(ServerPlayer player) {
        return LAST_OVERWORLD_POS.get(player.getUUID());
    }

    /**
     * 保存 Underworld 位置（进入地狱时调用）
     */
    public static void saveUnderworldPosition(ServerPlayer player, int x, int y, int z) {
        LAST_UNDERWORLD_POS.put(player.getUUID(), new int[]{x, y, z});
    }

    /**
     * 获取保存的 Underworld 位置（从地狱返回时调用）
     * 返回 null 表示没有记录
     */
    public static int[] getUnderworldPosition(ServerPlayer player) {
        return LAST_UNDERWORLD_POS.get(player.getUUID());
    }

    /**
     * 随玩家 NBT 落盘（addAdditionalSaveData 调用）。
     * 不再单独写 playerdata 文件，避免与原版玩家保存互相覆盖导致数据丢失。
     */
    public static void save(Player player, ValueOutput tag) {
        UUID uuid = player.getUUID();
        int[] overworldPos = LAST_OVERWORLD_POS.get(uuid);
        if (overworldPos != null) {
            tag.putInt("IcpmOverworldX", overworldPos[0]);
            tag.putInt("IcpmOverworldY", overworldPos[1]);
            tag.putInt("IcpmOverworldZ", overworldPos[2]);
        }
        int[] underworldPos = LAST_UNDERWORLD_POS.get(uuid);
        if (underworldPos != null) {
            tag.putInt("IcpmUnderworldX", underworldPos[0]);
            tag.putInt("IcpmUnderworldY", underworldPos[1]);
            tag.putInt("IcpmUnderworldZ", underworldPos[2]);
        }
    }

    /**
     * 从玩家 NBT 读取（readAdditionalSaveData 调用）。
     */
    public static void load(Player player, ValueInput tag) {
        UUID uuid = player.getUUID();
        if (tag.getInt("IcpmOverworldX").isPresent()) {
            LAST_OVERWORLD_POS.put(uuid, new int[]{
                    tag.getInt("IcpmOverworldX").orElse(0),
                    tag.getInt("IcpmOverworldY").orElse(0),
                    tag.getInt("IcpmOverworldZ").orElse(0)});
        }
        if (tag.getInt("IcpmUnderworldX").isPresent()) {
            LAST_UNDERWORLD_POS.put(uuid, new int[]{
                    tag.getInt("IcpmUnderworldX").orElse(0),
                    tag.getInt("IcpmUnderworldY").orElse(0),
                    tag.getInt("IcpmUnderworldZ").orElse(0)});
        }
    }
}
