package name.icpm.entity.monster

import name.icpm.entity.ai.ZombieDigGoal
import name.icpm.item.ICPMItems
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.SpawnGroupData
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
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor

/**
 * 矿工僵尸（ICPM 血月机制新增实体）。
 *
 * - 手持铁镐或铁战锤；穿戴铜 ~ 远古金属随机金属的四件锁链装备
 * - **挖掘行为完全交给 R196 忠实实现 [ZombieDigGoal]**（原 `EntityAnimalWatcher` 机制）：
 *   只在「已锁定目标、距离合适、却无路径可接近」时挖开挡路方块——典型是挖掉玩家脚下支柱、
 *   或挖穿墙壁；冷却 = `300 × 方块硬度 ÷ (1 + 工具速度 × 0.5)`（R196 getCooloffForBlock），
 *   每 tick 递减冷却、到 0 时进度 +1，累计 10 次破坏方块（R196 partiallyDestroyBlock）。
 *   R196 里挖掘是「僵尸」类怪的通用能力，并不是某个专属实体，故本实体与普通僵尸共用同一套规则。
 *   （此前自创的「3×3×3 扫描 + 无条件挖掘 + 3/4 速度 + 等级≤3」实现已整体废弃。）
 * - 手持剑 / 短棒 / 镰刀时按 R196 规则不挖掘（本实体始终持镐或战锤，故正常挖掘）
 * - 非血月仅矿洞（非露天）刷新；血月之夜地面也会刷新
 */
class MinerZombieEntity(type: EntityType<out MinerZombieEntity>, level: Level) : Monster(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 26.0)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.ARMOR, 4.0)

        /** 锁链头盔金属池（铜/银/金/铁/远古金属） */
        private val HELMET_METALS = arrayOf(
            ICPMItems.COPPER_CHAINMAIL_HELMET, ICPMItems.SILVER_CHAINMAIL_HELMET,
            ICPMItems.GOLD_CHAINMAIL_HELMET, ICPMItems.IRON_CHAINMAIL_HELMET,
            ICPMItems.ANCIENT_METAL_CHAINMAIL_HELMET
        )

        private val CHEST_METALS = arrayOf(
            ICPMItems.COPPER_CHAINMAIL_CHESTPLATE, ICPMItems.SILVER_CHAINMAIL_CHESTPLATE,
            ICPMItems.GOLD_CHAINMAIL_CHESTPLATE, ICPMItems.IRON_CHAINMAIL_CHESTPLATE,
            ICPMItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE
        )

        private val LEGS_METALS = arrayOf(
            ICPMItems.COPPER_CHAINMAIL_LEGGINGS, ICPMItems.SILVER_CHAINMAIL_LEGGINGS,
            ICPMItems.GOLD_CHAINMAIL_LEGGINGS, ICPMItems.IRON_CHAINMAIL_LEGGINGS,
            ICPMItems.ANCIENT_METAL_CHAINMAIL_LEGGINGS
        )

        private val BOOTS_METALS = arrayOf(
            ICPMItems.COPPER_CHAINMAIL_BOOTS, ICPMItems.SILVER_CHAINMAIL_BOOTS,
            ICPMItems.GOLD_CHAINMAIL_BOOTS, ICPMItems.IRON_CHAINMAIL_BOOTS,
            ICPMItems.ANCIENT_METAL_CHAINMAIL_BOOTS
        )
    }

    override fun registerGoals() {
        goalSelector.addGoal(1, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, false))
        // R196 挖掘 AI（EntityAnimalWatcher 机制）：与移动并列，排在闲逛之前
        goalSelector.addGoal(3, ZombieDigGoal(this))
        goalSelector.addGoal(4, WaterAvoidingRandomStrollGoal(this, 0.8))
        goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(6, RandomLookAroundGoal(this))
        targetSelector.addGoal(1, HurtByTargetGoal(this))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        reason: EntitySpawnReason,
        spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, reason, spawnData)
        // 主手：铁镐 / 铁战锤 二选一
        val weapon = if (random.nextBoolean()) ItemStack(Items.IRON_PICKAXE) else ItemStack(ICPMItems.IRON_WAR_HAMMER)
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon)
        // 四件锁链装备：每件随机金属（铜 ~ 远古金属）
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack(HELMET_METALS[random.nextInt(HELMET_METALS.size)]))
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack(CHEST_METALS[random.nextInt(CHEST_METALS.size)]))
        this.setItemSlot(EquipmentSlot.LEGS, ItemStack(LEGS_METALS[random.nextInt(LEGS_METALS.size)]))
        this.setItemSlot(EquipmentSlot.FEET, ItemStack(BOOTS_METALS[random.nextInt(BOOTS_METALS.size)]))
        return data
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.ZOMBIE_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.ZOMBIE_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.ZOMBIE_DEATH
}
