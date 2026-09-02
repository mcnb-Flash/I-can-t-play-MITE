package name.icpm.mixin;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 盔甲值替换（R196 ItemArmor / Damage.applyTargetDefenseModifiers 忠实移植）。
 *
 * <p>MITE 护甲体系（线性减法，非原版百分比曲线）：
 * <pre>
 *   totalProtection = Σ(部件数 × 材质保护 ÷ 24 × 耐久损伤因子) + 附魔保护
 *   reduced = max(amount - totalProtection, 1)
 * </pre>
 * <ul>
 *   <li>材质保护（R196 ItemArmor.getMaterialProtection）：leather=2、铜/银=7、金=6、
 *       铁/远古金属=8、秘银=9、精金=10；锁甲 −2（铁锁甲=6、铜锁甲=5、金锁甲=4）；
 *       钻石（MITE 无此材质）视作铁=8、下界合金视作 10。</li>
 *   <li>部件数（R196 ItemHelmet/Cuirass/Leggings/Boots）：头盔=5、胸甲=8、护腿=7、靴子=4
 *       （与 ICPMItems 的 *_COMPONENTS 常量一致）。</li>
 *   <li>耐久损伤因子（R196 getDamageFactor）：非玩家=0.5；最后 1 点耐久=0；
 *       否则 clamp(2 − 损伤/最大×2, 0, 1)。</li>
 *   <li>附魔保护：1.21.11 的 {@code EnchantmentHelper.getDamageProtection(ServerLevel, LivingEntity, DamageSource)}。</li>
 *   <li>绕过护甲（R196 bypassesMundaneArmor）：DamageTypeTags.BYPASSES_ARMOR 不削减。</li>
 * </ul>
 *
 * <p>注入点 {@code LivingEntity.getDamageAfterArmorAbsorb(DamageSource, float)}（1.21.11 伤害链
 * 护甲减伤入口，refmap 实证 method_6132）。HEAD cancel 完全接管；药水减伤由原版
 * getDamageAfterMagicAbsorb 继续处理（不重复）。ICPMCombatEnchantMixin 的穿刺注入在
 * RETURN（其后执行），无冲突。
 */
@Mixin(LivingEntity.class)
public abstract class ICPMArmorValueMixin {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @Inject(method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F",
            at = @At("HEAD"), cancellable = true)
    private void icpm$miteArmorAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        // R196 applyTargetDefenseModifiers：bypassesMundaneArmor 的伤害无视护甲
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }
        float armorProt = icpm$armorProtection(self);
        // 穿刺附魔（R196，镐/战斧）：每级穿透 20% 的【护甲】减伤；附魔保护不被穿透。
        float protection = armorProt * icpm$pierceFactor(source) + icpm$enchantProtection(self, source);
        float reduced = Math.max(amount - protection, 1.0f);
        cir.setReturnValue(reduced);
        cir.cancel();
    }

    /**
     * R196 ItemArmor.getTotalArmorProtection（仅护甲部分）。
     * 注：穿刺只能穿透护甲部分，故与附魔保护分开计算。
     */
    @Unique
    private static float icpm$armorProtection(LivingEntity self) {
        float total = 0.0f;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = self.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int components = icpm$components(slot);
            int matProt = icpm$materialProtection(stack.getItem());
            if (matProt <= 0) {
                continue;
            }
            float multiplied = components * matProt / 24.0f;
            multiplied *= icpm$damageFactor(stack, self);
            total += multiplied;
        }
        return total;
    }

    /** 附魔保护（1.21.11 EnchantmentHelper.getDamageProtection，仅服务端有值） */
    @Unique
    private static float icpm$enchantProtection(LivingEntity self, DamageSource source) {
        if (self.level() instanceof ServerLevel sl) {
            return EnchantmentHelper.getDamageProtection(sl, self, source);
        }
        return 0.0f;
    }

    /**
     * 穿刺因子：攻击者主手持 icpm:piercing 附魔武器时返回 (1 − min(1, 级×0.2))，
     * 否则 1.0（不穿透）。R196：piercing = levelFraction*5 护甲点，本实现按
     * "每级穿透 20% 护甲减伤"折算（与旧 icpm$piercing 语义一致）。
     */
    @Unique
    private static float icpm$pierceFactor(DamageSource source) {
        net.minecraft.world.entity.Entity attacker = source.getDirectEntity();
        if (!(attacker instanceof net.minecraft.world.entity.player.Player player)) {
            return 1.0f;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) {
            return 1.0f;
        }
        int lvl = name.icpm.common.ICPMEnchantEffects.level(player.level(), weapon, "piercing");
        if (lvl <= 0) {
            return 1.0f;
        }
        return Math.max(0.0f, 1.0f - Math.min(1.0f, lvl * 0.2f));
    }

    /** R196 ItemHelmet=5 / ItemCuirass=8 / ItemLeggings=7 / ItemBoots=4 */
    @Unique
    private static int icpm$components(EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            return 5;
        }
        if (slot == EquipmentSlot.CHEST) {
            return 8;
        }
        if (slot == EquipmentSlot.LEGS) {
            return 7;
        }
        if (slot == EquipmentSlot.FEET) {
            return 4;
        }
        return 0;
    }

    /**
     * R196 ItemArmor.getMaterialProtection，按物品 id 前缀判定材质（ICPM 与原版盔甲统一覆盖；
     * 锁甲 −2）。⚠️ 不能引 ICPMItems 静态字段（类加载时序铁律），用注册表 id 字符串匹配。
     */
    @Unique
    private static int icpm$materialProtection(Item item) {
        if (item == null) {
            return 0;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return 0;
        }
        String p = id.getPath();
        if (p.startsWith("leather_")) {
            return 2;
        }
        if (p.startsWith("chainmail_")) {
            return 6; // 原版铁锁甲：8 − 2
        }
        if (p.startsWith("iron_chainmail_")) {
            return 6; // ICPM 铁锁甲
        }
        if (p.startsWith("copper_chainmail_")) {
            return 5; // 7 − 2
        }
        if (p.startsWith("gold_chainmail_")) {
            return 4; // 6 − 2
        }
        if (p.startsWith("silver_")) {
            return 7;
        }
        if (p.startsWith("copper_")) {
            return 7;
        }
        if (p.startsWith("ancient_metal_")) {
            return 8;
        }
        if (p.startsWith("mithril_")) {
            return 9;
        }
        if (p.startsWith("adamantium_")) {
            return 10;
        }
        if (p.startsWith("iron_")) {
            return 8;
        }
        if (p.startsWith("gold_")) {
            return 6;
        }
        if (p.startsWith("diamond_")) {
            return 8; // MITE 无钻石材质，视作铁
        }
        if (p.startsWith("netherite_")) {
            return 10; // MITE 无下界合金，视作顶级
        }
        return 0;
    }

    /** R196 ItemArmor.getDamageFactor：耐久损伤因子 */
    @Unique
    private static float icpm$damageFactor(ItemStack stack, LivingEntity owner) {
        if (owner != null && !(owner instanceof Player)) {
            return 0.5f;
        }
        int maxD = stack.getMaxDamage();
        if (maxD > 1 && stack.getDamageValue() >= maxD - 1) {
            return 0.0f; // 最后 1 点耐久：失去保护
        }
        float f = 2.0f - (float) stack.getDamageValue() / (float) maxD * 2.0f;
        if (f > 1.0f) {
            f = 1.0f;
        }
        return f;
    }
}
