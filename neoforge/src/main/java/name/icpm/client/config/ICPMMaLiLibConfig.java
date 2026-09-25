package name.icpm.client.config;

import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import name.icpm.common.ICPMConfig;

/**
 * ICPM 配置句柄（client；NeoForge 端经 MaFgLib——malilib 的 (Neo)Forge 非官方移植，
 * 包名同为 fi.dy.masa.malilib——由 ICPMClientNeoForge 调用）。
 *
 * 提供 ICPM 全部可调项，值变更时经 ICPMConfig 写盘 config/icpm.json 立即生效：
 * - 上天眷顾（增益/便利）：noAttackCooldown（1.6.4 无攻击速度）
 * - 世界恶意（世界威胁）：witchWhisper（女巫低吟）、nightmareEra（噩梦时代）、
 *   poorTechnique（技术不佳，0~4 档）、weakStrike（弱击开关）
 *
 * 注意：**没有创造/作弊开关**——它由 JVM 参数 {@code -Dicpm.devMode=true} 控制，
 * 不提供任何游戏内或配置文件的开启途径，因此本类也不暴露该选项。
 */
public class ICPMMaLiLibConfig implements IConfigHandler {

    public static final String MOD_ID = "icpm";

    /** 1.6.4 无攻击速度（无攻击冷却）：近战不必等待武器充能，每次挥击都是满伤害。 */
    public static final ConfigBoolean NO_ATTACK_COOLDOWN = new ConfigBoolean(
            "noAttackCooldown",
            ICPMConfig.isNoAttackCooldownEnabled(),
            "true = 无攻击速度（1.6.4）：近战无视攻击冷却，每次挥击满伤害；false = 保留现代版攻击冷却（连点伤害衰减）");

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

    /** 弱击：R196 原版机制。true = 启用（未持工具时，生命<2/食物为0/攻击属性<1 之一成立则只击退不伤害）；false = 关闭（空手也能正常造成伤害）。 */
    public static final ConfigBoolean WEAK_STRIKE = new ConfigBoolean(
            "weakStrike",
            ICPMConfig.isWeakStrikeEnabled(),
            "true = 启用 R196 弱击（未持工具时，生命<2 或 饱食+营养=0 或 攻击属性<1 时近战只击退、不造成伤害）；false = 关闭弱击");

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
        NO_ATTACK_COOLDOWN.setBooleanValue(ICPMConfig.isNoAttackCooldownEnabled());
        WITCH_WHISPER.setBooleanValue(ICPMConfig.isWitchWhisperEnabled());
        NIGHTMARE_ERA.setBooleanValue(ICPMConfig.isNightmareEnabled());
        POOR_TECHNIQUE.setIntegerValue(ICPMConfig.poorTechniqueLevel());
        WEAK_STRIKE.setBooleanValue(ICPMConfig.isWeakStrikeEnabled());
    }

    @Override
    public void save() {
        // 值已通过 onConfigsChanged 写盘；此处兜底同步一次
        ICPMConfig.setNoAttackCooldown(NO_ATTACK_COOLDOWN.getBooleanValue());
        ICPMConfig.setWitchWhisper(WITCH_WHISPER.getBooleanValue());
        ICPMConfig.setNightmareEra(NIGHTMARE_ERA.getBooleanValue());
        ICPMConfig.setPoorTechnique(POOR_TECHNIQUE.getIntegerValue());
        ICPMConfig.setWeakStrike(WEAK_STRIKE.getBooleanValue());
    }

    @Override
    public void onConfigsChanged() {
        // MaFgLib 界面里改动选项 → 立即写入 icpm.json 并热生效
        ICPMConfig.setNoAttackCooldown(NO_ATTACK_COOLDOWN.getBooleanValue());
        ICPMConfig.setWitchWhisper(WITCH_WHISPER.getBooleanValue());
        ICPMConfig.setNightmareEra(NIGHTMARE_ERA.getBooleanValue());
        ICPMConfig.setPoorTechnique(POOR_TECHNIQUE.getIntegerValue());
        ICPMConfig.setWeakStrike(WEAK_STRIKE.getBooleanValue());
    }
}
