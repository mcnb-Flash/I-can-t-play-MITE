package name.icpm.block

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import name.icpm.inventory.ICPMWorkbenchMenu

/**
 * 燧石工作台（多原木衍生变体）
 *
 * 设计要点：**只有一个方块**、一套合成等级（tier=0）。不同原木只是它的衍生外观类型，
 * 由 `wood` 状态属性（0..10）决定侧边/底面材质——与砧的 STAGE 同类的多态方块，
 * 而不是 11 个彼此独立的方块。
 *
 * 木材信息在物品端由原版 `minecraft:block_state` 组件承载，闭环如下：
 *  - 合成：配方 `result.components` 写入 `{"wood": "<序号>"}`
 *  - 放置：FlintWorkbenchItem#getPlacementState 用 BlockItemStateProperties#apply 写回方块状态
 *  - 破坏：战利品表 `minecraft:copy_state` 把 wood 属性复制回物品的 block_state 组件
 *  - 显示：assets/icpm/items/flint_workbench.json 按 block_state 属性 select 对应模型
 */
class BlockICPMFlintWorkbench(properties: Properties) :
    BlockICPMWorkbench(0, "燧石工作台", properties) {

    /** 木材衍生类型；ordinal 即 `wood` 状态值，顺序不可随意调整 */
    enum class WoodType(val zhName: String) {
        OAK("橡木"),
        SPRUCE("云杉木"),
        BIRCH("白桦木"),
        JUNGLE("丛林木"),
        ACACIA("金合欢木"),
        DARK_OAK("深色橡木"),
        MANGROVE("红树木"),
        CHERRY("樱木"),
        BAMBOO("竹"),
        CRIMSON("绯红木"),
        WARPED("诡异木");

        /** 状态属性值 */
        val index: Int get() = ordinal

        /** 完整显示名，如「红树木燧石工作台」 */
        val workbenchName: String get() = zhName + "燧石工作台"

        companion object {
            @JvmStatic
            fun fromIndex(index: Int): WoodType = values().getOrElse(index) { OAK }
        }
    }

    companion object {
        /** 木材属性：0..10，顺序与 [WoodType] 一致 */
        @JvmField
        val WOOD: IntegerProperty = IntegerProperty.create("wood", 0, 10)
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(WOOD, 0))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(WOOD)
    }

    /** 容器标题随木材变化，如「红树木燧石工作台」 */
    override fun getMenuProvider(
        state: BlockState,
        level: Level,
        pos: BlockPos
    ): ExtendedScreenHandlerFactory<BlockPos> {
        val title = WoodType.fromIndex(state.getValue(WOOD)).workbenchName
        return object : ExtendedScreenHandlerFactory<BlockPos> {
            override fun getDisplayName(): Component = Component.literal(title)

            override fun getScreenOpeningData(player: ServerPlayer): BlockPos = pos

            override fun createMenu(
                syncId: Int,
                playerInventory: Inventory,
                player: Player
            ): AbstractContainerMenu =
                ICPMWorkbenchMenu(syncId, playerInventory, ContainerLevelAccess.create(level, pos), 0)
        }
    }
}
