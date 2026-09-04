package name.icpm.entity.monster

import name.icpm.common.ICPMMoonPhase
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import java.util.EnumSet
import java.util.UUID

/**
 * 恐狼（R196 EntityDireWolf 移植，数值与机制严格对齐）。
 *
 * R196 源码事实（EntityDireWolf.java，逐行确认）：
 * <ul>
 *   <li>继承 EntityWolf → 可喂骨驯服，仅覆写 getTamingOutcome 概率表：
 *       20% 直接失败并攻击 / 20% 无效果 / 5% 直接成功 /
 *       其余 60% 掷 roll += rand×玩家等级×0.02 → &lt;0.5 攻击、&lt;1.0 无效、≥1.0 成功。</li>
 *   <li>属性：MAX_HEALTH 16（驯服后 24）、ATTACK_DAMAGE 5。</li>
 *   <li>经验 ×2（此处以 xpReward=6 近似原版狼 ~1~3 ×2）。</li>
 *   <li>敌对语义：<b>默认不主动追杀玩家</b>——仅 0.4%/tick 在 4 格内有可攻击玩家时
 *      随机扑咬；被攻击/喂骨失败才锁定玩家；<b>蓝月夜相反：安静不咬人</b>（onUpdate 与
 *      getLivingSound 均排除蓝月夜）。</li>
 *   <li>驯服后：跟随主人、为主人御敌、空手右键坐下/起身、不再攻击玩家。</li>
 * </ul>
 *
 * 实现取舍：保持「独立 Monster」架构（规避 1.21.11 Wolf/TamableAnimal 状态机复杂度），
 * 用私有字段 + NBT 持久化 owner/tamed/sitting 状态，AI 目标以条件谓词切换。
 * 生成仍为全维度黑暗处自然生成（见 ICPMEntities，为 R196 狼群蓝月生成的简化近似）。
 */
class DireWolfEntity(type: EntityType<out DireWolfEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 20.0)
    }

    /** 驯服者 UUID（null = 野狼） */
    private var ownerUuid: UUID? = null
    private var tamed = false
    private var sitting = false
    /** 喂骨失败后的冷却 tick（与驯狼一致，冷却期喂骨只消耗无判定） */
    private var tameCooldownTicks = 0

    override fun registerGoals() {
        this.goalSelector.addGoal(0, FloatGoal(this))
        this.goalSelector.addGoal(1, LeapAtTargetGoal(this, 0.4f))
        this.goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, true))
        // 驯服后跟随主人（乘骑/坐下时不触发）
        this.goalSelector.addGoal(3, TamedFollowOwnerGoal(this))
        this.goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 1.0))
        this.goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        this.goalSelector.addGoal(6, RandomLookAroundGoal(this))
        this.targetSelector.addGoal(1, HurtByTargetGoal(this))
        // 野狼随机扑咬玩家（R196 0.4%/tick、4 格内；见 tick），无需常驻玩家索敌目标
        // 驯服后为主人御敌
        this.targetSelector.addGoal(2, DefendOwnerTargetGoal(this))
    }

    private fun ownerPlayer(): Player? {
        val uuid = ownerUuid ?: return null
        return level().getPlayerByUUID(uuid)
    }

    private fun setTamedBy(player: Player) {
        tamed = true
        ownerUuid = player.uuid
        this.setPersistenceRequired() // 驯服后不随怪物规则自然消失
        this.target = null
        // R196 驯服形态 24 血
        val attr = getAttribute(Attributes.MAX_HEALTH)
        if (attr != null) {
            attr.baseValue = 24.0
        }
        this.heal(this.maxHealth)
        sitting = true // 驯服瞬间坐下（R196 狼语义）
    }

    /** R196 为狼子类：不阻止玩家睡觉（Monster 默认会阻止）。1.21.11 该方法带(ServerLevel, Player)。 */
    override fun isPreventingPlayerRest(serverLevel: ServerLevel, player: Player): Boolean = false

    override fun tick() {
        super.tick()
        if (this.level().isClientSide) {
            return
        }
        // R196 onUpdate_：野狼仅在非蓝月夜、4 格内有可攻击玩家时按 0.4%/tick 概率扑咬。
        // 蓝月夜 = 安静不咬人（修正旧实现"蓝月主动索敌"的倒置）。
        if (!tamed && this.target == null && !ICPMMoonPhase.isBlueMoonNight(this.level())) {
            if (this.random.nextFloat() < 0.004f) {
                val player = this.level().getNearestPlayer(this, 4.0)
                if (player != null && this.canAttack(player)) {
                    this.setTarget(player)
                }
            }
        }
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        val level = this.level()

        // ===== 驯服流程（喂骨） =====
        if (!tamed && stack.`is`(Items.BONE)) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS // 客户端仅挥臂，服务端权威判定
            }
            val serverLevel = level as? ServerLevel ?: return InteractionResult.SUCCESS
            val cooldownActive = this.tickCount < tameCooldownTicks
            var outcome = -2 // 冷却中不掷骰
            if (!cooldownActive) {
                outcome = direTamingOutcome(level, player)
                if (outcome <= 0) {
                    tameCooldownTicks = this.tickCount + 100
                }
            }
            if (!player.abilities.instabuild) {
                stack.shrink(1)
            }
            if (cooldownActive) {
                return InteractionResult.SUCCESS // 冷却期：骨被吃掉但无判定
            }
            val p = this.position()
            if (outcome >= 1) {
                setTamedBy(player)
                serverLevel.sendParticles(ParticleTypes.HEART, p.x, p.y + 0.8, p.z, 8, 0.3, 0.35, 0.3, 0.1)
            } else {
                serverLevel.sendParticles(ParticleTypes.SMOKE, p.x, p.y + 0.8, p.z, 6, 0.25, 0.3, 0.25, 0.05)
                if (outcome < 0 && !ICPMMoonPhase.isBlueMoonDay(level)) {
                    // 20%：失败并扑咬玩家（蓝月夜不攻击）
                    this.setTarget(player)
                }
            }
            return InteractionResult.SUCCESS
        }

        // ===== 已驯服：主人空手右键 坐下/起身 =====
        if (tamed && player.uuid == ownerUuid && stack.isEmpty()) {
            if (!level.isClientSide) {
                sitting = !sitting
                this.target = null
                this.navigation.stop()
            }
            return InteractionResult.SUCCESS
        }
        return super.mobInteract(player, hand)
    }

    /** R196 EntityDireWolf.getTamingOutcome：20/20/5 + 等级加成 */
    private fun direTamingOutcome(level: Level, player: Player): Int {
        var roll = level.random.nextFloat()
        if (roll < 0.2f) {
            return -1
        }
        if (roll < 0.4f) {
            return 0
        }
        if (roll > 0.95f) {
            return 1
        }
        roll += level.random.nextFloat() * player.experienceLevel * 0.02f
        return if (roll < 0.5f) -1 else if (roll < 1.0f) 0 else 1
    }

    // ===== NBT 持久化（1.21.11 ValueOutput/ValueInput；UUID 存字符串，ownerUuid 为 null 时 discard 避免残留旧值） =====

    override fun addAdditionalSaveData(output: net.minecraft.world.level.storage.ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putBoolean("IcpmDireTamed", tamed)
        output.putBoolean("IcpmDireSitting", sitting)
        val uuid = ownerUuid
        if (uuid != null) {
            output.putString("IcpmDireOwner", uuid.toString())
        } else {
            output.discard("IcpmDireOwner")
        }
    }

    override fun readAdditionalSaveData(input: net.minecraft.world.level.storage.ValueInput) {
        super.readAdditionalSaveData(input)
        tamed = input.getBooleanOr("IcpmDireTamed", false)
        sitting = input.getBooleanOr("IcpmDireSitting", false)
        val ownerStr = input.getStringOr("IcpmDireOwner", "")
        ownerUuid = if (ownerStr.isEmpty()) null else runCatching { UUID.fromString(ownerStr) }.getOrNull()
        if (tamed) {
            this.setPersistenceRequired()
            val attr = getAttribute(Attributes.MAX_HEALTH)
            if (attr != null) {
                attr.baseValue = 24.0
            }
        }
    }

    override fun finalizeSpawn(
        level: net.minecraft.world.level.ServerLevelAccessor,
        difficulty: net.minecraft.world.DifficultyInstance,
        reason: EntitySpawnReason,
        spawnData: net.minecraft.world.entity.SpawnGroupData?
    ): net.minecraft.world.entity.SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, reason, spawnData)
        // R196 getExperienceValue ×2（原版狼基础 ~1~3，×2 约 2~6）
        this.xpReward = 6
        return data
    }

    // ===== 驯服后 AI：跟随主人 =====

    private inner class TamedFollowOwnerGoal(private val wolf: DireWolfEntity) : Goal() {
        init {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK))
        }

        override fun canUse(): Boolean {
            if (!wolf.tamed || wolf.sitting) {
                return false
            }
            val owner = wolf.ownerPlayer() ?: return false
            if (owner.isSpectator) {
                return false
            }
            return wolf.distanceToSqr(owner) > 6.0 * 6.0
        }

        override fun canContinueToUse(): Boolean {
            val owner = wolf.ownerPlayer() ?: return false
            return !wolf.sitting && wolf.distanceToSqr(owner) > 2.5 * 2.5
        }

        override fun start() {
            val owner = wolf.ownerPlayer() ?: return
            wolf.navigation.moveTo(owner, 1.15)
        }

        override fun tick() {
            val owner = wolf.ownerPlayer() ?: return
            if (wolf.distanceToSqr(owner) < 36.0 && wolf.navigation.isDone()) {
                wolf.navigation.moveTo(owner, 1.15)
            }
        }

        override fun stop() {
            wolf.navigation.stop()
        }
    }

    /** 驯服后：主人受伤/攻击某生物（近处）时扑向该生物 */
    private inner class DefendOwnerTargetGoal(private val wolf: DireWolfEntity) : Goal() {

        override fun canUse(): Boolean {
            if (!wolf.tamed || wolf.sitting) {
                return false
            }
            val owner = wolf.ownerPlayer() ?: return false
            val threat = when {
                owner.lastHurtByMob != null && wolf.distanceToSqr(owner.lastHurtByMob!!) <= 15.0 * 15.0 ->
                    owner.lastHurtByMob
                owner.lastHurtMob != null && wolf.distanceToSqr(owner.lastHurtMob!!) <= 15.0 * 15.0 ->
                    owner.lastHurtMob
                else -> null
            }
            if (threat == null || threat === wolf || threat === owner) {
                return false
            }
            return threat is LivingEntity && threat.isAlive
        }

        override fun start() {
            val owner = wolf.ownerPlayer() ?: return
            val threat = when {
                owner.lastHurtByMob != null -> owner.lastHurtByMob
                else -> owner.lastHurtMob
            }
            if (threat is LivingEntity && threat.isAlive) {
                wolf.target = threat
            }
        }
    }
}
