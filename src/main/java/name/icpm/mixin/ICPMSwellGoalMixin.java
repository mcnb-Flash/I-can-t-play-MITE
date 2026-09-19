package name.icpm.mixin;

import name.icpm.entity.monster.InfernalCreeperEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 苦力怕膨胀距离（R196 {@code EntityAICreeperSwell.java:67-68}）。
 *
 * <pre>
 * float health_fraction_for_swelling = clamp(getHealthFraction(), 0.4f, 1.0f);
 * float distance_limit_sq = (this instanceof EntityInfernalCreeper ? 36.0f : 16.0f) / health_fraction_for_swelling;
 * </pre>
 *
 * <p>即：距离上限**平方** = 普通苦力怕 **16** / 地狱苦力怕 **36**，再除以健康比（0.4～1.0）
 * —— 血越少，起爆距离越大（普通 16～40、地狱 36～90）。
 * 1.21.11 的 {@code SwellGoal.tick()} 里是固定 {@code distanceToSqr(target) &gt; 49.0}（= 7 格），
 * 本 mixin 把该常量替换为 R196 的动态值。
 */
@Mixin(SwellGoal.class)
public abstract class ICPMSwellGoalMixin {

    @Shadow
    @Final
    private Creeper creeper;

    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 49.0))
    private double icpm$r196SwellDistance(double original) {
        float healthFraction = Mth.clamp(this.creeper.getHealth() / this.creeper.getMaxHealth(), 0.4f, 1.0f);
        double base = (this.creeper instanceof InfernalCreeperEntity) ? 36.0 : 16.0;
        return base / healthFraction;
    }
}
