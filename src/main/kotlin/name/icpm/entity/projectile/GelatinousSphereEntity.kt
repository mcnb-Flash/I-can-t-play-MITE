package name.icpm.entity.projectile

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import name.icpm.common.ICPMDissolveHelper
import name.icpm.item.GelatinousSphereItem
import name.icpm.item.ICPMGelatinousItems

/**
 * R196 EntityGelatinousSphere 移植：投掷出去的凝胶球。
 * 命中生物造成 1 + 类型攻击力 伤害；命中方块可溶解方块。
 */
class GelatinousSphereEntity : ThrowableItemProjectile {

    constructor(type: EntityType<out GelatinousSphereEntity>, level: Level) : super(type, level)

    constructor(type: EntityType<out GelatinousSphereEntity>, thrower: LivingEntity, level: Level, stack: ItemStack) :
        super(type, thrower, level, stack)

    constructor(type: EntityType<out GelatinousSphereEntity>, x: Double, y: Double, z: Double, level: Level, stack: ItemStack) :
        super(type, x, y, z, level, stack)

    fun getSubtype(): Int {
        val item = getItem().item
        return if (item is GelatinousSphereItem) item.subtype else 0
    }

    fun getAttackDamage(): Float {
        val item = getItem().item
        return if (item is GelatinousSphereItem) item.attackDamage else 1.0f
    }

    private fun getImpactParticle(): ParticleOptions = when (getSubtype()) {
        1 -> ParticleTypes.ITEM_SLIME
        2 -> ParticleTypes.CRIMSON_SPORE
        3 -> ParticleTypes.ASH
        4 -> ParticleTypes.LARGE_SMOKE
        else -> ParticleTypes.ITEM_SLIME
    }

    override fun getDefaultItem(): Item = ICPMGelatinousItems.slimeSphere

    override fun onHitEntity(hitResult: EntityHitResult) {
        super.onHitEntity(hitResult)
        hitResult.entity.hurt(this.damageSources().thrown(this, this.getOwner()), 1.0f + getAttackDamage())
    }

    override fun onHitBlock(hitResult: BlockHitResult) {
        super.onHitBlock(hitResult)
        if (level() !is ServerLevel) return
        val serverLevel = level() as ServerLevel
        // 溶解命中点附近 1.5 格半径内的可溶解方块
        val center = hitResult.blockPos
        val area: AABB = AABB(
            center.x - 1.5, center.y - 1.5, center.z - 1.5,
            center.x + 2.5, center.y + 2.5, center.z + 2.5
        )
        val toDissolve = mutableListOf<BlockPos>()
        for (pos in BlockPos.betweenClosed(area)) {
            val p = pos.immutable()
            if (ICPMDissolveHelper.getDissolvePeriod(level(), p) >= 0) {
                toDissolve.add(p)
            }
        }
        for (p in toDissolve) {
            ICPMDissolveHelper.dissolveBlock(serverLevel, p)
        }
    }

    override fun onHit(hitResult: HitResult) {
        super.onHit(hitResult)
        if (level() !is ServerLevel) return
        val serverLevel = level() as ServerLevel
        val particle = getImpactParticle()
        for (i in 0 until 8) {
            serverLevel.addParticle(particle, x, y, z, 0.0, 0.0, 0.0)
        }
        discard()
    }
}
