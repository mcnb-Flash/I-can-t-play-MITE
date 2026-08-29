package name.icpm.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 符文石（R196 BlockRunestone，忠实移植）
 *
 * - 16 个魔法名变体（Nul, Quas, Por, An, Nox, Flam, Vas, Des, Ort, Tym, Corp, Lor, Mani, Jux, Ylem, Sanct），
 *   存于 {@link #VARIANT} 方块属性（0..15）。
 * - 黑曜石强度，可挖掘（注册进 mineable/pickaxe 标签）。
 * - 作为符文门框架 4 角时，4 角同金属符文石的变体组合成 seed，决定同维度内传送坐标（见 ICPMPortalHandler）。
 */
public class BlockRunestone extends Block {

    /** 符文变体属性（0..15） */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 15);

    /** R196 BlockRunestone.magic_names（与源码顺序一致） */
    public static final String[] MAGIC_NAMES = {
        "Nul", "Quas", "Por", "An", "Nox", "Flam", "Vas", "Des",
        "Ort", "Tym", "Corp", "Lor", "Mani", "Jux", "Ylem", "Sanct"
    };

    private final MetalType metal;

    public enum MetalType {
        MITHRIL,
        ADAMANTIUM
    }

    public BlockRunestone(MetalType metal, BlockBehaviour.Properties properties) {
        super(properties);
        this.metal = metal;
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    public MetalType getMetal() {
        return metal;
    }

    /** R196 BlockRunestone.getMagicName */
    public static String getMagicName(int metadata) {
        return MAGIC_NAMES[metadata & 15];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }
}
