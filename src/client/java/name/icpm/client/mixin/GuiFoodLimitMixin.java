package name.icpm.client.mixin;

import name.icpm.common.PlayerStatsManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 限制饱食度栏渲染的格数 = 当前饱食度上限 / 2。
 *
 * 原版 Gui.renderFood 用固定 for(l=0; l<10; l++) 画 10 个鸡腿格（右对齐）。
 * ICPM 的饱食度上限是动态的（PlayerStatsManager.calculateMaxFood(level)，6~20 恒偶数），
 * 因此这里把循环上界的常量 10 改成 cells = maxFood/2，只渲染当前上限对应的格数，
 * 多余的空格（原版里显示在左侧的空鸡腿轮廓）不再绘制。
 *
 * 实现要点：
 * - 不用 @Shadow（本模组 mixin 无 refmap，会崩溃），改用 @Inject(HEAD) 把格子数
 *   存进 @Unique 字段，再用 @ModifyConstant 把循环里的 10 替换成该值。
 * - renderFood 方法签名为 (GuiGraphics, Player, int, int)，Player 由调用方直接传入。
 * - 饱食度 foodLevel 已在服务端被钳制到 maxFood，fill/half 绘制逻辑天然落在 cells 内。
 */
@Mixin(Gui.class)
public class GuiFoodLimitMixin {

    @Unique
    private int icpm$maxFoodCells = 10;

    @Inject(method = "renderFood", at = @At("HEAD"))
    private void icpm$captureMaxFood(GuiGraphics guiGraphics, Player player, int i, int j, CallbackInfo ci) {
        if (player != null) {
            icpm$maxFoodCells = PlayerStatsManager.calculateMaxFood(player.experienceLevel) / 2;
        }
    }

    @ModifyConstant(method = "renderFood", constant = @Constant(intValue = 10))
    private int icpm$limitFoodCells(int original) {
        return icpm$maxFoodCells;
    }
}
