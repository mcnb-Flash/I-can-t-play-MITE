package name.icpm.client

import net.minecraft.client.Minecraft

object ZoomHandler {
    private val ZOOM_FACTOR = 0.25f

    @JvmStatic
    fun isZooming(): Boolean {
        val mc = Minecraft.getInstance()
        return ICPMKeyBindings.ZOOM.isDown && mc.screen == null
    }

    @JvmStatic
    fun applyZoom(fov: Float): Float {
        return fov * ZOOM_FACTOR
    }
}
