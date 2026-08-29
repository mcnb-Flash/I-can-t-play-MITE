package name.icpm.blockentity

import name.icpm.ICPM
import name.icpm.block.BlockMetalAnvil
import name.icpm.block.ICPMBlocks
import net.minecraft.core.Registry
import net.minecraft.world.level.block.Block
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BlockEntityType

/**
 * ICPM 方块实体注册
 */
object ICPMBlockEntities {

    /**
     * 初始化并注册所有方块实体类型
     */
    @JvmStatic
    fun init() {
        // 金属砧方块实体绑定的方块集合（含全阶段 / 全材质变体）
        // 注意：铁砧已删除（原版 minecraft:anvil 即铁砧，无 ICPM 方块实体耐久），不在此列表
        val anvilBlocks: Array<Block> = arrayOf(
            ICPMBlocks.COPPER_ANVIL!!,
            ICPMBlocks.SILVER_ANVIL!!,
            ICPMBlocks.GOLD_ANVIL!!,
            ICPMBlocks.ANCIENT_METAL_ANVIL!!,
            ICPMBlocks.MITHRIL_ANVIL!!,
            ICPMBlocks.ADAMANTIUM_ANVIL!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.COPPER, 1)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.COPPER, 2)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.SILVER, 1)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.SILVER, 2)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.GOLD, 1)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.GOLD, 2)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.ANCIENT_METAL, 1)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.ANCIENT_METAL, 2)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.MITHRIL, 1)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.MITHRIL, 2)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.ADAMANTIUM, 1)!!,
            ICPMBlocks.getAnvilVariant(BlockMetalAnvil.MetalType.ADAMANTIUM, 2)!!
        )

        // 原版砧方块（旧存档把砧方块实体挂在这些方块上，而非 ICPM 金属砧方块）
        val vanillaAnvilBlocks = arrayOf(
            net.minecraft.world.level.block.Blocks.ANVIL,
            net.minecraft.world.level.block.Blocks.CHIPPED_ANVIL,
            net.minecraft.world.level.block.Blocks.DAMAGED_ANVIL
        )

        // 旧存档把砧方块实体 id=icpm:metal_anvil 挂在了「原版砧方块」(anvil/chipped_anvil/damaged_anvil) 上，
        // 因此 metal_anvil 的有效方块集合除 ICPM 金属砧外，还**必须包含原版砧**，
        // 否则加载旧存档时 Mojang 校验「方块实体类型对该方块无效」抛
        // IllegalStateException: Invalid block entity icpm:metal_anvil ... got Block{minecraft:damaged_anvil}。
        val allAnvilBlocks = anvilBlocks + vanillaAnvilBlocks

        // 注册金属砧方块实体类型（当前 id：metal_anvil）
        TileEntityMetalAnvil.TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "metal_anvil"),
            BlockEntityTypeBuilder.create(TileEntityMetalAnvil.Companion::create, *allAnvilBlocks)
        )

        // 兼容更早版本：旧存档用 "vanilla_anvil" 作为砧方块实体 id（同样挂在原版砧方块上）。
        // 额外用旧 id 注册一个等价类型，使旧存档的砧方块实体能正常加载而不被丢弃。
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "vanilla_anvil"),
            BlockEntityTypeBuilder.create(TileEntityMetalAnvil.Companion::create, *vanillaAnvilBlocks)
        )

        // 原版铁砧已完全还原为纯原版（无 ICPM 方块实体 / 无耐久魔改）

        // 注册 ICPM 熔炉方块实体类型
        // 绑定：原版熔炉（mixin 替换为原石熔炉）+ 粘土/硬化粘土/沙石/黑曜石/地狱岩熔炉
        ICPMFurnaceBlockEntity.TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "mite_furnace"),
            BlockEntityTypeBuilder.create(
                ICPMFurnaceBlockEntity.Companion::create,
                net.minecraft.world.level.block.Blocks.FURNACE,
                ICPMBlocks.CLAY_FURNACE!!,
                ICPMBlocks.HARDENED_CLAY_FURNACE!!,
                ICPMBlocks.SANDSTONE_FURNACE!!,
                ICPMBlocks.OBSIDIAN_FURNACE!!,
                ICPMBlocks.NETHERRACK_FURNACE!!
            )
        )

        // 注册 ICPM 金属箱（强箱）方块实体类型
        ICPMStrongboxBlockEntity.TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "strongbox"),
            BlockEntityTypeBuilder.create(
                ICPMStrongboxBlockEntity.Companion::create,
                ICPMBlocks.SILVER_STRONGBOX!!,
                ICPMBlocks.GOLD_STRONGBOX!!,
                ICPMBlocks.IRON_STRONGBOX!!,
                ICPMBlocks.ANCIENT_METAL_STRONGBOX!!,
                ICPMBlocks.MITHRIL_STRONGBOX!!,
                ICPMBlocks.ADAMANTIUM_STRONGBOX!!
            )
        )
    }

    /**
     * BlockEntityType构建器
     * 使用Fabric API提供的构建器
     */
    private object BlockEntityTypeBuilder {
        @JvmStatic
        fun <T : net.minecraft.world.level.block.entity.BlockEntity> create(
            factory: (net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState) -> T,
            vararg blocks: net.minecraft.world.level.block.Block
        ): BlockEntityType<T> {
            return net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(factory, *blocks).build()
        }
    }
}