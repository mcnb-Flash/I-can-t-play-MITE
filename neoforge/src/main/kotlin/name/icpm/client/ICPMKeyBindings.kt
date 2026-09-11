package name.icpm.client

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import org.lwjgl.glfw.GLFW

object ICPMKeyBindings {
    /** NeoForge：KeyMapping 于 RegisterKeyMappingsEvent 注册（见 ICPMClientNeoForge）。 */
    val ZOOM: KeyMapping = KeyMapping(
        "key.icpm.zoom",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_C,
        KeyMapping.Category.GAMEPLAY
    )

    /** 打开 ICPM 配置界面（默认 H；未装配置 GUI 时提示用 /icpmconfig） */
    val CONFIG: KeyMapping = KeyMapping(
        "key.icpm.config",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        KeyMapping.Category.GAMEPLAY
    )

    @JvmStatic
    fun registerAll(evt: RegisterKeyMappingsEvent) {
        evt.register(ZOOM)
        evt.register(CONFIG)
    }
}
