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
 *
 * ⚠ 铁律（2026-08-30 · 弓射不出 ICPM 箭的根因）：
 * 绝不能在 getDefaultPickupItem() 里调用 getPickupItem()。
 * 原版 AbstractArrow(EntityType, Level) 构造器会用它初始化 pickupItemStack：
 *     this.pickupItemStack = this.getDefaultPickupItem();
 * 而 getPickupItem() 的实现是 `this.pickupItemStack.copy()` —— 此刻字段仍为 null，
 * 直接 NullPointerException，箭矢实体根本构造不出来，表现为「拉满弦也射不出箭」。
 * 必须像原版 Arrow 一样直接返回一个兜底物品，不许回溯自身状态。
 */
class ICPMArrowEntity : AbstractArrow {

    constructor(type: EntityType<out ICPMArrowEntity>, level: Level) : super(type, level)

    constructor(type: EntityType<out ICPMArrowEntity>, level: Level, shooter: LivingEntity, arrowStack: ItemStack, weaponStack: ItemStack) :
        super(type, shooter, level, arrowStack, weaponStack)

    constructor(type: EntityType<out ICPMArrowEntity>, level: Level, x: Double, y: Double, z: Double, arrowStack: ItemStack, weaponStack: ItemStack) :
        super(type, x, y, z, level, arrowStack, weaponStack)

    /**
     * 兜底拾取物。会在实体构造阶段被调用，此时 pickupItemStack 尚未赋值，
     * 只能返回常量，不能读取自身任何字段。
     */
    override fun getDefaultPickupItem(): ItemStack = ItemStack(ICPMItems.FLINT_ARROW)

    /**
     * 落地时按回收率决定箭矢是否保留为可拾取物品。
     * 回收判定失败则直接消失（R196 getChanceOfRecovery 语义）。
     */
    override fun onHitBlock(hitResult: BlockHitResult) {
        if (level() is ServerLevel) {
            val arrowStack = pickupStackSafe()
            val recoverChance = (arrowStack.item as? ICPMArrowItem)?.recoverChance ?: 1.0f
            if (random.nextFloat() > recoverChance) {
                discard()
                return
            }
        }
        super.onHitBlock(hitResult)
    }

    /** 读取当前拾取物，任何异常都退化为空气（避免回收判定把整支箭搞崩）。 */
    private fun pickupStackSafe(): ItemStack = runCatching { getPickupItem() }.getOrDefault(ItemStack.EMPTY)

    companion object {
        fun create(level: Level, shooter: LivingEntity, arrowStack: ItemStack, weaponStack: ItemStack): ICPMArrowEntity {
            return ICPMArrowEntity(ICPMEntities.ICPM_ARROW, level, shooter, arrowStack, weaponStack)
        }
    }
}
