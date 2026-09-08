package name.icpm.client.config;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * ICPM 配置界面（malilib GuiConfigsBase 子类）。
 *
 * 打开方式（安装 malilib 后）：
 * 1. 按 "ICPM 配置" 键（默认 H，可在 选项→控制→游戏玩法 修改）
 * 2. 从 malilib 的 mod 配置列表屏点进 ICPM（经 {@link #registerConfigScreenRegistry()} 注册）
 * 界面里勾选 enableCreativeMode 即写盘 config/icpm.json 生效。
 * 未安装 malilib 时本类不会被加载，用 /icpmconfig 命令。
 */
public class ICPMConfigScreen extends GuiConfigsBase {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ICPM");

    public ICPMConfigScreen() {
        this(null);
    }

    public ICPMConfigScreen(Screen parent) {
        super(10, 50, ICPMMaLiLibConfig.MOD_ID, parent, "ICPM Config", new Object[0]);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<ConfigOptionWrapper> getConfigs() {
        // 必须返回 ConfigOptionWrapper 包装（WidgetListConfigOptions 对列表项做强转）；
        // 直接返回 ConfigBoolean 会在渲染/点击时 ClassCastException
        return ConfigOptionWrapper.createFor(List.of(ICPMMaLiLibConfig.ENABLE_CREATIVE));
    }

    @Override
    protected WidgetListConfigOptions createListWidget(int listX, int listY) {
        return new WidgetListConfigOptions(listX, listY, getBrowserWidth(), getBrowserHeight(),
                getConfigWidth(), getBrowserHeight() - 200, false, this);
    }

    /** 若安装了 malilib：注册配置句柄 + 配置屏注册表（使 ICPM 出现在 malilib 的 mod 配置列表）；未安装则仅命令可用。 */
    public static void registerIfLoaded() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("malilib")) {
            return;
        }
        ConfigManager.getInstance().registerConfigHandler(ICPMMaLiLibConfig.MOD_ID,
                ICPMMaLiLibConfig.getInstance());
        registerConfigScreenRegistry();
    }

    /**
     * 向 malilib 的 ConfigScreenRegistry 注册本 mod 的配置屏（0.27 系 fork 特性）。
     *
     * Registry.CONFIG_SCREEN 与 ModInfo 仅存在于部分 malilib 构建（sakura-ryoko 系）。
     * 官方 masa 版 malilib 没有这两个类——若直接 import 会让官方版用户在类加载期崩溃，
     * 故用反射注册并 try/catch 降级：注册失败仅意味着 ICPM 不会出现在 malilib 的 mod 列表里，
     * 配置句柄（registerConfigHandler）与本 mod 自己的按键/命令路径不受影响。
     */
    private static void registerConfigScreenRegistry() {
        try {
            Class<?> modInfo = Class.forName("fi.dy.masa.malilib.util.data.ModInfo");
            Class<?> registry = Class.forName("fi.dy.masa.malilib.registry.Registry");
            java.lang.reflect.Field screenField = registry.getField("CONFIG_SCREEN");
            Object registryInstance = screenField.get(null);
            java.lang.reflect.Constructor<?> ctor = modInfo.getConstructor(
                    String.class, String.class, java.util.function.Supplier.class);
            Object info = ctor.newInstance(ICPMMaLiLibConfig.MOD_ID, "ICPM",
                    (java.util.function.Supplier<?>) ICPMConfigScreen::new);
            registryInstance.getClass()
                    .getMethod("registerConfigScreenFactory", modInfo)
                    .invoke(registryInstance, info);
        } catch (Throwable t) {
            // malilib 版本不支持该注册表则跳过（不崩溃、不打断其余功能）
            LOGGER.warn("[ICPM] malilib ConfigScreenRegistry unavailable, skip mod-list entry: {}", t.toString());
        }
    }

    /** 打开 ICPM 配置界面（需要 malilib；未安装时静默忽略，由 /icpmconfig 命令兜底）。 */
    public static void open(Screen parent) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("malilib")) {
            return;
        }
        Minecraft.getInstance().setScreen(new ICPMConfigScreen(parent));
    }
}
