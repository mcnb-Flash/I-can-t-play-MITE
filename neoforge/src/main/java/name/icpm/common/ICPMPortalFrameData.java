package name.icpm.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * ICPM 传送门框数据（从 FlintAndSteelMixin 拆出）。
 *
 * 同 {@link ICPMToolRulesData}：mixin 包内嵌套类型若被注入方法引用，
 * NeoForge 织入时会抛 IllegalClassLoadError，故移至普通包。
 */
public final class ICPMPortalFrameData {

    private ICPMPortalFrameData() {
    }

    /** 已找到的传送门框（黑曜石边界框 + 内部尺寸）。 */
    public static final class PortalFrame {
        public final Direction.Axis axis;
        public final Direction rightDir;
        public final BlockPos minPos;
        public final int width;
        public final int height;

        public PortalFrame(Direction.Axis axis, Direction rightDir, BlockPos minPos, int width, int height) {
            this.axis = axis;
            this.rightDir = rightDir;
            this.minPos = minPos;
            this.width = width;
            this.height = height;
        }
    }
}
