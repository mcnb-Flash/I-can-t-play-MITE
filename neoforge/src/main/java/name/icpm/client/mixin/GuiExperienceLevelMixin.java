package name.icpm.client.mixin;

import name.icpm.common.ICPMExperience;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 经验等级 HUD 覆盖（R196 带符号经验体系）：
 * 真实带符号等级从 player.totalExperience（带符号真值）派生，因为原版 experienceLevel 字段
 * 必须保持非负（负等级时 experienceLevel=0，详见 ICPMExperience.syncToVanilla），
 * 不能直接读 experienceLevel 显示负等级。
 *
 * 在屏幕底部中央（原版经验条正上方）绘制真实带符号等级：
 *   - 负等级只在屏幕底部中央显示等级数字"-N"并以红色标出（惩罚态），不加"等级"前缀、不加背景框；
 *   - 非负等级由原版负责绘制（experienceLevel==level，无需重复画）。
 *
 * 注意：drawString 的颜色参数按 0xAARRGGBB 解释，alpha 字节为 0 时文字完全透明不可见。
 * 因此文字颜色必须带满 alpha（0xFF 前缀），否则画出来等于没画（之前踩的坑）。
 *
 * 1.21.11 起 Gui.render 签名为 (GuiGraphics, DeltaTracker)，注入形参必须逐字匹配。
 */
@Mixin(Gui.class)
public class GuiExperienceLevelMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void icpm$drawLevel(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // 从带符号真值 totalExperience 推导真实等级（负等级为负数）
        int total = player.totalExperience;
        int level = ICPMExperience.getExperienceLevel(total);
        // 仅负等级需要自定义绘制；非负等级与原版 experienceLevel 一致，交给原版画即可。
        if (level >= 0) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // 功能文字：底部中央只显示等级数字（如 "-1"），红色，带满 alpha。
        String text = String.valueOf(level); // level<0 时 Java 自动输出 "-N"
        int color = 0xFFFF5555;               // 0xAARRGGBB：满 alpha + 红色
        int textW = mc.font.width(text);
        int x = (width - textW) / 2;
        int y = height - 31 - 4;             // 经验条正上方一格
        guiGraphics.drawString(mc.font, Component.literal(text), x, y, color, true);
    }
}
