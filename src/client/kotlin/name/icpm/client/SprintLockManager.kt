package name.icpm.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer

/**
 * Ctrl 疾跑锁定：按下疾跑键切换锁定状态，锁定后持续疾跑。
 * 使用原始键码检测，避免与 KeyBinding.setDown 互相干扰。
 */
object SprintLockManager {
    private var locked = false
    private var wasRawSprintDown = false

    @JvmStatic
    fun isLocked(): Boolean = locked

    @JvmStatic
    fun tick(player: LocalPlayer) {
        val mc = Minecraft.getInstance()
        val sprintKey = mc.options.keySprint
        val boundKey = KeyBindingHelper.getBoundKeyOf(sprintKey)
        val rawDown = InputConstants.isKeyDown(mc.window, boundKey.getValue())

        // 仅在物理按键按下瞬间切换锁定状态
        if (rawDown && !wasRawSprintDown) {
            locked = !locked
        }
        wasRawSprintDown = rawDown

        if (locked) {
            player.setSprinting(true)
        }
    }
}
