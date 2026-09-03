package name.icpm.curse;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * ICPM 女巫诅咒类型 —— R196 Curse.java 忠实移植。
 *
 * <p>R196 原文（src_deobf/.../Curse.java）：id 1..16 注册进 cursesList[64]，
 * 女巫死亡/击中施咒后按 id 判定生效。key 对应语言键 curse.&lt;key&gt;.name/.desc。
 *
 * <p>16 类：装备加速腐坏 / 无法屏息 / 无法奔跑 / 厌食动物 / 厌食植物 / 禁饮 /
 * 末影人敌对 / 笨拙 / 植物缠绕 / 无法穿戴盔甲 / 无法开启箱子 / 无法入眠 /
 * 蜘蛛/狼/苦力怕/亡灵之惧。
 */
public enum ICPMCurse {
    EQUIPMENT_DECAYS_FASTER(1, "equipmentDecay"),
    CANNOT_HOLD_BREATH(2, "cantHoldBreath"),
    CANNOT_RUN(3, "cantRun"),
    CANNOT_EAT_ANIMALS(4, "cantEatAnimals"),
    CANNOT_EAT_PLANTS(5, "cantEatPlants"),
    CANNOT_DRINK(6, "cantDrink"),
    ENDERMEN_AGGRO(7, "endermenEnemy"),
    CLUMSINESS(8, "clumsiness"),
    ENTANGLEMENT(9, "entanglement"),
    CANNOT_WEAR_ARMOR(10, "cantWearArmor"),
    CANNOT_OPEN_CHESTS(11, "cantOpenChests"),
    CANNOT_SLEEP(12, "cantSleep"),
    FEAR_OF_SPIDERS(13, "fearOfSpiders"),
    FEAR_OF_WOLVES(14, "fearOfWolves"),
    FEAR_OF_CREEPERS(15, "fearOfCreepers"),
    FEAR_OF_UNDEAD(16, "fearOfUndead");

    private static final List<ICPMCurse> ALL = List.of(values());

    private final int id;
    private final String key;

    ICPMCurse(int id, String key) {
        this.id = id;
        this.key = key;
    }

    public int id() {
        return id;
    }

    /** R196 key（用于语言键 curse.&lt;key&gt;.name / .desc） */
    public String key() {
        return key;
    }

    public String titleKey() {
        return "curse." + key + ".name";
    }

    public String descKey() {
        return "curse." + key + ".desc";
    }

    /** R196 Curse.getRandomCurse：从全部 16 类中随机取一。 */
    public static ICPMCurse getRandom(RandomSource random) {
        return ALL.get(random.nextInt(ALL.size()));
    }

    public static ICPMCurse fromId(int id) {
        for (ICPMCurse c : ALL) {
            if (c.id == id) {
                return c;
            }
        }
        return null;
    }

    /** 供 Manager 序列化前遍历使用。 */
    public static List<ICPMCurse> valuesList() {
        return new ArrayList<>(ALL);
    }
}
