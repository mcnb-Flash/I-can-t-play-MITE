package name.icpm.client.config;

import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import name.icpm.common.ICPMConfig;

/**
 * ICPM malilib 配置句柄（client，仅当安装了 malilib 时由 ICPMClient 调用）。
 *
 * 提供一个布尔选项 enableCreativeMode，值变更时经 ICPMConfig 写盘 config/icpm.json，
 * 服务端创造模式限制立即生效（与 /icpmconfig 命令同源）。
 */
public class ICPMMaLiLibConfig implements IConfigHandler {

    public static final String MOD_ID = "icpm";

    public static final ConfigBoolean ENABLE_CREATIVE = new ConfigBoolean(
            "enableCreativeMode",
            ICPMConfig.isCreativeEnabled(),
            "true = 允许玩家变更为创造模式；false = 禁止（默认）");

    private static ICPMMaLiLibConfig instance;

    private ICPMMaLiLibConfig() {
    }

    public static ICPMMaLiLibConfig getInstance() {
        if (instance == null) {
            instance = new ICPMMaLiLibConfig();
        }
        return instance;
    }

    @Override
    public void load() {
        ICPMConfig.load();
        ENABLE_CREATIVE.setBooleanValue(ICPMConfig.isCreativeEnabled());
    }

    @Override
    public void save() {
        // 值已通过 onConfigsChanged 写盘；此处兜底同步一次
        ICPMConfig.setCreativeEnabled(ENABLE_CREATIVE.getBooleanValue());
    }

    @Override
    public void onConfigsChanged() {
        // malilib 界面里改动选项 → 立即写入 icpm.json 并热生效
        ICPMConfig.setCreativeEnabled(ENABLE_CREATIVE.getBooleanValue());
    }
}
