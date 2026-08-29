package name.icpm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ICPM 传送门框架验证工具
 * 类似原版 PortalShape，用于检查传送门结构是否完整。
 *
 * 当黑曜石框架被破坏时，传送门方块应自动消失。
 */
public class ICPMPortalShape {

    private static final int MAX_WIDTH = 21;
    private static final int MAX_HEIGHT = 21;
    private static final int MIN_WIDTH = 2;
    private static final int MIN_HEIGHT = 3;

    private final BlockGetter level;
    private final BlockPos origin;
    private final Direction.Axis axis;
    private int width;
    private int height;
    private BlockPos bottomLeft;

    public ICPMPortalShape(BlockGetter level, BlockPos pos, Direction.Axis axis) {
        this.level = level;
        this.origin = pos;
        this.axis = axis;
        scan();
    }

    /**
     * 从原点向左/下扫描，找到传送门左下角和尺寸。
     */
    private void scan() {
        Direction.Axis axis = this.axis;
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        Direction downDir = Direction.DOWN;

        // 从原点向下找到最低的传送门方块
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        cursor.set(this.origin);
        while (cursor.getY() > this.level.getMinY() && isPortal(this.level.getBlockState(cursor.move(downDir)))) {
        }
        // cursor 现在在传送门下方的第一个非传送门方块
        BlockPos bottomCenter = cursor.above();

        // 从 bottomCenter 向右Dir 反方向扫描找到左边缘
        Direction leftDir = rightDir.getOpposite();
        int leftCount = 0;
        cursor.set(bottomCenter);
        while (leftCount <= MAX_WIDTH && isPortal(this.level.getBlockState(cursor.move(leftDir)))) {
            leftCount++;
        }
        // cursor 现在在传送门左边界的左侧（非传送门方块）
        BlockPos bottomLeft = cursor.relative(rightDir);

        // 向右扫描找到宽度
        int width = 0;
        cursor.set(bottomLeft);
        while (width <= MAX_WIDTH && isPortal(this.level.getBlockState(cursor))) {
            width++;
            cursor.move(rightDir);
        }

        // 向上扫描找到高度
        int height = 0;
        cursor.set(bottomLeft);
        while (height <= MAX_HEIGHT && isPortal(this.level.getBlockState(cursor))) {
            height++;
            // 检查这一行的最右端是否也是传送门
            BlockPos rightEdge = bottomLeft.relative(rightDir, width - 1).above(height - 1);
            if (!isPortal(this.level.getBlockState(rightEdge))) {
                break;
            }
            cursor.move(Direction.UP);
        }

        if (width < MIN_WIDTH || height < MIN_HEIGHT || width > MAX_WIDTH || height > MAX_HEIGHT) {
            this.width = 0;
            this.height = 0;
            this.bottomLeft = null;
            return;
        }

        this.width = width;
        this.height = height;
        this.bottomLeft = bottomLeft;
    }

    /**
     * 检查传送门框架是否有效（黑曜石完整）。
     */
    public boolean isValid() {
        if (this.bottomLeft == null || this.width < MIN_WIDTH || this.height < MIN_HEIGHT) {
            return false;
        }
        // scan() 中 X 轴使用 WEST 作为右方向（bottomLeft 为东端，传送门向西延伸），这里必须保持一致
        Direction rightDir = this.axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // 检查底部一排（y = bottomLeft.y - 1, 从 bottomLeft 向右 width 格）
        for (int i = 0; i < this.width; i++) {
            cursor.set(this.bottomLeft).move(rightDir, i).move(Direction.DOWN);
            if (!isFrame(this.level.getBlockState(cursor))) {
                return false;
            }
        }

        // 检查顶部一排（y = bottomLeft.y + height）
        for (int i = 0; i < this.width; i++) {
            cursor.set(this.bottomLeft).move(rightDir, i).move(Direction.UP, this.height);
            if (!isFrame(this.level.getBlockState(cursor))) {
                return false;
            }
        }

        // 检查左侧一列（bottomLeft 的右方向反方向一格的整列）
        for (int j = 0; j < this.height; j++) {
            cursor.set(this.bottomLeft).move(Direction.UP, j).move(rightDir.getOpposite());
            if (!isFrame(this.level.getBlockState(cursor))) {
                return false;
            }
        }

        // 检查右侧一列（bottomLeft 右方向 width 格的整列）
        for (int j = 0; j < this.height; j++) {
            cursor.set(this.bottomLeft).move(Direction.UP, j).move(rightDir, this.width);
            if (!isFrame(this.level.getBlockState(cursor))) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查一个位置是否在传送门内部。
     */
    public boolean isPortal(BlockPos pos) {
        if (this.bottomLeft == null) return false;
        Direction rightDir = this.axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        int relX;
        if (this.axis == Direction.Axis.X) {
            relX = this.bottomLeft.getX() - pos.getX();
        } else {
            relX = this.bottomLeft.getZ() - pos.getZ();
        }
        int relY = pos.getY() - this.bottomLeft.getY();
        if (relX < 0 || relX >= this.width || relY < 0 || relY >= this.height) {
            return false;
        }
        // 检查 z 坐标（对于 X 轴）或 x 坐标（对于 Z 轴）
        if (this.axis == Direction.Axis.X) {
            return pos.getZ() == this.bottomLeft.getZ();
        } else {
            return pos.getX() == this.bottomLeft.getX();
        }
    }

    /**
     * 检查一个方块是否是传送门方块（ICPM 三种传送门之一）。
     */
    public static boolean isPortal(BlockState state) {
        Block block = state.getBlock();
        return block instanceof UnderworldPortalBlock
                || block instanceof ReturnPortalBlock
                || block instanceof HellPortalBlock;
    }

    /**
     * 检查一个方块是否是框架方块（黑曜石）。
     */
    public static boolean isFrame(BlockState state) {
        return state.is(Blocks.OBSIDIAN);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public BlockPos getBottomLeft() {
        return bottomLeft;
    }

    public Direction.Axis getAxis() {
        return axis;
    }
}
