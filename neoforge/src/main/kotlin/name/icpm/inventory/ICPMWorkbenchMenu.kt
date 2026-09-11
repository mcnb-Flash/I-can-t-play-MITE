package name.icpm.inventory

import name.icpm.ICPM
import name.icpm.block.BlockICPMWorkbench
import name.icpm.common.EnumQuality
import name.icpm.common.ICPMCraftCooldowns
import name.icpm.common.ICPMExperience
import name.icpm.component.CraftPreviewComponent
import name.icpm.component.QualityComponent
import name.icpm.common.CraftingTimeHelper
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.StackedItemContents
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.inventory.ResultContainer
import net.minecraft.world.inventory.ResultSlot
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import kotlin.math.abs

/**
 * ICPM 工作台容器菜单
 *
 * 复刻 R196 的 ICPMContainerCrafting + SlotCrafting 机制：
 * - 3x3 合成格 + 结果槽
 * - 品质选择（右键结果槽循环切换，影响合成时间和成品品质）
 * - 合成进度（基于 ICPMCraftingTimeMixin 公式，每 tick 推进）
 * - 合成冷却（取走成品后 10 tick 冷却）
 * - 工作台等级限制（材料/成品要求的工作台等级不得超过当前工作台）
 *
 * 合成流程：
 * 1. 放入材料 → 自动匹配配方 → 显示结果预览（不可取走）
 * 2. 右键结果槽 → 切换品质（重新计算合成时间）
 * 3. 左键结果槽 → 开始合成（进度条开始填充）
 * 4. 合成完成 → 结果附加品质 → 可取走
 * 5. 取走成品 → 消耗材料 → 进入冷却
 */
class ICPMWorkbenchMenu(
    syncId: Int,
    playerInventory: Inventory,
    private val levelAccess: ContainerLevelAccess,
    val workbenchTier: Int
) : AbstractContainerMenu(ICPM.ICPM_WORKBENCH_MENU, syncId) {

    /** 打开工作台的玩家（用于等级→品质下限判定） */
    private val ownerPlayer: Player = playerInventory.player

    // ==================== 合成状态 ====================

    /**
     * 数据槽：
     * 0 = craftingState (0=空闲, 1=合成完成, 2=合成中)
     * 1 = craftingProgress (当前进度 tick)
     * 2 = totalCraftTime (总时间 tick)
     * 3 = craftingQuality (选中品质 ordinal)
     */

    /** 合成是否正在进行 */
    var isCrafting: Boolean = false
        private set

    /** 当前合成进度（tick） */
    var craftingProgress: Int = 0
        private set

    /** 合成所需总时间（tick） */
    var totalCraftTime: Int = 0
        private set

    /** 当前选中的品质等级 */
    var craftingQuality: Int = EnumQuality.AVERAGE.qualityLevel
        private set

    /** 当前配方的基础难度（用于合成时间计算） */
    private var recipeDifficulty: Float = 25f

    /** 合成完成标记（成品可取走） */
    var isCraftingComplete: Boolean = false
        private set

    /** 缓存当前结果物品（用于检测配方变化） */
    private var lastResultItem: ItemStack = ItemStack.EMPTY

    /** 自动续合成标记：取走一次成品后，若材料仍够则持续合成并直接入背包 */
    private var autoCrafting: Boolean = false

    // ==================== 容器 ====================

    private val craftSlots = ICPMCraftingContainer()
    val resultSlots = ResultContainer()

    companion object {
        @JvmStatic
        fun create(syncId: Int, playerInventory: Inventory, pos: BlockPos): ICPMWorkbenchMenu {
            val level = playerInventory.player.level()
            val tier = (level.getBlockState(pos).block as? BlockICPMWorkbench)?.tier ?: 1
            return ICPMWorkbenchMenu(syncId, playerInventory, ContainerLevelAccess.create(level, pos), tier)
        }

        @JvmStatic
        fun streamCodec(): net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, BlockPos> {
            return net.minecraft.network.codec.StreamCodec.of(
                { buf, pos -> buf.writeBlockPos(pos) },
                { buf -> buf.readBlockPos() }
            )
        }

        /**
         * 根据物品 ID 获取所需工作台等级（忠实移植 R196 RecipeHelper 语义）：
         *
         * R196 判定规则：
         * 1. 金属锭/粒（ItemIngot）：不设置工作台等级 → 任何工作台（含燧石台 tier 0）均可合成。
         * 2. 金属工作台自身：需要「材料弱一级」的工作台（RecipeHelper.next_strongest_material）：
         *    铜/银/金台(durability 4.0)→燧石台；铁台(8.0)→铜/银/金台；远古金属台(16.0)→铁台；
         *    秘银台(64.0)→远古金属台；艾德曼台(256.0)→秘银台。
         * 3. 金属制品（工具/装备等）：需要「自身材料」对应等级的工作台。
         *
         * 铜/银/金三者 durability 相等（均 4.0），因此工作台品质与合成等级完全同级。
         */
        fun getRequiredWorkbenchTier(item: ItemStack): Int {
            val holder = item.item.builtInRegistryHolder()
            val id = holder.key().identifier().path
            return when {
                // === 金属工作台自身：需要「材料弱一级」的工作台 ===
                // 艾德曼工作台（tier 6 台）→ 秘银台(tier 5)可合成
                id == "adamantium_workbench" -> 5
                // 秘银工作台（tier 5 台）→ 远古金属台(tier 4)可合成
                id == "mithril_workbench" -> 4
                // 远古金属工作台（tier 4 台）→ 铁台(tier 3)可合成
                id == "ancient_metal_workbench" -> 3
                // 铁工作台（tier 3 台）→ 铜/银/金台(tier 1)可合成
                id == "iron_workbench" -> 1
                // 铜/银/金工作台（tier 1 台，三者同级）→ 燧石台(tier 0)可合成
                id == "copper_workbench" || id == "silver_workbench" || id == "gold_workbench" -> 0
                // === 金属锭/粒：R196 ItemIngot 不设等级 → 任何工作台可合成（含熔炉冶炼所得）===
                id.endsWith("_ingot") || id.endsWith("_nugget") -> 0
                // === 金属制品：需要自身材料对应等级的工作台 ===
                id.contains("adamantium") || id.contains("netherite") -> 6
                id.contains("mithril") || id.contains("diamond") -> 5
                id.contains("ancient_metal") -> 4
                id.contains("iron") -> 3
                // 银/金 与 铜 同级(tier 1)：铜/银/金 工具/装备需对应工作台
                id.contains("silver") || id.contains("gold") -> 1
                id.contains("copper") -> 1
                id.contains("flint") -> 0
                else -> 0
            }
        }

        /**
         * 判断物品是否需要工作台才能合成
         *
         * 原木等基础木材必须在燧石工作台或更高级工作台中加工，
         * 不允许在背包 2x2 合成格中直接合成。
         */
        @JvmStatic
        fun requiresWorkbench(item: ItemStack): Boolean {
            val id = item.item.builtInRegistryHolder().key().identifier().path
            return id.contains("_log") || id.contains("log_")
        }
    }

    // ==================== 合成容器内部类 ====================

    private inner class ICPMCraftingContainer : CraftingContainer {
        private val items = Array(9) { ItemStack.EMPTY }

        override fun getContainerSize(): Int = 9
        override fun isEmpty(): Boolean = items.all { it.isEmpty }
        override fun getItem(index: Int): ItemStack = items[index]
        override fun removeItem(index: Int, count: Int): ItemStack {
            val stack = items[index]
            if (!stack.isEmpty) {
                val result = stack.split(count)
                if (stack.isEmpty) items[index] = ItemStack.EMPTY
                setChanged()
                return result
            }
            return ItemStack.EMPTY
        }
        override fun removeItemNoUpdate(index: Int): ItemStack {
            val stack = items[index].copy()
            items[index] = ItemStack.EMPTY
            return stack
        }
        override fun setItem(index: Int, stack: ItemStack) {
            items[index] = stack
            setChanged()
        }
        override fun setChanged() {
            updateResult()
        }
        override fun stillValid(player: Player): Boolean = true
        override fun getWidth(): Int = 3
        override fun getHeight(): Int = 3
        override fun getItems(): List<ItemStack> = items.toList()
        override fun fillStackedContents(stackedContents: StackedItemContents) {
            for (stack in items) {
                stackedContents.accountSimpleStack(stack)
            }
        }
        override fun clearContent() {
            for (i in items.indices) items[i] = ItemStack.EMPTY
        }
    }

    // ==================== 自定义结果槽 ====================

    /**
     * ICPM 合成结果槽
     *
     * 复刻 R196 SlotCrafting 行为：
     * - mayPickup: 仅在合成完成时允许取走
     * - mayPlace: 禁止手动放入物品
     * - onTake: 消耗材料、重置合成状态、触发冷却
     */
    private inner class ICPMCraftResultSlot(
        player: Player,
        container: CraftingContainer,
        resultContainer: ResultContainer,
        slot: Int, x: Int, y: Int
    ) : Slot(resultContainer, slot, x, y) {

        override fun mayPlace(stack: ItemStack): Boolean = false

        override fun mayPickup(player: Player): Boolean = isCraftingComplete

        override fun onTake(player: Player, stack: ItemStack) {
            // 剥离合成预览组件，避免泄漏进玩家背包（取走后即转为真实成品，仅保留品质）
            stack.remove(ICPM.CRAFT_PREVIEW_COMPONENT)
            // 品质经验成本（高于 average 时扣除；max 已由经验钳制，正常必可负担）
            val xpCost = this@ICPMWorkbenchMenu.getSelectedQualityXpCost()
            if (xpCost > 0 && player.totalExperience >= xpCost) {
                player.giveExperiencePoints(-xpCost)
            }
            // 金属币合成：扣除经验（与 takeResult 一致，覆盖 shift 点击取走路径）
            val coinXp = name.icpm.common.ICPMCoinHelper.xpForCoinByItem(stack)
            if (coinXp > 0 && player.totalExperience >= coinXp) {
                player.giveExperiencePoints(-coinXp)
            }
            // 消耗合成材料（每格消耗 1 个）
            val secondaryShield = captureShieldAttachSecondary()
            for (i in 0 until craftSlots.containerSize) {
                val craftItem = craftSlots.getItem(i)
                if (!craftItem.isEmpty) {
                    craftItem.shrink(1)
                    craftSlots.setItem(i, craftItem)
                }
            }
            // 重置合成状态
            resetCraftingState()
            // 装盾配方：返还 -25% 耐久的盾牌
            emitShieldAttachSecondary(player, secondaryShield)
            // 标记合成冷却（10 tick）
            if (!player.level().isClientSide) {
                ICPMCraftCooldowns.markCrafted(player)
            }
            // 更新配方
            updateResult()
        }
    }

    // ==================== 初始化 ====================

    init {
        // 结果槽（索引 0）
        addSlot(ICPMCraftResultSlot(playerInventory.player, craftSlots, resultSlots, 0, 124, 35))

        // 3x3 合成格（索引 1-9）
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                addSlot(object : Slot(craftSlots, col + row * 3, 30 + col * 18, 17 + row * 18) {
                    override fun mayPlace(stack: ItemStack): Boolean = true
                    override fun setChanged() { updateResult() }
                })
            }
        }

        // 玩家背包（索引 10-36）
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        // 玩家快捷栏（索引 37-45）
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }

        // ===== 数据槽：服务端 → 客户端同步 =====
        // 每个 DataSlot 封装一个合成状态字段
        addDataSlot(object : DataSlot() {
            override fun get(): Int = if (isCraftingComplete) 1 else if (isCrafting) 2 else 0
            override fun set(value: Int) {
                isCraftingComplete = value == 1
                isCrafting = value == 2
            }
        })
        addDataSlot(object : DataSlot() {
            override fun get(): Int = craftingProgress
            override fun set(value: Int) { craftingProgress = value }
        })
        addDataSlot(object : DataSlot() {
            override fun get(): Int = totalCraftTime
            override fun set(value: Int) { totalCraftTime = value }
        })
        addDataSlot(object : DataSlot() {
            override fun get(): Int = craftingQuality
            override fun set(value: Int) { craftingQuality = value }
        })
    }

    // ==================== 核心逻辑 ====================

    override fun stillValid(player: Player): Boolean {
        return levelAccess.evaluate { level, pos ->
            val block = level.getBlockState(pos).block
            block is BlockICPMWorkbench &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }.orElse(false)
    }

    override fun slotsChanged(container: net.minecraft.world.Container) {
        updateResult()
    }

    /**
     * 每次 tick 由 broadcastChanges 调用（服务端侧）
     * 推进合成进度
     */
    private fun advanceCrafting() {
        if (!isCrafting || isCraftingComplete) return
        craftingProgress++
        if (craftingProgress >= totalCraftTime) {
            completeCrafting()
            // 自动续合成：完成后立即取走并继续下一轮，直到材料耗尽或背包满
            if (autoCrafting) {
                if (performTake(ownerPlayer)) {
                    if (!resultSlots.getItem(0).isEmpty) {
                        // 材料仍够 → 直接开始下一轮（绕过冷却，节奏由合成时间保证）
                        isCrafting = true
                        craftingProgress = 0
                        recalculateCraftingTime()
                    } else {
                        autoCrafting = false
                    }
                } else {
                    autoCrafting = false
                }
            }
        }
    }

    /**
     * 覆写 broadcastChanges，在同步前推进合成进度
     * AbstractContainerMenu.tick() 每 tick 调用此方法
     * DataSlot.get() 从服务端字段读取，broadcastChanges 自动同步到客户端
     * 客户端 DataSlot.set() 被调用后更新本地字段
     */
    override fun broadcastChanges() {
        advanceCrafting()
        super.broadcastChanges()
    }

    /**
     * 更新合成结果
     *
     * 当合成矩阵变化时调用：
     * - 匹配配方 → 显示结果预览
     * - 检查工作台等级
     * - 计算合成时间（基于品质和难度）
     * - 如果结果物品变化，重置合成状态
     */
    private fun updateResult() {
        levelAccess.execute { level, pos ->
            if (level.isClientSide) return@execute
            val craftingInput = craftSlots.asCraftInput()
            val recipeManager = level.server?.recipeManager ?: return@execute
            val recipeHolder = recipeManager.getRecipeFor(RecipeType.CRAFTING, craftingInput, level)

            if (recipeHolder.isPresent) {
                val holder = recipeHolder.get()
                val result = holder.value().assemble(craftingInput, level.registryAccess())
                val requiredTier = getRequiredTier(craftingInput, result)

                if (requiredTier > workbenchTier) {
                    // 工作台等级不足
                    if (!lastResultItem.isEmpty) resetCraftingState()
                    resultSlots.setItem(0, ItemStack.EMPTY)
                    lastResultItem = ItemStack.EMPTY
                    recipeDifficulty = 25f
                    autoCrafting = false
                } else {
                    // 检查结果物品是否变化
                    val resultChanged = !ItemStack.isSameItem(lastResultItem, result)
                    if (resultChanged && isCrafting) {
                        resetCraftingState()
                    }
                    // 计算配方难度（基于输出物品所需工作台等级，对齐 R196 物品难度）
                    recipeDifficulty = calculateRecipeDifficulty(result)
                    // 将品质钳制进当前玩家可合成区间 [min, max]
                    clampCraftingQuality()
                    // 构造预览物品：附着"当前选中品质 + 合成预览组件"，使结果槽即时显示
                    // 当前品质状态、经验消耗与可切换品质提示（对齐 R196 SlotCrafting）
                    val preview = result.copy()
                    applyPreviewComponents(preview)
                    resultSlots.setItem(0, preview)
                    lastResultItem = result.copy()
                    // 重新计算合成时间（已基于上面的 recipeDifficulty）
                    recalculateCraftingTime()
                }
            } else {
                if (!lastResultItem.isEmpty) resetCraftingState()
                resultSlots.setItem(0, ItemStack.EMPTY)
                lastResultItem = ItemStack.EMPTY
                recipeDifficulty = 25f
                autoCrafting = false
            }
            broadcastChanges()
        }
    }

    /**
     * 开始合成
     *
     * 由客户端 WorkbenchCraftPacket(START_CRAFT) 触发
     */
    fun startCrafting(player: Player) {
        if (isCrafting || isCraftingComplete) return
        if (resultSlots.getItem(0).isEmpty) return
        if (ICPMCraftCooldowns.hasCraftCooldown(player)) {
            val remaining = ICPMCraftCooldowns.getCraftCooldownRemaining(player)
            player.displayClientMessage(
                Component.literal("合成冷却中... ${remaining}t"),
                true
            )
            return
        }
        val result = resultSlots.getItem(0)
        val requiredTier = getRequiredWorkbenchTier(result)
        if (requiredTier > workbenchTier) {
            player.displayClientMessage(
                Component.literal("需要等级 $requiredTier 或更高的工作台！(当前: $workbenchTier)"),
                true
            )
            return
        }
        // 金属币合成：检查经验是否足够
        val coinXp = name.icpm.common.ICPMCoinHelper.xpForCoinByItem(result)
        if (coinXp > 0) {
            val totalXp = player.totalExperience
            if (totalXp < coinXp) {
                player.displayClientMessage(
                    Component.literal("合成金属币需要 $coinXp 经验值！(当前: $totalXp)"),
                    true
                )
                return
            }
        }
        isCrafting = true
        craftingProgress = 0
        recalculateCraftingTime()
        // 开启自动取走：合成完成后成品直接进背包（无需二次点击确认），
        // 材料仍够则自动续合成，直到材料耗尽或背包满。
        autoCrafting = true
    }

    /**
     * 取走合成成品
     *
     * 由客户端 WorkbenchCraftPacket(TAKE_RESULT) 触发
     * - 将成品放入玩家背包
     * - 消耗合成材料
     * - 重置合成状态
     */
    /**
     * 实际取走成品：扣经验、放入背包、消耗材料、重置状态、刷新预览。
     * 返回 true 表示成品已取走（背包有空位），false 表示背包已满未取走（用于终止自动续合成）。
     */
    private fun performTake(player: Player): Boolean {
        if (!isCraftingComplete) return false
        val result = resultSlots.getItem(0)
        if (result.isEmpty) return false
        // 品质经验成本（R196 crafting_experience_cost：高于 average 品质时扣除，average 及以下为 0）
        val xpCost = getSelectedQualityXpCost()
        if (xpCost > 0) {
            if (player.totalExperience < xpCost) {
                player.displayClientMessage(
                    Component.literal("经验不足，无法以该品质合成！需要 $xpCost 经验值。"),
                    true
                )
                return false
            }
            player.giveExperiencePoints(-xpCost)
        }
        // 金属币合成：扣除经验
        val coinXp = name.icpm.common.ICPMCoinHelper.xpForCoinByItem(result)
        if (coinXp > 0) {
            if (player.totalExperience < coinXp) {
                player.displayClientMessage(
                    Component.literal("经验不足，无法取走金属币！"),
                    true
                )
                return false
            }
            player.giveExperiencePoints(-coinXp)
        }
        // 金属币分解：返还经验（与背包合成栏 CoinXpRefundMixin 一致）
        val refundXp = result.get(name.icpm.ICPM.COIN_XP_COMPONENT)
        if (refundXp != null && refundXp > 0) {
            player.giveExperiencePoints(refundXp)
            result.remove(name.icpm.ICPM.COIN_XP_COMPONENT)
        }
        // 将成品放入玩家背包（剥离仅用于结果槽展示的合成预览组件）；背包满则停止自动合成
        val copy = result.copy()
        copy.remove(ICPM.CRAFT_PREVIEW_COMPONENT)
        if (!player.inventory.add(copy)) {
            return false
        }
        // 消耗合成材料
        val secondaryShield = captureShieldAttachSecondary()
        for (i in 0 until craftSlots.containerSize) {
            val craftItem = craftSlots.getItem(i)
            if (!craftItem.isEmpty) {
                craftItem.shrink(1)
                craftSlots.setItem(i, craftItem)
            }
        }
        // 清空结果槽并重置状态
        resultSlots.setItem(0, ItemStack.EMPTY)
        resetCraftingState()
        // 装盾配方：返还 -25% 耐久的盾牌
        emitShieldAttachSecondary(player, secondaryShield)
        ICPMCraftCooldowns.markCrafted(player)
        updateResult()
        broadcastChanges()
        return true
    }

    /**
     * 取走合成成品（由客户端 WorkbenchCraftPacket(TAKE_RESULT) 触发）。
     * 取走后若材料仍够同一配方，则开启自动续合成（持续合成并直接入背包）。
     */
    fun takeResult(player: Player) {
        if (!performTake(player)) return
        // 取走后若同一配方仍可合成（网格材料仍够），开启自动续合成
        if (!resultSlots.getItem(0).isEmpty) {
            autoCrafting = true
            isCrafting = true
            craftingProgress = 0
            recalculateCraftingTime()
        } else {
            autoCrafting = false
        }
    }

    /** average 品质 ordinal（难度翻倍的基准） */
    private val AVG_ORD = EnumQuality.AVERAGE.ordinal
    /** wretched 品质 ordinal（品质下限绝对底） */
    private val MIN_ORD = EnumQuality.WRETCHED.ordinal
    /** legendary 品质 ordinal（物品允许的最高品质） */
    private val MAX_ORD = EnumQuality.LEGENDARY.ordinal

    /** 笨拙诅咒（R196 clumsiness）：等效等级 −20（getMinCraftingQuality）+ 品质经验花费 ×2 */
    private fun clumsyCursed(): Boolean =
            name.icpm.curse.ICPMCurseManager.isCursed(ownerPlayer, name.icpm.curse.ICPMCurse.CLUMSINESS, true)

    /** 由玩家等级决定的最低品质 ordinal（R196 getMinCraftingQuality 的等级部分） */
    private fun getMinQualityOrdinal(): Int {
        var level = ICPMExperience.getExperienceLevel(ICPMExperience.getExperience(ownerPlayer))
        if (clumsyCursed()) level -= 20 // R196：笨拙按等效低 20 级计算基准品质
        return ICPMExperience.getMinCraftingQualityOrdinal(level, AVG_ORD, MIN_ORD)
    }

    /** 由玩家经验决定的最高品质 ordinal（R196 getMaxCraftingQuality 的 XP 成本部分） */
    private fun getMaxQualityOrdinal(): Int {
        val exp = ICPMExperience.getExperience(ownerPlayer)
        return ICPMExperience.getMaxCraftingQualityOrdinal(exp, recipeDifficulty, MAX_ORD, AVG_ORD,
                getMinQualityOrdinal(), if (clumsyCursed()) 2 else 1)
    }

    /** 将当前选中品质钳制进 [min, max] 区间（R196 setCraftingResultIndex 的 clamp 行为） */
    private fun clampCraftingQuality() {
        craftingQuality = craftingQuality.coerceIn(getMinQualityOrdinal(), getMaxQualityOrdinal())
    }

    /** 计算指定品质 ordinal 对应的合成额外经验成本（average 及以下为 0） */
    private fun getQualityXpCost(ordinal: Int): Int {
        val q = EnumQuality.fromOrdinal(ordinal)
        if (q.ordinal <= AVG_ORD) return 0
        val qad = ICPMExperience.getQualityAdjustedDifficulty(recipeDifficulty, q.ordinal, AVG_ORD)
        return ICPMExperience.getCraftingExperienceCost(qad, if (clumsyCursed()) 2 else 1)
    }

    /** 计算当前选中品质对应的合成额外经验成本（average 及以下为 0） */
    private fun getSelectedQualityXpCost(): Int = getQualityXpCost(craftingQuality)

    /**
     * 物品是否支持品质系统。
     *
     * 仅工具/武器/盔甲类等**可损坏**物品支持品质（耐久与攻击伤害由此派生）。
     * 木板、木棍等不可损坏物品不应被赋予品质——否则它们会带品质组件却没有对应耐久/伤害修正，
     * 既无意义又会让玩家误以为能"合成有品质的木板"。
     */
    private fun supportsQuality(item: ItemStack): Boolean {
        return !item.isEmpty && item.isDamageableItem()
    }

    /**
     * 装盾配方（icpm:shield_attach）第二产出捕获：
     * 若当前结果物品带 [ICPM.SHIELD_ATTACHED] 组件，从合成格取出盾牌，
     * 计算「-25% 最大耐久」后的损坏盾牌返回（盾牌不消失，可继续用/再合成，约 4 次后损坏）。
     * 必须在消耗材料之前调用，以读取盾牌当前损坏值与最大耐久。
     */
    private fun captureShieldAttachSecondary(): ItemStack {
        val result = resultSlots.getItem(0)
        if (result.isEmpty || !result.has(ICPM.SHIELD_ATTACHED)) {
            return ItemStack.EMPTY
        }
        for (i in 0 until craftSlots.containerSize) {
            val it = craftSlots.getItem(i)
            if (!it.isEmpty && it.item === Items.SHIELD) {
                val max = it.maxDamage
                if (max <= 0) return ItemStack.EMPTY
                val newDamage = (it.damageValue + (max * 0.25).toInt()).coerceAtMost(max)
                val out = ItemStack(Items.SHIELD, 1)
                out.damageValue = newDamage
                return out
            }
        }
        return ItemStack.EMPTY
    }

    /** 将装盾配方返还的损坏盾牌放入玩家背包；背包满则掉落在玩家位置。 */
    private fun emitShieldAttachSecondary(player: Player, secondary: ItemStack) {
        if (secondary.isEmpty) return
        if (!player.inventory.add(secondary)) {
            player.drop(secondary, false)
        }
    }

    /**
     * 将"当前选中（已钳制）品质"与"合成预览信息"写入给定物品，
     * 使其在结果槽中即可通过 tooltip 显示当前品质状态、经验消耗与可切换品质提示。
     * 对齐 R196：SlotCrafting.modifyStackForRightClicks 会即时把品质写入结果槽物品。
     */
    private fun applyPreviewComponents(item: ItemStack) {
        // 非品质物品（木板、木棍等）：不赋予品质、不显示品质预览
        if (!supportsQuality(item)) return
        val minQ = getMinQualityOrdinal()
        val maxQ = getMaxQualityOrdinal()
        val clamped = craftingQuality.coerceIn(minQ, maxQ)
        item.set(ICPM.QUALITY_COMPONENT, QualityComponent.of(EnumQuality.fromOrdinal(clamped)))
        item.set(ICPM.CRAFT_PREVIEW_COMPONENT, CraftPreviewComponent(getQualityXpCost(clamped), minQ, maxQ))
    }

    /**
     * 循环切换品质等级（R196 SlotCrafting.tryIncrementCraftingResultIndex）
     * 在 [min, max] 区间内递增，超过 max 后回绕到 min；不再允许循环到 max 之外。
     * 由客户端 WorkbenchCraftPacket(CYCLE_QUALITY) 触发。
     */
    fun cycleQuality(player: Player) {
        // 非品质物品不支持切换品质（木板、木棍等）
        val preview = resultSlots.getItem(0)
        if (preview.isEmpty || !supportsQuality(preview)) return
        val minQ = getMinQualityOrdinal()
        val maxQ = getMaxQualityOrdinal()
        craftingQuality = if (craftingQuality + 1 > maxQ) minQ else craftingQuality + 1
        craftingQuality = craftingQuality.coerceIn(minQ, maxQ)
        val q = EnumQuality.fromOrdinal(craftingQuality)
        val cost = getSelectedQualityXpCost()
        player.displayClientMessage(
            Component.literal("合成品质: ${q.descriptor}" + if (cost > 0) " (经验消耗: $cost)" else ""),
            true
        )
        // 重新计算合成时间，并更新结果槽预览物品的品质/预览组件（即时反馈 tooltip）
        if (!preview.isEmpty) {
            applyPreviewComponents(preview)
            resultSlots.setItem(0, preview)
            recalculateCraftingTime()
            // 如果正在合成，重置进度
            if (isCrafting) {
                craftingProgress = 0
            }
        }
    }

    /**
     * 合成完成
     *
     * - 将选中品质写入结果物品
     * - 播放合成完成音效
     */
    private fun completeCrafting() {
        isCraftingComplete = true
        isCrafting = false
        // 将品质写入结果物品
        val result = resultSlots.getItem(0)
        if (!result.isEmpty) {
            // 合成完成 → 剥离预览组件，成为真实成品（removed 时据此区分成品/预览，避免丢失或刷物品）
            result.remove(ICPM.CRAFT_PREVIEW_COMPONENT)
            // 仅对支持品质的物品（工具/武器/盔甲）赋予品质；木板/木棍等不赋予
            if (supportsQuality(result)) {
                // R196：成品品质钳制进 [min, max] 区间（含等级下限与经验上限）
                clampCraftingQuality()
                val quality = EnumQuality.fromOrdinal(craftingQuality)
                result.set(ICPM.QUALITY_COMPONENT, QualityComponent.of(quality))
            }
            resultSlots.setItem(0, result)
        }
        // 播放音效
        levelAccess.execute { level, _ ->
            if (!level.isClientSide) {
                level.playSound(null, BlockPos.ZERO, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f)
            }
        }
    }

    /**
     * 重置合成状态
     */
    private fun resetCraftingState() {
        isCrafting = false
        craftingProgress = 0
        totalCraftTime = 0
        isCraftingComplete = false
    }

    /**
     * 根据当前品质重新计算合成时间
     *
     * 公式对齐 R196 EntityPlayerSP.getCraftingPeriod：
     *   period = calcUnmodifiedCraftingPeriod(品质调整难度) / (1 + 等级采集加成 + 工作台材质加成)
     *   - 品质调整难度 = difficulty * 2^(quality.ordinal - average.ordinal)
     *   - 等级采集加成 = 玩家等级 * 0.02（ICPMExperience.LevelBonus.CRAFTING）
     *   - 工作台材质加成 = 随当前工作台等级提升（flint 0.2 … adamantium 0.7）
     * 下限 25 tick。
     */
    private fun recalculateCraftingTime() {
        // 非品质物品按 AVERAGE 品质计算时间（无品质惩罚/加成，合成更快）
        val item = resultSlots.getItem(0)
        val quality = if (supportsQuality(item)) EnumQuality.fromOrdinal(craftingQuality) else EnumQuality.AVERAGE
        val level = ICPMExperience.getExperienceLevel(ICPMExperience.getExperience(ownerPlayer))
        val speedModifier = ICPMExperience.getLevelModifier(level, ICPMExperience.LevelBonus.CRAFTING)
            + benchSpeedModifier(workbenchTier)
        totalCraftTime = CraftingTimeHelper.calculateCraftingTime(quality, recipeDifficulty, speedModifier)
    }

    /**
     * 计算配方基础难度（对齐 R196 物品的 lowest_crafting_difficulty_to_produce）。
     * ICPM 无逐物品难度字段，按输出物品所需工作台等级近似：等级越高、合成越慢，
     * 与 R196「高等级金属制品合成更久」的语义一致。
     */
    private fun calculateRecipeDifficulty(item: ItemStack): Float {
        return difficultyForTier(getRequiredWorkbenchTier(item))
    }

    /** 工作台等级 → 基础难度（对齐 R196：低等级物品 ~30 tick，高等级物品 ~190 tick） */
    private fun difficultyForTier(tier: Int): Float = when (tier) {
        0 -> 30f
        1 -> 60f
        3 -> 100f
        4 -> 125f
        5 -> 155f
        6 -> 190f
        else -> 40f
    }

    /**
     * 工作台材质速度加成（对齐 R196 getBenchAndToolsModifier）：
     * 当前所处工作台等级越高，合成越快。flint/obsidian=0.2 … adamantium=0.7。
     * 与 ICPM 工作台等级（0 燧石台 / 1 铜银金台 / 3 铁台 / 4 远古金属台 / 5 秘银台 / 6 艾德曼台）一一对应。
     */
    private fun benchSpeedModifier(tier: Int): Float = when (tier) {
        0 -> 0.2f
        1 -> 0.3f
        3 -> 0.4f
        4 -> 0.5f
        5 -> 0.6f
        6 -> 0.7f
        else -> 0.2f
    }

    // ==================== Shift 点击 ====================

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots[index]
        if (!slot.hasItem()) return ItemStack.EMPTY

        val stack = slot.item
        val originalStack = stack.copy()

        when (index) {
            0 -> {
                // 结果槽：仅在合成完成时可取走
                if (!isCraftingComplete) return ItemStack.EMPTY
                if (ICPMCraftCooldowns.hasCraftCooldown(player)) {
                    val remaining = ICPMCraftCooldowns.getCraftCooldownRemaining(player)
                    player.displayClientMessage(
                        Component.literal("合成冷却中... ${remaining}t"),
                        true
                    )
                    return ItemStack.EMPTY
                }
                val requiredTier = getRequiredWorkbenchTier(stack)
                if (requiredTier > workbenchTier) {
                    player.displayClientMessage(
                        Component.literal("需要等级 $requiredTier 或更高的工作台！(当前: $workbenchTier)"),
                        true
                    )
                    return ItemStack.EMPTY
                }
                // 剥离合成预览组件，避免泄漏进玩家背包
                stack.remove(ICPM.CRAFT_PREVIEW_COMPONENT)
                if (!moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY
                // 先清空结果槽，再让 onTake 消耗材料并设置下次配方预览
                slot.setByPlayer(ItemStack.EMPTY)
                slot.onTake(player, stack)
                // onTake 已处理：材料消耗、状态重置、冷却标记、updateResult
                // 无需重复调用
            }
            in 1..9 -> {
                if (!moveItemStackTo(stack, 10, 46, false)) return ItemStack.EMPTY
            }
            in 10..45 -> {
                if (!moveItemStackTo(stack, 1, 10, false)) {
                    if (index < 37) {
                        if (!moveItemStackTo(stack, 37, 46, false)) return ItemStack.EMPTY
                    } else {
                        if (!moveItemStackTo(stack, 10, 37, false)) return ItemStack.EMPTY
                    }
                }
            }
        }

        if (stack.isEmpty) {
            slot.setByPlayer(ItemStack.EMPTY)
        } else {
            slot.setChanged()
        }
        broadcastChanges()
        return originalStack
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算当前配方所需的工作台等级（材料与成品中取最高者）
     */
    private fun getRequiredTier(craftingInput: CraftingInput, result: ItemStack): Int {
        var required = getRequiredWorkbenchTier(result)
        for (i in 0 until craftSlots.containerSize) {
            val stack = craftSlots.getItem(i)
            if (!stack.isEmpty) {
                required = maxOf(required, getRequiredWorkbenchTier(stack))
            }
        }
        return required
    }

    /**
     * 获取当前选中的品质枚举
     */
    fun getSelectedQuality(): EnumQuality = EnumQuality.fromOrdinal(craftingQuality)

    /**
     * 获取合成进度百分比（0.0 ~ 1.0）
     */
    fun getCraftingProgressFraction(): Float {
        return if (totalCraftTime > 0) craftingProgress.toFloat() / totalCraftTime.toFloat() else 0f
    }

    override fun removed(player: Player) {
        super.removed(player)
        // 在重置状态前记录是否已有真实成品（completeCrafting 后 resultSlots 剥离了预览组件）
        val hadComplete = isCraftingComplete
        resetCraftingState()
        autoCrafting = false
        levelAccess.execute { _, _ ->
            clearContainer(player, craftSlots)
            // 结果槽：仅合成完成且非预览的真实成品归还玩家；预览副本直接丢弃（否则会刷物品）。
            // 修复：此前直接 setItem(0, EMPTY) 会丢弃合成完成品 → 关闭容器（如 JEI 打开配方页）时成品凭空消失。
            val result = resultSlots.getItem(0)
            resultSlots.setItem(0, ItemStack.EMPTY)
            if (hadComplete && !result.isEmpty && !result.has(ICPM.CRAFT_PREVIEW_COMPONENT)) {
                if (!player.inventory.add(result)) {
                    player.drop(result, false)
                }
            }
        }
    }
}
