package name.icpm.mixin;

import name.icpm.common.ICPMPlantDisease;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 骨粉机制（1.6.4 ItemBoneMeal 移植）
 *
 * 原版骨粉可以催熟作物；ICPM 中骨粉**只能治疗患病作物**，不能催熟：
 * - 目标方块是患病的 CropBlock → 治疗（移除病害状态），消耗 1 个骨粉，成功
 * - 其他任何情况 → PASS（不催熟、不消耗）
 *
 * 作物病害来源见 CropBlockMixin（随机刻 1/512 感染）。
 */
@Mixin(BoneMealItem.class)
public abstract class BoneMealMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void icpm$cureDiseaseOnly(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ResourceKey<Level> dim = level.dimension();

        if (state.getBlock() instanceof CropBlock && ICPMPlantDisease.isDiseased(dim, pos)) {
            if (!level.isClientSide()) {
                ICPMPlantDisease.cure(dim, pos);
                context.getItemInHand().shrink(1);
                // 治疗粒子（绿色十字/经验粒子）
                level.levelEvent(2005, pos, 0);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        // 非患病作物（或非作物）：骨粉无效，禁止催熟
        cir.setReturnValue(InteractionResult.PASS);
    }
}
