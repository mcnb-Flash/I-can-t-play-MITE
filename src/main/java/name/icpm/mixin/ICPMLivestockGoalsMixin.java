package name.icpm.mixin;

import name.icpm.entity.LivestockState;
import name.icpm.entity.ai.ICPMSeekFoodIfHungry;
import name.icpm.entity.ai.ICPMSeekWaterIfThirsty;
import name.icpm.entity.ai.ICPMSeekOpenSpaceIfCrowded;
import name.icpm.entity.ai.ICPMSeekShelterFromRain;
import name.icpm.entity.ai.ICPMGetOutOfWater;
import name.icpm.entity.ai.ICPMFleeWhenSpooked;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MITE 牲畜 AI 注册 —— 挂在 Mob 上。
 *
 * registerGoals() 声明于 Mob，而 Cow/Pig/Sheep/Chicken 都不重写它，因此不能在
 * 多目标 {@link ICPMLivestockMixin}（目标为那四个具体类）里按 method_5959 注入
 * （目标类无该方法 → 启动崩溃）。这里改挂到声明类 Mob（单目标），用 isLivestock
 * 守卫，仅对 Cow/Pig/Sheep/Chicken 真正添加六个生理/受惊 Goal。
 *
 * 用官方方法名 "registerGoals" + 默认 remap=true（单目标无多目标冲突，AP 可正常
 * 解析并生成 refmap 条目）。goalSelector 是 Mob 的 protected 字段，mixin 不能
 * extends 目标自身，故用 @Shadow 暴露。
 */
@Mixin(Mob.class)
public abstract class ICPMLivestockGoalsMixin {

    @Shadow protected GoalSelector goalSelector;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void icpm$registerGoals(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!LivestockState.isLivestock(self)) {
            return;
        }
        PathfinderMob mob = (PathfinderMob) (Object) this;
        // 躲避捕食者（仅对怪物生效，与"受攻击"无关）：高优先级，遇怪即逃
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(
            mob, LivingEntity.class, 8.0F, 1.0, 1.2, entity -> entity instanceof Monster));
        // R196 受惊传染驱动：被同伴惊吓、或直接被攻击（spook 已标记）的动物，
        // 在此目标下随机四散奔逃。优先级 2——低于避怪(1)，高于生理需求目标(5-6)。
        this.goalSelector.addGoal(2, new ICPMFleeWhenSpooked(mob, 1.5));
        // 生理需求目标放到低优先级，平时动物保持安静。
        this.goalSelector.addGoal(5, new ICPMSeekFoodIfHungry(mob, 1.0, true));
        this.goalSelector.addGoal(5, new ICPMSeekWaterIfThirsty(mob, 1.0, false));
        this.goalSelector.addGoal(5, new ICPMSeekShelterFromRain(mob, 1.0, true));
        this.goalSelector.addGoal(6, new ICPMSeekOpenSpaceIfCrowded(mob, 1.0));
        this.goalSelector.addGoal(6, new ICPMGetOutOfWater(mob, 1.0));
    }
}
