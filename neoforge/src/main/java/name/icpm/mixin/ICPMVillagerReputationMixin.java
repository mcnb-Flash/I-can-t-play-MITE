package name.icpm.mixin;

import name.icpm.common.ICPMVillagerReputation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * R196 村民声望：受击 / 死亡变更（忠实移植 `EntityVillager.java`）。
 *
 * <ul>
 *   <li>{@code EntityVillager.java:254-270 setRevengeTarget} —— 被玩家攻击：声望 -1；攻击者为小孩时 -3。</li>
 *   <li>{@code EntityVillager.java:272-288 onDeath} —— 被玩家杀死：声望 -2。</li>
 * </ul>
 *
 * 1.21.11 对应入口：{@code Villager.setLastHurtByMob}（每次受伤由 LivingEntity 调用，
 * 与 R196 setRevengeTarget 触发时机一致）、{@code Villager.die}。村民=原版生物 → mixin 注入。
 */
@Mixin(Villager.class)
public abstract class ICPMVillagerReputationMixin {

    @Inject(method = "setLastHurtByMob", at = @At("TAIL"))
    private void icpm$reputationOnHurt(LivingEntity attacker, CallbackInfo ci) {
        if (!(attacker instanceof Player player)) {
            return;
        }
        Villager self = (Villager) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        // R196：受击 -1；攻击小孩 -3
        ICPMVillagerReputation.add(player, self.isBaby() ? -3 : -1);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void icpm$reputationOnDeath(DamageSource source, CallbackInfo ci) {
        if (!(source.getEntity() instanceof Player player)) {
            return;
        }
        Villager self = (Villager) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        // R196：被玩家杀死 -2
        ICPMVillagerReputation.add(player, -2);
    }
}
