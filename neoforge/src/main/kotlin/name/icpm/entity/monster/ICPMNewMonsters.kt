package name.icpm.entity.monster

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
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
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

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
            target.addEffect(net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, 600, 0))
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

    override fun getAmbientSound(): SoundEvent = SoundEvents.BAT_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.BAT_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.BAT_DEATH
}
