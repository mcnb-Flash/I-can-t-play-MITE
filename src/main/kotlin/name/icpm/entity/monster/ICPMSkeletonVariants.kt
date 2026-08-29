package name.icpm.entity.monster

import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
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
import net.minecraft.world.entity.monster.skeleton.Skeleton
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import name.icpm.item.ICPMItems
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import org.jspecify.annotations.Nullable

/**
 * R196 骷髅变种实体族（继承原版 Skeleton 以避免包内可见方法限制）。
 *
 *  - Longdead（长逝）：远程弓手，箭带缓慢，高生命
 *  - LongdeadGuardian（长逝守卫）：近战持铁斧，高伤害
 *  - BoneLord（骨领主）：近战持剑，高血量、极速
 *  - AnnihilationSkeleton（湮灭骷髅）：重型远程，射速极快、高伤害
 */
abstract class ICPMSkeletonVariant(type: EntityType<out ICPMSkeletonVariant>, level: Level) : Skeleton(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Skeleton.createAttributes()
            .add(Attributes.ATTACK_DAMAGE, 4.0)
    }

    protected abstract val healthValue: Double
    protected abstract val attackValue: Double
    protected abstract val moveSpeedValue: Double
    protected open val isRanged: Boolean = true
    protected open val armorValue: Double = 0.0

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
        this.getAttribute(Attributes.ARMOR)?.baseValue = armorValue
        this.setHealth(healthValue.toFloat())
        if (!isRanged) {
            this.setItemSlot(EquipmentSlot.MAINHAND, meleeWeapon())
            this.reassessWeaponGoal()
        }
        // 生成时必定穿戴的护甲（R196 addRandomEquipment）
        this.equipArmor()
        return data
    }

    /** 生成时穿戴的护甲（默认无） */
    protected open fun equipArmor() {}

    protected open fun meleeWeapon(): ItemStack = ItemStack(Items.IRON_SWORD)

    override fun getAmbientSound(): SoundEvent = SoundEvents.SKELETON_AMBIENT
    override fun getHurtSound(source: DamageSource): SoundEvent = SoundEvents.SKELETON_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.SKELETON_DEATH

    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        if (isExplosionImmune() && (source.`is`(DamageTypes.EXPLOSION) || source.`is`(DamageTypes.PLAYER_EXPLOSION))) {
            return false
        }
        return super.hurtServer(serverLevel, source, amount)
    }

    protected open fun isExplosionImmune(): Boolean = false
}

/** 长逝骷髅（远程弓手，箭带缓慢；R196：必定穿戴远古金属链甲全套） */
class LongdeadEntity(type: EntityType<out LongdeadEntity>, level: Level) : ICPMSkeletonVariant(type, level) {
    override val healthValue: Double = 18.0
    override val attackValue: Double = 4.0
    override val moveSpeedValue: Double = 0.22
    override val isRanged: Boolean = true

    override fun meleeWeapon(): ItemStack = ItemStack(Items.BOW)

    // R196 EntityLongdead.addRandomEquipment：远古金属链甲全套（必定）
    override fun equipArmor() {
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack(ICPMItems.ANCIENT_METAL_CHAINMAIL_HELMET))
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack(ICPMItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE))
        this.setItemSlot(EquipmentSlot.LEGS, ItemStack(ICPMItems.ANCIENT_METAL_CHAINMAIL_LEGGINGS))
        this.setItemSlot(EquipmentSlot.FEET, ItemStack(ICPMItems.ANCIENT_METAL_CHAINMAIL_BOOTS))
    }

    override fun performRangedAttack(target: LivingEntity, velocity: Float) {
        super.performRangedAttack(target, velocity)
        if (level() is ServerLevel) {
            target.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 140, 0))
        }
    }
}

/** 长逝守卫（近战持斧，高伤害；R196：必定穿戴远古金属链甲全套） */
class LongdeadGuardianEntity(type: EntityType<out LongdeadGuardianEntity>, level: Level) : ICPMSkeletonVariant(type, level) {
    override val healthValue: Double = 24.0
    override val attackValue: Double = 7.0
    override val moveSpeedValue: Double = 0.24
    override val isRanged: Boolean = false
    override val armorValue: Double = 4.0

    override fun meleeWeapon(): ItemStack = ItemStack(ICPMItems.ANCIENT_METAL_AXE)

    // R196：守卫同古尸，远古金属链甲全套（必定）
    override fun equipArmor() {
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack(ICPMItems.ANCIENT_METAL_CHAINMAIL_HELMET))
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack(ICPMItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE))
        this.setItemSlot(EquipmentSlot.LEGS, ItemStack(ICPMItems.ANCIENT_METAL_CHAINMAIL_LEGGINGS))
        this.setItemSlot(EquipmentSlot.FEET, ItemStack(ICPMItems.ANCIENT_METAL_CHAINMAIL_BOOTS))
    }

    /**
     * 古尸守卫动态切换（R196 EntityLongdeadGuardian.onLivingUpdate 的忠实移植）。
     *
     * 默认持远古金属斧（近战），当目标距离 > 6 时切换为远古金属弓（远程），
     * 距离 < 5 时切回斧；切换后 reassessWeaponGoal 让 AbstractSkeleton 自动启用对应 AI。
     */
    override fun tick() {
        if (!level().isClientSide && tickCount % 10 == 0) {
            val t = target
            if (t != null) {
                val dist = distanceTo(t)
                val mainItem = mainHandItem.item
                if (dist > 6.0 && mainItem != Items.BOW && mainItem != ICPMItems.ANCIENT_METAL_BOW) {
                    setItemSlot(EquipmentSlot.MAINHAND, ItemStack(ICPMItems.ANCIENT_METAL_BOW))
                    reassessWeaponGoal()
                } else if (dist < 5.0 && mainItem != ICPMItems.ANCIENT_METAL_AXE) {
                    setItemSlot(EquipmentSlot.MAINHAND, ItemStack(ICPMItems.ANCIENT_METAL_AXE))
                    reassessWeaponGoal()
                }
            }
        }
        super.tick()
    }
}

/** 骨领主（近战持剑，高血量极速；生成时随机手持远古金属战锤或远古金属剑） */
class BoneLordEntity(type: EntityType<out BoneLordEntity>, level: Level) : ICPMSkeletonVariant(type, level) {
    override val healthValue: Double = 60.0
    override val attackValue: Double = 10.0
    override val moveSpeedValue: Double = 0.3
    override val isRanged: Boolean = false
    override val armorValue: Double = 6.0

    override fun meleeWeapon(): ItemStack =
        if (random.nextBoolean()) ItemStack(ICPMItems.ANCIENT_METAL_WAR_HAMMER)
        else ItemStack(ICPMItems.ANCIENT_METAL_SWORD)
}

/** 湮灭骷髅（重型远程，射速快） */
class AnnihilationSkeletonEntity(type: EntityType<out AnnihilationSkeletonEntity>, level: Level) : ICPMSkeletonVariant(type, level) {
    override val healthValue: Double = 26.0
    override val attackValue: Double = 6.0
    override val moveSpeedValue: Double = 0.27
    override val isRanged: Boolean = true
    override val armorValue: Double = 5.0

    override fun meleeWeapon(): ItemStack = ItemStack(Items.BOW)

    override fun getAttackInterval(): Int = 10
    override fun getHardAttackInterval(): Int = 5
}
