package name.icpm.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * R196 枯死作物方块（BlockCropsDead / BlockPotatoDead / BlockCarrotDead / BlockOnionDead 忠实移植）。
 *
 * 触发（BlockCrops.updateTick:83-97，由 CropBlockMixin 驱动）：
 * - 旱死：生长速率 0（耕地干燥）且 5%/随机刻 —— 未成熟转本方块（保留生长进度）；成熟的直接掉落收获物变空气；
 * - 疫病致死：染病作物 1/64/随机刻 —— 转本方块（成熟的进度 -1，未成熟保持）。
 *
 * 忠实行为（BlockCropsDead.java）：
 * - setTickRandomly(false)：无随机刻（不会复活/生长）；
 * - fertilize=false：骨粉无效（isValidBonemealTarget 恒 false）；
 * - dropBlockAsEntityItem=0：破坏无任何掉落（由空 loot_table data/icpm/loot_table/blocks/dead_crop.json 保证）；
 * - 仅保留生长进度（AGE 0-7）用于视觉阶段（dead/0-6 贴图，源自 MITE RP 1.6.41）。
 */
public class ICPMDeadCropBlock extends CropBlock {

    // 父类 codec 返回 MapCodec<CropBlock>，子类不可协变（与 ICPMSparkBlock 同理）
    public static final MapCodec<CropBlock> CODEC = CropBlock.simpleCodec(ICPMDeadCropBlock::new);

    public ICPMDeadCropBlock(Properties properties) {
        super(properties);
        // R196: setTickRandomly(false) —— 枯死作物不再参与随机刻
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, Integer.valueOf(0)));
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        // 泛型不协变：声明为父类 codec 类型
        return CODEC;
    }

    /** R196 BlockCropsDead.fertilize = false：骨粉无效 */
    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, BlockState state) {
        return false;
    }

    /** 无 BlockItem；种子仅作 CropBlock 抽象兜底，实际无掉落 */
    @Override
    protected ItemLike getBaseSeedId() {
        return Items.WHEAT_SEEDS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AGE);
    }

    /** 供 Mixin 按生长进度放置：1.21 CropBlock.AGE 0-7 → 视觉阶段 0-6 */
    public static int stageOf(int age) {
        return Math.min(age, 6);
    }

    /** 供 CropBlockMixin 转死时构造目标状态（保留 R196 生长进度） */
    public static BlockState placeState(int age) {
        return INSTANCE.defaultBlockState().setValue(AGE, Math.max(0, Math.min(7, age)));
    }

    /** 由 ICPM.java 注册后回填（同 SPARK 模式） */
    public static ICPMDeadCropBlock INSTANCE;
}
