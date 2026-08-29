package name.icpm.entity.monster

import name.icpm.common.ICPMTension
import name.icpm.item.ICPMToolProperties
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
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
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

/**
 * 土元素（R196 EntityEarthElemental）：由岩石/泥土构成的大型立方体怪物。
 * 木变体（plank）受火焰伤害 x2，其余变体免疫火焰/岩浆并在接触时转化为熔岩态（magma）。
 */
class EarthElementalEntity(type: EntityType<out EarthElementalEntity>, level: Level) : Monster(type, level) {

    companion object {
        private val DATA_TYPE: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            EarthElementalEntity::class.java, EntityDataSerializers.INT
        )
        private val DATA_MAGMA: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            EarthElementalEntity::class.java, EntityDataSerializers.BOOLEAN
        )

        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 12.0)
            .add(Attributes.FOLLOW_RANGE, 20.0)
    }

    /** 土元素挖矿冷却（tick）。张力越高冷却越短、挖得越快（G3，见 {@link #miningCooldownForTension}）。 */
    private var miningCooldown: Int = 0

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_TYPE, EarthElementalType.STONE.id)
        builder.define(DATA_MAGMA, false)
    }

    override fun registerGoals() {
        this.goalSelector.addGoal(1, FloatGoal(this))
        this.goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, false))
        this.goalSelector.addGoal(3, WaterAvoidingRandomStrollGoal(this, 0.6))
        this.goalSelector.addGoal(4, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        this.goalSelector.addGoal(5, RandomLookAroundGoal(this))
        this.targetSelector.addGoal(1, HurtByTargetGoal(this))
        this.targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        reason: EntitySpawnReason,
        spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        val below = level.getBlockState(blockPosition().below()).block
        val elementType = when {
            below == Blocks.STONE -> if (random.nextBoolean()) EarthElementalType.STONE else EarthElementalType.PLANK
            below == Blocks.DEEPSLATE -> EarthElementalType.STONE
            below == Blocks.CLAY -> EarthElementalType.CLAY
            below == Blocks.TERRACOTTA -> EarthElementalType.CLAY_HARDENED
            below == Blocks.END_STONE -> EarthElementalType.END_STONE
            below == Blocks.NETHERRACK -> EarthElementalType.NETHERRACK
            below == Blocks.OBSIDIAN -> EarthElementalType.OBSIDIAN
            level.getLevel().dimension() == Level.NETHER -> {
                if (random.nextInt(3) == 0) EarthElementalType.OBSIDIAN else EarthElementalType.NETHERRACK
            }
            else -> EarthElementalType.STONE
        }
        setElementType(elementType)
        if (level.getLevel().dimension() == Level.NETHER) {
            getAttribute(Attributes.MAX_HEALTH)?.baseValue = 60.0
            setMagma(true)
        }
        setHealth(maxHealth)
        xpReward = 20
        return super.finalizeSpawn(level, difficulty, reason, spawnData)
    }

    fun getElementType(): EarthElementalType = EarthElementalType.fromId(entityData.get(DATA_TYPE))

    fun setElementType(type: EarthElementalType) {
        entityData.set(DATA_TYPE, type.id)
    }

    fun isMagma(): Boolean = entityData.get(DATA_MAGMA)

    fun setMagma(magma: Boolean) {
        if (magma && isWood()) return
        entityData.set(DATA_MAGMA, magma)
    }

    /** 木变体（R196 isWood）：受火焰伤害，不会转化为岩浆态。 */
    fun isWood(): Boolean = getElementType() == EarthElementalType.PLANK

    /** 转化为熔岩态（R196 convertToMagma）。木变体不转化。 */
    fun convertToMagma() {
        if (!isWood()) setMagma(true)
    }

    override fun tick() {
        super.tick()
        if (level() is ServerLevel) {
            // G3：挖矿冷却吃张力——有目标且冷却完毕时挖开正前方阻挡方块逼近玩家
            if (miningCooldown > 0) {
                miningCooldown--
            } else if (target != null) {
                if (tryDig()) {
                    miningCooldown = miningCooldownForTension()
                }
            }
            if (!isWood() && !isMagma()) {
                if (isInLava() || isOnFire()) {
                    convertToMagma()
                }
            }
            // 熔岩态：散发岩浆粒子（R196 magma 视觉）
            if (isMagma() && random.nextInt(6) == 0) {
                (level() as ServerLevel).sendParticles(
                    ParticleTypes.LAVA,
                    x + (random.nextDouble() - 0.5) * 0.8,
                    y + 0.2 + random.nextDouble() * 0.6,
                    z + (random.nextDouble() - 0.5) * 0.8,
                    1, 0.0, 0.0, 0.0, 0.0
                )
            }
        }
    }

    /**
     * G3：土元素"挖开路上的方块"（infx 计划：土元素挖矿冷却吃张力）。
     *
     * <p>R196 土元素虽无显式挖矿 AI，但 {@code getBlockHarvestLevel()} 表明其具备采掘材质能力；
     * infx 计划将其发展为"挖矿冷却吃张力"：张力越高，挖开挡路方块的间隔越短（逼近越快）。
     *
     * <p>挖矿目标 = 正前方身体/头部高度（0~2）的第一个可挖方块，直接破坏（音效 + 粒子 + 掉落）。
     * 禁止挖基岩/屏障等不可破坏方块。
     *
     * @return 是否成功挖掉一个方块（成功才进入冷却）
     */
    private fun tryDig(): Boolean {
        val sl = level() as ServerLevel
        val base = blockPosition()
        val dir: Direction = direction
        val front = base.relative(dir)
        for (dy in 0..2) {
            val p = front.above(dy)
            val state = sl.getBlockState(p)
            if (state.isAir) continue
            if (state.`is`(Blocks.BEDROCK) || state.`is`(Blocks.BARRIER)) continue
            val hardness = state.getDestroySpeed(sl, p)
            if (hardness < 0f) continue
            sl.levelEvent(2001, p, Block.getId(state))
            sl.destroyBlock(p, true, this, 0)
            return true
        }
        return false
    }

    /**
     * G3：挖矿冷却随张力缩短。
     * 张力 0 → 160 tick（约 8 秒），张力 1.5 → 40 tick（约 2 秒）。
     * 张力已内含难度系数（困难×1 / 其余×0.75）与月相因子。
     */
    private fun miningCooldownForTension(): Int {
        val tension = ICPMTension.getTension(level(), this)
        return 160 - (tension * 80).toInt()
    }

    /**
     * 受击逻辑（R196 EntityEarthElemental.isImmuneTo + attackEntityFrom，独立判定）。
     *
     * 火焰伤害：
     *  - 木变体（plank）受火焰伤害 x2；
     *  - 非木变体免疫火焰/岩浆，接触时转化为熔岩态（magma）。
     *
     * 其余伤害（R196 isImmuneTo）：仅以下来源可造成伤，其余一律免疫：
     *  - 坠落（FALL）/ 虚空（OUT_OF_WORLD）；
     *  - 铁傀儡近战（攻击者实体为 IronGolem）；
     *  - 爆炸（EXPLOSION / PLAYER_EXPLOSION）；
     *  - 近战武器：非木变体需 稿类(PICKAXE) 或 战锤(WAR_HAMMER)，
     *              木变体需 斧类(AXE) 或 战斧(BATTLE_AXE) 方可造成伤害，
     *              其余武器（剑/匕首/徒手等）均无效。
     */
    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        // —— 火焰伤害单独处理 ——
        if (source.`is`(DamageTypes.LAVA) || source.`is`(DamageTypes.ON_FIRE) || source.`is`(DamageTypes.IN_FIRE)
            || source.`is`(DamageTypes.CAMPFIRE) || source.`is`(DamageTypes.HOT_FLOOR) || source.`is`(DamageTypes.FIREBALL)
        ) {
            if (isWood()) {
                // R196 attackEntityFrom：木变体受火焰伤害 x2
                return super.hurtServer(serverLevel, source, amount * 2.0f)
            }
            if (source.`is`(DamageTypes.LAVA) || source.`is`(DamageTypes.ON_FIRE) || source.`is`(DamageTypes.IN_FIRE)) {
                convertToMagma()
            }
            return false
        }
        // —— 非火焰：R196 isImmuneTo 可伤来源 ——
        // 坠落 / 虚空 始终可伤
        if (source.`is`(DamageTypes.FALL) || source.`is`(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurtServer(serverLevel, source, amount)
        }
        // 铁傀儡近战可伤
        val attacker = source.entity
        if (attacker is IronGolem) {
            return super.hurtServer(serverLevel, source, amount)
        }
        // 爆炸可伤
        if (source.`is`(DamageTypes.EXPLOSION) || source.`is`(DamageTypes.PLAYER_EXPLOSION)) {
            return super.hurtServer(serverLevel, source, amount)
        }
        // 近战武器判定：所需工具类型依变体而定
        if (attacker is LivingEntity) {
            val category = ICPMToolProperties.getToolCategory(attacker.mainHandItem)
                ?: ICPMToolProperties.getToolCategory(attacker.offhandItem)
            val effective = if (isWood()) {
                // 木材质变种：斧类 / 战斧
                category == ICPMToolProperties.ToolCategory.AXE || category == ICPMToolProperties.ToolCategory.BATTLE_AXE
            } else {
                // 非木类变种（石 / 黏土 / 硬化黏土 / 末地石 / 地狱岩 / 黑曜石）：稿类 / 战锤
                category == ICPMToolProperties.ToolCategory.PICKAXE || category == ICPMToolProperties.ToolCategory.WAR_HAMMER
            }
            if (effective) {
                return super.hurtServer(serverLevel, source, amount)
            }
        }
        // 其余来源（剑、匕首、徒手、箭等）免疫
        return false
    }

    override fun addAdditionalSaveData(valueOutput: ValueOutput) {
        super.addAdditionalSaveData(valueOutput)
        valueOutput.putInt("ElementType", getElementType().id)
        valueOutput.putBoolean("Magma", isMagma())
    }

    override fun readAdditionalSaveData(valueInput: ValueInput) {
        super.readAdditionalSaveData(valueInput)
        setElementType(EarthElementalType.fromId(valueInput.getIntOr("ElementType", EarthElementalType.STONE.id)))
        setMagma(valueInput.getBooleanOr("Magma", false))
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.STONE_STEP
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.IRON_GOLEM_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.IRON_GOLEM_DEATH
    override fun playStepSound(pos: BlockPos, state: BlockState) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 0.15f, 1.0f)
    }
}
