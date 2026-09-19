package name.icpm.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ICPM 全局配置（config/icpm.json）。
 *
 * 字段（默认文件自带中文注释，方便服主直接阅读/修改）：
 * - enableCreativeMode（默认 false）：为 true 时才允许玩家变更为创造模式（服务端 setGameMode 拦截）。
 * - witchWhisper（默认 false）：女巫低吟——玩家永久携带一枚随机女巫诅咒（不可被去咒药水解除/变更），
 *   且女巫仍可叠加普通诅咒。
 * - nightmareEra（默认 false）：噩梦时代——世界始终为夜晚，且每日 80% 为血月。
 * - poorTechnique（默认 0，0~4 档）：技术不佳——每档 耐久消耗 +25%、攻击力 -25%、挖掘速度 -25%。
 * - weakStrike（默认 true）：弱击——R196 原版机制。未持工具（工具/木棍/骨头）时，
 *   只要「生命 小于 2 或 饱食+营养等于 0 或 攻击属性小于 1」之一成立，近战只击退、不造成伤害。
 *   true = 忠实 R196；false = 关闭弱击（空手/未持工具也能正常造成伤害）。
 * - noAttackCooldown（默认 true）：1.6.4 式无攻击速度（无攻击冷却）——近战无视武器攻击冷却，
 *   每次挥击都按满充能结算伤害（1.6.4/R196 手感）。false = 保留现代版攻击冷却（连点伤害衰减）。
 *
 * 文件格式：支持行注释与块注释——写入时生成带中文说明的文本，读取时自动剥离注释后再解析，
 * 因此服主可直接在 icpm.json 里阅读中文说明、按需改动数值。
 *
 * 修改途径：
 * 1. 直接编辑 config/icpm.json（重启生效，或经 MaFgLib GUI 立即生效）
 * 2. MaFgLib 配置 GUI（若安装，由 ICPMMaLiLibConfig 桥接，变更回调本类 setter 落盘）
 * 3. /icpmconfig 命令（creative / attackcooldown）
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
    public static final String KEY_WEAK_STRIKE = "weakStrike";
    public static final String KEY_NO_ATTACK_COOLDOWN = "noAttackCooldown";

    /** 注释标记（用于判断现有文件是否已是"带说明"格式）。 */
    private static final String COMMENT_MARK = "//";

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("icpm.json");
    }

    private static boolean enableCreativeMode = false;
    private static boolean witchWhisper = false;
    private static boolean nightmareEra = false;
    private static int poorTechnique = 0;
    private static boolean weakStrike = true;
    private static boolean noAttackCooldown = true;

    private ICPMConfig() {
    }

    /** 启动时加载（ICPM 主类调用）。 */
    public static void init() {
        load();
    }

    /** 从 config/icpm.json 读取（自动剥离注释）；文件缺失/损坏时回退默认值并重建默认文件。 */
    public static synchronized void load() {
        Path path = file();
        boolean creative = false;
        boolean whisper = false;
        boolean nightmare = false;
        int technique = 0;
        boolean weak = true;
        boolean noCooldown = true;
        if (Files.isRegularFile(path)) {
            boolean needsUpgrade = false;
            try {
                String raw = Files.readString(path, StandardCharsets.UTF_8);
                // 旧版本写出的无注释文件：本次顺带升级为"带中文说明"的新格式（数值保留）
                needsUpgrade = !raw.contains(COMMENT_MARK);
                JsonObject obj = GSON.fromJson(stripComments(raw), JsonObject.class);
                if (obj != null) {
                    if (obj.has(KEY_ENABLE_CREATIVE)) creative = obj.get(KEY_ENABLE_CREATIVE).getAsBoolean();
                    if (obj.has(KEY_WITCH_WHISPER)) whisper = obj.get(KEY_WITCH_WHISPER).getAsBoolean();
                    if (obj.has(KEY_NIGHTMARE)) nightmare = obj.get(KEY_NIGHTMARE).getAsBoolean();
                    if (obj.has(KEY_POOR_TECHNIQUE)) technique = Math.max(0, Math.min(4, obj.get(KEY_POOR_TECHNIQUE).getAsInt()));
                    if (obj.has(KEY_WEAK_STRIKE)) weak = obj.get(KEY_WEAK_STRIKE).getAsBoolean();
                    if (obj.has(KEY_NO_ATTACK_COOLDOWN)) noCooldown = obj.get(KEY_NO_ATTACK_COOLDOWN).getAsBoolean();
                }
            } catch (Exception e) {
                LOGGER.warn("[ICPM] 读取 {} 失败，使用默认值", path, e);
            }
            enableCreativeMode = creative;
            witchWhisper = whisper;
            nightmareEra = nightmare;
            poorTechnique = technique;
            weakStrike = weak;
            noAttackCooldown = noCooldown;
            // 旧文件缺少新键（如 noAttackCooldown）时同样重写一次，把新选项补全并带上中文说明
            if (needsUpgrade || !rawTextHasAllKeys(path)) {
                save();
            }
        } else {
            enableCreativeMode = false;
            witchWhisper = false;
            nightmareEra = false;
            poorTechnique = 0;
            weakStrike = true;
            noAttackCooldown = true;
            saveDefault(path);
        }
        LOGGER.info("[ICPM] config {} creative={} whisper={} nightmare={} poorTechnique={} weakStrike={} noAttackCooldown={}",
                path, enableCreativeMode, witchWhisper, nightmareEra, poorTechnique, weakStrike, noAttackCooldown);
    }

    /** 现有文件是否已包含全部键（缺键则需重写以补全 + 补中文说明）。 */
    private static boolean rawTextHasAllKeys(Path path) {
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            return raw.contains(KEY_ENABLE_CREATIVE)
                    && raw.contains(KEY_WITCH_WHISPER)
                    && raw.contains(KEY_NIGHTMARE)
                    && raw.contains(KEY_POOR_TECHNIQUE)
                    && raw.contains(KEY_WEAK_STRIKE)
                    && raw.contains(KEY_NO_ATTACK_COOLDOWN);
        } catch (Exception e) {
            return true; // 读取异常时不额外重写
        }
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

    /** 弱击：true = 启用 R196 原版弱击；false = 关闭（未持工具也能正常造成伤害）。 */
    public static boolean isWeakStrikeEnabled() {
        return weakStrike;
    }

    /** 1.6.4 无攻击速度：true = 近战无视攻击冷却、每击满伤害；false = 保留现代版攻击冷却。 */
    public static boolean isNoAttackCooldownEnabled() {
        return noAttackCooldown;
    }

    // ==================== setters（命令 / MaFgLib 回调调用，立即写盘） ====================

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

    public static synchronized void setWeakStrike(boolean value) {
        weakStrike = value;
        save();
    }

    public static synchronized void setNoAttackCooldown(boolean value) {
        noAttackCooldown = value;
        save();
    }

    // ==================== 文件 IO ====================

    private static void saveDefault(Path path) {
        write(path);
    }

    private static void save() {
        write(file());
    }

    /**
     * 生成带中文说明的配置文本（键值取自当前状态）。
     * 每一个选项上方都有一句中文解释，服主无需看英文即可理解含义。
     */
    private static String buildCommentedJson() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("// ===== ICPM 配置文件（config/icpm.json）=====\n");
        sb.append("// 支持 // 行注释；修改后重启游戏/服务器生效（装 MaFgLib 的客户端可在 GUI 里即时生效）。\n");
        sb.append("// 取值说明：true = 开启，false = 关闭。\n");
        sb.append("{\n");
        sb.append("  //允许玩家切换为创造模式：false = 禁止（默认）；true = 允许。\n");
        sb.append("  \"").append(KEY_ENABLE_CREATIVE).append("\": ").append(enableCreativeMode).append(",\n");
        sb.append("\n");
        sb.append("  //女巫低吟：true = 玩家永久携带一枚随机女巫诅咒（去咒药水无法解除或变更），且女巫仍可叠加普通诅咒；false = 关闭（默认）。\n");
        sb.append("  \"").append(KEY_WITCH_WHISPER).append("\": ").append(witchWhisper).append(",\n");
        sb.append("\n");
        sb.append("  //噩梦时代：true = 世界始终为夜晚，且每个游戏日 80% 为血月（天数照常推进）；false = 关闭（默认）。\n");
        sb.append("  \"").append(KEY_NIGHTMARE).append("\": ").append(nightmareEra).append(",\n");
        sb.append("\n");
        sb.append("  //技术不佳档位：0 = 关闭（默认）；1~4 档，每档 耐久消耗 +25%、攻击力 -25%、挖掘速度 -25%。\n");
        sb.append("  \"").append(KEY_POOR_TECHNIQUE).append("\": ").append(poorTechnique).append(",\n");
        sb.append("\n");
        sb.append("  //弱击（R196 原版机制）：true = 启用（默认）。未持工具（工具/木棍/骨头）时，只要「生命小于 2 或 饱食+营养等于 0 或 攻击属性小于 1」之一成立，近战只击退、不造成伤害。\n");
        sb.append("  //                       false = 关闭弱击，空手/未持工具也能正常对生物造成伤害。\n");
        sb.append("  \"").append(KEY_WEAK_STRIKE).append("\": ").append(weakStrike).append(",\n");
        sb.append("\n");
        sb.append("  //无攻击速度（1.6.4 原版机制）：true = 启用（默认）。近战无视武器攻击冷却，每次挥击都按满伤害结算（1.6.4/R196 手感），准星也不再有攻击冷却指示。\n");
        sb.append("  //                             false = 保留现代版攻击冷却（需要等待充能，连点会衰减伤害）。\n");
        sb.append("  \"").append(KEY_NO_ATTACK_COOLDOWN).append("\": ").append(noAttackCooldown).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void write(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, buildCommentedJson(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[ICPM] 写入 {} 失败", path, e);
        }
    }

    /**
     * 剥离行注释与块注释（字符串字面量内的注释符号不受影响），
     * 使"带中文说明"的 icpm.json 仍能被标准 JSON 解析器读取。
     */
    private static String stripComments(String text) {
        StringBuilder out = new StringBuilder(text.length());
        boolean inString = false;
        boolean escape = false;
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (inString) {
                out.append(c);
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                i++;
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
                i++;
                continue;
            }
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                i += 2;
                while (i < n && text.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(i + 2, n);
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }
}
