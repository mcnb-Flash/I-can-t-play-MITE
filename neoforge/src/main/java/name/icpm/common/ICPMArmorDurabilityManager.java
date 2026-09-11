package name.icpm.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * ICPM 护甲耐久管理器
 * 
 * 护甲挨打扣除耐久逻辑：
 * 1. 每次受到有效伤害，先算一个"本次护甲要承担的耐久池"
 *    耐久池 = min(本次伤害值, 你的护甲防护值(不含保护附魔))，并且至少按 1 计算
 * 
 * 2. 这个耐久池会随机分到你身上的护甲件上
 *    只穿 1 件就全扣那件；穿多件就随机分摊
 * 
 * 3. 分到单件后再算实际掉多少耐久
 *    有耐久附魔会按"每一点独立判定"减免（I~V 约 15%/30%/45%/60%/75% 免扣）
 *    有"装备衰败更快"诅咒则翻倍
 * 
 * 4. 普通战斗里护甲通常会被保到 1 耐久不直接爆
 *    但对玩家来说，护甲到 1 耐久时这件的防护等于 0（穿着像"空壳"）
 * 
 * 5. 不可格挡/绝对伤害不走这套护甲耐久流程
 */
public class ICPMArmorDurabilityManager {

    /**
     * 计算护甲总耐久（平均品质）
     * 公式：部件数 × 材质耐久系数 × 2（锁甲不乘2）
     */
    public static int calculateArmorDurability(float materialDurability, boolean isChainmail) {
        int multiplier = isChainmail ? 1 : 2;
        return (int) (materialDurability * multiplier);
    }

    /**
     * 处理护甲耐久消耗
     * 
     * @param player 受伤的玩家
     * @param damage 受到的伤害值
     * @return 是否成功处理护甲耐久消耗
     */
    public static boolean handleArmorDurabilityDamage(Player player, float damage) {
        // 获取玩家穿戴的所有护甲
        List<EquipmentSlot> armorPieces = getWornArmorPieces(player);
        if (armorPieces.isEmpty()) {
            return false;
        }

        // 计算护甲防护值（不含保护附魔）
        float totalArmorValue = calculateTotalArmorValue(player);
        
        // 计算耐久池：min(伤害值, 护甲防护值)，至少为1
        int durabilityPool = Math.max(Math.min((int) damage, (int) totalArmorValue), 1);

        // 随机分摊耐久消耗到各个护甲件上
        distributeDurabilityDamage(player, armorPieces, durabilityPool);

        return true;
    }

    /**
     * 获取玩家穿戴的所有护甲
     */
    private static List<EquipmentSlot> getWornArmorPieces(Player player) {
        List<EquipmentSlot> armorSlots = new ArrayList<>();
        
        // 检查所有护甲槽位
        EquipmentSlot[] slots = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            // 使用正确的方法检查护甲
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                armorSlots.add(slot);
            }
        }

        return armorSlots;
    }

    /**
     * 计算玩家总护甲值（不含保护附魔）
     * getArmorValue() 已经返回所有装备的总护甲值，直接调用一次即可
     */
    private static float calculateTotalArmorValue(Player player) {
        return (float) player.getArmorValue();
    }

    /**
     * 随机分摊耐久消耗到各个护甲件上
     */
    private static void distributeDurabilityDamage(Player player, List<EquipmentSlot> armorPieces, int durabilityPool) {
        if (armorPieces.isEmpty() || durabilityPool <= 0) {
            return;
        }

        // 如果只穿1件护甲，全部耐久消耗由该件承担
        if (armorPieces.size() == 1) {
            applyDurabilityDamageToPiece(player, armorPieces.get(0), durabilityPool);
            return;
        }

        // 穿多件护甲时，随机分摊耐久消耗
        int remainingPool = durabilityPool;

        while (remainingPool > 0) {
            // 随机选择一件护甲
            EquipmentSlot randomSlot = armorPieces.get(player.getRandom().nextInt(armorPieces.size()));
            
            // 为该件护甲分配1点耐久消耗
            applyDurabilityDamageToPiece(player, randomSlot, 1);
            remainingPool--;
        }
    }

    /**
     * 对单件护甲应用耐久消耗
     * 
     * @param player 玩家
     * @param slot 护甲槽位
     * @param damage 耐久消耗值
     */
    private static void applyDurabilityDamageToPiece(Player player, EquipmentSlot slot, int damage) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return;
        }

        // 获取耐久附魔等级
        int unbreakingLevel = getUnbreakingLevel(stack);

        // 计算实际耐久消耗（考虑耐久附魔减免）
        int actualDamage = calculateActualDurabilityDamage(stack, damage, unbreakingLevel, player);

        // 应用耐久伤害，但不让护甲直接破碎（至少保留1点耐久）
        int currentDurability = stack.getDamageValue();
        int maxDurability = stack.getMaxDamage();

        if (currentDurability + actualDamage < maxDurability) {
            // 还有耐久空间，正常消耗
            stack.hurtAndBreak(actualDamage, player, slot);
        } else {
            // 耐久即将耗尽，保留至少1点
            stack.setDamageValue(maxDurability - 1);
        }
    }

    /**
     * 获取耐久附魔等级（使用正确的方法）
     */
    private static int getUnbreakingLevel(ItemStack stack) {
        var allEnchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var entry : allEnchants.entrySet()) {
            var key = entry.getKey();
            if (key.is(Enchantments.UNBREAKING)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    /**
     * 计算实际耐久消耗（考虑耐久附魔减免）
     * 
     * 有耐久附魔会按"每一点独立判定"减免（I~V 约 15%/30%/45%/60%/75% 免扣）
     */
    private static int calculateActualDurabilityDamage(ItemStack stack, int baseDamage, int unbreakingLevel, Player player) {
        if (unbreakingLevel <= 0) {
            return baseDamage;
        }

        // 计算免损概率：每级15%，最高75%
        float reductionChance = Math.min(unbreakingLevel * 0.15f, 0.75f);

        // 对每一点耐久消耗独立判定
        int actualDamage = 0;
        for (int i = 0; i < baseDamage; i++) {
            // 随机判定是否减免
            if (player.getRandom().nextFloat() >= reductionChance) {
                actualDamage++;
            }
        }

        return Math.max(actualDamage, 1); // 至少消耗1点
    }

    /**
     * 计算护甲防护衰减
     * 
     * 护甲保护在耐久低于 50% 后才开始衰减
     * 玩家穿着时到"仅剩 1 点耐久"时防护按 0 处理
     */
    public static float calculateArmorProtectionReduction(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return 1.0f;
        }

        int currentDurability = stack.getDamageValue();
        int maxDurability = stack.getMaxDamage();

        // 如果耐久只剩1点，防护为0
        if (currentDurability >= maxDurability - 1) {
            return 0.0f;
        }

        // 计算耐久百分比
        float durabilityPercentage = 1.0f - ((float) currentDurability / maxDurability);

        // 耐久高于50%时，防护不衰减
        if (durabilityPercentage >= 0.5f) {
            return 1.0f;
        }

        // 耐久低于50%时，防护线性衰减
        return durabilityPercentage * 2.0f;
    }
}