package name.icpm.mixin;

import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 诅咒——恐惧系（R196 Entity 2497 / EntityArachnid / EntityCreeper / EntityWolf）：
 * 受诅咒玩家攻击亡灵 / 蜘蛛 / 苦力怕 / 狼时，对应生物 3/4 概率立即锁定玩家为目标
 * （不因诅咒而松懈追击）。映射为：命中即强制仇恨，等价"恐惧令它们咬住不放"。
 */
@Mixin(LivingEntity.class)
public abstract class CurseFearMixin {

    private static final TagKey<EntityType<?>> UNDEAD =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:undead"));

    private static ICPMCurse fearCurseFor(LivingEntity victim) {
        if (victim.getType().is(UNDEAD)) {
            return ICPMCurse.FEAR_OF_UNDEAD;
        }
        if (victim instanceof Spider) {
            return ICPMCurse.FEAR_OF_SPIDERS;
        }
        if (victim instanceof Wolf) {
            return ICPMCurse.FEAR_OF_WOLVES;
        }
        if (victim instanceof Creeper) {
            return ICPMCurse.FEAR_OF_CREEPERS;
        }
        return null;
    }

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void icpm$fearRetaliation(ServerLevel level, DamageSource source, float amount,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        ICPMCurse fear = fearCurseFor(victim);
        if (fear == null || victim.isDeadOrDying()) {
            return;
        }
        if (ICPMCurseManager.isCursed(player, fear, true)
                && victim instanceof Mob mob && victim.getRandom().nextInt(4) != 0) {
            mob.setTarget(player); // R196：3/4 概率咬住不放
        }
    }
}
