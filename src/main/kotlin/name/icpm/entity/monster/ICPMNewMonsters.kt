package name.icpm.entity.monster

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.skeleton.Skeleton
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import name.icpm.entity.ICPMEntities

/**
 * ICPM R196 新增怪物实体集合
 *
 * 包含 9 个 R196 特有怪物：
 * - GhoulEntity（食尸鬼）：捕食动物，命中施加缓慢
 * - WightEntity（尸妖）：亡灵，仅受火焰/银/魔法伤害，吸取经验
 * - ShadowEntity（暗影）：亡灵，熄灭光源，阳光下秒杀
 * - InvisibleStalkerEntity（隐形追猎者）：熄灭光源，无声
 * - RevenantEntity（复仇僵尸）：高血量高伤害僵尸变种
 * - ClayGolemEntity（黏土魔像）：土元素变种
 * - AncientBoneLordEntity（远古骨王）：骷髅变种，远古金属装备
 * - VampireBatEntity（吸血蝙蝠）：吸血恢复
 * - NightwingEntity（夜翼）：亡灵蝙蝠，仅受银/魔法伤害
 */

// ==================== 食尸鬼 ====================

/**
 * 食尸鬼（R196 EntityGhoul）
 * 捕食动物和村民，命中施加缓慢 V 药水效果（50 刻）
 * 经验值 ×2
 */
class GhoulEntity(type: EntityType<out GhoulEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.FOLLOW_RANGE, 40.0)
    }

    override fun registerGoals() {
        goalSelector.addGoal(1, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, true))
        goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 0.8))
        goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(6, RandomLookAroundGoal(this))
        targetSelector.addGoal(1, NearestAttackableTargetGoal(this, Player::class.java, true))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Animal::class.java, true))
    }

    override fun doHurtTarget(serverLevel: ServerLevel, target: net.minecraft.world.entity.Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        if (hit && target is LivingEntity) {
            target.addEffect(net.minecraft.world.effect.MobEffectInstance(MobEffects.SLOWNESS, 50, 4))
        }
        return hit
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.ZOMBIE_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.ZOMBIE_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.ZOMBIE_DEATH
}

// ==================== 尸妖 ====================

/**
 * 尸妖（R196 EntityWight）
 * 亡灵属性：仅受火焰/银质/魔法伤害
 * 攻击吸取玩家经验（40% 概率）
 * 经验值 ×2
 */
class WightEntity(type: EntityType<out WightEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FOLLOW_RANGE, 40.0)
    }

    override fun registerGoals() {
        goalSelector.addGoal(1, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, true))
        goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 1.0))
        goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(6, RandomLookAroundGoal(this))
        targetSelector.addGoal(1, HurtByTargetGoal(this))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        // R196 isImmuneTo：仅受 火焰 / 岩浆 / 银制武器 / 附魔武器 伤害
        if (source.`is`(DamageTypes.IN_FIRE) || source.`is`(DamageTypes.ON_FIRE) ||
            source.`is`(DamageTypes.LAVA)) return super.hurtServer(serverLevel, source, amount)
        if (ICPMDamageAspects.hasSilverAspect(source)) return super.hurtServer(serverLevel, source, amount)
        if (ICPMDamageAspects.hasMagicAspect(source)) return super.hurtServer(serverLevel, source, amount)
        return false
    }

    override fun doHurtTarget(serverLevel: ServerLevel, target: net.minecraft.world.entity.Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        if (hit && target is Player && random.nextFloat() < 0.4f) {
            // 吸取经验：(等级+1)×10，经抗性计算
            val drain = maxOf((target.experienceLevel + 1) * 10, 20)
            target.giveExperiencePoints(-drain)
        }
        return hit
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.SKELETON_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.SKELETON_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.SKELETON_DEATH
}

// ==================== 暗影 ====================

/**
 * 暗影（R196 EntityShadow）
 * 亡灵属性：仅受银质/魔法伤害
 * 阳光下受到秒杀（1000 伤害）
 * 黑暗中自动回血，尝试熄灭附近火把
 * 命中施加视觉变暗/虚弱效果
 * 无脚步声
 */
class ShadowEntity(type: EntityType<out ShadowEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.23)
            .add(Attributes.FOLLOW_RANGE, 40.0)
    }

    override fun registerGoals() {
        goalSelector.addGoal(1, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, true))
        goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 1.0))
        goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(6, RandomLookAroundGoal(this))
        targetSelector.addGoal(1, HurtByTargetGoal(this))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun tick() {
        super.tick()
        // R196 EntityShadow.onLivingUpdate：每 tick 尝试熄灭附近光源（服务端/未受击/4 格外无玩家）
        tryDisableNearbyLightSource(this)
        if (!level().isClientSide) {
            // 阳光下秒杀
            if (level().canSeeSky(blockPosition()) && level().getMaxLocalRawBrightness(blockPosition()) > 12) {
                hurtServer(level() as ServerLevel, damageSources().onFire(), 1000f)
            }
            // 黑暗中回血（每 40 刻）
            if (tickCount % 40 == 0) {
                val brightness = level().getMaxLocalRawBrightness(blockPosition())
                if (brightness < 10) {
                    heal((0.4f - brightness / 25f) * 10f)
                }
            }
        }
    }

    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        // R196 isImmuneTo：仅受 银制武器 / 附魔武器 / 阳光 伤害
        // 阳光下秒杀由本类 tick() 以 onFire 伤害源（无责任实体）触发，需放行
        if (source.entity == null && source.`is`(DamageTypes.ON_FIRE)) {
            return super.hurtServer(serverLevel, source, amount)
        }
        if (ICPMDamageAspects.hasSilverAspect(source)) return super.hurtServer(serverLevel, source, amount)
        if (ICPMDamageAspects.hasMagicAspect(source)) return super.hurtServer(serverLevel, source, amount)
        return false
    }

    override fun doHurtTarget(serverLevel: ServerLevel, target: net.minecraft.world.entity.Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        if (hit && target is LivingEntity) {
            target.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 600, 0))
            // R196 EntityShadow.attackEntityAsMob：命中玩家叠加 vision_dimming（1.21 无对应→DARKNESS 近似）
            applyVisionDimming(target, 2.0f)
        }
        return hit
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.ZOMBIE_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.ZOMBIE_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.ZOMBIE_DEATH
    override fun playStepSound(pos: BlockPos, state: BlockState) {
        // 无脚步声
    }
}

// ==================== 隐形追猎者 ====================

/**
 * 隐形追猎者（R196 EntityInvisibleStalker）
 * 尝试熄灭附近光源，无脚步声
 * 经验值 ×2
 */
class InvisibleStalkerEntity(type: EntityType<out InvisibleStalkerEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.23)
            .add(Attributes.FOLLOW_RANGE, 40.0)
    }

    override fun registerGoals() {
        goalSelector.addGoal(1, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, true))
        goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 1.0))
        goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(6, RandomLookAroundGoal(this))
        targetSelector.addGoal(1, HurtByTargetGoal(this))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun tick() {
        super.tick()
        // R196 EntityInvisibleStalker.onLivingUpdate：每 tick 尝试熄灭附近光源
        tryDisableNearbyLightSource(this)
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.ZOMBIE_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.ZOMBIE_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.ZOMBIE_DEATH
    override fun playStepSound(pos: BlockPos, state: BlockState) {
        // 无脚步声
    }
}

// ==================== 复仇僵尸 ====================

/**
 * 复仇僵尸（R196 EntityRevenant）
 * 高血量（30）高伤害（7.0）的僵尸变种
 * 每区块最多 1 个，经验值 ×3
 */
class RevenantEntity(type: EntityType<out RevenantEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ARMOR, 4.0)
    }

    override fun registerGoals() {
        goalSelector.addGoal(1, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, true))
        goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 1.0))
        goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(6, RandomLookAroundGoal(this))
        targetSelector.addGoal(1, HurtByTargetGoal(this))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    /**
     * R196 EntityRevenant.getMaxSpawnedInChunk() = 1：同一区块最多 1 个复仇僵尸。
     * 1.21 无「每区块刷怪上限」原语，故在 finalizeSpawn 时统计本区块现存复仇僵尸（含自身），
     * 超过 1 个则丢弃自身。
     */
    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        reason: EntitySpawnReason,
        spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, reason, spawnData)
        if (level is ServerLevel) {
            val cp = ChunkPos(blockPosition())
            val aabb = AABB(
                cp.minBlockX.toDouble(), level.minY.toDouble(), cp.minBlockZ.toDouble(),
                (cp.maxBlockX + 1).toDouble(), level.maxY.toDouble(), (cp.maxBlockZ + 1).toDouble()
            )
            if (level.getEntitiesOfClass(RevenantEntity::class.java, aabb).size > 1) {
                discard()
            }
        }
        return data
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.ZOMBIE_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.ZOMBIE_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.ZOMBIE_DEATH
}

// ==================== 黏土魔像 ====================

/**
 * 黏土魔像（R196 EntityClayGolem）
 * 继承土元素体系，高血量（30）中等伤害（6.0）
 * 天然防御 2.0（硬化黏土形态）
 */
class ClayGolemEntity(type: EntityType<out ClayGolemEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ARMOR, 2.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
    }

    override fun registerGoals() {
        goalSelector.addGoal(1, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, true))
        goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 0.7))
        goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(6, RandomLookAroundGoal(this))
        targetSelector.addGoal(1, HurtByTargetGoal(this))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.IRON_GOLEM_HURT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.IRON_GOLEM_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.IRON_GOLEM_DEATH
}

// ==================== 远古骨王 ====================

/**
 * 远古骨王（R196 EntityAncientBoneLord）
 * 高血量（24）高伤害（8.0）的骷髅变种
 * 装备远古金属武器和全套远古金属甲
 */
class AncientBoneLordEntity(type: EntityType<out AncientBoneLordEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.FOLLOW_RANGE, 40.0)
    }

    override fun registerGoals() {
        goalSelector.addGoal(1, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, true))
        goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 1.0))
        goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(6, RandomLookAroundGoal(this))
        targetSelector.addGoal(1, HurtByTargetGoal(this))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    // R196 EntityBoneLord.num_troops_summoned（远古骨王承继骨领主召唤链，召唤古尸）
    private var numTroopsSummoned = 0

    override fun tick() {
        super.tick()
        if (!level().isClientSide) {
            numTroopsSummoned = r196BoneLordTick(this, ICPMEntities.LONGDEAD, numTroopsSummoned)
        }
    }

    override fun addAdditionalSaveData(output: net.minecraft.world.level.storage.ValueOutput) {
        super.addAdditionalSaveData(output)
        if (numTroopsSummoned > 0) output.putInt("num_troops_summoned", numTroopsSummoned)
    }

    override fun readAdditionalSaveData(input: net.minecraft.world.level.storage.ValueInput) {
        super.readAdditionalSaveData(input)
        numTroopsSummoned = input.getInt("num_troops_summoned").orElse(0)
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.SKELETON_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.SKELETON_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.SKELETON_DEATH
}

// ==================== 吸血蝙蝠 ====================

/**
 * 吸血蝙蝠（R196 EntityVampireBat）
 * 碰撞攻击吸取等量生命值
 * 满血后进入饱食冷却（1200 刻 = 60 秒）
 * 继承原版 Bat：飞行 + 翅膀动画状态（渲染用）
 */
class VampireBatEntity(type: EntityType<out VampireBatEntity>, level: Level) : Bat(type, level) {

    private var feedCooldown = 0
    private var attackTick = 0

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Bat.createAttributes()
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.FLYING_SPEED, 0.5)
    }

    init {
        // 敌对蝙蝠：不悬挂休息（R196 主动攻击）
        if (!level().isClientSide) {
            setResting(false)
        }
    }

    override fun registerGoals() {
        targetSelector.addGoal(1, NearestAttackableTargetGoal(this, Player::class.java, true))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Animal::class.java, true))
    }

    override fun tick() {
        super.tick() // Bat.tick：休息处理 + setupAnimationStates（翅膀动画）
        if (feedCooldown > 0) feedCooldown--
    }

    override fun customServerAiStep(level: ServerLevel) {
        super.customServerAiStep(level) // Bat 随机飞行
        val target = target
        if (target != null && target.isAlive) {
            // 飞行追击目标
            val dx = target.x - x
            val dy = (target.y + target.bbHeight * 0.5) - y
            val dz = target.z - z
            val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            if (dist > 1.8) {
                val mv = deltaMovement
                setDeltaMovement(mv.add(dx / dist * 0.12, dy / dist * 0.12, dz / dist * 0.12).multiply(0.9, 0.9, 0.9))
            }
            // 近身攻击（20 刻冷却）
            if (dist < 2.2 && attackTick <= 0) {
                attackTick = 20
                doHurtTarget(level, target)
            }
        }
        if (attackTick > 0) attackTick--
    }

    override fun doHurtTarget(serverLevel: ServerLevel, target: net.minecraft.world.entity.Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        if (hit && target is LivingEntity) {
            // 吸血：恢复等量生命
            heal(this.getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat())
            feedCooldown = 1200
        }
        return hit
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.BAT_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.BAT_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.BAT_DEATH
}

// ==================== 夜翼 ====================

/**
 * 夜翼（R196 EntityNightwing）
 * 亡灵蝙蝠：仅受银质/魔法伤害
 * 阳光下秒杀，黑暗中回血
 * 命中施加视觉变暗效果
 * 继承原版 Bat：飞行 + 翅膀动画状态（渲染用）
 */
class NightwingEntity(type: EntityType<out NightwingEntity>, level: Level) : Bat(type, level) {

    private var attackTick = 0

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Bat.createAttributes()
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.FLYING_SPEED, 0.5)
    }

    init {
        // 敌对蝙蝠：不悬挂休息
        if (!level().isClientSide) {
            setResting(false)
        }
    }

    override fun registerGoals() {
        targetSelector.addGoal(1, NearestAttackableTargetGoal(this, Player::class.java, true))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Animal::class.java, true))
    }

    override fun tick() {
        super.tick() // Bat.tick：休息处理 + setupAnimationStates（翅膀动画）
        if (!level().isClientSide) {
            // 阳光下秒杀
            if (level().canSeeSky(blockPosition()) && level().getMaxLocalRawBrightness(blockPosition()) > 12) {
                hurtServer(level() as ServerLevel, damageSources().onFire(), 1000f)
            }
            // 黑暗中回血
            if (tickCount % 40 == 0) {
                val brightness = level().getMaxLocalRawBrightness(blockPosition())
                if (brightness < 10) {
                    heal((0.4f - brightness / 25f) * 10f)
                }
            }
        }
    }

    override fun customServerAiStep(level: ServerLevel) {
        super.customServerAiStep(level) // Bat 随机飞行
        val target = target
        if (target != null && target.isAlive) {
            // 飞行追击目标
            val dx = target.x - x
            val dy = (target.y + target.bbHeight * 0.5) - y
            val dz = target.z - z
            val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            if (dist > 1.8) {
                val mv = deltaMovement
                setDeltaMovement(mv.add(dx / dist * 0.12, dy / dist * 0.12, dz / dist * 0.12).multiply(0.9, 0.9, 0.9))
            }
            // 近身攻击（20 刻冷却）
            if (dist < 2.2 && attackTick <= 0) {
                attackTick = 20
                doHurtTarget(level, target)
            }
        }
        if (attackTick > 0) attackTick--
    }

    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        // R196 isImmuneTo：仅受 银制武器 / 附魔武器 / 阳光 伤害
        // 阳光下秒杀由本类 tick() 以 onFire 伤害源（无责任实体）触发，需放行
        if (source.entity == null && source.`is`(DamageTypes.ON_FIRE)) {
            return super.hurtServer(serverLevel, source, amount)
        }
        if (ICPMDamageAspects.hasSilverAspect(source)) return super.hurtServer(serverLevel, source, amount)
        if (ICPMDamageAspects.hasMagicAspect(source)) return super.hurtServer(serverLevel, source, amount)
        return false
    }

    override fun doHurtTarget(serverLevel: ServerLevel, target: net.minecraft.world.entity.Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        // R196 EntityNightwing.collideWithEntity：命中玩家叠加 vision_dimming（1.21 无对应→DARKNESS 近似）
        if (hit && target is LivingEntity) {
            applyVisionDimming(target, 1.25f)
        }
        return hit
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.BAT_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.BAT_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.BAT_DEATH
}

// ==================== R196 通用行为助手 ====================

/**
 * R196 EntityLiving.tryDisableNearbyLightSource 移植：
 * 服务端、未受击（hurtTime==0）、4 格内无玩家时，扫描自身 3×3×(1+身高) 范围：
 *  - 火把/红石火把（含墙挂）→ 破坏并掉落、播放 pop 音效、返回 true
 *  - 南瓜灯 → 还原为南瓜并掉落一个火把、播放 pop 音效、返回 true
 */
internal fun tryDisableNearbyLightSource(self: Mob): Boolean {
    val level = self.level()
    if (level.isClientSide) return false
    if (self.hurtTime != 0) return false
    if (level.getNearestPlayer(self, 4.0) != null) return false
    val bx = self.blockPosition().x
    val by = self.blockPosition().y
    val bz = self.blockPosition().z
    val top = 1 + self.bbHeight.toInt()
    for (dx in -1..1) {
        for (dy in -1..top) {
            for (dz in -1..1) {
                val p = BlockPos(bx + dx, by + dy, bz + dz)
                val state = level.getBlockState(p)
                if (state.`is`(Blocks.TORCH) || state.`is`(Blocks.REDSTONE_TORCH)
                    || state.`is`(Blocks.WALL_TORCH) || state.`is`(Blocks.REDSTONE_WALL_TORCH)) {
                    level.destroyBlock(p, true)
                    playPopSound(self)
                    return true
                }
                if (state.`is`(Blocks.JACK_O_LANTERN)) {
                    level.setBlock(p, Blocks.PUMPKIN.defaultBlockState(), 3)
                    val item = ItemEntity(level, p.x + 0.5, p.y + 0.5, p.z + 0.5, ItemStack(Items.TORCH))
                    item.setPickUpDelay(10)
                    level.addFreshEntity(item)
                    playPopSound(self)
                    return true
                }
            }
        }
    }
    return false
}

private fun playPopSound(self: Mob) {
    self.level().playSound(
        null, self.x, self.y, self.z, SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE,
        0.05f, ((self.random.nextFloat() - self.random.nextFloat()) * 0.7f + 1.0f) * 2.0f
    )
}

/**
 * R196 vision_dimming（屏幕变暗）在 1.21 无对应渲染，用 DARKNESS（黑暗）效果近似。
 * R196 为该 float 值随时间 0.01/tick 衰减、封顶 2.0；此处以固定时长近似叠加量。
 */
internal fun applyVisionDimming(target: LivingEntity, amount: Float) {
    if (target !is Player) return
    val duration = (amount * 100f).toInt().coerceIn(20, 600)
    target.addEffect(MobEffectInstance(MobEffects.DARKNESS, duration, 0, false, false))
}

/**
 * R196 EntityBoneLord.onLivingUpdate 召唤链移植（每 20 tick）：
 *  - 目标有效（玩家、16 格内可见）且 num<6 时，以 rand.nextInt(8) < 7-num 的概率召唤 1 只随从，
 *    随后 50% 概率再召唤 1 只（上限 6）。
 *  - 16×8×16 内可见的骷髅：血量未满则治疗 1，并使其攻击同一目标（frenzied）。
 * 返回更新后的 num_troops_summoned。
 */
internal fun r196BoneLordTick(self: Mob, troopType: EntityType<out Mob>, numSummoned: Int): Int {
    val level = self.level() as? ServerLevel ?: return numSummoned
    if (self.tickCount % 20 != 0) return numSummoned
    var num = numSummoned
    var target: LivingEntity? = self.target
    if (target != null && (!target.isAlive || self.distanceTo(target) > 16.0 || !self.hasLineOfSight(target))) {
        target = null
    }
    if (target is Player && num < 6 && self.random.nextInt(8) < 7 - num) {
        if (trySummonTroop(self, level, troopType, target)) num++
        if (num < 6 && self.random.nextBoolean()) {
            if (trySummonTroop(self, level, troopType, target)) num++
        }
    }
    val nearby = level.getEntitiesOfClass(Skeleton::class.java, self.boundingBox.inflate(16.0, 8.0, 16.0))
    for (sk in nearby) {
        if (sk === self) continue
        if (!sk.hasLineOfSight(self)) continue
        if (sk.health < sk.maxHealth) sk.heal(1.0f)
        if (target != null) sk.target = target
    }
    return num
}

/** 在施法者周围寻找可站立位置并召唤一只随从；成功返回 true。 */
private fun trySummonTroop(self: Mob, level: ServerLevel, troopType: EntityType<out Mob>, target: LivingEntity): Boolean {
    val troop: Mob = (troopType.create(level, EntitySpawnReason.MOB_SUMMONED) as? Mob) ?: return false
    for (attempt in 0 until 16) {
        val dx = self.random.nextInt(9) - 4
        val dz = self.random.nextInt(9) - 4
        val dy = self.random.nextInt(3) - 1
        val px = self.x + dx
        val py = self.y + dy
        val pz = self.z + dz
        val bp = BlockPos.containing(px, py, pz)
        if (!level.getBlockState(bp).isAir || !level.getBlockState(bp.above()).isAir) continue
        if (!level.getBlockState(bp.below()).isSolid) continue
        troop.setPos(px, py, pz)
        troop.setYRot(self.random.nextFloat() * 360f)
        troop.finalizeSpawn(level, level.getCurrentDifficultyAt(bp), EntitySpawnReason.MOB_SUMMONED, null)
        level.addFreshEntity(troop)
        troop.target = target
        return true
    }
    troop.discard()
    return false
}
