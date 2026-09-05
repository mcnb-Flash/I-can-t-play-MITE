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
import net.minecraft.tags.BiomeTags
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
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
        if (state.getValue(ICPMBlueberryBush.AGE) == 0 && random.nextInt(40) == 0) {
            level.setBlock(pos, state.setValue(ICPMBlueberryBush.AGE, 1), 2)
        }
    }

    override fun getStateForPlacement(context: net.minecraft.world.item.context.BlockPlaceContext): BlockState =
        defaultBlockState()

    /** 右键摘果（R196 空手摘蓝莓并重置生长） */
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (state.getValue(ICPMBlueberryBush.AGE) == 0) {
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

    /** 骨粉催熟无果丛（R196 fertilize 语义） */
    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (stack.`is`(Items.BONE_MEAL) && state.getValue(ICPMBlueberryBush.AGE) == 0) {
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
}
