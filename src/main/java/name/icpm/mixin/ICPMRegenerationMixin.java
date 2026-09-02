package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import name.icpm.common.ICPMHealProgressManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 再生附魔（R196）：回血速度 ×(1+0.5×级)，基础回血 64s/点 → 间隔 3840 tick/半心。
 *
 * ⚠️ 修复（2026-09-02）：此前直接 self.heal() 会被 DisableVanillaHealingMixin 拦截
 * （其对无"生命恢复"药水效果的玩家无条件 cancel）→ 再生附魔有名无实。
 * 必须经 ICPMHealProgressManager.healAuthorized 授权回血。
 */
@Mixin(LivingEntity.class)
public abstract class ICPMRegenerationMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void icpm$regeneration(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player) || self.level().isClientSide()) {
            return;
        }
        int lvl = ICPMEnchantEffects.armorLevel(self, "regeneration");
        if (lvl <= 0) {
            return;
        }
        int interval = (int) (3840 / (1.0f + 0.5f * lvl));
        if (interval < 20) {
            interval = 20;
        }
        if (self.tickCount % interval == 0 && self.getHealth() < self.getMaxHealth()) {
            ICPMHealProgressManager.healAuthorized(self, 1.0f);
        }
    }
}
