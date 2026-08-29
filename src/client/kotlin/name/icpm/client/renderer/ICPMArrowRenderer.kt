package name.icpm.client.renderer

import name.icpm.entity.projectile.ICPMArrowEntity
import net.minecraft.client.renderer.entity.ArrowRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.ArrowRenderState
import net.minecraft.resources.Identifier

/**
 * ICPM 箭矢渲染器。复用原版箭矢模型与纹理（实体飞行姿态）。
 */
class ICPMArrowRenderer(context: EntityRendererProvider.Context) :
    ArrowRenderer<ICPMArrowEntity, ArrowRenderState>(context) {

    override fun createRenderState(): ArrowRenderState = ArrowRenderState()

    override fun getTextureLocation(state: ArrowRenderState): Identifier {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/entity/projectiles/arrow.png")
    }
}
