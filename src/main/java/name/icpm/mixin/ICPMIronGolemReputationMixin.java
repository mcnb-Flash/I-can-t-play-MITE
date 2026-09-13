package name.icpm.mixin;

import name.icpm.common.ICPMVillagerReputation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * R196 村民声望效果：铁傀儡主动攻击"仇恨"玩家（忠实移植 `Village.func_82685_c`）。
 *
 * R196 源码：
 *  - {@code Village.java:218-229 func_82685_c(EntityLivingBase)}：遍历 playerReputation，
 *    返回距离给定实体最近的、{@code isPlayerReputationTooLow()}（声望 <= -15）的玩家；
 *  - {@code EntityAIDefendVillage.java:31}：铁傀儡防御 AI 用它选出攻击目标。
 *
 * 1.21.11 对应入口：{@code IronGolem.aiStep()}（每 tick 服务端执行）——节流每 16 tick
 * 检查一次：若当前无攻击目标，则在 16 格内寻找最近的声望 <= -15 的玩家并设为攻击目标。
 * 铁傀儡 = 原版生物 → mixin 注入。
 */
@Mixin(IronGolem.class)
public abstract class ICPMIronGolemReputationMixin {

    /** 索敌半径（R196 防御 AI 的村庄范围以内；取常见村庄半径量级）。 */
    private static final double RANGE = 16.0;

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void icpm$defendVillageFromHatedPlayer(CallbackInfo ci) {
        IronGolem self = (IronGolem) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        // 节流：每 16 tick 才做一次全玩家扫描
        if ((self.tickCount & 0b1111) != 0) {
            return;
        }
        // 已有存活目标时不覆盖（先处理当前威胁）
        LivingEntity current = self.getTarget();
        if (current != null && current.isAlive()) {
            return;
        }
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }

        Player target = null;
        double best = RANGE * RANGE;
        for (Player p : level.players()) {
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) {
                continue;
            }
            if (!ICPMVillagerReputation.isHostile(p)) {
                continue;
            }
            double d = p.distanceToSqr(self);
            if (d <= best) {
                best = d;
                target = p;
            }
        }
        if (target != null) {
            self.setTarget(target);
        }
    }
}
