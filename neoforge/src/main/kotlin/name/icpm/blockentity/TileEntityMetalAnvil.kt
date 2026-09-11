package name.icpm.blockentity

import name.icpm.block.BlockMetalAnvil
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * 金属砧方块实体
 *
 * 参考1.18.2 ICPM源码（Icpm_fgj）实现：
 * - 存储砧的损坏值 damage，NBT key 为 "Damage"
 * - addDamage 根据阈值切换方块变体（完好/chipped/damaged/销毁）
 */
class TileEntityMetalAnvil(
    pos: BlockPos,
    state: BlockState,
    val metalType: BlockMetalAnvil.MetalType
) : BlockEntity(TYPE, pos, state) {

    companion object {
        @JvmStatic
        lateinit var TYPE: BlockEntityType<TileEntityMetalAnvil>

        /**
         * 创建方块实体的工厂函数
         */
        @JvmStatic
        fun create(pos: BlockPos,  state: BlockState): TileEntityMetalAnvil {
            val block = state.block as? BlockMetalAnvil
            // 原版铁砧（minecraft:anvil/chipped_anvil/damaged_anvil）走 ICPM 砧耐久体系时，
            // 等价 IRON 级砧；ICPM 金属砧各自有其 metalType。
            val metalType = block?.metalType ?: BlockMetalAnvil.MetalType.IRON
            return TileEntityMetalAnvil(pos, state, metalType)
        }
    }

    // 砧的损坏值
    var damage: Int = 0
        private set

    /**
     * 增加损坏值
     * 达到销毁阈值则销毁方块；跨阶段则切换为对应变体方块并保留损伤
     * 原版铁砧（minecraft:*_anvil）走同一套机制：以原版 ANVIL_DAMAGE 状态表现裂痕/损坏外观。
     */
    fun addDamage(level: Level, pos: BlockPos, amount: Int) {
        if (level.isClientSide) {
            return
        }

        this.damage += amount

        val blockAt = level.getBlockState(pos).block
        val damageStage = getDamageStage()

        if (damageStage >= 3) {
            // 销毁方块（不掉落物品）
            level.destroyBlock(pos, false)
            return
        }

        // ICPM 金属砧：在同一方块上切换 stage 状态属性（不再换方块）
        if (blockAt is BlockMetalAnvil) {
            val state = level.getBlockState(pos)
            if (state.getValue(BlockMetalAnvil.STAGE) != damageStage) {
                level.setBlock(pos, state.setValue(BlockMetalAnvil.STAGE, damageStage), 2)
            }
        } else if (blockAt is net.minecraft.world.level.block.AnvilBlock) {
            // 原版铁砧：通过切换到 Anvil/ChippedAnvil/DamagedAnvil 方块表现裂痕/损坏外观
            val target = when (damageStage) {
                1 -> net.minecraft.world.level.block.Blocks.CHIPPED_ANVIL
                2 -> net.minecraft.world.level.block.Blocks.DAMAGED_ANVIL
                else -> net.minecraft.world.level.block.Blocks.ANVIL
            }
            val facing = level.getBlockState(pos).getValue(BlockMetalAnvil.FACING)
            val newState = target.defaultBlockState().setValue(BlockMetalAnvil.FACING, facing)
            level.setBlock(pos, newState, 2)
        }

        // 更新客户端
        setChanged()
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3)
    }

    /**
     * 获取当前损坏阶段
     */
    fun getDamageStage(): Int {
        val block = this.blockState.block as? BlockMetalAnvil ?: return 0
        return block.getDamageStage(this.damage)
    }

    /**
     * 获取砧的剩余耐久百分比
     */
    fun getDurabilityPercentage(): Float {
        val block = this.blockState.block as? BlockMetalAnvil ?: return 1.0f
        val maxDurability = block.maxDurability
        return 1.0f - (this.damage.toFloat() / maxDurability.toFloat())
    }

    /**
     * 检查砧是否已损坏
     */
    fun isDestroyed(): Boolean {
        return getDamageStage() >= 3
    }

    override fun loadAdditional(valueInput: net.minecraft.world.level.storage.ValueInput) {
        super.loadAdditional(valueInput)
        this.damage = valueInput.getInt("Damage").orElse(0)
    }

    override fun saveAdditional(valueOutput: net.minecraft.world.level.storage.ValueOutput) {
        super.saveAdditional(valueOutput)
        valueOutput.putInt("Damage", this.damage)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}
