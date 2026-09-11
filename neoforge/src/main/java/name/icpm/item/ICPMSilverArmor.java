package name.icpm.item;

import name.icpm.ICPM;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/**
 * 银制盔甲检测工具
 *
 * 通过检查 Item 引用判断是否为银盔甲，
 * 避免 Mixin 中重复列举每个盔甲物品。
 */
public final class ICPMSilverArmor {

    private ICPMSilverArmor() {}

    /**
     * 给定的 Item 是否为银制盔甲（头盔/胸甲/护腿/靴子）
     */
    public static boolean isSilverArmor(Item item) {
        return item == ICPMItems.SILVER_HELMET
            || item == ICPMItems.SILVER_CHESTPLATE
            || item == ICPMItems.SILVER_LEGGINGS
            || item == ICPMItems.SILVER_BOOTS;
    }

    /**
     * 给定的 Item 是否为银制工具（剑/镐/斧/锹/锄/匕首等所有 SILVER_* 工具）
     */
    public static boolean isSilverTool(Item item) {
        return item == ICPMItems.SILVER_SWORD
            || item == ICPMItems.SILVER_PICKAXE
            || item == ICPMItems.SILVER_AXE
            || item == ICPMItems.SILVER_SHOVEL
            || item == ICPMItems.SILVER_HOE
            || item == ICPMItems.SILVER_HATCHET
            || item == ICPMItems.SILVER_DAGGER
            || item == ICPMItems.SILVER_WAR_HAMMER
            || item == ICPMItems.SILVER_BATTLE_AXE
            || item == ICPMItems.SILVER_SCYTHE
            || item == ICPMItems.SILVER_MATTOCK;
    }
}
