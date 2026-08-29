package name.icpm.entity.monster

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Difficulty
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.util.RandomSource
import name.icpm.common.ICPMDissolveHelper

/**
 * R196 EntityGelatinousCube 移植（基类）。
 * 继承原版 Slime 获得跳动/分裂/挤压动画，补充：
 * 酸碱接触伤害、溶解方块/物品、进食、免疫、生成规则。
 */
abstract class GelatinousCubeEntity(type: EntityType<out GelatinousCubeEntity>, level: Level) : Slime(type, level) {

    companion object {
        const val BLOCK_FEED_MAX = 20
        const val ITEM_FEED_MAX = 20

        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.ATTACK_DAMAGE)
            .add(Attributes.MAX_HEALTH, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)

        fun checkGelatinousCubeSpawnRules(
            entityType: EntityType<out GelatinousCubeEntity>,
            level: ServerLevelAccessor,
            reason: EntitySpawnReason,
            pos: BlockPos,
            random: RandomSource
        ): Boolean {
            if (level.getDifficulty() == Difficulty.PEACEFUL) return false
            return Monster.checkMonsterSpawnRules(entityType, level, reason, pos, random)
        }
    }

    /** 攻击强度倍率：攻击伤害 = size * multiplier（R196 getAttackStrengthMultiplierForType） */
    abstract val attackStrengthMultiplier: Int

    /** 是否为酸性（ooze/pudding），酸性可溶解物品/更快溶解方块（R196 isAcidic） */
    abstract val isAcidicType: Boolean

    /** 是否含胃蛋白酶（jelly/blob/slime），可溶解软方块（R196 hasPepsin） */
    abstract val isPepsinType: Boolean

    /** 命中时是否施加饥饿（blob，R196 EntityCubic.attackEntityAsMob） */
    open val appliesHungerOnHit: Boolean = false

    /** 自然生成最大尺寸（ooze 为 2，其余 4，R196 setSize 上限） */
    open val maxNaturalSize: Int = 4

    /** 是否对岩浆/熔岩免疫（pudding 免疫火焰） */
    open val lavaImmune: Boolean = false

    private var blockFeedCountdown: Int = 0
    private var itemFeedCountdown: Int = 0
    private val dissolvingProgress = mutableMapOf<BlockPos, Int>()
    private var ticksUntilNextFizzSound: Int = 0

    override fun getParticleType(): ParticleOptions = ParticleTypes.ITEM_SLIME

    protected fun isFeeding(): Boolean = blockFeedCountdown > 0 || itemFeedCountdown > 0

    private fun setBlockFeeding(countdown: Int) {
        blockFeedCountdown = countdown.coerceIn(0, BLOCK_FEED_MAX)
    }

    private fun setItemFeeding(countdown: Int) {
        itemFeedCountdown = countdown.coerceIn(0, ITEM_FEED_MAX)
    }

    override fun getAttackDamage(): Float = (getSize() * attackStrengthMultiplier).toFloat()

    override fun getJumpDelay(): Int = if (getTarget() != null) 10 else random.nextInt(81) + 40

    override fun setSize(size: Int, heal: Boolean) {
        super.setSize(size, heal)
        xpReward = size * (attackStrengthMultiplier + if (isAcidicType) 1 else 0)
    }

    override fun dealDamage(target: LivingEntity) {
        super.dealDamage(target)
        if (level() !is ServerLevel) return
        if (target !is Player) return
        // R196 EntityCubic.b_: 接触时减速玩家
        target.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 30, 0))
        // R196 EntityCubic.attackEntityAsMob: blob 施加饥饿
        if (appliesHungerOnHit) {
            target.addEffect(MobEffectInstance(MobEffects.HUNGER, 50, 5))
        }
        // R196 EntityCubic.attackEntityAsMob: 酸性伤害玩家装备耐久
        if (isAcidicType) {
            damagePlayerArmor(target)
        }
    }

    private fun damagePlayerArmor(player: Player) {
        val dmg = (0.05f * getSize() * attackStrengthMultiplier).toInt().coerceAtLeast(1)
        val serverLevel = level() as? ServerLevel ?: return
        for (slot in listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            val stack = player.getItemBySlot(slot)
            if (stack.isEmpty) continue
            stack.hurtAndBreak(dmg, player, slot)
        }
    }

    /** 溶解接触到的方块与物品（R196 EntityGelatinousCube.c 的简化移植）。 */
    private fun dissolveBlocksAndItems() {
        val serverLevel = level() as? ServerLevel ?: return
        val expanded: AABB = boundingBox.inflate(0.01, 0.01, 0.01)
        var feeding = false

        val infos = mutableListOf<BlockPos>()
        for (pos in BlockPos.betweenClosed(expanded)) {
            infos.add(pos.immutable())
        }

        for (pos in infos) {
            if (level().getBlockState(pos).isAir) continue
            val period = ICPMDissolveHelper.getDissolvePeriod(level(), pos)
            if (period == -1) continue
            if (period == 0) {
                ICPMDissolveHelper.dissolveBlock(level(), pos)
                feeding = true
                continue
            }
            val progress = (dissolvingProgress[pos] ?: 0) + getSize()
            if (progress >= period) {
                ICPMDissolveHelper.dissolveBlock(level(), pos)
                dissolvingProgress.remove(pos)
                feeding = true
            } else {
                dissolvingProgress[pos] = progress
                feeding = true
            }
        }

        setBlockFeeding(if (feeding) BLOCK_FEED_MAX else 0)

        // 物品溶解：接触立方体的掉落物受到 pepsin/acid 伤害
        var itemsDamaged = false
        val items = level().getEntitiesOfClass(ItemEntity::class.java, expanded)
        for (item in items) {
            if (item.getItem().isEmpty) continue
            item.setPickUpDelay(60)
            if (item.hurtServer(serverLevel, damageSources().generic(), 1.0f)) {
                itemsDamaged = true
            }
        }
        setItemFeeding(if (itemsDamaged) ITEM_FEED_MAX else 0)
    }

    override fun tick() {
        super.tick()
        if (level() is ServerLevel) {
            if (tickCount % 20 == 0) {
                dissolveBlocksAndItems()
            }
            if (isInLava()) {
                if (--ticksUntilNextFizzSound <= 0) {
                    level().playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.7f, 1.6f + (random.nextFloat() - random.nextFloat()) * 0.4f)
                    ticksUntilNextFizzSound = random.nextInt(7) + 2
                }
            }
        }
        // 进食时挤压动画（R196 EntityCubic.l_）
        if (isFeeding()) {
            targetSquish = Math.sin(tickCount / 5.0).toFloat() * 0.1f
        }
    }

    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        if (isAcidicType && !isVulnerableToAcidic(source)) return false
        return super.hurtServer(serverLevel, source, amount)
    }

    protected open fun isVulnerableToAcidic(source: DamageSource): Boolean {
        if (source.`is`(DamageTypes.LAVA)) return true
        // R196 magicAspect：武器被附魔即可伤害灰/黑史莱姆（1.21 附魔武器不产生 magic 伤害类型，
        // 必须直接检查武器是否被附魔，否则对所有武器免疫）
        if (ICPMDamageAspects.hasMagicAspect(source)) return true
        if (lavaImmune) {
            // pudding 对火焰类伤害豁免（含火焰附加武器，其本质仍是附魔武器），
            // 免疫除火/岩浆/魔法外的一切
            if (source.`is`(DamageTypes.ON_FIRE) || source.`is`(DamageTypes.IN_FIRE)
                || source.`is`(DamageTypes.CAMPFIRE) || source.`is`(DamageTypes.HOT_FLOOR)
                || source.`is`(DamageTypes.FIREBALL)) return true
        }
        return false
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        reason: EntitySpawnReason,
        spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        val size = (1 shl random.nextInt(3)).coerceAtMost(maxNaturalSize)
        setSize(size, true)
        return super.finalizeSpawn(level, difficulty, reason, spawnData)
    }

    override fun isDealsDamage(): Boolean = !isTiny() && isEffectiveAi()
}
