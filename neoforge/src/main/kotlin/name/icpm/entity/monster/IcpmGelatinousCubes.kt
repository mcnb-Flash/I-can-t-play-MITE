package name.icpm.entity.monster

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

/** 果冻 (R196 EntityJelly)：胃蛋白酶，攻击倍率 2，自然尺寸上限 4 */
class JellyEntity(type: EntityType<out JellyEntity>, level: Level) : GelatinousCubeEntity(type, level) {
    override val attackStrengthMultiplier: Int = 2
    override val isAcidicType: Boolean = false
    override val isPepsinType: Boolean = true
    override fun getParticleType(): ParticleOptions = ParticleTypes.ITEM_SLIME
}

/** 血块 (R196 EntityBlob)：胃蛋白酶，攻击倍率 3，命中施加饥饿，自然尺寸上限 4 */
class BlobEntity(type: EntityType<out BlobEntity>, level: Level) : GelatinousCubeEntity(type, level) {
    override val attackStrengthMultiplier: Int = 3
    override val isAcidicType: Boolean = false
    override val isPepsinType: Boolean = true
    override val appliesHungerOnHit: Boolean = true
    override fun getParticleType(): ParticleOptions = ParticleTypes.CRIMSON_SPORE
}

/** 软泥 (R196 EntityOoze)：酸性，攻击倍率 3，免疫除岩浆/魔法外伤害，自然尺寸上限 2，可爬墙 */
class OozeEntity(type: EntityType<out OozeEntity>, level: Level) : GelatinousCubeEntity(type, level) {
    override val attackStrengthMultiplier: Int = 3
    override val isAcidicType: Boolean = true
    override val isPepsinType: Boolean = false
    override val maxNaturalSize: Int = 2
    override fun getParticleType(): ParticleOptions = ParticleTypes.ASH

    // R196 EntityOoze: 水平碰撞时爬墙（蜘蛛式同步数据标志）
    private companion object {
        val DATA_CLIMBING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(OozeEntity::class.java, EntityDataSerializers.BOOLEAN)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_CLIMBING, false)
    }

    override fun onClimbable(): Boolean = isClimbing()

    fun isClimbing(): Boolean = entityData.get(DATA_CLIMBING)

    fun setClimbing(climbing: Boolean) {
        entityData.set(DATA_CLIMBING, climbing)
    }

    override fun tick() {
        super.tick()
        if (!level().isClientSide) {
            setClimbing(horizontalCollision)
        }
    }
}

/** 布丁 (R196 EntityPudding)：酸性，攻击倍率 4，免疫除火/岩浆/魔法外伤害，自然尺寸上限 4 */
class PuddingEntity(type: EntityType<out PuddingEntity>, level: Level) : GelatinousCubeEntity(type, level) {
    override val attackStrengthMultiplier: Int = 4
    override val isAcidicType: Boolean = true
    override val isPepsinType: Boolean = false
    override val lavaImmune: Boolean = true
    override fun getParticleType(): ParticleOptions = ParticleTypes.LARGE_SMOKE
}
