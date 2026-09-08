package name.icpm.client.config

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.slf4j.LoggerFactory

/**
 * ICPM malilib 配置的“安全入口”。
 *
 * 为什么需要这层间接：ICPM 对 malilib 是可选依赖（CF 玩家可不装）。直接 import/调用
 * ICPMConfigScreen（其父类是 malilib 的 GuiConfigsBase）会让未装 malilib 的客户端在
 * 类加载阶段就 NoClassDefFoundError 崩溃。因此全部经反射调用，且先探测 malilib 是否加载。
 */
object ICPMConfigAccess {
    private val LOG = LoggerFactory.getLogger("ICPM")

    private const val SCREEN = "name.icpm.client.config.ICPMConfigScreen"

    private fun malilibLoaded(): Boolean = FabricLoader.getInstance().isModLoaded("malilib")

    /** 客户端初始化时调用：malilib 在场才注册配置句柄 + 配置屏注册表。 */
    fun init() {
        if (!malilibLoaded()) return
        try {
            Class.forName(SCREEN).getMethod("registerIfLoaded").invoke(null)
        } catch (t: Throwable) {
            LOG.warn("[ICPM] malilib config init failed (ignored): {}", t.toString())
        }
    }

    /** 玩家按 ICPM 配置键（默认 H）时调用。非游戏界面层才打开，避免劫持其它 GUI。 */
    fun openConfig() {
        if (!malilibLoaded()) return
        if (Minecraft.getInstance().screen != null) return
        try {
            Class.forName(SCREEN).getMethod("open", Screen::class.java).invoke(null, null)
        } catch (t: Throwable) {
            LOG.warn("[ICPM] open malilib config failed (ignored): {}", t.toString())
        }
    }
}
