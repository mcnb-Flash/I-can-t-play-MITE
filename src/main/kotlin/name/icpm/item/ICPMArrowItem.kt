package name.icpm.item

import name.icpm.entity.projectile.ICPMArrowEntity
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.item.ArrowItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level
import java.util.function.Consumer

/**
 * ICPM 自定义箭矢物品。
 * 对应 R196 ItemArrow / 1.18 ArrowItem：
 *   - damage: 箭矢基础伤害（setBaseDamage）
 *   - recoverChance: 回收率（落地后成为可拾取物品的概率）
 */
class ICPMArrowItem(
    val damage: Int,
    val recoverChance: Float,
    properties: Properties
) : ArrowItem(properties) {

    override fun createArrow(
        level: Level,
        stack: ItemStack,
        shooter: LivingEntity,
        weaponStack: ItemStack?
    ): AbstractArrow {
        val arrow = ICPMArrowEntity.create(level, shooter, stack, weaponStack ?: ItemStack.EMPTY)
        arrow.setBaseDamage(damage.toDouble())
        return arrow
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltipDisplay: TooltipDisplay,
        consumer: Consumer<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, consumer, tooltipFlag)
        consumer.accept(
            Component.translatable("tooltip.icpm.arrow.damage", damage)
                .setStyle(Style.EMPTY.withColor(0x55FF55))
        )
        consumer.accept(
            Component.translatable("tooltip.icpm.arrow.recovery", (recoverChance * 100).toInt())
                .setStyle(Style.EMPTY.withColor(0xAAAAAA))
        )
    }
}
