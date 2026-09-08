package name.icpm.client.config;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * ICPM 配置界面（malilib GuiConfigsBase 子类，标签页 UI）。
 *
 * 仿 MITE-ITF-Reborn 风格：顶部一排标签按钮（自然恶意 / 疯狂劲敌 / 天赐福星 /
 * 实验性玩法 / 参数配置 / 杂项）+ 右侧重置/默认顺序按钮 + 下方配置列表。
 * 当前仅 "参数配置" 标签页有实际选项（enableCreativeMode），其余标签为占位。
 *
 * 打开方式（装 malilib 后）：
 * 1. 按 "ICPM 配置" 键（默认 H，可在 选项→控制→游戏玩法 修改）
 * 2. 从 malilib 的 mod 配置列表屏点进 ICPM（经 registerConfigScreenRegistry() 注册）
 * 未安装 malilib 时本类不会被加载，用 /icpmconfig 命令。
 */
public class ICPMConfigScreen extends GuiConfigsBase {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ICPM");

    /** 标签页定义（顺序与截图一致：左→右）。 */
    public enum ConfigTab {
        NATURAL_EVIL("自然恶意"),
        FRENZY_ENEMY("疯狂劲敌"),
        HEAVEN_BLESSING("天赐福星"),
        EXPERIMENTAL_PLAY("实验性玩法"),
        PARAMETER_CONFIG("参数配置"),
        MISC("杂项");

        public final String displayName;
        ConfigTab(String displayName) { this.displayName = displayName; }

        public static ConfigTab fromOrdinal(int idx) {
            ConfigTab[] all = values();
            if (idx < 0 || idx >= all.length) return PARAMETER_CONFIG;
            return all[idx];
        }
    }

    /** 默认标签 = "参数配置"（放 enableCreativeMode 开关）。 */
    private int currentTab = ConfigTab.PARAMETER_CONFIG.ordinal();

    public ICPMConfigScreen() {
        this(null, ConfigTab.PARAMETER_CONFIG.ordinal());
    }

    public ICPMConfigScreen(Screen parent) {
        this(parent, ConfigTab.PARAMETER_CONFIG.ordinal());
    }

    public ICPMConfigScreen(Screen parent, int tabIndex) {
        super(10, 50, ICPMMaLiLibConfig.MOD_ID, parent, "ICPM Config", new Object[0]);
        this.currentTab = Math.max(0, Math.min(ConfigTab.values().length - 1, tabIndex));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<ConfigOptionWrapper> getConfigs() {
        if (ConfigTab.fromOrdinal(this.currentTab) == ConfigTab.PARAMETER_CONFIG) {
            // 必须返回包装列表（WidgetListConfigOptions 对列表项强转 ConfigOptionWrapper）
            return ConfigOptionWrapper.createFor(List.of(ICPMMaLiLibConfig.ENABLE_CREATIVE));
        }
        // 其他标签页：占位空列表（避免崩溃；实际选项后续再加）
        return ConfigOptionWrapper.createFor(List.of());
    }

    @Override
    protected WidgetListConfigOptions createListWidget(int listX, int listY) {
        return new WidgetListConfigOptions(listX, listY, getBrowserWidth(), getBrowserHeight(),
                getConfigWidth(), getBrowserHeight() - 200, false, this);
    }

    /** 关闭 malilib 的 keybind 搜索框——本 UI 用标签页导航，不需要搜索条。 */
    @Override
    public boolean useKeybindSearch() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();
        buildTabButtons();
        buildToolbarButtons();
        buildEmptyHint();
    }

    /** 顶部一排标签按钮（截图中的"自然恶意 / 疯狂劲敌 / ..."那一行）。 */
    private void buildTabButtons() {
        ConfigTab[] tabs = ConfigTab.values();
        int btnW = 80;
        int btnH = 20;
        int spacing = 4;
        int startX = 10;
        int y = 22;
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            ConfigTab t = tabs[i];
            int x = startX + i * (btnW + spacing);
            ButtonGeneric btn = new ButtonGeneric(x, y, btnW, btnH, t.displayName, (String[]) null);
            // 当前标签按钮：禁用交互；非当前：点击切换
            btn.setEnabled(i != this.currentTab);
            this.addButton(btn, (button, mouseButton) -> switchTab(idx));
        }
    }

    /** 顶部右侧工具按钮（截图中的"⟲ 重置"和"默认顺序"）。当前为占位（用户暂不要求功能）。 */
    private void buildToolbarButtons() {
        int y = 22;
        int h = 20;
        int resetW = 24;
        int orderW = 80;
        int rightMargin = 10;
        int orderX = this.getScreenWidth() - rightMargin - orderW;
        int resetX = orderX - 4 - resetW;

        ButtonGeneric resetBtn = new ButtonGeneric(resetX, y, resetW, h, "\u27F2", (String[]) null);
        this.addButton(resetBtn, (b, m) -> { /* 占位：重置该标签选项到默认 */ });

        ButtonGeneric orderBtn = new ButtonGeneric(orderX, y, orderW, h, "默认顺序", (String[]) null);
        this.addButton(orderBtn, (b, m) -> { /* 占位：按名称排序选项 */ });
    }

    /** 非参数配置标签页：列表区显示一条"此栏目待开发"的占位提示。 */
    private void buildEmptyHint() {
        if (ConfigTab.fromOrdinal(this.currentTab) == ConfigTab.PARAMETER_CONFIG) {
            return;
        }
        // 居中提示标签。addLabel(x,y,w,h,color,text) 返回 WidgetLabel，存到屏幕即可。
        int listY = 50;
        int listH = this.getBrowserHeight();
        int listX = 10;
        int listW = this.getScreenWidth() - listX - 10;
        int color = 0xFFAAAAAA;
        this.addLabel(listX, listY + listH / 2 - 4, listW, 12, color, "（此栏目待开发）");
    }

    /** 切换标签页：刷新列表控件。 */
    private void switchTab(int idx) {
        if (idx == this.currentTab) return;
        this.currentTab = idx;
        this.clearOptions();
        this.reCreateListWidget();
        this.initGui(); // 重新构建顶部按钮 + 提示标签
    }

    /** 若安装了 malilib：注册配置句柄 + 配置屏注册表（使 ICPM 出现在 malilib 的 mod 配置列表）。 */
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
