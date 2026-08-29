package name.icpm.common;

/**
 * 物品品质等级枚举（基于ICPM R196）
 *
 * 品质等级影响物品的耐久度修正：
 * - wretched (粗糙): 0.5x 耐久修正
 * - poor (劣质): 0.75x 耐久修正
 * - average (普通): 1.0x 耐久修正（默认）
 * - fine (精良): 1.5x 耐久修正
 * - excellent (优秀): 2.0x 耐久修正
 * - superb (史诗): 2.5x 耐久修正
 * - masterwork (杰作): 3.0x 耐久修正
 * - legendary (传说): 3.5x 耐久修正
 */
public enum EnumQuality {
    WRETCHED("wretched", "粗糙", 0.5f, 0),
    POOR("poor", "劣质", 0.75f, 1),
    AVERAGE("average", "普通", 1.0f, 2),
    FINE("fine", "精良", 1.5f, 3),
    EXCELLENT("excellent", "优秀", 2.0f, 4),
    SUPERB("superb", "卓越", 2.5f, 5),
    MASTERWORK("masterwork", "大师", 3.0f, 6),
    LEGENDARY("legendary", "传说", 3.5f, 7);

    private final String name;
    private final String descriptor;
    private final float durabilityModifier;
    private final int ordinal;

    EnumQuality(String name, String descriptor, float durabilityModifier, int ordinal) {
        this.name = name;
        this.descriptor = descriptor;
        this.durabilityModifier = durabilityModifier;
        this.ordinal = ordinal;
    }

    /**
     * 获取品质名称（序列化用，英文小写）
     */
    public String getName() {
        return name;
    }

    /**
     * 获取品质中文描述符（tooltip 展示用，对齐 R196 EnumQuality.getDescriptor）
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * 获取耐久度修正系数
     */
    public float getDurabilityModifier() {
        return durabilityModifier;
    }

    /**
     * 获取品质等级（用于合成时间计算）
     * quality_adjusted_difficulty = difficulty * 2^(quality.ordinal - average.ordinal)
     */
    public int getQualityLevel() {
        return ordinal;
    }

    /**
     * 获取下一个品质等级
     * @return 下一个品质等级，如果已经是最高级则返回null
     */
    public EnumQuality next() {
        int nextOrdinal = ordinal + 1;
        for (EnumQuality quality : values()) {
            if (quality.ordinal == nextOrdinal) {
                return quality;
            }
        }
        return null;
    }

    /**
     * 获取上一个品质等级
     * @return 上一个品质等级，如果已经是最低级则返回null
     */
    public EnumQuality previous() {
        int prevOrdinal = ordinal - 1;
        for (EnumQuality quality : values()) {
            if (quality.ordinal == prevOrdinal) {
                return quality;
            }
        }
        return null;
    }

    /**
     * 根据名称获取品质
     */
    public static EnumQuality fromName(String name) {
        for (EnumQuality quality : values()) {
            if (quality.name.equals(name)) {
                return quality;
            }
        }
        return AVERAGE; // 默认返回普通品质
    }

    /**
     * 根据ordinal获取品质
     */
    public static EnumQuality fromOrdinal(int ordinal) {
        for (EnumQuality quality : values()) {
            if (quality.ordinal == ordinal) {
                return quality;
            }
        }
        return AVERAGE; // 默认返回普通品质
    }
}