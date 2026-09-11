package name.icpm.entity.monster

import name.icpm.common.ICPMMaterialHelper
import name.icpm.item.ICPMItems
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
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
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.jspecify.annotations.Nullable

/**
 * 矿工僵尸（ICPM 血月机制新增实体）。
 *
 * - 手持铁镐或铁战锤；穿戴铜 ~ 远古金属随机金属的四件锁链装备
 * - 可挖掘任意「挖掘等级 <= 3」的方块（石头/铜/银/金/铁/远古金属/黑曜石等，
 *   秘银4、艾德曼5、钻石4 不可挖；基岩等不可破坏方块除外）
 * - 挖掘速度 = 玩家手持对应工具速度的 3/4（约每刻进度 toolSpeed / (hardness * 30) * 0.75）
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

        /** 挖掘等级上限（铁镐 / 铁战锤 = 3 级） */
        private const val MAX_MINE_LEVEL = 3

        /** 挖掘速度系数：玩家手持对应工具的 3/4 */
        private const val MINE_SPEED_FACTOR = 0.75f

        /** 挖掘检查间隔（tick） */
        private const val MINE_INTERVAL = 5
    }

    /** 当前正在挖掘的方块位置 */
    private var miningPos: BlockPos? = null

    /** 当前挖掘进度（0 ~ 1） */
    private var miningProgress: Float = 0f

    override fun registerGoals() {
        goalSelector.addGoal(1, FloatGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.0, false))
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

    override fun tick() {
        super.tick()
        val serverLevel = level()
        if (serverLevel !is ServerLevel) return
        if (tickCount % MINE_INTERVAL != 0) return
        tryMine(serverLevel)
    }

    /**
     * 挖掘逻辑：锁定一个周围可挖方块并累积破坏进度，
     * 进度满后破坏并播放破坏音效/粒子。
     */
    private fun tryMine(serverLevel: ServerLevel) {
        val target = findMineableBlock(serverLevel) ?: run {
            clearMining(serverLevel)
            return
        }
        if (miningPos != target) {
            miningPos = target
            miningProgress = 0f
        }

        val state = serverLevel.getBlockState(target)
        val hardness = state.getDestroySpeed(serverLevel, target)
        if (hardness < 0f) { // 不可破坏（基岩/屏障等）
            clearMining(serverLevel)
            return
        }
        // 玩家手持对应工具每刻进度 ≈ toolSpeed / (hardness * 30)；矿工僵尸为 3/4
        val toolSpeed = getMainHandItem().getDestroySpeed(state).coerceAtLeast(0.5f)
        val rate = (toolSpeed / (hardness * 30f)) * MINE_SPEED_FACTOR * MINE_INTERVAL
        miningProgress += rate

        // 破坏裂纹动画（0~10 级）
        serverLevel.destroyBlockProgress(id, target, (miningProgress * 10f).toInt().coerceIn(0, 10))

        if (miningProgress >= 1f) {
            serverLevel.levelEvent(2001, target, Block.getId(state))
            serverLevel.destroyBlock(target, true, this, 0)
            clearMining(serverLevel)
        }
    }

    private fun clearMining(serverLevel: ServerLevel) {
        if (miningPos != null) {
            serverLevel.destroyBlockProgress(id, miningPos!!, -1)
        }
        miningPos = null
        miningProgress = 0f
    }

    /**
     * 在自身周围 3x3x3 范围内寻找第一个可挖掘方块。
     * 排除脚下（避免挖空自己站立的方块）。
     */
    private fun findMineableBlock(serverLevel: ServerLevel): BlockPos? {
        val center = blockPosition()
        val below = center.below()
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    val pos = center.offset(dx, dy, dz)
                    if (pos == below) continue
                    val state = serverLevel.getBlockState(pos)
                    if (!isMineable(state, serverLevel, pos)) continue
                    return pos
                }
            }
        }
        return null
    }

    /** 判定方块是否可被矿工僵尸挖掘（挖掘等级 <= 3 且可破坏） */
    private fun isMineable(state: BlockState, serverLevel: ServerLevel, pos: BlockPos): Boolean {
        if (state.isAir) return false
        // 不可破坏方块（基岩/屏障/水等硬度为负或 0 的特殊方块）
        val hardness = state.getDestroySpeed(serverLevel, pos)
        if (hardness < 0f) return false
        if (state.`is`(Blocks.BEDROCK) || state.`is`(Blocks.BARRIER)) return false
        // 挖掘等级 <= 3
        return ICPMMaterialHelper.getMinHarvestLevel(state.block) <= MAX_MINE_LEVEL
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.ZOMBIE_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.ZOMBIE_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.ZOMBIE_DEATH
}
