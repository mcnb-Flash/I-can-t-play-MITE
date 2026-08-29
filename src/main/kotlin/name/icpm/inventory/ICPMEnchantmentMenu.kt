package name.icpm.inventory

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.EnchantmentMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import name.icpm.block.ICPMBlocks

/**
 * 绿宝石附魔台专用菜单。
 *
 * 直接继承 [EnchantmentMenu] 以复用全部附魔逻辑（消耗青金石/经验、读取周围书架加成、附魔等级计算）。
 *
 * 必须 override [stillValid]：原版 EnchantmentMenu.stillValid 会校验"所处方块是否为
 * Blocks.ENCHANTING_TABLE"，而我们的绿宝石附魔台是普通 Block 子类（非 EnchantingTableBlock），
 * 不 override 的话校验失败 → 菜单一打开立即被服务端关闭（客户端表现为"打不开页面"）。
 * 这里改为校验我们自己注册的方块实例。
 */
class ICPMEnchantmentMenu(
    containerId: Int,
    inventory: Inventory,
    private val accessRef: ContainerLevelAccess
) : EnchantmentMenu(containerId, inventory, accessRef) {

    override fun stillValid(player: Player): Boolean {
        val block = ICPMBlocks.EMERALD_ENCHANTING_TABLE ?: return false
        return AbstractContainerMenu.stillValid(accessRef, player, block)
    }
}
