package name.icpm.inventory

import name.icpm.ICPM
import name.icpm.block.BlockMetalAnvil
import name.icpm.blockentity.TileEntityMetalAnvil
import name.icpm.item.ICPMItems
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.inventory.ResultSlot
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.minecraft.world.level.block.state.BlockState

/**
 * 金属砧容器菜单
 * 
 * 槽位布局：
 * - 槽位 0: 输入物品（待修复的工具/护甲）
 * - 槽位 1: 修复材料（金属粒）
 * - 槽位 2: 输出结果
 * - 槽位 3-29: 玩家背包
 * - 槽位 30-38: 玩家快捷栏
 */
class MetalAnvilMenu(
    syncId: Int,
    private val playerInventory: Inventory,
    private val levelAccess: ContainerLevelAccess,
    private val anvilEntity: TileEntityMetalAnvil?,
    private val fallbackMetalType: BlockMetalAnvil.MetalType = BlockMetalAnvil.MetalType.IRON
) : AbstractContainerMenu(ICPM.METAL_ANVIL_MENU, syncId) {

    companion object {
        // 客户端构造函数 - 接收反序列化后的 BlockPos
        @JvmStatic
        fun create(syncId: Int, playerInventory: Inventory, pos: BlockPos): MetalAnvilMenu {
            val level = playerInventory.player.level()
            val blockEntity = level.getBlockEntity(pos) as? TileEntityMetalAnvil
            return MetalAnvilMenu(syncId, playerInventory, ContainerLevelAccess.create(level, pos), blockEntity)
        }
        
        @JvmStatic
        fun streamCodec(): net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, BlockPos> {
            return net.minecraft.network.codec.StreamCodec.of(
                { buf, pos -> buf.writeBlockPos(pos) },
                { buf -> buf.readBlockPos() }
            )
        }
    }

    private val input = SimpleContainer(3)
    private val resultSlotIndex = 2
    
    // 用于追踪修复状态
    private val repairAmountData = DataSlot.standalone()
    private var repairAmount = 0

    /** R196 stackSizeToBeUsedInRepair：本次修复消耗的金属粒数量（修复量循环计算） */
    private var stackSizeToBeUsedInRepair = 0

    /** 命名框文字（R196 repairedItemName）：为空表示不命名 */
    private var repairedItemName: String = ""

    // 砧耐久同步（服务端 → 客户端；原版铁砧 anvilEntity==null 时恒为 -1，GUI 不显示耐久条）
    private var clientAnvilDamage = -1
    private var clientAnvilMaxDurability = -1
    
    init {
        // 结果槽（只接受输出）
        addSlot(object : Slot(input, resultSlotIndex, 134, 47) {
            override fun mayPlace(stack: ItemStack): Boolean {
                return false // 结果槽不允许放入
            }
            
            override fun onTake(player: Player, stack: ItemStack) {
                super.onTake(player, stack)
                onResultTaken(player)
            }
        })
        
        // 输入槽
        addSlot(object : Slot(input, 0, 27, 47) {
            override fun mayPlace(stack: ItemStack): Boolean {
                return stack.isDamageableItem || stack.item == Items.BOW || stack.item == Items.FISHING_ROD || stack.item == Items.ENCHANTED_BOOK
            }
            
            override fun set(stack: ItemStack) {
                super.set(stack)
                updateRepairResult()
            }
        })
        
        // 材料槽（只接受金属粒）
        addSlot(object : Slot(input, 1, 76, 47) {
            override fun mayPlace(stack: ItemStack): Boolean {
                return isRepairMaterial(stack) || stack.item == Items.ENCHANTED_BOOK
            }
            
            override fun set(stack: ItemStack) {
                super.set(stack)
                updateRepairResult()
            }
        })
        
        // 玩家背包
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        
        // 玩家快捷栏
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }

        // ===== 砧耐久数据槽（服务端实时读取方块实体；客户端经 set 接收）=====
        addDataSlot(object : DataSlot() {
            override fun get(): Int = anvilEntity?.damage ?: -1
            override fun set(value: Int) { clientAnvilDamage = value }
        })
        addDataSlot(object : DataSlot() {
            override fun get(): Int = (anvilEntity?.blockState?.block as? BlockMetalAnvil)?.maxDurability ?: -1
            override fun set(value: Int) { clientAnvilMaxDurability = value }
        })
    }

    override fun stillValid(player: Player): Boolean {
        return levelAccess.evaluate { level, pos ->
            val block = level.getBlockState(pos).block
            // ICPM 金属砧 或 原版铁砧（mixin 接入，fallback=IRON）
            (block is BlockMetalAnvil || block is net.minecraft.world.level.block.AnvilBlock) &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }.orElse(false)
    }

    override fun quickMoveStack(player: Player, slotIndex: Int): ItemStack {
        val slot = slots[slotIndex]
        if (!slot.hasItem()) return ItemStack.EMPTY
        
        val itemStack = slot.item
        val originalStack = itemStack.copy()
        
        return when {
            // 从结果槽移出（shift 点击）
            slotIndex == 2 -> {
                if (!moveItemStackTo(itemStack, 3, 39, true)) {
                    return ItemStack.EMPTY
                }
                slot.onQuickCraft(itemStack, originalStack)
                if (itemStack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY)
                } else {
                    slot.setChanged()
                }
                // 直接在此执行取走消耗（消耗金属粒 + 输入工具 + 砧损耗）。
                // AbstractContainerMenu.doClick 的 QUICK_MOVE 分支会循环重试：
                // 若 quickMoveStack 返回非空且与光标物品不同，它会再次调用 quickMoveStack。
                // 而 onResultTaken 会立即生成下一件结果，若返回 originalStack 将导致一次
                // shift 点击连续取走所有可用输入/材料。因此成功后必须返回 EMPTY 终止循环。
                onResultTaken(player)
                broadcastChanges()
                ItemStack.EMPTY
            }
            // 从输入槽移出
            slotIndex in 0..1 -> {
                if (!moveItemStackTo(itemStack, 3, 39, true)) {
                    return ItemStack.EMPTY
                }
                originalStack
            }
            // 从背包移入输入槽
            isRepairMaterial(itemStack) -> {
                if (!moveItemStackTo(itemStack, 1, 2, false)) {
                    return ItemStack.EMPTY
                }
                originalStack
            }
            itemStack.isDamageableItem -> {
                if (!moveItemStackTo(itemStack, 0, 1, false)) {
                    return ItemStack.EMPTY
                }
                originalStack
            }
            // 从背包移到快捷栏或反之
            slotIndex in 3..29 -> {
                if (!moveItemStackTo(itemStack, 30, 39, false)) {
                    return ItemStack.EMPTY
                }
                originalStack
            }
            slotIndex in 30..38 -> {
                if (!moveItemStackTo(itemStack, 3, 30, false)) {
                    return ItemStack.EMPTY
                }
                originalStack
            }
            else -> ItemStack.EMPTY
        }
    }

    override fun removed(player: Player) {
        super.removed(player)
        levelAccess.execute { _, _ ->
            // 结果槽（槽 2）是修复预览物品，直接清空不归还——
            // 否则关闭 GUI 时会把"修复后工具"也还回背包（白赚一把，刷物品漏洞）。
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            clearContainer(player, input)
        }
    }

    /**
     * 判断物品是否为修复材料
     */
    private fun isRepairMaterial(stack: ItemStack): Boolean {
        if (stack.isEmpty()) return false
        val item = stack.item
        return item == Items.IRON_NUGGET ||
               item == Items.GOLD_NUGGET ||
               item == Items.COPPER_NUGGET ||
               item == ICPMItems.SILVER_NUGGET ||
               item == ICPMItems.ANCIENT_METAL_NUGGET ||
               item == ICPMItems.MITHRIL_NUGGET ||
               item == ICPMItems.ADAMANTIUM_NUGGET
    }

    /**
     * 更新修复结果
     */
    private fun updateRepairResult() {
        val inputStack = input.getItem(0)
        val materialStack = input.getItem(1)
        
        // 无输入 → 无结果
        if (inputStack.isEmpty()) {
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            stackSizeToBeUsedInRepair = 0
            return
        }

        // (c) 两本附魔书融合：合并两本书的全部附魔，同名同等级 +1，异等级取高
        if (inputStack.item == Items.ENCHANTED_BOOK && materialStack.item == Items.ENCHANTED_BOOK) {
            combineEnchantedBooks(inputStack, materialStack)
            return
        }

        // (d) 装备 + 附魔书：把书内附魔砸到装备上（合并/升级已有附魔）
        if (materialStack.item == Items.ENCHANTED_BOOK && inputStack.isDamageableItem) {
            applyBookToEquipment(inputStack, materialStack)
            return
        }

        // ===== 纯命名（R196 is_renaming）：材料空 + 命名框有文字变化 → 结果 = 输入副本 + 名字，不消耗材料 =====
        if (materialStack.isEmpty()) {
            handlePureRename(inputStack)
            return
        }

        if (!inputStack.isDamageableItem || inputStack.damageValue <= 0) {
            if (!inputStack.isEmpty && inputStack.isDamageableItem && inputStack.damageValue <= 0) {
                playerInventory.player.displayClientMessage(
                    Component.literal("§e工具耐久已满，无需修复"),
                    true
                )
            }
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            return
        }
        
        // R196 isRepairing：砧等级必须 >= 工具材质等级
        // （ICPM 金属砧取 anvilEntity.metalType；原版铁砧经 mixin 接入，fallback=IRON 铁砧等级）
        val anvilMetal = anvilEntity?.metalType ?: fallbackMetalType
        val toolMaterial = getToolMaterial(inputStack)
        if (toolMaterial == null) {
            playerInventory.player.displayClientMessage(
                Component.literal("§c无法识别的工具材质：" + (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(inputStack.item) ?: "?")),
                true
            )
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            stackSizeToBeUsedInRepair = 0
            return
        }
        if (anvilMetal.level < toolMaterial.level) {
            playerInventory.player.displayClientMessage(
                Component.literal("§c砧等级不足：${toolMaterial.id} 工具需要更高等级的砧（当前 ${anvilMetal.id}）"),
                true
            )
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            stackSizeToBeUsedInRepair = 0
            return
        }
        
        // R196：修复材料必须是对应金属粒
        val nuggetMaterial = getNuggetMetal(materialStack)
        if (nuggetMaterial == null) {
            playerInventory.player.displayClientMessage(
                Component.literal("§c材料槽不是可用的金属粒：" + (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(materialStack.item) ?: "?")),
                true
            )
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            stackSizeToBeUsedInRepair = 0
            return
        }
        if (toolMaterial != nuggetMaterial) {
            playerInventory.player.displayClientMessage(
                Component.literal("§c金属粒与工具材质不匹配（需要 ${toolMaterial.id} 粒，放入的是 ${nuggetMaterial.id} 粒）"),
                true
            )
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            stackSizeToBeUsedInRepair = 0
            return
        }
        
        // ===== R196 updateRepairOutput 材料修复 =====
        // 修复量 = min(当前损伤, maxDamage / getRepairCost())；链甲特殊：maxDamage * 2 / repairCost
        val id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(inputStack.item)
        if (id == null) {
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            stackSizeToBeUsedInRepair = 0
            return
        }
        val path = id.getPath()
        val maxDamage = inputStack.maxDamage
        val damage = inputStack.damageValue
        val chain = isChainMail(path)
        val repairCost = getRepairCost(path)
        if (repairCost <= 0 || maxDamage <= 0) {
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            stackSizeToBeUsedInRepair = 0
            return
        }
        var unit = if (chain) minOf(damage, maxDamage * 2 / repairCost)
                   else minOf(damage, maxDamage / repairCost)
        if (unit <= 0) {
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            stackSizeToBeUsedInRepair = 0
            return
        }
        
        // R196 材料消耗循环：每次消耗 1 粒修 unit 点；只要修复量不变（=初始量）且材料够就继续
        val initial = unit
        var consumed = 0
        var remaining = damage
        while (unit > 0 && unit == initial && consumed < materialStack.count) {
            remaining -= unit
            unit = if (chain) minOf(remaining, maxDamage * 2 / repairCost)
                   else minOf(remaining, maxDamage / repairCost)
            consumed++
        }
        stackSizeToBeUsedInRepair = consumed
        
        // 结果：总修复 = 每粒修复量 × 消耗粒数（R196 循环语义）
        val newDamage = maxOf(0, damage - initial * consumed)
        val resultStack = inputStack.copy()
        // ⚠️ 结果只代表"本次取出"的 1 件物品：输入为堆叠时若保留原数量，
        // onResultTaken 每次仅消耗 1 个输入却会再次生成整堆结果 -> 刷物品。
        // 故强制结果数量=1，玩家对整堆输入逐次 shift 取出即可（每次消耗 1 输入 + 对应材料）。
        resultStack.setCount(1)
        resultStack.damageValue = newDamage
        // 修复 + 命名同时生效（R196：结果套用命名框文字）
        applyNameToResult(resultStack, inputStack)
        
        input.setItem(resultSlotIndex, resultStack)
        repairAmount = initial
    }

    /**
     * 纯命名（R196 is_renaming）：输入物品 + 命名框文字 → 结果 = 输入副本 + 名字，不消耗材料。
     * - 命名框空白且输入无自定义名 → 无结果
     * - 名字与输入原名相同 → 无结果
     * - 否则生成命名副本（空白名字 = 清除自定义名）
     */
    private fun handlePureRename(inputStack: ItemStack) {
        val inputHasCustomName = inputStack.has(DataComponents.CUSTOM_NAME)
        val displayName = inputStack.getHoverName().string
        val isRenaming = !((repairedItemName.isBlank() && !inputHasCustomName) || repairedItemName == displayName)
        if (isRenaming) {
            val named = inputStack.copy()
            named.setCount(1) // 结果仅代表 1 件命名物品，防止堆叠输入刷物品
            applyNameToResult(named, inputStack)
            input.setItem(resultSlotIndex, named)
        } else {
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
        }
        repairAmount = 0
        stackSizeToBeUsedInRepair = 0
    }

    /**
     * 将命名框文字应用到结果物品（R196：空白名字清除自定义名；不同名字则设置新名）。
     */
    private fun applyNameToResult(resultStack: ItemStack, inputStack: ItemStack) {
        if (repairedItemName.isBlank()) {
            if (inputStack.has(DataComponents.CUSTOM_NAME)) {
                resultStack.remove(DataComponents.CUSTOM_NAME)
            }
        } else if (repairedItemName != inputStack.getHoverName().string) {
            resultStack.set(DataComponents.CUSTOM_NAME, Component.literal(repairedItemName))
        }
    }

    /**
     * 获取工具的材质类型（统一前缀匹配注册表 id path，覆盖原版与 ICPM 全部工具/护甲/弓/鱼竿）。
     * ⚠️ 不能用 item.toString() 匹配（格式不可靠），用 BuiltInRegistries.ITEM.getKey().getPath()。
     */
    private fun getToolMaterial(stack: ItemStack): BlockMetalAnvil.MetalType? {
        if (stack.isEmpty() || !stack.isDamageableItem) return null
        val id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.item) ?: return null
        val p = id.getPath()
        return when {
            // 原版铜工具(minecraft:copper_*) + ICPM 铜系特殊工具(icpm:copper_*)
            p.startsWith("copper_") -> BlockMetalAnvil.MetalType.COPPER
            p.startsWith("silver_") -> BlockMetalAnvil.MetalType.SILVER
            // 原版铁工具(minecraft:iron_*) + ICPM 铁系特殊工具(icpm:iron_*)
            p.startsWith("iron_") -> BlockMetalAnvil.MetalType.IRON
            // 原版金工具是 golden_ 前缀；ICPM 金系特殊工具是 gold_
            p.startsWith("gold_") || p.startsWith("golden_") -> BlockMetalAnvil.MetalType.GOLD
            p.startsWith("ancient_metal_") -> BlockMetalAnvil.MetalType.ANCIENT_METAL
            p.startsWith("mithril_") -> BlockMetalAnvil.MetalType.MITHRIL
            p.startsWith("adamantium_") -> BlockMetalAnvil.MetalType.ADAMANTIUM
            // 燧石工具（无法修复，无对应金属粒）
            else -> null
        }
    }

    /**
     * 获取金属粒的材质类型
     */
    private fun getNuggetMetal(stack: ItemStack): BlockMetalAnvil.MetalType? {
        val item = stack.item
        return when {
            item == Items.COPPER_NUGGET -> BlockMetalAnvil.MetalType.COPPER
            item == ICPMItems.SILVER_NUGGET -> BlockMetalAnvil.MetalType.SILVER
            item == Items.IRON_NUGGET -> BlockMetalAnvil.MetalType.IRON
            item == Items.GOLD_NUGGET -> BlockMetalAnvil.MetalType.GOLD
            item == ICPMItems.ANCIENT_METAL_NUGGET -> BlockMetalAnvil.MetalType.ANCIENT_METAL
            item == ICPMItems.MITHRIL_NUGGET -> BlockMetalAnvil.MetalType.MITHRIL
            item == ICPMItems.ADAMANTIUM_NUGGET -> BlockMetalAnvil.MetalType.ADAMANTIUM
            else -> null
        }
    }

    /**
     * R196 getRepairCost：修复成本分母（每 1 粒金属修复 maxDamage / repairCost 点耐久）。
     * - 工具（ItemTool）: 部件数 × 2（镐3→6、斧3→6、战锤5→10、战斧4→8、短斧1→2、匕首1→2、剑2→4、铲1→2、锄2→4、镰2→4、鸭嘴锄4→8、剪刀2→4）
     * - 护甲（ItemArmor）: 部件数 × 2（头5→10、胸8→16、腿7→14、靴4→8）
     * - 链甲: 部件数（修复量公式 maxDamage*2/repairCost 单独处理）
     * - 弓（ItemBow）: 2（金属加固 1 部件 × 2）
     * - 鱼竿（ItemFishingRod）: 1
     */
    private fun getRepairCost(path: String): Int {
        if (isChainMail(path)) {
            return when {
                path.contains("helmet") -> 5
                path.contains("chestplate") -> 8
                path.contains("leggings") -> 7
                path.contains("boots") -> 4
                else -> 5
            }
        }
        return when {
            path.contains("helmet") -> 10
            path.contains("chestplate") -> 16
            path.contains("leggings") -> 14
            path.contains("boots") -> 8
            path.contains("fishing_rod") -> 1
            path.contains("bow") -> 2
            path.contains("pickaxe") -> 6
            path.contains("mattock") -> 8
            path.contains("battle_axe") -> 8
            path.contains("war_hammer") -> 10
            path.contains("hatchet") -> 2
            path.contains("dagger") -> 2
            path.contains("knife") -> 2
            path.contains("cudgel") -> 2
            path.contains("axe") -> 6
            path.contains("shovel") -> 2
            path.contains("hoe") -> 4
            path.contains("scythe") -> 4
            path.contains("shears") -> 4
            path.contains("sword") -> 4
            else -> 2
        }
    }

    /** 是否链甲（R196 链甲修复量公式 maxDamage*2/repairCost） */
    private fun isChainMail(path: String): Boolean = path.contains("chainmail")

    /**
     * 获取砧的损坏阶段（用于显示）
     */
    fun getAnvilDamageStage(): Int {
        return anvilEntity?.getDamageStage() ?: 0
    }

    /**
     * 获取修复量（用于显示）
     */
    fun getRepairAmount(): Int {
        return repairAmount
    }

    /**
     * 设置命名框文字（R196 ContainerRepair.updateItemName）。
     * 由客户端 AnvilRenamePacket 触发；更新后重算结果（纯命名/修复+命名共用）。
     */
    fun setItemName(name: String) {
        repairedItemName = name.trim()
        updateRepairResult()
    }

    /** 客户端砧当前损伤值（-1 表示非 ICPM 金属砧，不显示耐久条） */
    fun getAnvilDamage(): Int = clientAnvilDamage

    /** 客户端砧最大耐久（-1 表示非 ICPM 金属砧） */
    fun getAnvilMaxDurability(): Int = clientAnvilMaxDurability

    /**
     * 取出结果物品时调用
     */
    fun onResultTaken(player: Player) {
        val inputStack = input.getItem(0)
        // 纯命名也消耗输入工具（R196 onPickupFromSlot 清空输入槽），故不要求材料非空
        if (inputStack.isEmpty()) return

        val materialStack = input.getItem(1)
        // 消耗金属粒（仅修复时 stackSizeToBeUsedInRepair > 0；纯命名不消耗材料）
        val toConsume = stackSizeToBeUsedInRepair
        if (toConsume > 0 && !materialStack.isEmpty) {
            if (materialStack.count > toConsume) {
                materialStack.shrink(toConsume)
            } else {
                materialStack.setCount(0)
            }
        }

        // 砧损耗 = 本次总修复点数 × 耐久比（对齐 R196 ContainerRepairINNER2.onPickupFromSlot）。
        // 仅修复时有损耗（stackSizeToBeUsedInRepair > 0）；纯命名不损耗。
        // 仅 ICPM 金属砧有耐久；原版铁砧（anvilEntity==null）无砧损耗。
        if (anvilEntity != null && stackSizeToBeUsedInRepair > 0) {
            val level = player.level()
            val pos = anvilEntity.blockPos
            val currentEntity = level.getBlockEntity(pos) as? TileEntityMetalAnvil
            currentEntity?.let { entity ->
                val block = entity.blockState.block as? BlockMetalAnvil
                val totalRepaired = repairAmount * stackSizeToBeUsedInRepair
                val anvilDamage = block?.calculateAnvilDurabilityLoss(inputStack, totalRepaired) ?: 0
                entity.addDamage(level, pos, anvilDamage)
            }
        }

        // R196：消耗输入工具
        inputStack.shrink(1)

        // 更新结果
        updateRepairResult()
    }

    /**
     * (c) 融合两本附魔书：将第二本书的附魔合并进第一本书（同名同等级 +1，异等级取较高者）。
     * 注：当前为宽松合并（不强制最大等级上限 / 互斥校验），后续可据 1.21.11 API 收紧。
     */
    private fun combineEnchantedBooks(bookA: ItemStack, bookB: ItemStack) {
        val a = bookA.get(DataComponents.STORED_ENCHANTMENTS)
        val b = bookB.get(DataComponents.STORED_ENCHANTMENTS)
        if (b == null || b.isEmpty) {
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            return
        }
        val mutable = if (a != null) ItemEnchantments.Mutable(a) else ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
        var changed = false
        for (entry in b.entrySet()) {
            val holder = entry.key
            val j = entry.value
            val i = a?.getLevel(holder) ?: 0
            val target = if (i == j) j + 1 else maxOf(j, i)
            mutable.set(holder, target)
            changed = true
        }
        if (!changed) {
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            return
        }
        val result = Items.ENCHANTED_BOOK.defaultInstance
        result.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable())
        input.setItem(resultSlotIndex, result)
        repairAmount = 0
    }

    /**
     * (d) 装备 + 附魔书：把书内附魔砸到装备上，合并/升级装备已有附魔。
     */
    private fun applyBookToEquipment(item: ItemStack, book: ItemStack) {
        val bookEnch = book.get(DataComponents.STORED_ENCHANTMENTS)
        if (bookEnch == null || bookEnch.isEmpty) {
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            return
        }
        val existing = item.get(DataComponents.ENCHANTMENTS)
        val mutable = if (existing != null) ItemEnchantments.Mutable(existing) else ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
        var changed = false
        for (entry in bookEnch.entrySet()) {
            val holder = entry.key
            val j = entry.value
            val i = existing?.getLevel(holder) ?: 0
            val target = if (i == j) j + 1 else maxOf(j, i)
            mutable.set(holder, target)
            changed = true
        }
        if (!changed) {
            input.setItem(resultSlotIndex, ItemStack.EMPTY)
            repairAmount = 0
            return
        }
        val result = item.copy()
        result.setCount(1) // 结果仅代表 1 件附魔后的装备，防止堆叠输入刷物品
        result.set(DataComponents.ENCHANTMENTS, mutable.toImmutable())
        input.setItem(resultSlotIndex, result)
        repairAmount = 0
    }
}
