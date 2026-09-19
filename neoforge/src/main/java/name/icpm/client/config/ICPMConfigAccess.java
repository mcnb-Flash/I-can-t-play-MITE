package name.icpm.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ICPM 配置界面的"安全入口"（NeoForge 端）。
 *
 * NeoForge 没有原版 malilib，改用其非官方移植 <b>MaFgLib</b>（同为 {@code fi.dy.masa.malilib} 包，
 * 同步自 sakura-ryoko/malilib）。因此配置界面类（{@link ICPMConfigScreen}）代码与 Fabric 端一致。
 *
 * 为什么需要这层间接：MaFgLib 是可选前置。直接 import/调用 ICPMConfigScreen（其父类是
 * MaFgLib 的 GuiConfigsBase）会让未装 MaFgLib 的客户端在类加载阶段 NoClassDefFoundError 崩溃。
 * 故全部经反射调用，并先探测 MaFgLib 是否在场；不在场时静默降级（用 /icpmconfig 命令）。
 *
 * 探测方式不依赖任何 loader API：直接尝试加载 MaFgLib 主类 {@code fi.dy.masa.malilib.MaLiLibReference}。
 */
public final class ICPMConfigAccess {

    private static final Logger LOG = LoggerFactory.getLogger("ICPM");

    private static final String SCREEN = "name.icpm.client.config.ICPMConfigScreen";

    private ICPMConfigAccess() {
    }

    /** MaFgLib 是否在场（探测其主类存在性）。 */
    public static boolean mafglibLoaded() {
        try {
            Class.forName("fi.dy.masa.malilib.MaLiLibReference");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 客户端初始化时调用：MaFgLib 在场才注册配置句柄 + 配置屏注册表。 */
    public static void init() {
        if (!mafglibLoaded()) {
            return;
        }
        try {
            Class.forName(SCREEN).getMethod("registerIfLoaded").invoke(null);
        } catch (Throwable t) {
            LOG.warn("[ICPM] MaFgLib config init failed (ignored)", t);
        }
    }

    /** 玩家按 ICPM 配置键（默认 H）时调用。非游戏界面层才打开，避免劫持其它 GUI。 */
    public static void openConfig() {
        if (!mafglibLoaded()) {
            return;
        }
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        try {
            Class.forName(SCREEN).getMethod("open", Screen.class).invoke(null, new Object[]{null});
        } catch (Throwable t) {
            // 打印完整堆栈（含反射 InvocationTargetException 的真实根因）
            LOG.warn("[ICPM] open MaFgLib config failed", t);
        }
    }
}
