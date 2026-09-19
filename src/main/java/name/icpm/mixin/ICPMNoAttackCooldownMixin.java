package name.icpm.mixin;

import name.icpm.common.ICPMConfig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.6.4「无攻击速度（无攻击冷却）」—— 可配置开关（config/icpm.json: {@code noAttackCooldown}，默认 true）。
 *
 * <p><b>判定源（R196）</b>：1.6.4 完全没有攻击冷却机制。对判决源
 * {@code EntityPlayer.java} / {@code EntityLivingBase.java} 整文件检索
 * {@code attackStrength} / {@code attackCooldown} / {@code attackTime} / {@code getAttackStrengthScale}
 * 全部 0 匹配（仅存手臂挥动动画 {@code swingProgress}）。玩家挥击只受「挥臂动画 + 目标受击无敌帧」限制，
 * 每一次点击都按满伤害结算。攻击冷却与充能伤害缩放是 <b>1.9 才引入</b>的机制。
 *
 * <p><b>1.21.11 真实链路（已反汇编核实，非猜测）</b>：
 * <pre>
 *   Player.attack(Entity):
 *     ATTACK_DAMAGE -> dmg
 *     getAttackStrengthScale(0.5f) -> s        // ← 本 mixin 拦截点（充能 0~1）
 *     getEnchantedDamage(target, s, src)
 *     baseDamageScaleFactor()                  // 由 s 派生的伤害缩放（旧版 0.2 + s²·0.8 的位置）
 *     ldc 0.9; fcmpl                           // "充能是否满" → 决定暴击/横扫资格
 *   Player.getAttackStrengthScale(float):
 *     clamp((attackStrengthTicker + adjustTicks) / getCurrentItemAttackStrengthDelay(), 0, 1)
 *   Player.getCurrentItemAttackStrengthDelay():
 *     (1.0 / ATTACK_SPEED) * 20
 * </pre>
 *
 * <p><b>实现取舍</b>：直接让 {@code getAttackStrengthScale} 恒返回 {@code 1.0F}（满充能），
 * 而不是去改 {@code ATTACK_SPEED} 属性——把攻速调到极大并不能得到 1.0
 * （ticker 刚被重置时 {@code (0 + 0.5) / delay} 仍 &lt; 1，且 delay→0 会除零/得 NaN）。
 * 返回 1.0 对任何形式的缩放公式（{@code 0.2 + 0.8·g(s)} 且 g(1)=1，或线性式）都恰好等于「满充能伤害」；
 * 同时客户端准星攻击指示器也恒为满格（1.6.4 准星本就没有攻击冷却指示），
 * 且"按住左键"的连续攻击不再被充能门控打断（1.6.4 同样是按住即连续挥击）。
 *
 * <p>关掉开关（{@code noAttackCooldown=false}）时本注入不生效，完全恢复现代版攻击冷却。
 */
@Mixin(Player.class)
public abstract class ICPMNoAttackCooldownMixin {

    /** 满充能：恒返回 1.0f（等价于"每次挥击都等满了再打"）。 */
    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void icpm$noAttackCooldown(float adjustTicks, CallbackInfoReturnable<Float> cir) {
        if (ICPMConfig.isNoAttackCooldownEnabled()) {
            cir.setReturnValue(1.0F);
        }
    }
}
