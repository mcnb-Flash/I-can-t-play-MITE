package name.icpm.blockentity

import name.icpm.block.ICPMFurnaceBlock
import name.icpm.item.ICPMBucketItem
import name.icpm.item.ICPMBuckets
import name.icpm.item.ICPMItems
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.network.chat.Component
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.FurnaceMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.FurnaceBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.slf4j.LoggerFactory

/**
 * ICPM 熔炉方块实体（原石熔炉 / 黑曜石熔炉）
 *
 * 移植自 ICPM R196 (1.6.4) TileEntityFurnace 的热量系统，适配 1.21.11 Fabric：
 *
 * 1. 热量等级（heat_level）：
 *    - 1 = 木/木炭  - 2 = 煤  - 3 = 岩浆  - 4 = 烈焰棒
 * 2. 熔炉承受上限（maxHeatLevel）：原石熔炉=2、黑曜石熔炉=3。
 *    燃料热量 > 上限 → 无法放入燃料槽（复刻 SlotFuel 检查），
 *    因此"原石熔炉承受不了岩浆"（岩浆=3 > 2）。
 * 3. 矿石所需热量（getHeatLevelRequired）：
 *    秘银矿=3（必须岩浆+黑曜石熔炉）、艾德曼矿=4（必须烈焰棒+更高级熔炉）、
 *    铜/银/金/铁/石英/绿宝石/钻石/红石/青金石矿=2、其余=1。
 * 4. 灭火机制：熔炉正面被水淹没（isFlooded）或被固体方块堵住（isSmothered）→ 熄灭。
 *
 * UI 采用原版熔炉（FurnaceMenu + 原版 GuiFurnace）：实现 Container + ContainerData
 * （索引布局与原版 AbstractFurnaceBlockEntity 一致：0=燃料剩余, 1=燃料总时长, 2=烧炼进度, 3=烧炼总时长）。
 */
class ICPMFurnaceBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(TYPE, pos, state), Container, MenuProvider {

    companion object {
        @JvmStatic
        lateinit var TYPE: BlockEntityType<ICPMFurnaceBlockEntity>

        @JvmStatic
        fun create(pos: BlockPos, state: BlockState): ICPMFurnaceBlockEntity {
            return ICPMFurnaceBlockEntity(pos, state)
        }

        private val LOGGER = LoggerFactory.getLogger("ICPM-Furnace")
    }

    // ==================== 库存与状态 ====================

    /** 槽位：0=输入, 1=燃料, 2=输出 */
    private val items: NonNullList<ItemStack> = NonNullList.withSize(3, ItemStack.EMPTY)

    /** 燃料剩余燃烧时间（tick） */
    var litTimeRemaining: Int = 0
        private set

    /** 当前燃料总燃烧时间 */
    var litTotalTime: Int = 0
        private set

    /** 烧炼已进行时间 */
    var cookingTimer: Int = 0
        private set

    /** 烧炼总时长（由配方 cookingtime 决定，默认 200） */
    var cookingTotalTime: Int = 200
        private set

    /** ICPM 当前热量等级（由当前燃料决定） */
    var heatLevel: Int = 0
        private set

    /** 与 GUI 同步的数据（布局与原版 AbstractFurnaceBlockEntity.dataAccess 一致） */
    private val dataAccess: ContainerData = object : ContainerData {
        override fun get(i: Int): Int = when (i) {
            0 -> litTimeRemaining
            1 -> litTotalTime
            2 -> cookingTimer
            3 -> cookingTotalTime
            else -> 0
        }

        override fun set(i: Int, value: Int) {
            when (i) {
                0 -> litTimeRemaining = value
                1 -> litTotalTime = value
                2 -> cookingTimer = value
                3 -> cookingTotalTime = value
            }
        }

        override fun getCount(): Int = 4
    }

    /** 熔炉承受热量上限（从方块读取；原版熔炉=原石熔炉 maxHeat 2） */
    private val maxHeatLevel: Int
        get() = (level?.getBlockState(worldPosition)?.block as? ICPMFurnaceBlock)?.maxHeatLevel ?: 2

    /** 是否可烧大物品（从方块读取；粘土熔炉=false） */
    private val acceptsLargeItems: Boolean
        get() = (level?.getBlockState(worldPosition)?.block as? ICPMFurnaceBlock)?.acceptsLargeItems ?: true

    /**
     * 是否为"大物品"（ICPM R196 Slot.isLargeItem）：
     * 方块类物品（火把/树苗/花/草/蘑菇/按钮/睡莲/藤蔓除外）、门、船、床
     */
    private fun isLargeItem(stack: ItemStack): Boolean {
        val item = stack.item
        if (item is net.minecraft.world.item.BlockItem) {
            val block = (item as net.minecraft.world.item.BlockItem).block
            return block !is net.minecraft.world.level.block.TorchBlock
                    && block !is net.minecraft.world.level.block.SaplingBlock
                    && block !is net.minecraft.world.level.block.FlowerBlock
                    && block !is net.minecraft.world.level.block.TallGrassBlock
                    && block !is net.minecraft.world.level.block.MushroomBlock
                    && block !is net.minecraft.world.level.block.ButtonBlock
                    && block !is net.minecraft.world.level.block.WaterlilyBlock
                    && block !is net.minecraft.world.level.block.VineBlock
        }
        return item is net.minecraft.world.item.DoubleHighBlockItem
                || item is net.minecraft.world.item.BoatItem
                || item is net.minecraft.world.item.BedItem
    }

    private fun isLit(): Boolean = litTimeRemaining > 0

    // ==================== 每 tick 逻辑（复刻 ICPM R196 updateEntity） ====================

    fun miteTick() {
        val level = level ?: return
        if (level.isClientSide) return

        // ICPM：被水淹 / 正面被固体堵住 → 立即熄灭并清空烧炼进度
        if (litTimeRemaining != 1 && (isFlooded() || isSmothered())) {
            litTimeRemaining = 0
            heatLevel = 0
            cookingTimer = 0
            updateLitState(false)
            setChanged()
            return
        }

        val wasLit = isLit()
        var dirty = false

        // 燃料燃烧
        if (litTimeRemaining > 0) {
            litTimeRemaining--
            if (litTimeRemaining == 0) {
                heatLevel = 0
            }
        }

        val fuel = items[1]
        val input = items[0]
        val hasFuel = !fuel.isEmpty
        val hasInput = !input.isEmpty

        // 尝试点燃：燃料热量须在 1..maxHeatLevel 之间，且不低于输入物品所需热量；大物品需熔炉支持
        if (litTimeRemaining == 0 && hasFuel && hasInput) {
            val recipe = getRecipe(input)
            val fuelHeat = getHeatLevel(fuel)
            val inputOk = !isLargeItem(input) || acceptsLargeItems
            if (inputOk && recipe != null && fuelHeat in 1..maxHeatLevel && fuelHeat >= getHeatLevelRequired(input)) {
                val burnTime = getBurnDuration(fuel)
                if (burnTime > 0) {
                    litTimeRemaining = burnTime
                    litTotalTime = burnTime
                    heatLevel = fuelHeat
                    dirty = true
                    // 必须在 shrink 之前捕获燃料物品！
                    // ItemStack.shrink(1) 在 count 归零时会调用 setEmpty() 把 item 改成 Items.AIR，
                    // 若先 shrink 再读 fuel.item，拿到的是 AIR，getContainerItemForFuel 会返回空物品，
                    // 导致 ICPM 岩浆桶作为燃料被直接消耗而不返还空桶（原版熔炉则在 shrink 前取
                    // getCraftingRemainder 避免此问题）。故此处先保存 fuelItem，shrink 后再据此返还。
                    val fuelItem = fuel.item
                    fuel.shrink(1)
                    if (fuel.isEmpty) {
                        // 燃料消耗后返还容器物品（R196 TileEntityFurnace.getContainerItem）：
                        // ICPM 桶 → 本金属空桶；原版岩浆桶 → 原版空桶；其余 → craftingRemainder。
                        items[1] = getContainerItemForFuel(fuelItem)
                    }
                }
            }
        }

        // 烧炼：需要当前热量 >= 输入物品所需热量，且大物品需熔炉支持
        if (isLit() && hasInput) {
            val recipe = getRecipe(input)
            val inputOk = !isLargeItem(input) || acceptsLargeItems
            if (inputOk && recipe != null && heatLevel >= getHeatLevelRequired(input)) {
                cookingTimer++
                if (cookingTimer >= cookingTotalTime) {
                    cookingTimer = 0
                    cookingTotalTime = getTotalCookTime(level, input)
                    if (burnRecipe(level.registryAccess(), recipe, input)) {
                        dirty = true
                    }
                }
            } else {
                cookingTimer = 0
            }
        } else if (!isLit() && cookingTimer > 0) {
            cookingTimer = 0
        }

        // LIT 状态切换
        if (wasLit != isLit()) {
            dirty = true
            updateLitState(isLit())
        }

        if (dirty) {
            setChanged()
        }
    }

    /** 是否被水淹没（熔炉正面一格是水） */
    private fun isFlooded(): Boolean {
        val level = level ?: return false
        val state = level.getBlockState(worldPosition)
        if (state.block !is FurnaceBlock) return false
        val facing = state.getValue(FurnaceBlock.FACING)
        return level.getBlockState(worldPosition.relative(facing)).fluidState.`is`(FluidTags.WATER)
    }

    /** 是否被固体方块堵住（熔炉正面一格是实心方块） */
    private fun isSmothered(): Boolean {
        val level = level ?: return false
        val state = level.getBlockState(worldPosition)
        if (state.block !is FurnaceBlock) return false
        val facing = state.getValue(FurnaceBlock.FACING)
        val neighborPos = worldPosition.relative(facing)
        val neighbor = level.getBlockState(neighborPos)
        return neighbor.isFaceSturdy(level, neighborPos, facing.opposite)
    }

    /** 切换 LIT 方块状态 */
    private fun updateLitState(lit: Boolean) {
        val level = level ?: return
        val state = level.getBlockState(worldPosition)
        if (state.hasProperty(FurnaceBlock.LIT) && state.getValue(FurnaceBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(FurnaceBlock.LIT, lit), 3)
        }
    }

    // ==================== 热量与配方 ====================

    /** 燃料热量等级（ICPM R196 Item.getHeatLevel） */
    fun getHeatLevel(stack: ItemStack): Int {
        if (stack.isEmpty) return 0
        val item = stack.item
        return when {
            item == Items.BLAZE_ROD -> 4
            // 原版岩浆桶 + 全部 ICPM 金属岩浆桶：热量 3
            item == Items.LAVA_BUCKET || (item is ICPMBucketItem && item.getContent() == Fluids.LAVA) -> 3
            item == Items.COAL -> 2
            // 木炭 / 粪便：热量 1
            item == Items.CHARCOAL || item == ICPMItems.MANURE -> 1
            else -> if (level != null && level!!.fuelValues().isFuel(stack)) 1 else 0
        }
    }

    /** 燃料燃烧时长（原版 fuelValues；ICPM 桶/粪便补表） */
    private fun getBurnDuration(stack: ItemStack): Int {
        val level = level ?: return 0
        val item = stack.item
        if (item is ICPMBucketItem && item.getContent() == Fluids.LAVA) {
            // 全部 ICPM 金属岩浆桶燃烧时长与原版岩浆桶一致（R196：与材质无关）
            return level.fuelValues().burnDuration(ItemStack(Items.LAVA_BUCKET)).coerceAtLeast(20000)
        }
        if (item == ICPMItems.MANURE) return 100
        return level.fuelValues().burnDuration(stack)
    }

    /**
     * 燃料消耗后应返还的容器物品（对齐 R196 TileEntityFurnace.getContainerItem）。
     * - ICPM 桶（岩浆/水/奶/石）：按金属映射返还本金属空桶（ICPMBuckets.emptyOf）
     * - 原版岩浆桶：返还原版空桶（Items.BUCKET）
     * - 其他（如粪便）：走 item.craftingRemainder
     * 这样 ICPM 岩浆桶作为燃料时不会再被直接消耗，而是像原版一样返还空桶。
     */
    private fun getContainerItemForFuel(item: Item): ItemStack {
        if (item is ICPMBucketItem) {
            val empty = ICPMBuckets.emptyOf(item.getMetal())
            if (empty != null) return ItemStack(empty)
        }
        if (item == Items.LAVA_BUCKET) {
            return ItemStack(Items.BUCKET)
        }
        return item.craftingRemainder
    }

    /** 输入物品所需热量等级（ICPM R196 getHeatLevelRequired） */
    fun getHeatLevelRequired(stack: ItemStack): Int {
        if (stack.isEmpty) return 1
        val path = BuiltInRegistries.ITEM.getKey(stack.item)?.path ?: return 1
        return when (path) {
            "adamantium_ore", "deepslate_adamantium_ore" -> 4
            "mithril_ore", "deepslate_mithril_ore" -> 3
            "copper_ore", "deepslate_copper_ore",
            "silver_ore", "deepslate_silver_ore",
            "gold_ore", "deepslate_gold_ore",
            "iron_ore", "deepslate_iron_ore",
            "raw_iron", "raw_copper", "raw_gold",
            "nether_quartz_ore", "emerald_ore", "deepslate_emerald_ore",
            "diamond_ore", "deepslate_diamond_ore",
            "redstone_ore", "deepslate_redstone_ore",
            "lapis_ore", "deepslate_lapis_ore",
            "sandstone" -> 2
            else -> 1
        }
    }

    /** 查询输入物品的烧炼配方（仅在服务端有效；客户端返回 null，输入槽不做配方校验） */
    private fun getRecipe(input: ItemStack): RecipeHolder<SmeltingRecipe>? {
        if (input.isEmpty) return null
        val level = level ?: return null
        val recipeManager = level.recipeAccess() as? RecipeManager ?: return null
        return recipeManager
            .getRecipeFor(RecipeType.SMELTING, SingleRecipeInput(input), level)
            .orElse(null)
    }

    /** 烧炼总时长（配方 cookingtime，默认 200） */
    private fun getTotalCookTime(level: Level, input: ItemStack): Int {
        val holder = getRecipe(input) ?: return 200
        return holder.value().cookingTime()
    }

    /** 烧炼产出（原版 canBurn + burn 合并，含热量已在外层校验） */
    private fun burnRecipe(registryAccess: RegistryAccess, recipe: RecipeHolder<SmeltingRecipe>, input: ItemStack): Boolean {
        val result = recipe.value().assemble(SingleRecipeInput(input), registryAccess)
        if (result.isEmpty) return false
        val output = items[2]
        if (output.isEmpty) {
            items[2] = result.copy()
        } else if (ItemStack.isSameItemSameComponents(output, result)) {
            val maxStack = minOf(getMaxStackSize(), result.maxStackSize)
            if (output.count >= maxStack) return false
            output.grow(result.count)
        } else {
            return false
        }
        input.shrink(1)
        return true
    }

    // ==================== Container 接口 ====================

    override fun getContainerSize(): Int = 3

    override fun isEmpty(): Boolean = items.all { it.isEmpty }

    override fun getItem(slot: Int): ItemStack = items[slot]

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        return ContainerHelper.removeItem(items, slot, amount)
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        return ContainerHelper.takeItem(items, slot)
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        val old = items[slot]
        items[slot] = stack
        if (stack.count > getMaxStackSize()) {
            stack.count = getMaxStackSize()
        }
        if (old !== stack) {
            setChanged()
        }
    }

    override fun stillValid(player: Player): Boolean {
        val level = level ?: return false
        if (level.getBlockEntity(worldPosition) !== this) return false
        return player.distanceToSqr(worldPosition.x + 0.5, worldPosition.y + 0.5, worldPosition.z + 0.5) <= 64.0
    }

    override fun clearContent() {
        items.clear()
    }

    /**
     * 槽位放置限制（复刻 ICPM SlotFuel / isItemValidForSlot）：
     * - 槽0（输入）：放行（配方校验由烧炼时的热量检查承担，客户端无法可靠查配方）
     * - 槽1（燃料）：热量须在 1..maxHeatLevel 之间（原石熔炉放不进岩浆）
     * - 槽2（输出）：不可放入
     */
    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        return when (slot) {
            0 -> !isLargeItem(stack) || acceptsLargeItems
            1 -> getHeatLevel(stack) in 1..maxHeatLevel
            2 -> false
            else -> false
        }
    }

    // ==================== MenuProvider（原版熔炉 UI） ====================

    override fun getDisplayName(): Component {
        // 忠实移植 R196：熔炉标题来自方块自身名称（BlockType.getUnlocalizedName() + ".name"），
        // 故每种熔炉（粘土/硬化粘土/沙石/黑曜石/地狱岩）显示各自名字；
        // 被注入的原版熔炉（非 ICPMFurnaceBlock）视为原石熔炉。
        val block = level?.getBlockState(worldPosition)?.block
        val key = if (block is ICPMFurnaceBlock) {
            "container.icpm." + BuiltInRegistries.BLOCK.getKey(block).path
        } else {
            "container.icpm.cobblestone_furnace"
        }
        return Component.translatable(key)
    }

    override fun createMenu(id: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
        return FurnaceMenu(id, playerInventory, this, dataAccess)
    }

    // ==================== NBT 存取（1.21.11 ValueInput/ValueOutput API） ====================

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        items.clear()
        ContainerHelper.loadAllItems(input, items)
        litTimeRemaining = input.getShortOr("lit_time_remaining", 0).toInt()
        litTotalTime = input.getShortOr("lit_total_time", 0).toInt()
        cookingTimer = input.getShortOr("cooking_time_spent", 0).toInt()
        cookingTotalTime = input.getShortOr("cooking_total_time", 200).toInt()
        heatLevel = input.getShortOr("heat_level", 0).toInt()
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        output.putShort("lit_time_remaining", litTimeRemaining.toShort())
        output.putShort("lit_total_time", litTotalTime.toShort())
        output.putShort("cooking_time_spent", cookingTimer.toShort())
        output.putShort("cooking_total_time", cookingTotalTime.toShort())
        output.putShort("heat_level", heatLevel.toShort())
        ContainerHelper.saveAllItems(output, items)
    }

    /** 破坏方块时掉落内容物（由方块 onRemove 调用） */
    fun dropContent(level: Level, pos: BlockPos) {
        net.minecraft.world.Containers.dropContents(level, pos, this)
    }
}
