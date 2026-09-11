package name.icpm.mixin;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 马「喂食逆反」—— EntityHorse.onEntityRightClicked 移植（sky）：
 * <pre>
 *   野马（未驯服）在“健康喂食”后进入 4000 tick 逆反窗口：此期间再喂任何食物，
 *   马后腿直立拒绝（不消耗食物）；喂食若用于治疗受伤野马则不会触发逆反窗口。
 * </pre>
 * 1.21.11 原版已内置 temper/开食（vanilla mobInteract → fedFood → handleEating），
 * 这里只补 R196 差值：健康野马喂食冷却 4000 + 逆反期拒绝进食。
 *
 * fedFood(Player,ItemStack) 声明于 AbstractHorse（Horse 未覆写，实测反汇编确认）
 * —— 按铁律 @Mixin 用声明类 AbstractHorse；马/驴/骡同为马科语义一致。
 * 注：逆反窗口存于字段（不持久化），与 R196 倒计时字段同语义（重载后重置）。
 */
@Mixin(AbstractHorse.class)
public abstract class HorseFeedR196Mixin {

    /** 下次允许喂食的 tick 截止（野马逆反窗口） */
    @Unique
    private int icpmRebelliousUntil = 0;

    @Inject(method = "fedFood", at = @At("HEAD"), cancellable = true)
    private void icpm$rebelliousRefuseFeed(Player player, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse self = (AbstractHorse) (Object) this;
        if (self.isTamed() || self.level().isClientSide()) {
            return;
        }
        // 逆反窗口内再喂 → 拒绝（后腿直立怒声 + 烟雾），不消耗食物（R196 makeHorseRearWithSound）
        if (self.tickCount < icpmRebelliousUntil) {
            if (self.level() instanceof ServerLevel serverLevel) {
                var pos = self.blockPosition();
                serverLevel.playSound(null, pos, SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.0f, 0.9f);
                var p = self.position();
                serverLevel.sendParticles(ParticleTypes.SMOKE, p.x, p.y + 1.0, p.z, 6, 0.25, 0.3, 0.25, 0.02);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }
    }

    @Inject(method = "fedFood", at = @At("RETURN"))
    private void icpm$wildFeedCooldown(Player player, ItemStack stack, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse self = (AbstractHorse) (Object) this;
        if (self.isTamed() || self.level().isClientSide()) {
            return;
        }
        InteractionResult result = cir.getReturnValue();
        // R196：野马接受喂食后，若喂后仍/已满血（即本次非“治疗中”喂食）→ 进入 4000 tick 逆反
        if (result != null && result.consumesAction() && self.getHealth() >= self.getMaxHealth()) {
            icpmRebelliousUntil = self.tickCount + 4000;
        }
    }
}
