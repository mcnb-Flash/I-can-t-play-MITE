package name.icpm.mixin;

import name.icpm.common.ICPMMoonPhase;
import name.icpm.common.ICPMTension;
import name.icpm.entity.ICPMEntities;
import name.icpm.entity.ai.ZombieMiteState;
import name.icpm.entity.monster.GiantZombieEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 僵尸系：生成时按 MITE 规则设置聪明（1/8 天生）与首领（张力×0.05，maxHealth 加成）。
 *
 * <p>挂 {@code finalizeSpawn} 的声明类 {@link Mob}（铁律 2026-08-19：Zombie 自身不重写
 * finalizeSpawn，@Mixin(Zombie)+@Inject(finalizeSpawn) 运行期崩），用 instanceof 过滤。
 */
@Mixin(Mob.class)
public abstract class ZombieMiteSpawnMixin {

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void icpm$finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                    EntitySpawnReason reason, SpawnGroupData data,
                                    CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!((Object) this instanceof Zombie self)) {
            return;
        }
        if (self.level().isClientSide()) {
            return;
        }
        // 巨型僵尸自身：跳过本 mixin（避免聪明/首领加成与递归替换）
        if (self instanceof GiantZombieEntity) {
            return;
        }
        // ==================== 血月地表 1/200 替换为巨型僵尸 ====================
        // R196 巨型僵尸为 Mojang 废案，此处移植为「血月之夜地表自然刷新的普通僵尸」稀有替换体。
        // 仅对原版僵尸（getType()==EntityType.ZOMBIE）生效，reason==NATURAL（自然刷新），
        // 且生成点露天（canSeeSky）、血月当夜；1/200 概率生成巨型僵尸并移除原僵尸。
        if (self.getType() == EntityType.ZOMBIE
                && reason == EntitySpawnReason.NATURAL
                && ICPMMoonPhase.isBloodMoonNight(self.level())
                && self.level().canSeeSky(self.blockPosition())
                && self.getRandom().nextInt(200) == 0) {
            ServerLevel serverLevel = (ServerLevel) self.level();
            GiantZombieEntity giant = new GiantZombieEntity(ICPMEntities.INSTANCE.getGIANT_ZOMBIE(), serverLevel);
            giant.setPos(self.getX(), self.getY(), self.getZ());
            giant.setYRot(self.getYRot());
            giant.setXRot(self.getXRot());
            // 触发巨型僵尸自身的 finalizeSpawn（setBaby(false)/xpReward）；其内部不会再进入本替换块
            giant.finalizeSpawn(serverLevel, difficulty, reason, data);
            serverLevel.addFreshEntity(giant);
            self.discard();
            return;
        }
        ZombieMiteState.Entry entry = ZombieMiteState.get(self);
        // 聪明：1/8 天生概率（R196: rand.nextInt(8) == 0；Revenant 必聪明但 Revenant 不继承 Zombie，此处不处理）
        if (self.getRandom().nextInt(8) == 0) {
            entry.smart = true;
        }
        // 首领：随区块张力概率（R196: tension * 0.05）
        float tension = ICPMTension.getTension(self.level(), self);
        if (self.getRandom().nextFloat() < tension * 0.05f) {
            entry.leader = true;
            AttributeInstance maxHp = self.getAttribute(Attributes.MAX_HEALTH);
            if (maxHp != null) {
                // R196 首领额外最大生命 rand*3+1（用 ADD_VALUE 加法，避免 MULTIPLY 失衡）
                double bonus = self.getRandom().nextDouble() * 3.0 + 1.0;
                maxHp.addTransientModifier(new AttributeModifier(
                        Identifier.parse("icpm:zombie.leader"),
                        bonus, AttributeModifier.Operation.ADD_VALUE));
                self.setHealth(self.getMaxHealth());
            }
        }
    }
}
