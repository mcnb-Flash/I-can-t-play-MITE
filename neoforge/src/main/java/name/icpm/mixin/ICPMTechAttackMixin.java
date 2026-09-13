package name.icpm.mixin;

import name.icpm.common.ICPMFoodStats;
import name.icpm.common.ICPMWorldEvil;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 近战攻击结算出口：R196 三项校正。
 *
 * 全部落在 vanilla/ICPM 最终结算点
 * `Player.attack` → `Entity.hurtOrSimulate(伤害源, 伤害量)`：
 *
 * <ol>
 *   <li><b>R196 工具攻击伤害校准（P2-9）</b>：R196 最终近战伤害 =
 *       玩家基础 1.0 + `ItemTool.getCombinedDamageVsEntity()`
 *       (= `getBaseDamageVsEntity()` + `Material.getDamageVsEntity()`)。
 *       ICPM 自注册工具已在注册参数上校准到 R196 基准值；**原版工具**
 *       （copper / iron / gold / diamond 镐斧锹锄剑）由 vanilla 属性修饰决定，
 *       无法改注册参数，只能在出口补偿差值 `R196 目标 − 当前 ATTACK_DAMAGE`。
 *       已校准的 ICPM 工具 delta 恒为 0，天然幂等。</li>
 *   <li><b>技术不佳</b>：ICPM 自创机制，近战伤害 ×(1 − 0.25×档)。</li>
 *   <li><b>弱击（canOnlyPerformWeakStrike）</b>：R196 `EntityPlayer.java:2653-2656`
 *       + `EntityLivingBase.java:684-693`——未持工具（`preventsHandDamage()`
 *       = ItemTool ∪ stick ∪ bone）且（生命 &lt; 2 ‖ 无食物能量 ‖ 攻击属性 &lt; 1）时，
 *       近战只击退、不造成伤害。</li>
 * </ol>
 *
 * 之所以在出口处改写而不改属性：部分 mod 会自行重算属性、属性修饰会被绕过；
 * 且出口处才能拿到含附魔/暴击/药水在内的最终伤害量。
 */
@Mixin(Player.class)
public abstract class ICPMTechAttackMixin {

    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean icpm$reduceMeleeDamage(Entity target, DamageSource source, float amount) {
        Player self = (Player) (Object) this;

        // ③ R196 弱击：只击退、不造成伤害
        if (amount > 0.0f && icpm$canOnlyPerformWeakStrike(self)) {
            icpm$applyWeakStrikeKnockback(target, self);
            return true;
        }

        if (amount <= 0.0f) {
            return target.hurtOrSimulate(source, amount);
        }

        // ① R196 工具攻击伤害校准
        float adjusted = amount + icpm$r196AttackDelta(self);

        // ② ICPM 自创：技术不佳
        float factor = ICPMWorldEvil.penaltyFactor();
        if (factor > 0.0f && factor < 1.0f) {
            adjusted *= factor;
        }
        return target.hurtOrSimulate(source, adjusted);
    }

    /**
     * R196 目标攻击修饰 − 1.21 当前攻击修饰。仅对手持工具成立，其余返回 0。
     */
    @Unique
    private float icpm$r196AttackDelta(Player self) {
        ItemStack stack = self.getMainHandItem();
        if (stack.isEmpty()) {
            return 0.0f;
        }
        if (ICPMToolProperties.getToolCategory(stack) == null) {
            return 0.0f;
        }
        float target = ICPMToolProperties.getR196AttackDamageModifier(stack);
        float current = ICPMToolProperties.getCurrentAttackDamageModifier(stack);
        return target - current;
    }

    /**
     * R196 `EntityPlayer.canOnlyPerformWeakStrike()`：
     * `!isHoldingItemThatPreventsHandDamage() && (health < 2 || !hasFoodEnergy() || attackDamage < 1)`。
     * `preventsHandDamage()` = `Item instanceof ItemTool || stick || bone`。
     */
    @Unique
    private boolean icpm$canOnlyPerformWeakStrike(Player self) {
        ItemStack held = self.getMainHandItem();
        if (!held.isEmpty() && icpm$preventsHandDamage(held)) {
            return false;
        }
        if (self.getHealth() < 2.0f) {
            return true;
        }
        // R196 hasFoodEnergy() = satiation + nutrition != 0
        if (ICPMFoodStats.getSatiation(self) + ICPMFoodStats.getNutrition(self) == 0) {
            return true;
        }
        return self.getAttributeValue(Attributes.ATTACK_DAMAGE) < 1.0;
    }

    /** R196 `Item.preventsHandDamage()`：ItemTool 系 ∪ 木棍 ∪ 骨头。 */
    @Unique
    private boolean icpm$preventsHandDamage(ItemStack stack) {
        if (stack.is(Items.STICK) || stack.is(Items.BONE)) {
            return true;
        }
        return ICPMToolProperties.getToolCategory(stack) != null;
    }

    /**
     * R196 `EntityLivingBase.knockBack(attacker, 0.6f)` 的近似：
     * 水平方向沿「攻击者 → 受击者」、强度 0.4×0.6，竖直 +0.4×0.6（封顶 0.4），并先把既有动量减半。
     * 说明：不重放 R196 的击退抗性随机判定与 0.8 缩放，属近似。
     */
    @Unique
    private void icpm$applyWeakStrikeKnockback(Entity target, Player self) {
        double dx = self.getX() - target.getX();
        double dz = self.getZ() - target.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) {
            dx = (self.getRandom().nextDouble() - self.getRandom().nextDouble()) * 0.01;
            dz = (self.getRandom().nextDouble() - self.getRandom().nextDouble()) * 0.01;
            len = Math.sqrt(dx * dx + dz * dz);
        }
        double strength = 0.4 * 0.6;
        Vec3 dm = target.getDeltaMovement();
        target.setDeltaMovement(
                dm.x / 2.0 - dx / len * strength,
                Math.min(dm.y / 2.0 + strength, 0.4),
                dm.z / 2.0 - dz / len * strength);
        target.hurtMarked = true;
    }
}
