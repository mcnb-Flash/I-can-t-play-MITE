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
}
