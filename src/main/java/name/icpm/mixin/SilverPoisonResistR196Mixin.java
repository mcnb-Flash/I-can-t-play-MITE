package name.icpm.mixin;

import name.icpm.item.ICPMSilverArmor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 银甲抗毒（R196 类型化减伤 · 毒部分）——
 * R196 EntityLivingBase：施加毒效果时 duration ×= (1 − getResistanceToPoison())，
 * 而 getResistanceToPoison() = getSilverArmorCoverage() × 0.5。
 * 银覆盖率 ≈ 各部位权重和（头0.2/胸0.4/腿0.3/靴0.1，全套=1.0）。
 *
 * R196 同族抗性：Drain/Shadow 同为 coverage×0.5（Wight 吸经验、Shadow/Nightwing 致盲），
 * ICPM 暂无对应伤害源，故仅落地毒时长抗性；麻痹抗性在 R196 来自「自由行动」附魔而非银甲。
 */
@Mixin(LivingEntity.class)
public abstract class SilverPoisonResistR196Mixin {

    /** 单部位覆盖率权重（近似 R196 coverage=部件材料占比/24，全套=1） */
    private static final float[] SLOT_WEIGHTS = {0.2f, 0.4f, 0.3f, 0.1f}; // HEAD CHEST LEGS FEET
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void icpm$silverPoisonDuration(MobEffectInstance instance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel)) {
            return; // 仅服务端做权威时长缩短
        }
        if (instance == null || !instance.getEffect().is(MobEffects.POISON)) {
            return;
        }
        float coverage = silverCoverage(self);
        if (coverage <= 0.0f) {
            return;
        }
        float resist = Math.min(coverage * 0.5f, 0.99f);
        int oldDuration = instance.getDuration();
        int newDuration = Math.max(1, Math.round(oldDuration * (1.0f - resist)));
        if (newDuration >= oldDuration) {
            return; // 已缩放或无变化 → 放行原版
        }
        MobEffectInstance scaled = new MobEffectInstance(instance.getEffect(), newDuration,
                instance.getAmplifier(), instance.isAmbient(), instance.isVisible(), instance.showIcon());
        // 用缩放后的实例重入 addEffect（第二次进入时 newDuration==oldDuration 即放行，无死循环）
        cir.setReturnValue(self.addEffect(scaled, source));
        cir.cancel();
    }

    /** 统计银甲覆盖率：穿戴银盔甲部位权重和 */
    @Unique
    private static float silverCoverage(LivingEntity self) {
        float coverage = 0.0f;
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = self.getItemBySlot(SLOTS[i]);
            if (!stack.isEmpty() && ICPMSilverArmor.isSilverArmor(stack.getItem())) {
                coverage += SLOT_WEIGHTS[i];
            }
        }
        return Math.min(coverage, 1.0f);
    }
}
