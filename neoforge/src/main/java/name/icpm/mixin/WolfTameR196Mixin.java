package name.icpm.mixin;

import name.icpm.common.ICPMMoonPhase;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 驯狼概率 —— R196 EntityWolf.getTamingOutcome 忠实移植：
 * <pre>
 *   roll = rand
 *   roll &lt; 0.05  → −1 直接失败并攻击玩家（蓝月夜不攻击）
 *   roll &lt; 0.10  → 0  无效果
 *   roll &gt; 0.90  → +1 直接成功
 *   其余：roll += rand × player.experienceLevel × 0.02 → &lt;0.2 失败攻击 / &lt;0.8 无效果 / ≥0.8 成功
 *   判定后 100 tick 冷却（期间喂骨仍消耗但无判定）
 * </pre>
 * 取代原版「骨头 1/3 驯服」。
 */
@Mixin(Wolf.class)
public class WolfTameR196Mixin {

    @Unique
    private int icpmTameCooldownTicks = 0;

    @Unique
    private static int r196TamingOutcome(Level level, Player player) {
        float roll = level.random.nextFloat();
        if (roll < 0.05f) {
            return -1;
        }
        if (roll < 0.1f) {
            return 0;
        }
        if (roll > 0.9f) {
            return 1;
        }
        roll += level.random.nextFloat() * (float) player.experienceLevel * 0.02f;
        return roll < 0.2f ? -1 : (roll < 0.8f ? 0 : 1);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void icpm$r196BoneTaming(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Wolf self = (Wolf) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        if (self.isTame() || self.isAngry() || !stack.is(Items.BONE)) {
            return; // 交给原版处理（染色/装甲/治疗等）
        }
        Level level = self.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
            return;
        }
        // 服务端权威判定
        boolean cooldownActive = self.tickCount < icpmTameCooldownTicks;
        int outcome = -2; // 未判定（冷却中不掷骰）
        if (!cooldownActive) {
            outcome = r196TamingOutcome(level, player);
            if (outcome <= 0) {
                icpmTameCooldownTicks = self.tickCount + 100;
            }
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (cooldownActive) {
            // 冷却期：骨被吃掉但无任何判定（无效果）
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
            return;
        }
        var p = self.position();
        if (outcome >= 1) {
            self.tame(player);
            self.setOrderedToSit(true);
            serverLevel.sendParticles(ParticleTypes.HEART, p.x, p.y + 0.8, p.z, 6, 0.25, 0.3, 0.25, 0.1);
        } else {
            serverLevel.sendParticles(ParticleTypes.SMOKE, p.x, p.y + 0.8, p.z, 5, 0.2, 0.3, 0.2, 0.05);
            if (outcome < 0 && !ICPMMoonPhase.isBlueMoonDay(level)) {
                self.setTarget(player);
            }
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
        cir.cancel();
    }
}
