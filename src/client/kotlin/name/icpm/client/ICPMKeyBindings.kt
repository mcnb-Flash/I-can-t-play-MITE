package name.icpm.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object ICPMKeyBindings {
    val ZOOM: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.icpm.zoom",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KeyMapping.Category.GAMEPLAY
        )
    )

    /** 打开 ICPM 配置界面（默认 H；装 malilib 显示配置 GUI，未装则提示用 /icpmconfig） */
    val CONFIG: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.icpm.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KeyMapping.Category.GAMEPLAY
        )
    )
}
