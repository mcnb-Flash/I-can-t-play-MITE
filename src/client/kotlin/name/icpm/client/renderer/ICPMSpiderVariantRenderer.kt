package name.icpm.client.renderer

import name.icpm.entity.monster.ICPMSpiderVariant
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.SpiderRenderer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.resources.Identifier

/**
 * 蜘蛛变种渲染器：复用原版蜘蛛模型 + 眼睛发光层，按实体类型选择纹理。
 */
class ICPMSpiderVariantRenderer(
    context: EntityRendererProvider.Context,
    private val texture: Identifier
) : SpiderRenderer<ICPMSpiderVariant>(context, ModelLayers.SPIDER) {

    override fun getTextureLocation(state: LivingEntityRenderState): Identifier = texture
}
