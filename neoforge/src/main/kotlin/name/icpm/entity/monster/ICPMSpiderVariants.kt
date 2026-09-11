package name.icpm.entity.monster

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.spider.Spider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

/**
 * R196 蜘蛛变种实体族（继承原版 Spider）。
 *
 *  - WoodSpider（木蛛）：普通近战，基础属性
 *  - CaveSpider（洞窟蜘蛛）：中毒攻击、体型较小、移速较快
 *  - BlackWidow（黑寡妇）：剧毒攻击、更高攻击
 *  - PhaseSpider（相位蜘蛛）：受击时随机短距传送，难以命中
 *  - DemonSpider（恶魔蜘蛛）：火焰免疫、命中点燃目标
 */
abstract class ICPMSpiderVariant(type: EntityType<out ICPMSpiderVariant>, level: Level) : Spider(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Spider.createAttributes()
            .add(Attributes.ATTACK_DAMAGE, 2.0)
    }

    protected abstract val healthValue: Double
    protected abstract val attackValue: Double
    protected abstract val moveSpeedValue: Double
    protected open val poisonDuration: Int = 0
    protected open val poisonAmplifier: Int = 0
    protected open val isFireImmune: Boolean = false
    protected open val teleportOnHurt: Boolean = false
    /**
     * 相位蜘蛛机制：受击时回到满血并瞬移至周围随机安全位置的次数（0 = 不启用）。
     * 每次成功瞬移消耗 1 次，耗尽后正常受击。
     */
    protected open val phaseHealCharges: Int = 0
    private var phaseChargesLeft: Int = -1

    @Override
    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        reason: EntitySpawnReason,
        spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, reason, spawnData)
        this.getAttribute(Attributes.MAX_HEALTH)?.baseValue = healthValue
        this.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue = attackValue
        this.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue = moveSpeedValue
        this.setHealth(healthValue.toFloat())
        if (phaseHealCharges > 0) phaseChargesLeft = phaseHealCharges
        return data
    }

    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        if (isFireImmune && (source.`is`(DamageTypes.IN_FIRE) || source.`is`(DamageTypes.ON_FIRE))) {
            return false
        }
        if (phaseHealCharges > 0 && !serverLevel.isClientSide) {
            if (phaseChargesLeft < 0) phaseChargesLeft = phaseHealCharges
            if (phaseChargesLeft > 0) {
                // 回到满血
                this.setHealth(this.maxHealth)
                // 瞬移至周围随机安全位置
                if (teleportToSafeNearby(serverLevel)) {
                    phaseChargesLeft--
                }
            }
        } else if (teleportOnHurt && !serverLevel.isClientSide && random.nextFloat() < 0.3f) {
            this.teleportRandomly()
        }
        return super.hurtServer(serverLevel, source, amount)
    }

    /**
     * 相位蜘蛛瞬移：在周围 radius 范围内随机寻找一个安全（脚部/头部为空气、脚下有支撑）的
     * 位置瞬移，避免嵌入墙体。找不到则返回 false（本次不消耗机会）。
     */
    private fun teleportToSafeNearby(serverLevel: ServerLevel): Boolean {
        val radius = 6
        for (i in 0 until 24) {
            val nx = x + (random.nextDouble() - 0.5) * 2.0 * radius
            val ny = Mth.clamp(
                y + (random.nextInt(2 * radius + 1) - radius).toDouble(),
                1.0,
                (serverLevel.height - 2).toDouble()
            )
            val nz = z + (random.nextDouble() - 0.5) * 2.0 * radius
            val pos = BlockPos.containing(nx, ny, nz)
            if (isSafeStanding(serverLevel, pos)) {
                if (this.randomTeleport(nx, ny, nz, true)) {
                    serverLevel.playSound(
                        null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        this.soundSource, 1.0f, 1.0f
                    )
                    return true
                }
            }
        }
        return false
    }

    /** 目标位置是否可安全站立：脚/头为空气、脚下非空气（有支撑） */
    private fun isSafeStanding(serverLevel: ServerLevel, pos: BlockPos): Boolean {
        val feet = serverLevel.getBlockState(pos)
        val head = serverLevel.getBlockState(pos.above())
        val ground = serverLevel.getBlockState(pos.below())
        return feet.isAir && head.isAir && !ground.isAir
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putInt("ICPMPhaseCharges", if (phaseChargesLeft < 0) phaseHealCharges else phaseChargesLeft)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        phaseChargesLeft = input.getInt("ICPMPhaseCharges").orElse(phaseHealCharges)
    }

    private fun teleportRandomly(): Boolean {
        if (!this.level().isClientSide() && this.isAlive()) {
            val x = this.x + (random.nextDouble() - 0.5) * 16.0
            val y = this.y + (random.nextInt(8) - 4).toDouble()
            val z = this.z + (random.nextDouble() - 0.5) * 16.0
            val moved = this.randomTeleport(x, Mth.clamp(y, 1.0, this.level().height - 1.0), z, true)
            if (moved) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, this.soundSource, 1.0f, 1.0f)
                return true
            }
        }
        return false
    }

    override fun doHurtTarget(serverLevel: ServerLevel, target: net.minecraft.world.entity.Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        if (hit && poisonDuration > 0 && target is Player) {
            target.addEffect(MobEffectInstance(MobEffects.POISON, poisonDuration, poisonAmplifier))
        }
        return hit
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.SPIDER_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.SPIDER_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.SPIDER_DEATH
    override fun playStepSound(pos: BlockPos, state: BlockState) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.15f, 1.0f)
    }
}

/** 木蛛：基础属性 */
class WoodSpiderEntity(type: EntityType<out WoodSpiderEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 14.0
    override val attackValue: Double = 2.0
    override val moveSpeedValue: Double = 0.3
}

/** 洞窟蜘蛛：中毒攻击，体型小移速快 */
class CaveSpiderVariantEntity(type: EntityType<out CaveSpiderVariantEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 12.0
    override val attackValue: Double = 2.0
    override val moveSpeedValue: Double = 0.33
    override val poisonDuration: Int = 100
    override val poisonAmplifier: Int = 0
}

/** 黑寡妇：剧毒攻击，高攻击 */
class BlackWidowEntity(type: EntityType<out BlackWidowEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 18.0
    override val attackValue: Double = 4.0
    override val moveSpeedValue: Double = 0.31
    override val poisonDuration: Int = 160
    override val poisonAmplifier: Int = 1
}

/** 相位蜘蛛：受击回到满血并瞬移，共 10 次机会 */
class PhaseSpiderEntity(type: EntityType<out PhaseSpiderEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 16.0
    override val attackValue: Double = 3.0
    override val moveSpeedValue: Double = 0.32
    override val teleportOnHurt: Boolean = true
    override val phaseHealCharges: Int = 10
}

/** 恶魔蜘蛛：火焰免疫，命中点燃 */
class DemonSpiderEntity(type: EntityType<out DemonSpiderEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 22.0
    override val attackValue: Double = 5.0
    override val moveSpeedValue: Double = 0.34
    override val isFireImmune: Boolean = true

    override fun doHurtTarget(serverLevel: ServerLevel, target: net.minecraft.world.entity.Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        if (hit) {
            target.setRemainingFireTicks(120)
        }
        return hit
    }
}
