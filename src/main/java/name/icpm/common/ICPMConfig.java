package name.icpm.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
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
 *
 * 修改途径：
 * 1. 游戏内命令：/icpmconfig creative on|off|status
 * 2. malilib 配置 GUI（若安装了 malilib，由 ICPMMaLiLibConfig 桥接，变更回调本类 setter 落盘）
 * 3. 直接编辑 config/icpm.json（重启生效，或走上述命令/ malilib 立即生效）
 *
 * 线程：仅 Server/游戏主线程读写（单机安全）。
 */
public final class ICPMConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("ICPM-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String KEY_ENABLE_CREATIVE = "enableCreativeMode";

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("icpm.json");
    }

    private static boolean enableCreativeMode = false;

    private ICPMConfig() {
    }

    /** 启动时加载（ICPM.onInitialize 调用）。 */
    public static void init() {
        load();
    }

    /** 从 config/icpm.json 读取；文件缺失/损坏时回退默认值并重建默认文件。 */
    public static synchronized void load() {
        Path path = file();
        boolean value = false;
        if (Files.isRegularFile(path)) {
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                if (obj != null && obj.has(KEY_ENABLE_CREATIVE)) {
                    value = obj.get(KEY_ENABLE_CREATIVE).getAsBoolean();
                }
            } catch (Exception e) {
                LOGGER.warn("[ICPM] 读取 {} 失败，使用默认值", path, e);
            }
        } else {
            saveDefault(path);
        }
        enableCreativeMode = value;
        LOGGER.info("[ICPM] config {} enableCreativeMode={}", path, enableCreativeMode);
    }

    /** 创造模式是否允许（拦截点查询用）。 */
    public static boolean isCreativeEnabled() {
        return enableCreativeMode;
    }

    /** 修改并立即写盘（命令 / malilib 回调调用）。 */
    public static synchronized void setCreativeEnabled(boolean value) {
        enableCreativeMode = value;
        save();
    }

    private static void saveDefault(Path path) {
        JsonObject obj = new JsonObject();
        obj.addProperty(KEY_ENABLE_CREATIVE, enableCreativeMode);
        write(path, obj);
    }

    private static void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty(KEY_ENABLE_CREATIVE, enableCreativeMode);
        write(file(), obj);
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
