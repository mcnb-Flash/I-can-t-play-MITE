package name.icpm.common;

/**
 * ICPM 附魔经验值消耗辅助类
 *
 * R196 Enchantment.getExperienceCost(enchantment_levels) = enchantment_levels * 100。
 * 附魔不再检查/消耗【经验等级】，而是直接检查/消耗【经验值】。
 */
public final class ICPMEnchantmentHelper {

    private ICPMEnchantmentHelper() {}

    /**
     * 附魔经验值消耗（R196: 附魔等级 × 100）
     */
    public static int experienceCost(int enchantmentLevels) {
        return enchantmentLevels * 100;
    }
}
