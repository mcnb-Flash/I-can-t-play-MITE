package name.icpm.block;

import name.icpm.common.ICPMPortalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 地狱传送门方块
 * 实现原版 Portal 接口，通过 PortalProcessor 处理传送
 */
public class HellPortalBlock extends Block implements Portal {

    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create("axis", Direction.Axis.class, (axis) -> axis == Direction.Axis.X || axis == Direction.Axis.Z);

    protected static final VoxelShape X_SHAPE = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
    protected static final VoxelShape Z_SHAPE = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);

    public HellPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        Direction.Axis axis = direction.getAxis();
        Direction.Axis axis1 = state.getValue(AXIS);
        // 只有沿传送门轴向或垂直（上下）方向变化的邻居才可能是框架方块；
        // 垂直于传送门平面的水平邻居（传送门的侧面）永远不是框架，无需检查。
        if (axis1 != axis && axis.isHorizontal()) {
            return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
        }
        if (!ICPMPortalShape.isPortal(neighborState) && !new ICPMPortalShape(level, pos, axis1).isValid()) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    public float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean flag) {
        if (level.isClientSide()) {
            return;
        }
        if (!entity.canUsePortal(false)) {
            return;
        }
        entity.setAsInsidePortal(this, pos);
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 0;
    }

    @Override
    public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return ICPMPortalHandler.createTeleportTransition(level, entity, pos, ICPMPortalHandler.PortalType.HELL);
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.NONE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.addParticle(
                net.minecraft.core.particles.ParticleTypes.PORTAL,
                pos.getX() + random.nextDouble(),
                pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(),
                0.0, 0.0, 0.0
            );
        }
    }
}
