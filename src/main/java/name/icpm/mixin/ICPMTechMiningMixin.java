package name.icpm.mixin;

import name.icpm.common.ICPMWorldEvil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 世界恶意「技术不佳」：挖掘速度 ×(1 - 0.25×档)。
 *
 * Player.getDestroySpeed 是各方（原版挖掘进度 / ICPMToolRulesMixin 等）获取玩家挖掘速度的唯一入口，
 * 在 RETURN 处统一缩放即可全局生效（含空手）。
 */
@Mixin(Player.class)
public abstract class ICPMTechMiningMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void icpm$slowMining(BlockState state, CallbackInfoReturnable<Float> cir) {
        float factor = ICPMWorldEvil.penaltyFactor();
        float v = cir.getReturnValueF();
        if (factor < 1.0f && v > 0.0f) {
            cir.setReturnValue(v * factor);
        }
    }
}
