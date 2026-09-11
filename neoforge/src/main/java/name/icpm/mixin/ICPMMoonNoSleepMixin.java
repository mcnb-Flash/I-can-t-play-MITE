package name.icpm.mixin;

import name.icpm.common.ICPMMoonPhase;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 血月：夜晚无法入睡（R196 百科：整夜不能睡觉）。
 */
@Mixin(ServerPlayer.class)
public abstract class ICPMMoonNoSleepMixin {

    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void icpm$bloodMoonNoSleep(BlockPos blockPos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (ICPMMoonPhase.isBloodMoonNight(player.level())) {
            player.displayClientMessage(Component.translatable("message.icpm.blood_moon_no_sleep"), true);
            cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
        }
    }
}
