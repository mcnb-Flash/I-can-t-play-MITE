package name.icpm.mixin;

import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import name.icpm.ICPM;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

/**
 * 诅咒——LivingEntity 层被动：
 * <ul>
 *   <li>{@code setSprinting}：无法奔跑（R196 EntityPlayerSP/PlayerControllerMP 禁疾跑）；</li>
 *   <li>{@code removeAllEffects}：诅咒豁免——牛奶/死亡等清除全部效果时保留 witch_curse
 *       （R196 语义：仅杀施咒女巫 / 去咒药水可解除，牛奶不是解咒捷径）。</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class CurseEntityMixin {

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void icpm$blockSprint(boolean sprinting, CallbackInfo ci) {
        if (!sprinting) {
            return;
        }
        if ((Object) this instanceof ServerPlayer player
                && ICPMCurseManager.isCursed(player, ICPMCurse.CANNOT_RUN, true)) {
            ci.cancel();
        }
    }

    @Inject(method = "removeAllEffects", at = @At("HEAD"), cancellable = true)
    private void icpm$curseImmuneToPurge(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        MobEffectInstance curse = self.getEffect(ICPM.WITCH_CURSE_HOLDER);
        if (curse == null) {
            return; // 未中咒走原版
        }
        boolean removedAny = false;
        for (MobEffectInstance inst : new ArrayList<>(self.getActiveEffects())) {
            if (inst.getEffect().equals(ICPM.WITCH_CURSE_HOLDER)) {
                continue;
            }
            if (self.removeEffect(inst.getEffect())) {
                removedAny = true;
            }
        }
        cir.setReturnValue(removedAny);
        cir.cancel();
    }
}
