package name.icpm.block

import name.icpm.ICPM
import name.icpm.blockentity.TileEntityMetalAnvil
import name.icpm.component.QualityComponent
import name.icpm.item.ICPMItems
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.TypedEntityData
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.MenuProvider
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import name.icpm.inventory.MetalAnvilMenu

/**
 * 金属砧方块基类
 *
 * 参考ICPM R196源码实现：
 * - 有耐久度系统，使用时会消耗
 * - 每个金属有3个损坏阶段变体：完好(0)、chipped(1)、damaged(2)，达到最大耐久则销毁
 * - 阶段阈值（对齐 R196）：损伤比例 < 0.5 完好，< 0.8 chipped，< 1.0 damaged，>= 1.0 销毁
 * - 砧的等级必须>=被修复工具的材质等级
 * - 不消耗经验
 */
class BlockMetalAnvil(
    val metalType: MetalType,
    private     val defaultStage: Int, // 仅为旧存档兼容保留：0=完好, 1=chipped, 2=damaged；主方块恒为 0；耐久阶段统一由 BlockEntity 的 damage 驱动
    properties: Properties
) : Block(properties), EntityBlock {

    companion object {
        val FACING: EnumProperty<Direction> = BlockStateProperties.HORIZONTAL_FACING

        /** 砧损耗阶段状态属性（0=完好, 1=裂痕, 2=损坏），对齐 R196 的 metadata 位。 */
        val STAGE: IntegerProperty = IntegerProperty.create("stage", 0, 2)

        // 砧的碰撞箱（参考原版铁砧）
        // X轴朝向（南北方向）：宽16像素，高16像素，长12像素
        val SHAPE_X = Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 14.0)
        // Z轴朝向（东西方向）：宽12像素，高16像素，长16像素
        val SHAPE_Z = Block.box(2.0, 0.0, 0.0, 14.0, 16.0, 16.0)

        // 砧损耗比例（严格对齐 R196 ContainerRepairINNER2.onPickupFromSlot）
        //   ratio_of_tool_to_armor = 铁工具最大耐久 / 铁靴最大耐久
        //   ratio_of_tool_to_bow   = 秘银铲最大耐久 / 秘银弓最大耐久
        // 修复护甲时砧损耗 × ratio_tool_to_armor，修复弓 × ratio_tool_to_bow，
        // 修复鱼竿 × ratio_tool_to_armor/9，修工具 × 1（与 R196 完全一致）。
        // 用 lazy 避免与 ICPMItems 的初始化顺序竞争。
        val RATIO_TOOL_TO_ARMOR: Float by lazy {
            ItemStack(Items.IRON_PICKAXE).getMaxDamage().toFloat() / maxOf(1, ItemStack(Items.IRON_BOOTS).getMaxDamage())
        }
        val RATIO_TOOL_TO_BOW: Float by lazy {
            ItemStack(ICPMItems.MITHRIL_SHOVEL).getMaxDamage().toFloat() / maxOf(1, ItemStack(ICPMItems.MITHRIL_BOW).getMaxDamage())
        }
    }

    // 基础耐久度（每个金属粒提供的耐久）
    private val BASE_DURABILITY_PER_NUGGET = 1600

    // 砧的最大耐久度 = 基础耐久 * 31 * 金属耐久系数（iron = 1600*31*8 = 396800，与1.18.2一致）
    val maxDurability: Int = (BASE_DURABILITY_PER_NUGGET * 31 * metalType.durabilityFactor).toInt()

    init {
        // 初始状态：朝向 NORTH，损耗阶段 = defaultStage（主方块为 0；旧存档兼容方块按预设阶段）
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STAGE, defaultStage))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, STAGE)
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        val facing = state.getValue(FACING)
        return if (facing.axis == Direction.Axis.X) SHAPE_X else SHAPE_Z
    }

    override fun getCollisionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        // 碰撞形状与渲染形状相同
        return getShape(state, level, pos, context)
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        // 打开金属砧UI界面
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? TileEntityMetalAnvil
            if (blockEntity != null) {
                player.openMenu(
                    getMenuProvider(state, level, pos)
                )
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun getMenuProvider(state: BlockState, level: Level, pos: BlockPos): ExtendedScreenHandlerFactory<BlockPos> {
        return object : ExtendedScreenHandlerFactory<BlockPos> {
            override fun getDisplayName(): net.minecraft.network.chat.Component {
                return net.minecraft.network.chat.Component.translatable("container.icpm.metal_anvil")
            }

            override fun getScreenOpeningData(player: net.minecraft.server.level.ServerPlayer): BlockPos {
                return pos
            }

            override fun createMenu(syncId: Int, playerInventory: Inventory, player: net.minecraft.world.entity.player.Player): net.minecraft.world.inventory.AbstractContainerMenu {
                val blockEntity = level.getBlockEntity(pos) as? TileEntityMetalAnvil
                return MetalAnvilMenu(syncId, playerInventory, ContainerLevelAccess.create(level, pos), blockEntity)
            }
        }
    }

    /**
     * 检查是否可以修复
     */
    private fun canRepair(
        toolStack: ItemStack,
        nuggetStack: ItemStack,
        player: Player,
        anvilEntity: TileEntityMetalAnvil
    ): Boolean {
        if (toolStack.isEmpty || nuggetStack.isEmpty) return false
        if (!toolStack.isDamageableItem) return false
        if (toolStack.damageValue <= 0) return false // 已满耐久

        // 检查工具材质是否与砧等级匹配
        val toolMaterial = getToolMaterial(toolStack)
        if (toolMaterial == null) return false
        if (!canRepairMaterial(toolMaterial)) return false

        // 检查金属粒是否匹配工具材质
        val nuggetMetal = getNuggetMetal(nuggetStack)
        if (nuggetMetal != toolMaterial) return false

        return true
    }

    /**
     * 执行修复
     * ICPM修复公式：
     * - 工具：每粒恢复 200 × 材质系数 × 品质系数
     * - 护甲：每粒恢复 材质系数 × 品质系数
     * - 弓：每粒恢复 16（普通）/ 32（远古）/ 64（秘银）
     * - 鱼竿：每粒恢复 2 × 材质系数
     * 
     * 砧耐久损耗：
     * - 工具：损耗 = 恢复点数
     * - 护甲/弓：损耗 = 恢复点数 × 200
     * - 鱼竿：损耗 = 恢复点数 × 22
     */
    private fun repairItem(
        toolStack: ItemStack,
        nuggetStack: ItemStack,
        player: Player,
        anvilEntity: TileEntityMetalAnvil,
        level: Level,
        pos: BlockPos
    ): Boolean {
        val currentDamage = toolStack.damageValue
        val maxDamage = toolStack.maxDamage

        if (currentDamage <= 0) return false

        // 计算基础修复量
        val baseRepairAmount = calculateRepairAmount(toolStack, nuggetStack)

        // 应用品质系数
        val qualityComponent: QualityComponent? = toolStack.get(ICPM.QUALITY_COMPONENT)
        val qualityMultiplier = if (qualityComponent != null) {
            qualityComponent.quality().getDurabilityModifier()
        } else {
            1.0f
        }
        val repairAmount = (baseRepairAmount * qualityMultiplier).toInt()

        // 计算新耐久（不超过最大值）
        val newDamage = maxOf(0, currentDamage - repairAmount)
        toolStack.damageValue = newDamage

        // 消耗金属粒
        nuggetStack.shrink(1)

        // 计算砧耐久损耗
        val anvilDamage = calculateAnvilDurabilityLoss(toolStack, repairAmount)
        
        // 增加砧的损坏值
        anvilEntity.addDamage(level, pos, anvilDamage)

        // 发送修复信息
        val repairedAmount = currentDamage - newDamage
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(
                "§a修复了 $repairedAmount 点耐久（砧损耗 $anvilDamage）"
            ),
            true
        )

        return true
    }

    /**
     * 计算修复量
     */
    private fun calculateRepairAmount(toolStack: ItemStack, nuggetStack: ItemStack): Int {
        val id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(toolStack.item) ?: return 0
        val itemId = id.getPath()
        val materialFactor = getMaterialFactor(nuggetStack.item)

        val isArmor = itemId.contains("helmet") ||
                      itemId.contains("chestplate") ||
                      itemId.contains("leggings") ||
                      itemId.contains("boots")

        val isBow = itemId.contains("bow")
        val isFishingRod = itemId.contains("fishing_rod")

        return when {
            isArmor -> materialFactor.toInt()
            isBow -> {
                when {
                    itemId.contains("ancient_metal") -> 32
                    itemId.contains("mithril") -> 64
                    else -> 16
                }
            }
            isFishingRod -> (2 * materialFactor).toInt()
            else -> (200 * materialFactor).toInt() // 工具
        }
    }

    /**
     * 获取修复材料的材质系数
     */
    private fun getMaterialFactor(nuggetItem: net.minecraft.world.item.Item): Float {
        return when (nuggetItem) {
            net.minecraft.world.item.Items.IRON_NUGGET -> 8.0f
            net.minecraft.world.item.Items.GOLD_NUGGET -> 2.0f
            net.minecraft.world.item.Items.COPPER_NUGGET -> 1.0f
            ICPMItems.SILVER_NUGGET -> 4.0f
            ICPMItems.ANCIENT_METAL_NUGGET -> 16.0f
            ICPMItems.MITHRIL_NUGGET -> 64.0f
            ICPMItems.ADAMANTIUM_NUGGET -> 256.0f
            else -> 1.0f
        }
    }

    /**
     * 计算砧耐久损耗（对齐 R196：砧损耗 = 本次恢复点数 × 耐久比）
     * - 工具：× 1
     * - 护甲：× RATIO_TOOL_TO_ARMOR
     * - 弓：  × RATIO_TOOL_TO_BOW
     * - 鱼竿：× RATIO_TOOL_TO_ARMOR / 9
     * 类型判定沿用 ICPM 的字符串匹配（ICPM 护甲为 Item+组件，非 ArmorItem 子类，
     * 不能用 instanceof，与原 1.18.2 写法保持一致）。
     */
    internal fun calculateAnvilDurabilityLoss(toolStack: ItemStack, repairAmount: Int): Int {
        val id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(toolStack.item) ?: return 0
        val itemId = id.getPath()

        val isArmor = itemId.contains("helmet") ||
                      itemId.contains("chestplate") ||
                      itemId.contains("leggings") ||
                      itemId.contains("boots")

        val isBow = itemId.contains("bow")
        val isFishingRod = itemId.contains("fishing_rod")

        val ratio = when {
            isArmor -> RATIO_TOOL_TO_ARMOR
            isBow -> RATIO_TOOL_TO_BOW
            isFishingRod -> RATIO_TOOL_TO_ARMOR / 9.0f
            else -> 1.0f // 工具
        }
        return (repairAmount * ratio).toInt()
    }

    /**
     * 获取工具的材质类型（统一前缀匹配注册表 id path，覆盖原版与 ICPM 全部工具/护甲/弓/鱼竿）。
     * ⚠️ 不能用 item.toString() 匹配（格式不可靠），用 BuiltInRegistries.ITEM.getKey().getPath()。
     */
    private fun getToolMaterial(stack: ItemStack): MetalType? {
        if (stack.isEmpty() || !stack.isDamageableItem) return null
        val id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.item) ?: return null
        val p = id.getPath()
        return when {
            // 原版铜工具(minecraft:copper_*) + ICPM 铜系特殊工具(icpm:copper_*)
            p.startsWith("copper_") -> MetalType.COPPER
            p.startsWith("silver_") -> MetalType.SILVER
            // 原版铁工具(minecraft:iron_*) + ICPM 铁系特殊工具(icpm:iron_*)
            p.startsWith("iron_") -> MetalType.IRON
            // 原版金工具是 golden_ 前缀；ICPM 金系特殊工具是 gold_
            p.startsWith("gold_") || p.startsWith("golden_") -> MetalType.GOLD
            p.startsWith("ancient_metal_") -> MetalType.ANCIENT_METAL
            p.startsWith("mithril_") -> MetalType.MITHRIL
            p.startsWith("adamantium_") -> MetalType.ADAMANTIUM
            // 燧石工具（无法修复，无对应金属粒）
            else -> null
        }
    }

    /**
     * 获取金属粒的材质类型
     */
    private fun getNuggetMetal(stack: ItemStack): MetalType? {
        val item = stack.item
        return when {
            item == Items.COPPER_NUGGET -> MetalType.COPPER
            item == ICPMItems.SILVER_NUGGET -> MetalType.SILVER
            item == Items.IRON_NUGGET -> MetalType.IRON
            item == Items.GOLD_NUGGET -> MetalType.GOLD
            item == ICPMItems.ANCIENT_METAL_NUGGET -> MetalType.ANCIENT_METAL
            item == ICPMItems.MITHRIL_NUGGET -> MetalType.MITHRIL
            item == ICPMItems.ADAMANTIUM_NUGGET -> MetalType.ADAMANTIUM
            else -> null
        }
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        // 使用玩家的朝向（不反转）；若手持物品带有砧损伤（掉落物还原），按损伤预置 stage 阶段
        val state = this.defaultBlockState().setValue(FACING, context.horizontalDirection)
        val held = context.itemInHand
        if (held.damageValue > 0) {
            val stage = getDamageStage(held.damageValue)
            return state.setValue(STAGE, if (stage >= 3) 2 else stage)
        }
        return state
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return TileEntityMetalAnvil(pos, state, metalType)
    }

    /**
     * 获取损坏阶段（0=完好, 1=chipped, 2=damaged, 3=销毁）
     * 严格对齐 R196 BlockAnvil.getDamageStage：
     * - factor >= 1.0  → 销毁(3)
     * - factor >= 0.8  → damaged(2)  （veryDamaged）
     * - factor >= 0.5  → chipped(1)  （slightlyDamaged）
     * - 否则          → 完好(0)
     * （注：1.18.2 ICPM 用的是 1/3、2/3，与 MITE 源码不符，已纠正）
     */
    fun getDamageStage(damage: Int): Int {
        val factor = damage.toFloat() / maxDurability.toFloat()
        return when {
            factor >= 1.0f -> 3  // 销毁
            factor >= 0.8f -> 2  // damaged（veryDamaged）
            factor >= 0.5f -> 1  // chipped（slightlyDamaged）
            else -> 0  // 完好
        }
    }

    /**
     * 覆写掉落逻辑：砧被破坏时，掉落物携带损伤值（BlockEntityTag.Damage）
     * 完全损坏的砧不掉落任何物品
     */
    override fun spawnAfterBreak(
        state: BlockState,
        level: ServerLevel,
        pos: BlockPos,
        tool: ItemStack,
        dropExperience: Boolean
    ) {
        val blockEntity = level.getBlockEntity(pos) as? TileEntityMetalAnvil
        val damage = blockEntity?.damage ?: 0

        if (damage < this.maxDurability) {
            val drop = ItemStack(this.asItem())
            // R196：砧的耐久值同时写入物品 damage（计入物品数据）+ BLOCK_ENTITY_DATA（放置时还原 BE）
            drop.setDamageValue(damage)
            val tag = CompoundTag()
            tag.putInt("Damage", damage)
            drop.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(TileEntityMetalAnvil.TYPE, tag))
            Block.popResource(level, pos, drop)
        }
        // 完全损坏不掉落
    }

    /**
     * 获取指定损坏阶段的最小 damage 值（严格对齐 R196 的 getMinimumDamageForStage(stage, false)）
     * R196 源码：while (this.getDamageStage(damage) < stage) ++damage; return damage;
     * 即「最小的 damage，使其 getDamageStage() >= stage」。这里用同样语义求解，
     * 避免闭式近似在整数边界上与 R196 出现 1 点偏差。
     */
    fun getMinimumDamageForStage(stage: Int): Int {
        if (stage <= 0) return 0
        if (stage >= 3) return maxDurability
        var damage = 0
        while (getDamageStage(damage) < stage) {
            damage++
        }
        return damage
    }

    /**
     * 判断砧是否可以修复指定金属的工具
     * 规则：砧的金属等级必须>=被修复工具的金属等级
     */
    fun canRepairMaterial(toolMetal: MetalType): Boolean {
        return this.metalType.level >= toolMetal.level
    }

    /**
     * 金属类型枚举
     * durabilityFactor 与 ICPMDurabilityManager.MaterialDurability 一致
     */
    enum class MetalType(
        val id: String,
        val level: Int,           // 挖掘等级
        val durabilityFactor: Float // 耐久系数（与工具/护甲材质系数一致）
    ) {
        COPPER("copper", 2, 1.0f),
        SILVER("silver", 2, 4.0f),
        IRON("iron", 3, 8.0f),
        GOLD("gold", 2, 2.0f),
        ANCIENT_METAL("ancient_metal", 4, 16.0f),
        MITHRIL("mithril", 4, 64.0f),
        ADAMANTIUM("adamantium", 5, 256.0f)
    }
}