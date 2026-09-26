package name.icpm.client.mixin;

import name.icpm.common.ICPMDevMode;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创建世界界面：把「难度」选择器锁死为困难且置灰（不可点、不可改）。
 *
 * <p>功能层的强制在 {@code ICPMHardDifficultyMixin}（服务端）兜底，这里只负责界面侧：让难度按钮显示
 * 「困难」并变为灰色、不可交互，避免出现“显示可改、实际被拉回”的误导性界面。
 *
 * <p>难度按钮是 {@code CycleButton<Difficulty>}，与游戏模式（{@code CycleButton<SelectedGameMode>}）、
 * 允许作弊（{@code CycleButton<Boolean>}）同为该界面下的 CycleButton；此处仅按取值类型识别难度按钮，
 * 不影响其它选择器。
 *
 * <p>dev 模式开启时不做任何限制。
 */
@Mixin(CreateWorldScreen.class)
public class ICPMWorldDifficultyGateMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void icpm$lockDifficulty(CallbackInfo ci) {
        if (ICPMDevMode.isEnabled()) {
            return;
        }
        Screen self = (Screen) (Object) this;
        for (GuiEventListener child : self.children()) {
            if (child instanceof CycleButton<?> button) {
                if (button.getValue() instanceof Difficulty) {
                    ((CycleButton<Difficulty>) button).setValue(Difficulty.HARD);
                    button.active = false;
                }
            }
        }
    }
}
