package name.icpm.mixin;

import name.icpm.item.ICPMItems;
import name.icpm.common.ICPMTension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 怪物随机穿戴盔甲 Mixin
 *
 * 复刻 R196 EntityLiving.addRandomArmor() 逻辑：
 * - 基于难度和随机因子决定装备等级（0-4）
 * - 从脚到头逐槽位装备，每层有概率中断
 * - 等级越高材质越好：铁链→铜链→银→远古金属→秘银
 *
 * 注入时机：Mob.populateDefaultEquipmentSlots 末尾（原版装备已穿好）
 * 仅填充空的装备槽，不覆盖原版已装备的物品
 */
@Mixin(Mob.class)
public class MobRandomIcpmArmorMixin {

    /**
     * R196 护甲等阶对照表（适配 1.21.11 可用材质）
     *
     * 等级 0: 铁锁链甲（最低级）
     * 等级 1: 铜锁链甲
     * 等级 2: 银板甲
     * 等级 3: 远古金属板甲
     * 等级 4: 秘银板甲（最高级）
     */
    private static Item icpm$getArmorForSlot(int slot, int level) {
        switch (slot) {
            case 4: // 头盔
                switch (level) {
                    case 0: return ICPMItems.IRON_CHAINMAIL_HELMET;
                    case 1: return ICPMItems.COPPER_CHAINMAIL_HELMET;
                    case 2: return ICPMItems.SILVER_HELMET;
                    case 3: return ICPMItems.ANCIENT_METAL_HELMET;
                    case 4: return ICPMItems.MITHRIL_HELMET;
                }
                break;
            case 3: // 胸甲
                switch (level) {
                    case 0: return ICPMItems.IRON_CHAINMAIL_CHESTPLATE;
                    case 1: return ICPMItems.COPPER_CHAINMAIL_CHESTPLATE;
                    case 2: return ICPMItems.SILVER_CHESTPLATE;
                    case 3: return ICPMItems.ANCIENT_METAL_CHESTPLATE;
                    case 4: return ICPMItems.MITHRIL_CHESTPLATE;
                }
                break;
            case 2: // 护腿
                switch (level) {
                    case 0: return ICPMItems.IRON_CHAINMAIL_LEGGINGS;
                    case 1: return ICPMItems.COPPER_CHAINMAIL_LEGGINGS;
                    case 2: return ICPMItems.SILVER_LEGGINGS;
                    case 3: return ICPMItems.ANCIENT_METAL_LEGGINGS;
                    case 4: return ICPMItems.MITHRIL_LEGGINGS;
                }
                break;
            case 1: // 靴子
                switch (level) {
                    case 0: return ICPMItems.IRON_CHAINMAIL_BOOTS;
                    case 1: return ICPMItems.COPPER_CHAINMAIL_BOOTS;
                    case 2: return ICPMItems.SILVER_BOOTS;
                    case 3: return ICPMItems.ANCIENT_METAL_BOOTS;
                    case 4: return ICPMItems.MITHRIL_BOOTS;
                }
                break;
        }
        return null;
    }

    /**
     * 在原版装备填充后追加 ICPM 盔甲
     *
     * R196 逻辑：
     * - 基础概率 0.15 × 紧张度因子（1.21.11 简化为基础概率随难度提升）
     * - 起始等级 0-1，每次 9.5% 概率提升（最高 4）
     * - 从脚(slot=1)到头(slot=4)遍历，每层有概率中断
     * - 仅填充空槽位
     */
    @Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
    private void icpm$addRandomIcpmArmor(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        // 张力（Tension）难度体系：装备基础概率 = 15% × 张力。
        // 张力已内含难度系数（困难×1 / 其余×0.75）与月相因子；
        // 新区块张力≈0 → 不穿甲，随玩家停留与月相升高而提升。
        Level lvl = self.level();
        float tension = 0.0f;
        if (lvl instanceof ServerLevel sl) {
            tension = ICPMTension.getTension(sl, self.blockPosition());
        }
        float baseChance = 0.15f * tension;

        if (random.nextFloat() >= baseChance) {
            return;
        }

        // 计算护甲等阶（R196 逻辑：起始 0-1，每次 9.5% 概率升级，最高 4）
        int armorLevel = random.nextInt(2);
        if (random.nextFloat() < 0.095f) armorLevel++;
        if (random.nextFloat() < 0.095f) armorLevel++;
        if (random.nextFloat() < 0.095f) armorLevel++;
        armorLevel = Math.min(armorLevel, 4);

        // 中断概率（困难模式更低 = 更完整的套装）
        float breakChance = difficulty.isHard() ? 0.1f : 0.25f;

        // 从脚到头遍历（slot 1=靴子 → 4=头盔）
        EquipmentSlot[] slots = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS,
            EquipmentSlot.CHEST, EquipmentSlot.HEAD
        };

        for (int i = 0; i < slots.length; i++) {
            EquipmentSlot eqSlot = slots[i];
            int slotNum = i + 1; // 1-4 对应 getArmorForSlot

            // 已有装备则跳过
            if (!self.getItemBySlot(eqSlot).isEmpty()) {
                continue;
            }

            // 非首个槽位有概率中断（不再装备更高级位）
            if (i > 0 && random.nextFloat() < breakChance) {
                break;
            }

            Item armorItem = icpm$getArmorForSlot(slotNum, armorLevel);
            if (armorItem != null) {
                self.setItemSlot(eqSlot, new ItemStack(armorItem));
            }
        }

        // 武器附魔概率吃张力（G2，忠实 R196 EntityLiving.enchantEquipment）：
        //   概率 = 10% × 张力；附魔等级 = 5 + 张力 × rand(18)
        icpm$enchantMainHandByTension(random, self, tension);
    }

    /**
     * G2：怪物主手武器附魔概率吃张力（R196 EntityLiving.enchantEquipment 忠实移植）。
     *
     * <p>R196 原文（EntityLiving.java 第 1040-1043 行）：
     * <pre>
     *   float tension = worldObj.getLocationTensionFactor(posX, posY, posZ);
     *   if (rand.nextFloat() &lt; 0.10f * tension) {
     *       EnchantmentHelper.addRandomEnchantment(rand, item_stack,
     *           (int)(5.0f + tension * rand.nextInt(18)));
     *   }
     * </pre>
     * 1.21.11 对应 {@code EnchantmentHelper.enchantItem(RandomSource, ItemStack, int, Stream<ResourceKey<Enchantment>>)}
     * （附魔台风格随机附魔，等级换算由方法内部处理）。
     */
    private static void icpm$enchantMainHandByTension(RandomSource random, Mob self, float tension) {
        if (tension <= 0.0f) {
            return;
        }
        if (random.nextFloat() >= 0.10f * tension) {
            return;
        }
        ItemStack weapon = self.getMainHandItem();
        if (weapon.isEmpty()) {
            return;
        }
        int level = 5 + (int) (tension * random.nextInt(18));
        try {
            // 1.21.11：RegistryAccess.registryOrThrow 已改名 lookupOrThrow；enchantItem 的允许集为
            // Stream<Holder<Enchantment>>，由 reg.stream()（全部附魔元素）映射为 holder 流
            Registry<Enchantment> reg = self.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ItemStack enchanted = EnchantmentHelper.enchantItem(random, weapon, level, reg.stream().map(reg::wrapAsHolder));
            self.setItemSlot(EquipmentSlot.MAINHAND, enchanted);
        } catch (Exception ignored) {
            // 附魔失败（如物品不可附魔/注册表异常）不中断怪物生成
        }
    }
}
