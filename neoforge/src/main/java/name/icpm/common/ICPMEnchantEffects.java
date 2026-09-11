package name.icpm.common;

import name.icpm.ICPM;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * ICPM 附魔等级查询辅助（1.21.11 附魔为数据驱动注册，运行时从数据包动态注册表获取）。
 *
 * <p>重要：1.21.x 数据驱动附魔体系下，物品上的附魔以 {@code Holder<Enchantment>} 形式存储于
 * {@code DataComponents.ENCHANTMENTS}。若用
 * {@code EnchantmentHelper.getItemEnchantmentLevel(holder(registryAccess), stack)} 查询，
 * 该 {@code holder} 取自 {@code level.registryAccess()}，而物品存储的附魔 {@code Holder} 来自另一
 * 份 registry 视图时，二者 {@code equals}/{@code is} 比较可能失败，导致查询恒返回 0 —— 表现为
 * "附魔有名无实、所有效果静默失效"。
 *
 * <p>因此这里改用 {@link EnchantmentHelper#getEnchantmentsForCrafting} 直接遍历物品自身的附魔列表，
 * 并按 {@link ResourceLocation} 精确匹配 icpm 命名空间下的附魔，完全规避 registry/Holder 一致性问题。
 */
public final class ICPMEnchantEffects {

    /** 单格物品的附魔等级（稳健查询：遍历物品自身附魔，按 ResourceLocation 匹配） */
    public static int level(Level level, ItemStack stack, String path) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        var enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var entry : enchants.entrySet()) {
            var key = entry.getKey().unwrapKey().orElse(null);
            if (key != null && key.identifier().equals(ICPM.id(path))) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    /** 全身装备中该附魔的最大等级（R196 getMaxEnchantmentLevel） */
    public static int armorLevel(LivingEntity entity, String path) {
        int max = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                int lvl = level(entity.level(), entity.getItemBySlot(slot), path);
                if (lvl > max) {
                    max = lvl;
                }
            }
        }
        return max;
    }

    /** R196 getEnchantmentLevelFraction：附魔等级分数（level/10） */
    public static float fraction(int level) {
        return level / 10.0f;
    }

    private ICPMEnchantEffects() {
    }
}
