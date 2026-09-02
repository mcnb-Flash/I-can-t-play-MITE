package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import name.icpm.common.ICPMExperience;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 近战附魔（R196 EntityPlayer.attackEntityAsMob 移植）：
 * 击晕(战锤/棍棒)、吸血(剑/镰刀)、缴械(剑) —— 注入 LivingEntity.actuallyHurt；
 * 穿刺(镐/战斧，穿透护甲) —— 由 ICPMArmorValueMixin 在护甲结算内实现（本 mixin 原在
 * getDamageAfterArmorAbsorb RETURN 注入，但该方法被 ICPMArmorValueMixin HEAD-cancel 短路，
 * RETURN 注入永不触发 → 穿刺有名无实，已迁移合并进护甲计算）。
 */
@Mixin(LivingEntity.class)
public abstract class ICPMCombatEnchantMixin {

    /** 近战命中结算时：击晕 / 吸血 / 缴械 */
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void icpm$meleeEffects(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfo ci) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) {
            return;
        }
        Entity direct = damageSource.getDirectEntity();
        if (!(direct instanceof Player player)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();

        // 击晕：概率 level/10，给目标缓慢 level*5 级，持续 level*50 tick（R196）
        int stun = ICPMEnchantEffects.level(player.level(), weapon, "stun");
        if (stun > 0 && target.level().random.nextFloat() < stun / 10.0f) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stun * 50, stun * 5));
        }

        // 吸血：概率 level/10，治疗 伤害*0.5*random（≥1）（R196 getVampiricTransfer）
        // ⚠️ 修复（2026-09-02）：必须经 healAuthorized，否则被 DisableVanillaHealingMixin 无条件
        // cancel（玩家无"生命恢复"药水效果时）→ 吸血附魔有名无实。
        int vamp = ICPMEnchantEffects.level(player.level(), weapon, "vampiric");
        if (vamp > 0 && f > 0.0f && target.isAlive() && target.level().random.nextFloat() < vamp / 10.0f) {
            int transfer = (int) (f * 0.5f * target.level().random.nextFloat());
            if (transfer < 1) {
                transfer = 1;
            }
            name.icpm.common.ICPMHealProgressManager.healAuthorized(player, transfer);
        }

        // 缴械：概率 level/10，打落目标手持武器（R196 EntityPlayer.disarming）
        int disarm = ICPMEnchantEffects.level(player.level(), weapon, "disarming");
        if (disarm > 0 && target.level().random.nextFloat() < disarm / 10.0f) {
            ItemStack held = target.getMainHandItem();
            if (!held.isEmpty()) {
                target.spawnAtLocation(serverLevel, held.copy());
                target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }
    }

    /**
     * R196 近战等级伤害加成：在最终伤害结算后，乘以 (1 + getLevelModifier(level, MELEE_DAMAGE))。
     * - 正等级：level*0.005（与负等级区分对待）
     * - 负等级 / 非近战：level*0.02（惩罚）
     * 与原版 actuallyHurt 内伤害计算链末端一致（R196 在伤害乘完各种系数后再乘等级修正）。
     * 仅在攻击者为人形玩家时生效，避免影响环境伤害 / 生物互殴。
     */
    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void icpm$levelMeleeDamage(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfo ci) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.level().isClientSide()) {
            return;
        }
        Entity direct = damageSource.getDirectEntity();
        if (!(direct instanceof Player player)) {
            return;
        }
        int level = ICPMExperience.getExperienceLevel(ICPMExperience.getExperience(player));
        // 仅当存在非零修正时才改动（level==0 时修正为 0，乘 1 无变化）
        if (level == 0) {
            return;
        }
        float modifier = ICPMExperience.getLevelModifier(level, ICPMExperience.LevelBonus.MELEE_DAMAGE);
        float cur = target.getHealth();
        // 等级修正作用在"本次伤害额 f"上（R196: 伤害 *= 1+getLevelModifier）：
        // 正等级 → after = cur - f*正 → 额外增伤；负等级 → after = cur - f*负 → 伤害降低（不治疗）。
        // 注意：绝不能用 cur*modifier，否则负等级时 cur - cur*(负) = cur + |...|，反而把目标治满血。
        float after = cur - f * modifier;
        if (after < 0.0f) {
            after = 0.0f;
        }
        target.setHealth(after);
    }
}
