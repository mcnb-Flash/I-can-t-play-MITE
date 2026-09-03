package name.icpm.mixin;

import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * R196 女巫诅咒施放与死亡移除。
 *
 * <p>R196（EntityAITarget 通用目标确认 + EntityWitch）：
 * <ul>
 *   <li>施咒：女巫锁定玩家目标时 1/4 概率 cursePlayer → addCurse(player, witch, randomCurse,
 *       6000 tick 延迟)。玩家已咒或有 pending 时拒绝（防叠加）。</li>
 *   <li>死亡：EntityWitch.onDeath → removeCursesForWitch(witch)：其施加的诅咒全部撤销。</li>
 * </ul>
 *
 * <p>1.21.11 无 EntityAITarget 女巫分支，取女巫远程攻击锚点 {@link Witch#performRangedAttack}
 * （每轮投掷药水即一次"攻击意图"，1/4 概率施咒，语义等价且行为可见）。
 */
@Mixin(Witch.class)
public abstract class WitchCurseMixin {

    @Inject(method = "performRangedAttack", at = @At("HEAD"))
    private void icpm$curseOnAttack(LivingEntity target, float power, CallbackInfo ci) {
        if (!(target instanceof ServerPlayer player)) {
            return;
        }
        Witch witch = (Witch) (Object) this;
        if (witch.getRandom().nextInt(4) != 0) {
            return; // R196 1/4 概率
        }
        ICPMCurseManager.curse(player, witch, ICPMCurse.getRandom(witch.getRandom()),
                ICPMCurseManager.CURSE_DELAY_TICKS);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void icpm$removeCursesOnDeath(DamageSource source, CallbackInfo ci) {
        // R196 onDeath → removeCursesForWitch：该女巫施放的全部诅咒（含 pending）撤销
        ICPMCurseManager.removeForWitch((Witch) (Object) this);
        // 清理召狼倒计时/记录
        name.icpm.curse.WitchSummonManager.onWitchRemoved((Witch) (Object) this);
    }
}
