package name.icpm.block

import name.icpm.ICPM
import name.icpm.item.ICPMItems
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BiomeTags
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BushBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.entity.player.Player

/**
 * 蓝莓丛 —— R196 BlockBush(blueberry) 移植（简化两态）：
 *   age=1 有果（森林生成即此态）：右键摘果 → 掉 1~2 蓝莓并回 age=0（R196 drop blueberries + setBerryGrowth 0）
 *   age=0 无果：骨粉催熟回 age=1；随机刻 1/40 自然再结果（R196 再生语义近似）
 *   破坏：age=1 时掉落 1~2 蓝莓（走 loot 表 age 条件），age=0 不掉
 *   贴图：RP bushes/blueberry.png + blueberry_picked.png（两态）
 */
object ICPMBlueberryBush {

    val AGE: IntegerProperty = IntegerProperty.create("age", 0, 1)

    val BLUEBERRY_BUSH_ID: Identifier = Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "blueberry_bush")

    @JvmField
    var BLUEBERRY_BUSH_BLOCK: ICPMBlueberryBushBlock? = null

    @JvmField
    var BLUEBERRY_BUSH_ITEM: net.minecraft.world.item.Item? = null

    fun register() {
        val blockKey = ResourceKey.create(Registries.BLOCK, BLUEBERRY_BUSH_ID)
        val props = BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .instabreak()
            .sound(SoundType.GRASS)
            .noCollision()
            .noOcclusion()
            .randomTicks()
            .setId(blockKey)
        val block = ICPMBlueberryBushBlock(props)
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block)
        BLUEBERRY_BUSH_BLOCK = block

        // 方块物品注册（可放置/可被剪刀剪下带走；入创造标签由 icpm 物品组管理）
        val itemKey = ResourceKey.create(Registries.ITEM, BLUEBERRY_BUSH_ID)
        val itemProps = net.minecraft.world.item.Item.Properties().setId(itemKey)
        val blockItem = net.minecraft.world.item.BlockItem(block, itemProps)
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem)
        BLUEBERRY_BUSH_ITEM = blockItem

        // 森林 biome：VEGETAL_DECORATION 步骤挂载 placed_feature（json: icpm/worldgen/placed_feature/blueberry_bush.json）
        val placedKey = ResourceKey.create(
            Registries.PLACED_FEATURE,
            Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "blueberry_bush")
        )
        BiomeModifications.addFeature(
            BiomeSelectors.tag(BiomeTags.IS_FOREST),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            placedKey
        )
    }
}

class ICPMBlueberryBushBlock(properties: BlockBehaviour.Properties) : BushBlock(properties) {

    init {
        registerDefaultState(this.stateDefinition.any().setValue(ICPMBlueberryBush.AGE, 1))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(ICPMBlueberryBush.AGE)
    }

    override fun isRandomlyTicking(state: BlockState): Boolean = true

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        // 无果丛随机再结果（≈R196 自然再生长）
        if (state.getValue(ICPMBlueberryBush.AGE) == 0 && random.nextInt(8) == 0) {
            level.setBlock(pos, state.setValue(ICPMBlueberryBush.AGE, 1), 2)
        }
    }

    override fun getStateForPlacement(context: net.minecraft.world.item.context.BlockPlaceContext): BlockState =
        defaultBlockState()

    /** 右键交互（空手与手持非骨粉物品共用）：仅在有果(age=1)时可摘取；空枝(age=0)返回 PASS（R196 语义：被采后需等自然再生或骨粉） */
    private fun rightClickBush(state: BlockState, level: Level, pos: BlockPos): InteractionResult {
        val age = state.getValue(ICPMBlueberryBush.AGE)
        if (age == 0) {
            // 空枝不可摘（R196：结果期过后需要再生/骨粉）
            return InteractionResult.PASS
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        }
        val serverLevel = level as ServerLevel
        val n = 1 + serverLevel.random.nextInt(2) // 1~2 颗
        Block.popResource(serverLevel, pos, ItemStack(ICPMItems.BLUEBERRY, n))
        serverLevel.setBlock(pos, state.setValue(ICPMBlueberryBush.AGE, 0), 2)
        return InteractionResult.SUCCESS
    }

    /** 空手右键摘果 */
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult = rightClickBush(state, level, pos)

    /** 手持物品右键：剪刀剪取整丛；骨粉催熟空枝；其余物品仅在有果时摘取 */
    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): InteractionResult {
        // R196 ItemShears.onItemRightClick：剪刀右键 = silk 剪下整丛（方块物品掉落，可重放）
        if (stack.item is net.minecraft.world.item.ShearsItem) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS
            }
            val serverLevel = level as ServerLevel
            val bushItem = ICPMBlueberryBush.BLUEBERRY_BUSH_ITEM
            if (bushItem != null && bushItem != net.minecraft.world.item.Items.AIR) {
                Block.popResource(serverLevel, pos, ItemStack(bushItem))
            }
            serverLevel.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0f, 1.0f)
            serverLevel.destroyBlock(pos, false)
            if (!player.abilities.instabuild) {
                val slot = if (hand == InteractionHand.MAIN_HAND) EquipmentSlot.MAINHAND else EquipmentSlot.OFFHAND
                stack.hurtAndBreak(1, player, slot)
            }
            return InteractionResult.SUCCESS
        }
        if (stack.`is`(Items.BONE_MEAL)) {
            if (state.getValue(ICPMBlueberryBush.AGE) == 0) {
                if (!level.isClientSide) {
                    level.setBlock(pos, state.setValue(ICPMBlueberryBush.AGE, 1), 2)
                    if (!player.abilities.instabuild) {
                        stack.shrink(1)
                    }
                }
                return InteractionResult.SUCCESS
            }
            return InteractionResult.PASS
        }
        // 手持其它物品：直接走摘取/再生交互
        return rightClickBush(state, level, pos)
    }
}
