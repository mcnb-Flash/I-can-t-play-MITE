package name.icpm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * R196 {@code BlockSpark}（火花）忠实移植。
 *
 * <p>1.6.4 权威实现：
 * <pre>
 * public class BlockSpark extends BlockFire {
 *     public void onBlockAdded(World w, int x, int y, int z) {
 *         if (Block.portal.tryToCreatePortal_(w, x, y, z)) return;   // 传送门优先
 *         w.scheduleBlockUpdate(x, y, z, this.blockID, 2);            // 2 tick 后判定
 *     }
 *     public boolean updateTick(World w, int x, int y, int z, Random r) {
 *         if (this.canNeighborBurn(w, x, y, z) || w.getBlock(x, y - 1, z) == Block.netherrack) {
 *             w.setBlock(x, y, z, Block.fire.blockID);                // 邻格可燃/下方地狱岩 → 变火
 *         } else {
 *             w.setBlockToAir(x, y, z);                               // 否则直接消失
 *         }
 *         return true;
 *     }
 * }
 * </pre>
 *
 * <p>要点：火花本身不具任何原版火行为——不蔓延、不因邻居变化被移除、不受降雨影响、不参与随机刻，
 * 只等自己的排程 tick 做一次「变火 or 消失」判定。
 * （1.6.4 的 {@code Block.portal.tryToCreatePortal_} 对应本移植的 ICPM 传送门逻辑，
 * 由 {@code FlintAndSteelMixin} 的黑曜石框架分支处理，此处不需要。）
 */
public class ICPMSparkBlock extends FireBlock {

    public static final MapCodec<FireBlock> CODEC = simpleCodec(ICPMSparkBlock::new);

    /** R196 {@code scheduleBlockUpdate(..., 2)}：火花存活 2 tick。 */
    public static final int SPARK_TICKS = 2;

    public ICPMSparkBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<FireBlock> codec() {
        return CODEC;
    }

    /**
     * R196 {@code updateTick}：邻格可燃（canNeighborBurn）或下方为地狱岩 → 变成火，否则消失。
     * 必须覆盖而不调用 super —— 原版火的 tick 会蔓延并自行排程。
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (canNeighborBurn(level, pos) || level.getBlockState(pos.below()).is(Blocks.NETHERRACK)) {
            BlockState fire = BaseFireBlock.getState(level, pos);
            level.setBlock(pos, fire.isAir() ? Blocks.FIRE.defaultBlockState() : fire, 3);
        } else {
            level.removeBlock(pos, false);
        }
    }

    /**
     * R196 {@code onBlockAdded}：放置后 2 tick 判定。
     * 不调用 super（super 按原版火排程 30~40 tick 并进入蔓延流程）。
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, SPARK_TICKS);
        }
    }

    /** 火花是纯中间态：状态恒为默认值（R196 metadata 恒为 0）。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    /**
     * R196 {@code canNeighborBurn}：六向邻格任一「可被点燃」即真。
     *
     * <p>1.6.4 用的是 {@code BlockFire.canBlockCatchFire} = {@code abilityToCatchFire[blockID] > 0}；
     * 现代等价公开入口为 {@link FireBlock#getIgniteOdds(BlockState)}（同一张可燃/助燃表，
     * 凡 vanilla 可燃方块两列均 > 0，判定集合一致）。
     */
    public boolean canNeighborBurn(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (this.canBurn(neighbor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 火花不因相邻方块变化而被移除：1.6.4 无 updateShape 语义，火花只由自身排程 tick 结束。
     * （原版火会在此返回空气，导致无燃料处放置的火花被立刻清掉。）
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        return state;
    }

    /** 火花可在任意位置存在满 2 tick（原版火的 canSurvive 会拒绝无燃料位置）。 */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    /** 不参与随机刻（R196 火花只跑一次排程判定）。 */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return false;
    }
}
