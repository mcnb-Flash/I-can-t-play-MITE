package name.icpm.entity.projectile

import name.icpm.entity.ICPMEntities
import name.icpm.item.ICPMArrowItem
import name.icpm.item.ICPMItems
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult

/**
 * ICPM 自定义箭矢实体。
 * 对应 R196 EntityArrow / 1.18 自定义 ArrowItem.createArrow。
 * 伤害由 ICPMArrowItem.damage 决定（setBaseDamage），
 * 回收率由 ICPMArrowItem.recoverChance 决定（落地时按概率消失/保留）。
 */
class ICPMArrowEntity : AbstractArrow {

    constructor(type: EntityType<out ICPMArrowEntity>, level: Level) : super(type, level)

    constructor(type: EntityType<out ICPMArrowEntity>, level: Level, shooter: LivingEntity, arrowStack: ItemStack, weaponStack: ItemStack) :
        super(type, shooter, level, arrowStack, weaponStack)

    constructor(type: EntityType<out ICPMArrowEntity>, level: Level, x: Double, y: Double, z: Double, arrowStack: ItemStack, weaponStack: ItemStack) :
        super(type, x, y, z, level, arrowStack, weaponStack)

    override fun getDefaultPickupItem(): ItemStack {
        val stack = getPickupItem()
        return if (stack.isEmpty) ItemStack(ICPMItems.FLINT_ARROW) else stack
    }

    /**
     * 落地时按回收率决定箭矢是否保留为可拾取物品。
     * 回收判定失败则直接消失（R196 getChanceOfRecovery 语义）。
     */
    override fun onHitBlock(hitResult: BlockHitResult) {
        val arrowStack = getPickupItem()
        val recoverChance = (arrowStack.item as? ICPMArrowItem)?.recoverChance ?: 1.0f
        if (level() is ServerLevel && random.nextFloat() > recoverChance) {
            discard()
            return
        }
        super.onHitBlock(hitResult)
    }

    companion object {
        fun create(level: Level, shooter: LivingEntity, arrowStack: ItemStack, weaponStack: ItemStack): ICPMArrowEntity {
            return ICPMArrowEntity(ICPMEntities.ICPM_ARROW, level, shooter, arrowStack, weaponStack)
        }
    }
}
