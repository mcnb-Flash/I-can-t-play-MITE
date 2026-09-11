package name.icpm.mixin;

import name.icpm.curse.ICPMCurseManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 诅咒随玩家 NBT 持久化（与 PlayerMixin 的存档注入并列，同一方法多注入互不冲突）。
 * 读档恢复在线诅咒表；写档保存 pending/realized 状态。实现见 ICPMCurseManager。
 */
@Mixin(Player.class)
public abstract class CurseDataMixin {

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void icpm$writeCurseData(ValueOutput tag, CallbackInfo ci) {
        ICPMCurseManager.save((Player) (Object) this, tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void icpm$readCurseData(ValueInput tag, CallbackInfo ci) {
        ICPMCurseManager.load((Player) (Object) this, tag);
    }
}
