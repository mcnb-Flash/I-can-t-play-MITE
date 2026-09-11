package name.icpm.mixin;

import com.mojang.datafixers.util.Either;
import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 诅咒：无法入眠 —— R196 EntityClientPlayerMP（入睡中断）语义移植。
 * 被诅咒玩家点击床直接失败（OTHER_PROBLEM），无法进入睡眠。
 */
@Mixin(ServerPlayer.class)
public abstract class CurseSleepMixin {

    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void icpm$blockSleep(BlockPos pos, CallbackInfoReturnable<Either> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (ICPMCurseManager.isCursed(player, ICPMCurse.CANNOT_SLEEP, true)) {
            cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
        }
    }
}
