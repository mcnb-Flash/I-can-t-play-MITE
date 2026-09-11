package name.icpm.entity.monster

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/**
 * 地狱犬（R196 Hellhound）：燃烧的猎犬形怪物。
 * 火焰免疫、跳跃扑击、命中点燃目标。
 */
class HellhoundEntity(type: EntityType<out HellhoundEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.FOLLOW_RANGE, 24.0)
    }

    override fun registerGoals() {
        this.goalSelector.addGoal(1, FloatGoal(this))
        this.goalSelector.addGoal(2, LeapAtTargetGoal(this, 0.4f))
        this.goalSelector.addGoal(3, HellhoundAttackGoal(this))
        this.goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 0.9))
        this.goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        this.goalSelector.addGoal(6, RandomLookAroundGoal(this))
        this.targetSelector.addGoal(1, HurtByTargetGoal(this))
        this.targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        if (source.`is`(DamageTypes.IN_FIRE) || source.`is`(DamageTypes.ON_FIRE) || source.`is`(DamageTypes.LAVA)) {
            return false
        }
        return super.hurtServer(serverLevel, source, amount)
    }

    override fun doHurtTarget(serverLevel: ServerLevel, target: net.minecraft.world.entity.Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        if (hit) {
            target.setRemainingFireTicks(100)
        }
        return hit
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.BLAZE_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.BLAZE_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.BLAZE_DEATH
    override fun playStepSound(pos: BlockPos, state: BlockState) {
        this.playSound(SoundEvents.WOLF_STEP, 0.15f, 1.0f)
    }

    private inner class HellhoundAttackGoal(mob: net.minecraft.world.entity.PathfinderMob) : MeleeAttackGoal(mob, 1.2, true)
}
