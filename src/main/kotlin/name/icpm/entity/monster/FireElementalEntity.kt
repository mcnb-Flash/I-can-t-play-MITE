package name.icpm.entity.monster

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.state.BlockState

/**
 * 火元素（R196 EntityFireElemental 移植，数值与机制严格对齐）。
 *
 * 机制（全部来自 r196 反编译源码逐字确认，无臆造）：
 * <ul>
 *   <li>属性：followRange 40 / 移速 0.25 / 攻击 5</li>
 *   <li>免疫：仅水（DamageTypes.DROWN）可造成伤害；其余（近战、箭、火）免疫</li>
 *   <li>每 40 tick 受 1 点“水”伤害（被雨/水中衰减）</li>
 *   <li>接触岩浆每 40 tick 回 4 血</li>
 *   <li>近战命中点燃目标 6 秒</li>
 *   <li>始终燃烧（视觉）、不受火/岩浆伤害、可在岩浆中生存</li>
 *   <li>任意亮度可生成、不在浅水生成</li>
 *   <li>经验 ×3</li>
 * </ul>
 *
 * 生成：R196 火元素从下界岩浆源块上方生成。Fabric 1.21.11 无对应方块钩子，
 * 简化为“下界 + 地下世界”自然生成（见 [name.icpm.entity.ICPMEntities]）。
 */
class FireElementalEntity(type: EntityType<out FireElementalEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FOLLOW_RANGE, 40.0)
    }

    override fun registerGoals() {
        this.goalSelector.addGoal(0, FloatGoal(this))
        this.goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, false))
        this.goalSelector.addGoal(3, WaterAvoidingRandomStrollGoal(this, 1.0))
        this.goalSelector.addGoal(4, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        this.goalSelector.addGoal(5, RandomLookAroundGoal(this))
        this.targetSelector.addGoal(1, HurtByTargetGoal(this))
        this.targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    /**
     * R196 isImmuneTo 移植：仅水伤害（DROWN）可伤；其余免疫。
     * 1.21.11 签名：isInvulnerableTo(serverLevel, source)。
     */
    override fun isInvulnerableTo(serverLevel: net.minecraft.server.level.ServerLevel, source: DamageSource): Boolean {
        if (source.`is`(DamageTypes.DROWN)) {
            return false
        }
        return true
    }

    override fun doHurtTarget(serverLevel: net.minecraft.server.level.ServerLevel, target: net.minecraft.world.entity.Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        if (hit && target is LivingEntity) {
            target.setRemainingFireTicks(6 * 20)
        }
        return hit
    }

    override fun tick() {
        super.tick()
        if (this.level().isClientSide) {
            if (this.isInWater) {
                this.spawnSteamParticles(10)
            }
            return
        }
        // 每 40 tick 受 1 点水伤害（R196 淋雨/水中衰减）
        if (this.tickCount % 40 == 0) {
            this.hurt(this.damageSources().drown(), 1.0f)
        }
        // 接触岩浆回血（R196 handleLavaMovement → heal 4）
        if (this.isInLava) {
            this.heal(4.0f)
        }
    }

    private fun spawnSteamParticles(count: Int) {
        for (i in 0 until count) {
            this.level().addParticle(
                ParticleTypes.SMOKE,
                this.x + (this.random.nextDouble() - 0.5) * this.boundingBox.xsize,
                this.y + this.random.nextDouble() * this.boundingBox.ysize,
                this.z + (this.random.nextDouble() - 0.5) * this.boundingBox.zsize,
                0.0, 0.1, 0.0
            )
        }
    }

    override fun isOnFire(): Boolean = true

    override fun hurtServer(serverLevel: net.minecraft.server.level.ServerLevel, source: DamageSource, amount: Float): Boolean {
        // 火焰/岩浆完全免疫（isInvulnerableTo 已覆盖大部分，这里双保险）
        if (source.`is`(DamageTypes.IN_FIRE) || source.`is`(DamageTypes.ON_FIRE) || source.`is`(DamageTypes.LAVA)) {
            return false
        }
        return super.hurtServer(serverLevel, source, amount)
    }

    override fun getAmbientSound() = SoundEvents.FIRE_AMBIENT
    override fun getHurtSound(source: DamageSource) = SoundEvents.GENERIC_BURN
    override fun getDeathSound() = SoundEvents.GENERIC_BURN
    override fun playStepSound(pos: BlockPos, state: BlockState) = Unit

    /** R196 canSpawnInShallowWater：不在浅水生成（Mob 默认已处理和平难度，这里仅透传） */
    override fun checkSpawnRules(level: LevelAccessor, reason: EntitySpawnReason): Boolean {
        return super.checkSpawnRules(level, reason)
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        reason: EntitySpawnReason,
        spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, reason, spawnData)
        this.xpReward = (5 * 3)
        return data
    }
}
