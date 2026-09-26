package name.icpm.entity.projectile

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

/**
 * R196 EntityBrick 移植：右键砖/下界砖掷出的抛射物。
 * - gravity 沿用原版 ThrowableProjectile 默认 0.03（R196 getGravityVelocity=0.07；1.21.11 的 getGravity() 为 final 不可覆盖，故取近似，弧线略低，伤害/碎玻璃/投掷行为忠实）
 * - 命中实体造成 2.0 伤害（R196 attackEntityFrom(thrown, 2.0f)）
 * - 命中玻璃板（thinGlass）将其击碎（R196 destroyBlock）
 * - 命中后冒 brickpoof 粒子并消失
 * 渲染复用 ThrownItemRenderer（按携带的砖物品贴图渲染砖/下界砖）
 */
class BrickEntity : ThrowableItemProjectile {

    constructor(type: EntityType<out BrickEntity>, level: Level) : super(type, level)

    constructor(type: EntityType<out BrickEntity>, thrower: LivingEntity, level: Level, stack: ItemStack) :
        super(type, thrower, level, stack)

    override fun getDefaultItem(): Item = Items.BRICK

    override fun onHitEntity(hitResult: EntityHitResult) {
        super.onHitEntity(hitResult)
        hitResult.entity.hurt(this.damageSources().thrown(this, this.getOwner() ?: this), 2.0f)
    }

    override fun onHitBlock(hitResult: BlockHitResult) {
        super.onHitBlock(hitResult)
        // R196：命中玻璃板 → 击碎
        if (level().getBlockState(hitResult.blockPos).`is`(Blocks.GLASS_PANE) && !level().isClientSide) {
            level().destroyBlock(hitResult.blockPos, true)
        }
    }

    override fun onHit(hitResult: HitResult) {
        super.onHit(hitResult)
        if (level() is ServerLevel) {
            val serverLevel = level() as ServerLevel
            repeat(8) {
                serverLevel.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.0, 0.0)
            }
        }
        discard()
    }
}
