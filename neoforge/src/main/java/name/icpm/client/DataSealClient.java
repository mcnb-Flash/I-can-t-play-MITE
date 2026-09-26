package name.icpm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.UUID;

/**
 * 防改物客户端桥接：由服务端 {@code DataSeal} 经反射调用（避免在服务端代码静态引用客户端类）。
 *
 * <p>在客户端渲染线程打开「存档核心被破坏」弹窗。世界保持暂停（{@code DataBreachScreen#isPauseScreen}），
 * 玩家无法操作，必须键入口令从备份还原或放弃存档。
 */
public final class DataSealClient {

    private DataSealClient() {
    }

    public static void openBreach(Path worldDir, Path levelBackup, Path playerBackup, UUID playerId) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(
                new name.icpm.client.gui.DataBreachScreen(worldDir, levelBackup, playerBackup, playerId)));
    }
}
