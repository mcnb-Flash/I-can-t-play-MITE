package name.icpm.client.gui;

import name.icpm.common.DataSeal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.UUID;

/**
 * 「存档核心被破坏」弹窗：玩家被判定篡改存档后，进入世界前显示。
 *
 * <p>世界保持暂停（{@link #isPauseScreen} 返回 true），玩家无法操作。
 * 在输入框键入「我再也不作弊了」并点击「恢复存档核心」即可从 {@code icpmback} 备份还原 level.dat 与玩家数据；
 * 或点击「放弃此存档」直接返回标题（存档核心已破坏，需自行处理）。
 */
public class DataBreachScreen extends Screen {

    private final Path worldDir;
    private final Path levelBackup;
    private final Path playerBackup;
    private final UUID playerId;

    private EditBox inputBox;
    private String currentText = "";
    private String error = "";

    public DataBreachScreen(Path worldDir, Path levelBackup, Path playerBackup, UUID playerId) {
        super(Component.literal("存档核心已被破坏"));
        this.worldDir = worldDir;
        this.levelBackup = levelBackup;
        this.playerBackup = playerBackup;
        this.playerId = playerId;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        this.inputBox = new EditBox(this.font, cx - 120, this.height / 2 + 4, 240, 20,
                Component.literal("我再也不作弊了"));
        this.inputBox.setResponder(s -> this.currentText = s);
        this.inputBox.setMaxLength(64);
        this.addRenderableWidget(this.inputBox);
        this.setInitialFocus(this.inputBox);

        this.addRenderableWidget(Button.builder(Component.literal("恢复存档核心"), b -> this.onRestore())
                .bounds(cx - 120, this.height / 2 + 32, 240, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("放弃此存档"), b -> this.onAbandon())
                .bounds(cx - 120, this.height / 2 + 58, 240, 20)
                .build());
    }

    private void onRestore() {
        if (!"我再也不作弊了".equals(this.currentText.trim())) {
            this.error = "口令错误，请原样输入：我再也不作弊了";
            return;
        }
        if (DataSeal.restoreFromBackup(this.worldDir, this.playerId)) {
            Minecraft.getInstance().disconnectFromWorld(Component.literal("存档核心已恢复，请从标题重新进入。"));
        } else {
            this.error = "无可用备份，无法恢复（icpmback/level.dat.bak 缺失）。";
        }
    }

    private void onAbandon() {
        Minecraft.getInstance().disconnectFromWorld(Component.literal("存档核心已被破坏。"));
    }

    // 背景由本方法绘制；super.render() 会先调用它再画 widget，我们的提示文字最后画在 widget 之上
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC000000);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1) 先画背景 + widget（输入框、按钮）
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 2) 之后再把提示文字画在最上层，避免被背景覆盖
        //    颜色必须带 alpha 位（0xAARRGGBB）！drawString 对 alpha==0 的颜色会整条静默丢弃
        //    （0xFF5555 是 24 位字面量 = 0x00FF5555，alpha=0 → 文字不可见，此为上一版“没有提示”的根因）
        int cx = this.width / 2;
        guiGraphics.drawCenteredString(this.font, Component.literal("你进行了修改数据活动！"), cx, this.height / 2 - 64, 0xFFFF5555);
        guiGraphics.drawCenteredString(this.font, Component.literal("存档核心已被破坏"), cx, this.height / 2 - 44, 0xFFFF5555);
        guiGraphics.drawCenteredString(this.font,
                Component.literal("若想恢复，在下面的框里输入“我再也不作弊了”即可"), cx, this.height / 2 - 20, 0xFFDDDDDD);
        if (!this.error.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal(this.error), cx, this.height / 2 + 86, 0xFFFF7777);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // 篡改窗口不允许 Esc 关闭逃逸（存盘已被封锁，逃进世界也不会有任何进度落盘）
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
