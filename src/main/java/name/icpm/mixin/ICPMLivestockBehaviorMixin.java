package name.icpm.mixin;

import name.icpm.entity.ICPMLivestock;
import name.icpm.entity.LivestockState;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MITE 牲畜行为逻辑 —— 集中挂载到 Animal（覆盖整条向上继承链：
 * Mob.tick / Animal.setInLove / AgeableMob.setAge / Animal.finalizeSpawnChildFromBreeding /
 * Entity.addAdditionalSaveData / Entity.readAdditionalSaveData）。
 *
 * 不能直接挂到 {@link ICPMLivestockMixin} 的多目标 {Cow,Pig,Sheep,Chicken} 上，
 * 因为上述方法声明于 Mob/Animal/Entity，不是那四个具体类的 mixin 目标，
 * 会触发 Mixin "target class not supported" 启动崩溃。且跨类状态已迁出到
 * {@link LivestockState} 静态表，避免"共享继承方法 + 子集字段"的运行时崩溃。
 *
 * 所有注入均以 isLivestock 守卫，仅对 Cow/Pig/Sheep/Chicken 真正生效。
 */
@Mixin(Animal.class)
public abstract class ICPMLivestockBehaviorMixin {

    // ===================== 交配门槛（Animal.setInLove） =====================
    // 注：生理 tick 注入在 ICPMLivestockTickMixin（@Mixin(LivingEntity.class)），
    // 因为 tick 声明于 LivingEntity/Entity，挂到 Animal 目标时 AP 无法生成 refmap 条目。

    @Inject(method = "setInLove", at = @At("HEAD"), cancellable = true)
    private void icpm$gateBreed(Player player, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Animal animal) || !LivestockState.isLivestock(animal)) {
            return;
        }
        if (!LivestockState.get(animal).isWell()) {
            ci.cancel();
        }
    }

    // ===================== 幼崽继承双亲健康值（Animal.finalizeSpawnChildFromBreeding） =====================
    // 注：成长门槛 setAge 注入在 ICPMLivestockGrowthMixin（@Mixin(AgeableMob.class)），
    // 因为 AgeableMob 有 setAge(int) 与 setAge(int,boolean) 两个重载，挂到 Animal 目标时
    // AP 无法消歧生成 refmap 条目（显式描述符又只查 Animal 自身不含父类），故挂到声明类。

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("TAIL"))
    private void icpm$adoptWellness(ServerLevel level, Animal parent, AgeableMob child, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Animal animal) || !LivestockState.isLivestock(animal)) {
            return;
        }
        if (!(child instanceof ICPMLivestock)) {
            return;
        }
        LivestockState me = LivestockState.get(animal);
        LivestockState other = LivestockState.get((Entity) (Object) parent);
        LivestockState kid = LivestockState.get((Entity) child);
        kid.food = Math.min(me.food, other.food);
        kid.water = Math.min(me.water, other.water);
        kid.freedom = Math.min(me.freedom, other.freedom);
    }

    // ===================== NBT 持久化（Entity.addAdditionalSaveData / readAdditionalSaveData） =====================

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void icpm$writeNbt(ValueOutput output, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!LivestockState.isLivestock(self)) {
            return;
        }
        LivestockState.writeNbt(LivestockState.get(self), output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void icpm$readNbt(ValueInput tag, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!LivestockState.isLivestock(self)) {
            return;
        }
        LivestockState.readNbt(LivestockState.get(self), tag);
    }
}
