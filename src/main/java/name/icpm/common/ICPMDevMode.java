package name.icpm.common;

/**
 * ICPM 开发模式（dev mode）—— 唯一的“作弊后门”，**默认关闭**。
 *
 * <p><b>开启方式只有一种：JVM 启动参数 {@code -Dicpm.devMode=true}。</b>
 * 配置文件与游戏内命令**都无法开启**（曾经把 {@code config/icpm.json} 的
 * {@code enableCreativeMode} 也当作开关，等于把后门摆在玩家随手可改的地方——已移除）。
 *
 * <p>关闭（默认）时，ICPM 按 R196 语义禁用一切非生存玩法：
 * <ul>
 *   <li>创建世界界面不能开启“允许作弊”、不能选创造/调试模式（MITE 原版行为）；</li>
 *   <li>局域网开放强制生存 + 不允许作弊（R196 {@code CommandServerPublishLocal:30}
 *       —— {@code shareToLAN(EnumGameType.SURVIVAL, false)}）；</li>
 *   <li>进入世界后的任何非生存模式请求都被归一为生存
 *       （R196 {@code CommandGameMode:40} —— 只允许切 SURVIVAL）。</li>
 * </ul>
 *
 * <p>开启后上述限制全部解除，且**不对玩家的游戏模式做任何改动**——开发者可自由切换创造调试。
 */
public final class ICPMDevMode {

    /** JVM 启动参数名：{@code -Dicpm.devMode=true}。 */
    public static final String PROPERTY = "icpm.devMode";

    /** 测试/内部强制值（null = 不强制，走正常判定）。 */
    private static Boolean forced = null;

    private ICPMDevMode() {
    }

    /** dev 模式是否开启（**只看 JVM 参数**；配置文件不再参与判定）。 */
    public static boolean isEnabled() {
        if (forced != null) {
            return forced;
        }
        return Boolean.parseBoolean(System.getProperty(PROPERTY, "false").trim());
    }

    /** 仅供测试使用：强制 dev 模式取值（传 null 取消强制）。 */
    public static void forceForTesting(Boolean value) {
        forced = value;
    }
}
