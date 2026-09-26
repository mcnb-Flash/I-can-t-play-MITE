package name.icpm.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;

/**
 * 防改存档核心（v3：基于 level.dat 内容指纹比对，废弃旧的背包签名/封印物机制）。
 *
 * <p>设计（严格贴合用户需求）：
 * <ul>
 *   <li><b>退出存档</b>：{@link #backupLevelDat} 把刚落盘的 {@code level.dat}（及单人玩家数据
 *       {@code playerdata/<uuid>.dat}）复制进 {@code <存档>/icpmback/} 作为基准备份，并写入内容指纹。</li>
 *   <li><b>进入存档前</b>：{@link #verifyAndHandle} 计算当前 {@code level.dat} 的「内容指纹」
 *       （剔除 {@code LastPlayed}/{@code SizeOnDisk}/{@code Time}/{@code DayTime}/天气计时等
 *       每次加载都会变化的易变字段，详见 {@link #VOLATILE_FIELDS}），与基准备份指纹比对。
 *       不一致（外部工具在两次会话之间改动了世界身份/设置）→ 破坏存档核心并弹「篡改」窗。</li>
 *   <li><b>篡改处理</b>：写 {@code .breached} 标记并弹窗。<b>不删除任何文件</b>——拆解期内
 *       {@code ICPMDataSealMixin}（玩家数据）与 {@code ICPMLevelSaveGateMixin}（level.dat/区块）
 *       封锁一切落盘，内存里的被篡改数据无法写回，磁盘保持进入时原样。</li>
 *   <li><b>恢复</b>：弹窗键入「我再也不作弊了」→ {@link #restoreFromBackup} 从备份还原
 *       level.dat 与玩家数据，<b>标记保留</b>（本会话继续封锁落盘直到断开）；
 *       下次进入时比对通过后由 {@link #verifyAndHandle} 自动清除标记。</li>
 * </ul>
 *
 * <p>为什么不用整文件 SHA-256：Minecraft 每次加载世界都会重写 {@code level.dat} 的
 * {@code LastPlayed}（及 {@code SizeOnDisk}）等字段，导致即使没有外部篡改，进入时的
 * level.dat 也必然与退出时备份的字节不同 → 整文件哈希会「每次进入都误判篡改」。
 * 因此比对的是<b>剔除易变字段后的内容指纹</b>：正常游玩（时间流逝、天气变化）不会触发，
 * 但外部改动了世界种子/出生点/难度/游戏规则等身份字段则必然不一致。</p>
 *
 * <p>基准/指纹缺失（首次进入 / 新世界 / 从没被 ICPM 接管过的老存档 / 旧版升级）→ 直接以当前
 * level.dat 建立基准（可信：刚被游戏成功加载），不判篡改。</p>
 *
 * <p>devMode（{@code -Dicpm.devMode=true}）下整机制跳过——开发者可自由调试。
 * 仅单人（集成服务器）启用；多人服务器不触碰 level.dat。</p>
 */
public final class DataSeal {

    /** 备份目录名（位于存档根目录）。 */
    private static final String BACKUP_DIR = "icpmback";
    /** 拆解期标记文件名。 */
    private static final String BREACHED = ".breached";
    /** 内容指纹文件名（剔除易变字段后的 level.dat 指纹）。 */
    private static final String FINGERPRINT = "level.dat.fp";

    /**
     * 比对时应忽略的易变字段：这些字段在正常游戏过程中（或每次加载时）必然变化，
     * 不应作为「是否篡改」的判据。改 World 身份/设置的外部工具不会只改这些字段。
     */
    private static final Set<String> VOLATILE_FIELDS = Set.of(
            "LastPlayed",      // 每次加载都会更新为当前时间戳 → 永远不同
            "SizeOnDisk",      // 文件大小，随区块/数据变化
            "Time",            // 世界总刻数，正常游玩会推进
            "DayTime",         // 一天内的刻数，正常游玩会推进
            "thunderTime",     // 雷暴计时
            "clearWeatherTime",// 晴朗计时
            "rainTime"         // 降雨计时
    );

    private DataSeal() {
    }

    // ==================== 路径工具 ====================

    private static Path worldDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.LEVEL_DATA_FILE).getParent();
    }

    private static Path backupDir(Path worldDir) {
        return worldDir.resolve(BACKUP_DIR);
    }

    private static Path levelBackup(Path worldDir) {
        return backupDir(worldDir).resolve("level.dat.bak");
    }

    private static Path fingerprintFile(Path worldDir) {
        return backupDir(worldDir).resolve(FINGERPRINT);
    }

    private static Path playerBackup(Path worldDir, UUID id) {
        return backupDir(worldDir).resolve("playerdata").resolve(id + ".dat.bak");
    }

    private static Path breachedMarker(Path worldDir) {
        return backupDir(worldDir).resolve(BREACHED);
    }

    private static Path playerDataFile(MinecraftServer server, UUID id) {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id + ".dat");
    }

    // ==================== 退出存档：备份（保留） + 指纹 ====================

    /** 存盘后调用：把刚落盘的 level.dat 与玩家数据备份到 icpmback，并刷新内容指纹。 */
    public static void backupLevelDat(MinecraftServer server, ServerPlayer player) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        if (server == null || !server.isSingleplayer()) {
            return;
        }
        Path wd = worldDir(server);
        UUID id = player.getUUID();
        Path srcLevel = server.getWorldPath(LevelResource.LEVEL_DATA_FILE);
        if (!Files.exists(srcLevel)) {
            return;
        }
        try {
            Files.createDirectories(backupDir(wd));
            Files.copy(srcLevel, levelBackup(wd), StandardCopyOption.REPLACE_EXISTING);
            storeFingerprint(wd, computeFingerprint(srcLevel));
        } catch (IOException ignored) {
            // 忽略
        }
        Path srcPd = playerDataFile(server, id);
        if (Files.exists(srcPd)) {
            try {
                Files.createDirectories(playerBackup(wd, id).getParent());
                Files.copy(srcPd, playerBackup(wd, id), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // 忽略
            }
        }
    }

    // ==================== 进入存档前：比对 level.dat 内容指纹 ====================

    /**
     * 进入存档前调用：比对当前 level.dat（剔除易变字段后的内容指纹）与基准备份指纹。
     * <ul>
     *   <li>基准/指纹缺失 → 以当前为准建立基线，不判篡改。</li>
     *   <li>不一致 → 判定篡改，弹窗。</li>
     *   <li>一致且上次处于拆解期 → 自动清除标记，恢复正常存盘。</li>
     * </ul>
     */
    public static void verifyAndHandle(ServerPlayer player) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null || !server.isSingleplayer()) {
            return;
        }
        Path wd = worldDir(server);
        UUID id = player.getUUID();
        Path cur = server.getWorldPath(LevelResource.LEVEL_DATA_FILE);
        Path bak = levelBackup(wd);
        Path fp = fingerprintFile(wd);
        boolean wasBreached = Files.exists(breachedMarker(wd));

        if (!Files.exists(bak)) {
            // 完全没有基准（首次进入 / 未接管过的老存档）：以当前为准初始化
            if (!Files.exists(cur)) {
                return;
            }
            try {
                Files.createDirectories(backupDir(wd));
                Files.copy(cur, bak, StandardCopyOption.REPLACE_EXISTING);
                storeFingerprint(wd, computeFingerprint(cur));
            } catch (IOException ignored) {
                // 忽略
            }
            clearBreachedIfPresent(wd, wasBreached);
            return;
        }

        if (!Files.exists(fp)) {
            // 有备份但无指纹（旧版升级场景）：以可信备份为基线，不立即判篡改
            storeFingerprint(wd, computeFingerprint(bak));
            clearBreachedIfPresent(wd, wasBreached);
            return;
        }

        if (!Files.exists(cur)) {
            if (!wasBreached) {
                return;
            }
            // 拆解期异常中断（游戏崩溃/强杀）导致 level.dat 丢失：从基准修复后继续走比对
            copyBackupIntoWorld(wd, id);
        }
        if (!Files.exists(cur)) {
            return;
        }
        String fCur = computeFingerprint(cur);
        String fBak = readFingerprint(fp);
        if (fCur != null && fBak != null && fCur.equals(fBak)) {
            // 比对通过：若上次处于拆解期（即「恢复成功后的重进」），解除拆解期恢复存盘
            clearBreachedIfPresent(wd, wasBreached);
            return;
        }
        onBreach(server, wd, player);
    }

    private static void onBreach(MinecraftServer server, Path wd, ServerPlayer player) {
        UUID id = player.getUUID();
        try {
            Files.createDirectories(backupDir(wd));
            Files.writeString(breachedMarker(wd), "1");
        } catch (IOException ignored) {
            // 忽略
        }
        // 注意：不删除 level.dat / level.dat_old / 玩家数据。
        // 拆解期由 ICPMDataSealMixin（玩家数据）+ ICPMLevelSaveGateMixin（level.dat/区块）封锁全部落盘，
        // 内存中的被篡改数据无法写回；磁盘保持进入时原样，恢复时直接从基准覆盖。
        openBreachScreen(wd, levelBackup(wd), playerBackup(wd, id), id);
    }

    // ==================== 备份 / 恢复 ====================

    /**
     * 从备份还原 level.dat 与玩家数据（弹窗键入口令后调用）。
     * <b>不清除 .breached 标记</b>：本会话剩余时间（恢复后立即断开）继续封锁一切落盘，
     * 防止服务器停机时把内存里的被篡改数据写回 level.dat（这正是「恢复后重进仍弹窗」的根因）。
     * 标记由下次进入时 verifyAndHandle 比对通过后自动清除。
     */
    public static boolean restoreFromBackup(Path wd, UUID id) {
        return copyBackupIntoWorld(wd, id);
    }

    /** 把基准备份复制回存档根目录（level.dat + 玩家数据）。 */
    private static boolean copyBackupIntoWorld(Path wd, UUID id) {
        Path lb = levelBackup(wd);
        if (!Files.exists(lb)) {
            return false;
        }
        try {
            Files.copy(lb, wd.resolve("level.dat"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return false;
        }
        Path pb = playerBackup(wd, id);
        if (Files.exists(pb)) {
            try {
                Files.createDirectories(wd.resolve("playerdata"));
                Files.copy(pb, wd.resolve("playerdata").resolve(id + ".dat"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // 玩家数据缺失时仅还原 level.dat 也可接受
            }
        }
        return true;
    }

    /** 拆解期是否应跳过保存（防止 level.dat 自愈）。 */
    public static boolean shouldSkipSave(MinecraftServer server) {
        if (server == null || !server.isSingleplayer()) {
            return false;
        }
        return Files.exists(breachedMarker(worldDir(server)));
    }

    // ==================== 内容指纹 ====================

    /** 计算 level.dat 的内容指纹：剔除易变字段后取确定性序列化字符串的 SHA-256。失败时返回 null。 */
    private static String computeFingerprint(Path levelDat) {
        try {
            CompoundTag root = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap());
            CompoundTag data = root.getCompoundOrEmpty("Data");
            CompoundTag stable = data.copy();
            for (String key : VOLATILE_FIELDS) {
                stable.remove(key);
            }
            // CompoundTag.toString() 对相同内容确定性输出，足以作为可比对的指纹
            return sha256(stable.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private static void storeFingerprint(Path wd, String fp) {
        if (fp == null) {
            return;
        }
        try {
            Files.writeString(fingerprintFile(wd), fp);
        } catch (IOException ignored) {
            // 忽略
        }
    }

    private static String readFingerprint(Path fp) {
        try {
            return Files.readString(fp).trim();
        } catch (IOException e) {
            return null;
        }
    }

    private static void clearBreachedIfPresent(Path wd, boolean wasBreached) {
        if (wasBreached) {
            try {
                Files.deleteIfExists(breachedMarker(wd));
            } catch (IOException ignored) {
                // 忽略
            }
        }
    }

    // ==================== 内部 ====================

    /** 计算字节 SHA-256（小写十六进制）。出错返回 null。 */
    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** 反射调用客户端弹窗（避免在服务端代码静态引用客户端类）。 */
    private static void openBreachScreen(Path worldDir, Path levelBackup, Path playerBackup, UUID id) {
        try {
            Class<?> c = Class.forName("name.icpm.client.DataSealClient");
            c.getMethod("openBreach", Path.class, Path.class, Path.class, UUID.class)
                    .invoke(null, worldDir, levelBackup, playerBackup, id);
        } catch (Throwable ignored) {
            // 专用服务器等无客户端环境：静默忽略
        }
    }
}
