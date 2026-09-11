package name.icpm.mixin;

import name.icpm.common.MobGoalSelectorAccess;
import name.icpm.entity.ai.ZombieBurnTreeGoal;
import name.icpm.entity.ai.ZombieDigGoal;
import name.icpm.entity.ai.ZombieSeekFoodGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 僵尸系：注册 MITE 专属 AI（挖墙 / 寻食生肉 / 烧树）。
 *
 * <p>挂 {@code registerGoals} 的声明类 {@link Mob}（铁律 2026-08-19：1.21.11 中 Zombie 将
 * registerGoals 拆为 addBehaviourGoals，Zombie 自身不重写 registerGoals，
 * @Mixin(Zombie)+@Inject(registerGoals) 运行期崩），用 instanceof 过滤。
 */
@Mixin(Mob.class)
public abstract class ZombieMiteGoalsMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void icpm$registerGoals(CallbackInfo ci) {
        if (!((Object) this instanceof Zombie self)) {
            return;
        }
        var gs = MobGoalSelectorAccess.get(self);
        if (gs == null) {
            return;
        }
        gs.addGoal(4, new ZombieDigGoal(self));
        gs.addGoal(6, new ZombieSeekFoodGoal(self));
        gs.addGoal(7, new ZombieBurnTreeGoal(self, 1.0));
    }
}
