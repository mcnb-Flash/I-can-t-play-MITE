package name.icpm.entity.monster

import name.icpm.common.ICPMMoonPhase
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
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

/**
 * 恐狼（R196 EntityDireWolf 移植，数值与机制严格对齐）。
 *
 * 机制（来自 r196 反编译源码逐字确认）：
 * <ul>
 *   <li>属性：MAX_HEALTH 16（驯服形态 24，此处自然生成用 16）、ATTACK_DAMAGE 5</li>
 *   <li>经验 ×2（原版狼基础 ~1~3，×2 约 2~6）</li>
 *   <li>蓝月之夜（isBlueMoonNight）会主动索敌附近玩家（onUpdate_ 中概率设攻击目标）</li>
 *   <li>跳跃扑击 + 近战（R196 狼类攻击方式）</li>
 *   <li>保留“狼”外观（渲染复用 WolfModel），但作为独立 Monster 实现，规避 1.21.11 Wolf 状态机复杂度</li>
 * </ul>
 *
 * 生成：R196 恐狼随狼群（含蓝月触发）生成。Fabric 1.21.11 简化为全维度黑暗处自然生成（见 [name.icpm.entity.ICPMEntities]）。
 */
class DireWolfEntity(type: EntityType<out DireWolfEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 20.0)
    }

    override fun registerGoals() {
        this.goalSelector.addGoal(0, FloatGoal(this))
        this.goalSelector.addGoal(1, LeapAtTargetGoal(this, 0.4f))
        this.goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, true))
        this.goalSelector.addGoal(3, WaterAvoidingRandomStrollGoal(this, 1.0))
        this.goalSelector.addGoal(4, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        this.goalSelector.addGoal(5, RandomLookAroundGoal(this))
        this.targetSelector.addGoal(1, HurtByTargetGoal(this))
        this.targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun tick() {
        super.tick()
        // R196 onUpdate_：蓝月之夜时大幅提升主动索敌概率（NearestAttackableTargetGoal 已持续锁定玩家，
        // 这里仅在蓝月且未锁定目标时额外触发一次近距索敌，避免抖动）。
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            if (ICPMMoonPhase.isBlueMoonNight(this.level()) && this.target == null) {
                val player = this.level().getNearestPlayer(this, 8.0)
                if (player != null) {
                    this.setTarget(player)
                }
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
}
