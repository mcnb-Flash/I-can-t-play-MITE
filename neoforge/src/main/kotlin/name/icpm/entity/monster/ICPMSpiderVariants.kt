package name.icpm.entity.monster

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.spider.Spider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.abs

/**
 * R196 蜘蛛变种实体族（继承原版 Spider）。逐条对照 R196 反编译源码：
 *
 *  | 变种 | HP | 攻击 | 移速 | 剧毒 | 特殊 |
 *  |---|---|---|---|---|---|
 *  | 木蛛 WoodSpider | 6 | 1 | 0.8 | 240t | 音量×0.6/音调×1.2；明亮处索敌半径减半 |
 *  | 洞窟蛛 EntityCaveSpider | 16 | 4 | 1.0 | 480t | 体型 0.7；XP×2 |
 *  | 黑寡妇 EntityBlackWidowSpider | 6 | 1 | 0.8 | 960t | XP×8/5 |
 *  | 相位蛛 EntityPhaseSpider | 6 | 3 | 0.8 | — | **闪避制**：受击消耗闪避次数瞬移脱离并**抵消伤害**；每 100t 回补 1 次，上限 rand(3)+2 |
 *  | 恶魔蛛 EntityDemonSpider | 18 | 5 | 1.0 | 480t + 缓慢(50t, V) | 免火/免岩浆；XP×3 |
 *
 * R196 数值锚点：
 *  - 基准 `EntityArachnid.java:135-140`（HP12/followRange28/speed1.0/atk4）
 *  - 木蛛 `EntityWoodSpider.java:25-27,53`；洞窟 `EntityCaveSpider.java:24,39`；
 *    黑寡妇 `EntityBlackWidowSpider.java:16-18`；相位 `EntityPhaseSpider.java:30,37,140-156`；
 *    恶魔 `EntityDemonSpider.java:26-27,57-58,69-76`
 *
 * ⚠ 已知未移植（另记）：R196 蛛类【吐网】(EntityArachnid.onUpdate_ → EntityWeb 抛射物)
 *   需自定义抛射实体，暂缺。
 */
abstract class ICPMSpiderVariant(type: EntityType<out ICPMSpiderVariant>, level: Level) : Spider(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Spider.createAttributes()
            .add(Attributes.MAX_HEALTH, 12.0)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 1.0)
            .add(Attributes.FOLLOW_RANGE, 28.0)
    }

    protected abstract val healthValue: Double
    protected abstract val attackValue: Double
    protected abstract val moveSpeedValue: Double
    protected open val poisonDuration: Int = 0
    protected open val poisonAmplifier: Int = 0
    /** 命中附加的缓慢时长/等级（R196 恶魔蛛 moveSlowdown 50t, amplifier 5）。 */
    protected open val slowDuration: Int = 0
    protected open val slowAmplifier: Int = 0
    protected open val isFireImmune: Boolean = false

    /** R196 相位蛛：是否启用"闪避式瞬移"（受击抵消伤害并传送脱离）。 */
    protected open val hasEvasion: Boolean = false
    private var maxEvasions: Int = 0
    private var evasions: Int = 0

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
        this.getAttribute(Attributes.FOLLOW_RANGE)?.baseValue = 28.0
        this.setHealth(healthValue.toFloat())
        if (hasEvasion) {
            // R196 EntityPhaseSpider:30 —— max_num_evasions = num_evasions = rand.nextInt(3) + 2（2~4）
            maxEvasions = random.nextInt(3) + 2
            evasions = maxEvasions
        }
        return data
    }

    override fun tick() {
        super.tick()
        if (!this.level().isClientSide() && hasEvasion && evasions < maxEvasions && tickCount % 100 == 0) {
            // R196 EntityPhaseSpider:63-65 —— 每 100t 回补 1 次闪避（不超过上限）
            evasions++
        }
    }

    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        if (isFireImmune && (source.`is`(DamageTypes.IN_FIRE) || source.`is`(DamageTypes.ON_FIRE) || source.`is`(DamageTypes.LAVA))) {
            return false
        }
        // R196 EntityPhaseSpider.attackEntityFrom:140-156 —— 坠落/火焰/中毒不可闪避；
        // 其余伤害在闪避次数 > 0 时：消耗 1 次 → 远离攻击者瞬移 → 成功则抵消本次伤害（返回 null）
        if (hasEvasion && evasions > 0
            && !source.`is`(DamageTypes.FALL)
            && !source.`is`(DamageTypes.IN_FIRE)
            && !source.`is`(DamageTypes.ON_FIRE)
            && !source.`is`(DamageTypes.MAGIC)
            && !serverLevel.isClientSide
        ) {
            val attacker: Entity? = source.directEntity ?: source.entity
            if (tryTeleportAwayFrom(serverLevel, attacker, 3.0)) {
                evasions--
                return false
            }
        }
        return super.hurtServer(serverLevel, source, amount)
    }

    /**
     * R196 EntityPhaseSpider.tryTeleportAwayFrom:106-137 —— 最多 64 次尝试，
     * 水平偏移 dx/dz ∈ [-5,5]、垂直 dy ∈ [-4,4]，跳过 |dx|<3 且 |dz|<3 的过近候选，
     * 且必须与威胁保持 >= min_distance；找到安全落点后瞬移。
     */
    private fun tryTeleportAwayFrom(serverLevel: ServerLevel, threat: Entity?, minDistance: Double): Boolean {
        val minSq = minDistance * minDistance
        val tx = threat?.x ?: this.x
        val tz = threat?.z ?: this.z
        val baseX = this.blockX
        val baseY = this.blockY
        val baseZ = this.blockZ
        repeat(64) {
            val dx = random.nextInt(11) - 5
            val dy = random.nextInt(9) - 4
            val dz = random.nextInt(11) - 5
            if (abs(dx) < 3 && abs(dz) < 3) return@repeat
            val nx = baseX + dx
            val nz = baseZ + dz
            val px = nx + 0.5
            val pz = nz + 0.5
            val dxT = px - tx
            val dzT = pz - tz
            if (dxT * dxT + dzT * dzT < minSq) return@repeat

            var ny = baseY + dy
            if (ny < serverLevel.minY + 1) return@repeat
            // 向下寻找第一个"上方可站立"的高度
            while (ny > serverLevel.minY + 1 && !serverLevel.getBlockState(BlockPos(nx, ny - 1, nz)).isSolid) {
                ny--
            }
            val feet = serverLevel.getBlockState(BlockPos(nx, ny, nz))
            val head = serverLevel.getBlockState(BlockPos(nx, ny + 1, nz))
            if (!feet.isAir || !head.isAir) return@repeat
            val ground = BlockPos(nx, ny - 1, nz)
            if (!serverLevel.getBlockState(ground).isSolid) return@repeat
            if (!serverLevel.getFluidState(ground).isEmpty) return@repeat

            if (this.randomTeleport(px, ny.toDouble(), pz, true)) {
                serverLevel.playSound(
                    null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    this.soundSource, 1.0f, 1.0f
                )
                return true
            }
        }
        return false
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        if (hasEvasion) {
            output.putInt("ICPMMaxEvasions", maxEvasions)
            output.putInt("ICPMEvasions", evasions)
        }
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        if (hasEvasion) {
            maxEvasions = input.getInt("ICPMMaxEvasions").orElse(0)
            evasions = input.getInt("ICPMEvasions").orElse(0)
        }
    }

    override fun doHurtTarget(serverLevel: ServerLevel, target: Entity): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        // R196：命中并造成掉血后附加中毒（对任意 LivingEntity，不限玩家）
        if (hit && target is LivingEntity) {
            if (poisonDuration > 0) {
                target.addEffect(MobEffectInstance(MobEffects.POISON, poisonDuration, poisonAmplifier))
            }
            if (slowDuration > 0) {
                target.addEffect(MobEffectInstance(MobEffects.SLOWNESS, slowDuration, slowAmplifier))
            }
        }
        return hit
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.SPIDER_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.SPIDER_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.SPIDER_DEATH
}

/**
 * 木蛛：R196 `EntityWoodSpider.java:25-27,53` —— HP 6 / 攻击 1 / 移速 0.8 / 毒 240t。
 * 音量 ×0.6、音调 ×1.2；明亮处索敌半径减半。
 */
class WoodSpiderEntity(type: EntityType<out WoodSpiderEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 6.0
    override val attackValue: Double = 1.0
    override val moveSpeedValue: Double = 0.8
    override val poisonDuration: Int = 240
    // R196 另有 音量 ×0.6 / 音调 ×1.2（cosmetic，未移植）
}

/** 洞窟蜘蛛：R196 `EntityCaveSpider.java:24,39` —— HP 16（攻击/移速继承基线 4/1.0）/ 毒 480t。 */
class CaveSpiderVariantEntity(type: EntityType<out CaveSpiderVariantEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 16.0
    override val attackValue: Double = 4.0
    override val moveSpeedValue: Double = 1.0
    override val poisonDuration: Int = 480
}

/** 黑寡妇：R196 `EntityBlackWidowSpider`（继承木蛛）+ 毒 960t；XP ×8/5。 */
class BlackWidowEntity(type: EntityType<out BlackWidowEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 6.0
    override val attackValue: Double = 1.0
    override val moveSpeedValue: Double = 0.8
    override val poisonDuration: Int = 960
}

/**
 * 相位蜘蛛：R196 `EntityPhaseSpider` —— HP 6 / 攻击 3 / 移速 0.8。
 * 受击闪避：消耗次数瞬移脱离并抵消伤害，初始 2~4 次，每 100t 回补 1 次。
 */
class PhaseSpiderEntity(type: EntityType<out PhaseSpiderEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 6.0
    override val attackValue: Double = 3.0
    override val moveSpeedValue: Double = 0.8
    override val hasEvasion: Boolean = true
}

/** 恶魔蜘蛛：R196 `EntityDemonSpider.java:26-27,57-58,69-76` —— HP 18 / 攻击 5 / 移速 1.0 / 毒 480t + 缓慢 V 50t；免火免岩浆。 */
class DemonSpiderEntity(type: EntityType<out DemonSpiderEntity>, level: Level) : ICPMSpiderVariant(type, level) {
    override val healthValue: Double = 18.0
    override val attackValue: Double = 5.0
    override val moveSpeedValue: Double = 1.0
    override val poisonDuration: Int = 480
    override val slowDuration: Int = 50
    override val slowAmplifier: Int = 5
    override val isFireImmune: Boolean = true
}
