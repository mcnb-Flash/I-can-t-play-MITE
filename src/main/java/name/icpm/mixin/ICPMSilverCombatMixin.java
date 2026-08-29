package name.icpm.mixin;

import name.icpm.item.ICPMSilverArmor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * ICPM 银制工具 / 银质盔甲特殊效果 Mixin
 *
 * 1. 银制工具对亡灵生物有 +2.5 亡灵杀手 I 的额外伤害（ICPM 强制附加效果）
 * 2. 银质盔甲对亡灵生物攻击有 20% 减伤
 *
 * 通过修改 LivingEntity.hurt 的 amount 参数实现，无需修改附魔系统，
 * 也不会影响其他附魔效果（玩家仍可自由附魔）。
 */
@Mixin(LivingEntity.class)
public class ICPMSilverCombatMixin {

    /**
     * 亡灵杀手 I 提供的额外伤害（与原版 1.21.x Smite I 一致：每级 +2.5 伤害）
     */
    @Unique
    private static final float SICPM_I_DAMAGE_BONUS = 2.5f;

    /**
     * 银盔甲单件减伤比例
     */
    @Unique
    private static final float SILVER_ARMOR_REDUCTION_PER_PIECE = 0.05f; // 5% / 件，4件 = 20%

    /**
     * 在 hurtServer 内部调用 actuallyHurt 的时机修改最终伤害（index 2）。
     *
     * 关键：@ModifyArgs 只能拦截方法体内部的「方法调用(INVOKE)」，不能修改方法自身参数。
     * 因此必须把 target 指向 hurtServer 内部的 actuallyHurt(...) 调用，
     * 而不是 [错误地] 写成 @ModifyArgs(method="hurtServer")（会把目标解析成 hurtServer 自身，
     * 报 "targetting a non-method insn"）。
     * hurtServer(ServerLevel, DamageSource, float) → actuallyHurt(ServerLevel, DamageSource, float)
     * hurtServer 内 actuallyHurt 有两处调用（冷却分支 / 普通分支），都会命中本拦截，效果一致。
     * args [0]=level [1]=source [2]=amount
     */
    @ModifyArgs(method = "hurtServer", expect = -1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void icpm$modifyHurtAmount(Args args) {
        LivingEntity self = (LivingEntity) (Object) this;
        // hurtServer 是服务器端专属方法，无需客户端判断
        DamageSource source = (DamageSource) args.get(1);
        if (source == null) {
            return;
        }
        float amount = (float) args.get(2);

        float modified = amount;

        // 1) 银制工具对亡灵生物的强制 Smite I 伤害加成
        modified = icpm$applySilverSmiteBonus(modified, source, self);

        // 2) 银质盔甲对亡灵生物的 20% 减伤
        modified = icpm$applySilverArmorReduction(modified, source, self);

        args.set(2, modified);
    }

    /**
     * 当攻击者使用银制工具攻击亡灵生物时，附加 +2.5 伤害（Smite I 等价效果）
     */
    @Unique
    private float icpm$applySilverSmiteBonus(float amount, DamageSource source, LivingEntity victim) {
        // 攻击者必须是 LivingEntity
        var attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return amount;
        }

        // 攻击者主手必须是银制工具
        ItemStack heldItem = attacker.getMainHandItem();
        if (heldItem.isEmpty()) {
            return amount;
        }
        Item held = heldItem.getItem();
        if (!isSilverTool(held)) {
            return amount;
        }

        // 目标必须是亡灵生物
        if (!isUndead(victim)) {
            return amount;
        }

        return amount + SICPM_I_DAMAGE_BONUS;
    }

    /**
     * 当玩家穿戴银质盔甲且被亡灵生物攻击时，按件数减免 20% 伤害（4件=20%）
     */
    @Unique
    private float icpm$applySilverArmorReduction(float amount, DamageSource source, LivingEntity victim) {
        // 只对玩家生效
        if (!(victim instanceof Player player)) {
            return amount;
        }

        // 攻击者必须是亡灵生物
        var attackerEntity = source.getEntity();
        if (attackerEntity == null) {
            return amount;
        }
        // 攻击者本身是亡灵生物，或攻击来源（如箭）来自亡灵生物
        boolean attackerIsUndead = false;
        if (attackerEntity instanceof LivingEntity leAttacker) {
            if (isUndead(leAttacker)) {
                attackerIsUndead = true;
            }
        } else if (source.getDirectEntity() instanceof LivingEntity directAttacker) {
            if (isUndead(directAttacker)) {
                attackerIsUndead = true;
            }
        }
        if (!attackerIsUndead) {
            return amount;
        }

        int silverPieces = countSilverArmorPieces(player);
        if (silverPieces <= 0) {
            return amount;
        }

        float reduction = amount * SILVER_ARMOR_REDUCTION_PER_PIECE * silverPieces;
        return Math.max(0.0f, amount - reduction);
    }

    /**
     * 判断实体是否为亡灵生物
     * 使用原版 EntityTypeTags.UNDEAD 标签
     */
    @Unique
    private boolean isUndead(LivingEntity entity) {
        return entity.getType().is(EntityTypeTags.UNDEAD);
    }

    /**
     * 判断物品是否为银制工具
     */
    @Unique
    private boolean isSilverTool(Item item) {
        return ICPMSilverArmor.isSilverTool(item);
    }

    /**
     * 统计玩家身上的银质盔甲件数（最多 4 件）
     */
    @Unique
    private int countSilverArmorPieces(Player player) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            if (ICPMSilverArmor.isSilverArmor(stack.getItem())) {
                count++;
            }
        }
        return count;
    }
}
