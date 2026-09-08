package name.icpm.client.config;

import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import name.icpm.common.ICPMConfig;

/**
 * ICPM malilib 配置句柄（client，仅当安装了 malilib 时由 ICPMClient 调用）。
 *
 * 提供 ICPM 全部可调项，值变更时经 ICPMConfig 写盘 config/icpm.json 立即生效：
 * - 上天眷顾（增益/便利）：enableCreativeMode
 * - 世界恶意（世界威胁）：witchWhisper（女巫低吟）、nightmareEra（噩梦时代）、
 *   poorTechnique（技术不佳，0~4 档）
 */
public class ICPMMaLiLibConfig implements IConfigHandler {

    public static final String MOD_ID = "icpm";

    public static final ConfigBoolean ENABLE_CREATIVE = new ConfigBoolean(
            "enableCreativeMode",
            ICPMConfig.isCreativeEnabled(),
            "true = 允许玩家变更为创造模式；false = 禁止（默认）");

    /** 女巫低吟：玩家永久携带一枚随机女巫诅咒，不可被去咒药水解除或变更类型；女巫仍可叠加普通诅咒。 */
    public static final ConfigBoolean WITCH_WHISPER = new ConfigBoolean(
            "witchWhisper",
            ICPMConfig.isWitchWhisperEnabled(),
            "true = 玩家永久获得一枚随机女巫诅咒（去咒药水无法消除/变更），且女巫可再咒不覆盖旧诅咒");

    /** 噩梦时代：世界始终为夜晚，且每个游戏日 80% 为血月；天数照常推进。 */
    public static final ConfigBoolean NIGHTMARE_ERA = new ConfigBoolean(
            "nightmareEra",
            ICPMConfig.isNightmareEnabled(),
            "true = 时间始终锁定为夜晚，且每日 80% 概率为血月（天数正常推进）");

    /** 技术不佳档位（0 关 / 1~4 档）：每档 耐久消耗 +25%、攻击力 -25%、挖掘速度 -25%。 */
    public static final ConfigInteger POOR_TECHNIQUE = new ConfigInteger(
            "poorTechnique",
            ICPMConfig.poorTechniqueLevel(), 0, 4,
            "技术不佳档位：0=关；每档 耐久消耗+25%、攻击力-25%、挖掘速度-25%");

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
        WITCH_WHISPER.setBooleanValue(ICPMConfig.isWitchWhisperEnabled());
        NIGHTMARE_ERA.setBooleanValue(ICPMConfig.isNightmareEnabled());
        POOR_TECHNIQUE.setIntegerValue(ICPMConfig.poorTechniqueLevel());
    }

    @Override
    public void save() {
        // 值已通过 onConfigsChanged 写盘；此处兜底同步一次
        ICPMConfig.setCreativeEnabled(ENABLE_CREATIVE.getBooleanValue());
        ICPMConfig.setWitchWhisper(WITCH_WHISPER.getBooleanValue());
        ICPMConfig.setNightmareEra(NIGHTMARE_ERA.getBooleanValue());
        ICPMConfig.setPoorTechnique(POOR_TECHNIQUE.getIntegerValue());
    }

    @Override
    public void onConfigsChanged() {
        // malilib 界面里改动选项 → 立即写入 icpm.json 并热生效
        ICPMConfig.setCreativeEnabled(ENABLE_CREATIVE.getBooleanValue());
        ICPMConfig.setWitchWhisper(WITCH_WHISPER.getBooleanValue());
        ICPMConfig.setNightmareEra(NIGHTMARE_ERA.getBooleanValue());
        ICPMConfig.setPoorTechnique(POOR_TECHNIQUE.getIntegerValue());
    }
}
