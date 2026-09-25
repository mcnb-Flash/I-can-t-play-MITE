package name.icpm.client.mixin;

import name.icpm.common.ICPMDevMode;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 局域网开放界面：把「游戏模式」与「允许作弊」两个选择器直接锁死（灰掉、不可点）。
 *
 * <p>{@link ICPMLanCheatGateMixin} 已经在 {@code IntegratedServer.publishServer} 把参数归一为
 * 生存 + 不允许作弊（硬保证）；这里补上界面侧，避免出现「按钮显示创造、实际发布的是生存」这种
 * 误导性显示：LAN 界面的 CycleButton 只有这两个（游戏模式 / 允许作弊），
 * 因此在 {@code init()} 结束时把它们统一置为不可交互，显示值停在初始的「生存 / 关」。
 *
 * <p>R196 {@code CommandServerPublishLocal:30} 即 {@code shareToLAN(SURVIVAL, false)}。
 *
 * <p>dev 模式开启时不做任何限制。
 */
@Mixin(ShareToLanScreen.class)
public class ICPMLanScreenGateMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void icpm$lockLanSelectors(CallbackInfo ci) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        Screen self = (Screen) (Object) this;
        for (GuiEventListener child : self.children()) {
            if (child instanceof CycleButton<?> button) {
                button.active = false;
            }
        }
    }
}
