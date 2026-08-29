package name.icpm.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.ItemStack

/**
 * ICPM 强化弓。
 * 对应 R196 ItemBow (reinforcement_material)：
 *   - ancient_metal_bow: 速度 x1.1，耐久 64
 *   - mithril_bow:       速度 x1.25，耐久 128
 * 通过在 shootProjectile 中对初速应用倍率实现。
 */
class ICPMBowItem(
    private val velocityMultiplier: Float,
    properties: Properties
) : BowItem(properties) {

    override fun shootProjectile(
        shooter: LivingEntity,
        projectile: Projectile,
        index: Int,
        velocity: Float,
        inaccuracy: Float,
        divergence: Float,
        owner: LivingEntity?
    ) {
        super.shootProjectile(shooter, projectile, index, velocity * velocityMultiplier, inaccuracy, divergence, owner)
    }
}
