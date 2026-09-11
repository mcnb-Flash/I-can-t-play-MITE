package name.icpm.blockentity

import name.icpm.block.ICPMStrongboxBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

/**
 * ICPM 金属箱（强箱）方块实体（1.6.4 TileEntityStrongbox）
 *
 * - 27 槽容器，UI 用原版箱子界面（ChestMenu）
 * - 记录放置者（owner，UUID），仅所有者（或创造模式）能打开
 * - 外观为金属块贴图的箱子方块模型（1.6.4 强箱同款）
 */
class ICPMStrongboxBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(TYPE, pos, state), Container, MenuProvider {

    companion object {
        @JvmStatic
        lateinit var TYPE: BlockEntityType<ICPMStrongboxBlockEntity>

        @JvmStatic
        fun create(pos: BlockPos, state: BlockState): ICPMStrongboxBlockEntity {
            return ICPMStrongboxBlockEntity(pos, state)
        }
    }

    private val items: NonNullList<ItemStack> = NonNullList.withSize(27, ItemStack.EMPTY)

    /** 所有者 UUID（放置者） */
    var owner: UUID? = null
        private set

    fun setOwner(player: Player) {
        this.owner = player.uuid
        setChanged()
    }

    fun isOwner(player: Player): Boolean {
        return owner == null || owner == player.uuid
    }

    // ==================== Container ====================

    override fun getContainerSize(): Int = 27

    override fun isEmpty(): Boolean = items.all { it.isEmpty }

    override fun getItem(slot: Int): ItemStack = items[slot]

    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(items, slot, amount)

    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(items, slot)

    override fun setItem(slot: Int, stack: ItemStack) {
        items[slot] = stack
        if (stack.count > getMaxStackSize()) {
            stack.count = getMaxStackSize()
        }
        setChanged()
    }

    override fun stillValid(player: Player): Boolean {
        val level = level ?: return false
        if (level.getBlockEntity(worldPosition) !== this) return false
        return player.distanceToSqr(worldPosition.x + 0.5, worldPosition.y + 0.5, worldPosition.z + 0.5) <= 64.0
    }

    override fun clearContent() {
        items.clear()
    }

    // ==================== MenuProvider（原版箱子 UI） ====================

    override fun getDisplayName(): Component {
        val metal = (level?.getBlockState(worldPosition)?.block as? ICPMStrongboxBlock)?.metalName
        return Component.translatable("container.icpm.strongbox.${metal ?: "iron"}")
    }

    override fun createMenu(id: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
        return ChestMenu.threeRows(id, playerInventory, this)
    }

    // ==================== NBT ====================

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        items.clear()
        ContainerHelper.loadAllItems(input, items)
        owner = input.getString("owner").orElse(null)?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        ContainerHelper.saveAllItems(output, items)
        owner?.let { output.putString("owner", it.toString()) }
    }

    /** 破坏方块时掉落内容物 */
    fun dropContent(level: net.minecraft.world.level.Level, pos: BlockPos) {
        net.minecraft.world.Containers.dropContents(level, pos, this)
    }
}
