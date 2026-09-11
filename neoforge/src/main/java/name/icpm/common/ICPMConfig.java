package name.icpm.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ICPM 全局配置（config/icpm.json）。
 *
 * 字段：
 * - enableCreativeMode（默认 false）：为 true 时才允许玩家变更为创造模式（服务端 setGameMode 拦截）。
 * - witchWhisper（默认 false）：女巫低吟——玩家永久携带一枚随机女巫诅咒（不可被去咒药水解除/变更），
 *   且女巫仍可叠加普通诅咒。
 * - nightmareEra（默认 false）：噩梦时代——世界始终为夜晚，且每日 80% 为血月。
 * - poorTechnique（默认 0，0~4 档）：技术不佳——每档 耐久消耗 +25%、攻击力 -25%、挖掘速度 -25%。
 *
 * 修改途径：
 * 1. malilib 配置 GUI（若安装，由 ICPMMaLiLibConfig 桥接，变更回调本类 setter 落盘）
 * 2. 直接编辑 config/icpm.json（重启生效，或经 malilib GUI 立即生效）
 *
 * 线程：仅 Server/游戏主线程读写（单机安全）。
 */
public final class ICPMConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("ICPM-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String KEY_ENABLE_CREATIVE = "enableCreativeMode";
    public static final String KEY_WITCH_WHISPER = "witchWhisper";
    public static final String KEY_NIGHTMARE = "nightmareEra";
    public static final String KEY_POOR_TECHNIQUE = "poorTechnique";

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("icpm.json");
    }

    private static boolean enableCreativeMode = false;
    private static boolean witchWhisper = false;
    private static boolean nightmareEra = false;
    private static int poorTechnique = 0;

    private ICPMConfig() {
    }

    /** 启动时加载（ICPM.onInitialize 调用）。 */
    public static void init() {
        load();
    }

    /** 从 config/icpm.json 读取；文件缺失/损坏时回退默认值并重建默认文件。 */
    public static synchronized void load() {
        Path path = file();
        boolean creative = false;
        boolean whisper = false;
        boolean nightmare = false;
        int technique = 0;
        if (Files.isRegularFile(path)) {
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                if (obj != null) {
                    if (obj.has(KEY_ENABLE_CREATIVE)) creative = obj.get(KEY_ENABLE_CREATIVE).getAsBoolean();
                    if (obj.has(KEY_WITCH_WHISPER)) whisper = obj.get(KEY_WITCH_WHISPER).getAsBoolean();
                    if (obj.has(KEY_NIGHTMARE)) nightmare = obj.get(KEY_NIGHTMARE).getAsBoolean();
                    if (obj.has(KEY_POOR_TECHNIQUE)) technique = Math.max(0, Math.min(4, obj.get(KEY_POOR_TECHNIQUE).getAsInt()));
                }
            } catch (Exception e) {
                LOGGER.warn("[ICPM] 读取 {} 失败，使用默认值", path, e);
            }
        } else {
            saveDefault(path);
        }
        enableCreativeMode = creative;
        witchWhisper = whisper;
        nightmareEra = nightmare;
        poorTechnique = technique;
        LOGGER.info("[ICPM] config {} creative={} whisper={} nightmare={} poorTechnique={}",
                path, enableCreativeMode, witchWhisper, nightmareEra, poorTechnique);
    }

    // ==================== getters（各拦截点查询用） ====================

    public static boolean isCreativeEnabled() {
        return enableCreativeMode;
    }

    /** 女巫低吟：玩家永久携带随机女巫诅咒（服务器 tick 轮询此值）。 */
    public static boolean isWitchWhisperEnabled() {
        return witchWhisper;
    }

    /** 噩梦时代：始终夜晚 + 每日 80% 血月。 */
    public static boolean isNightmareEnabled() {
        return nightmareEra;
    }

    /** 技术不佳档位（0~4）。 */
    public static int poorTechniqueLevel() {
        return poorTechnique;
    }

    // ==================== setters（命令 / malilib 回调调用，立即写盘） ====================

    public static synchronized void setCreativeEnabled(boolean value) {
        enableCreativeMode = value;
        save();
    }

    public static synchronized void setWitchWhisper(boolean value) {
        witchWhisper = value;
        save();
    }

    public static synchronized void setNightmareEra(boolean value) {
        nightmareEra = value;
        save();
    }

    public static synchronized void setPoorTechnique(int value) {
        poorTechnique = Math.max(0, Math.min(4, value));
        save();
    }

    // ==================== 文件 IO ====================

    private static void saveDefault(Path path) {
        write(path, buildJson());
    }

    private static void save() {
        write(file(), buildJson());
    }

    private static JsonObject buildJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty(KEY_ENABLE_CREATIVE, enableCreativeMode);
        obj.addProperty(KEY_WITCH_WHISPER, witchWhisper);
        obj.addProperty(KEY_NIGHTMARE, nightmareEra);
        obj.addProperty(KEY_POOR_TECHNIQUE, poorTechnique);
        return obj;
    }

    private static void write(Path path, JsonObject obj) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, w);
            }
        } catch (IOException e) {
            LOGGER.error("[ICPM] 写入 {} 失败", path, e);
        }
    }
}
