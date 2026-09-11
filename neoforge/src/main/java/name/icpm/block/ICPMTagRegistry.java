package name.icpm.block;

import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * ICPM 标签注册
 *
 * 用于追踪 ICPM 方块，便于在 ICPMToolRulesMixin 中识别。
 * 由于 1.21.11 的标签机制限制，方块标签通过
 * src/main/resources/data/icpm/tags/block/ 下的 JSON 文件定义。
 */
public class ICPMTagRegistry {

    /**
     * 需要镐挖掘的 ICPM 方块列表
     */
    private static final List<Block> PICKAXE_REQUIRED = new ArrayList<>();

    /**
     * 注册方块（占位，实际标签通过 JSON 定义）
     */
    public static void addToPickaxeMineable(Block block) {
        PICKAXE_REQUIRED.add(block);
    }

    /**
     * 检查方块是否需要镐
     */
    public static boolean isPickaxeRequired(Block block) {
        return PICKAXE_REQUIRED.contains(block);
    }
}
