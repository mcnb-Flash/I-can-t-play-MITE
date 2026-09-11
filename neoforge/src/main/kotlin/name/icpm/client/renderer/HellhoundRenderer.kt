package name.icpm.client.renderer

import name.icpm.entity.monster.HellhoundEntity
import net.minecraft.client.model.animal.wolf.WolfModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.state.WolfRenderState
import net.minecraft.resources.Identifier

/**
 * 地狱犬渲染器：复用原版狼模型 + 发光眼睛层，纹理取自 hellhound 贴图。
 */
class HellhoundRenderer(
    context: EntityRendererProvider.Context,
    private val texture: Identifier
) : MobRenderer<HellhoundEntity, WolfRenderState, WolfModel>(
    context,
    WolfModel(context.bakeLayer(ModelLayers.WOLF)),
    0.5f
) {

    override fun getTextureLocation(state: WolfRenderState): Identifier = texture

    override fun createRenderState(): WolfRenderState = WolfRenderState()

    override fun extractRenderState(entity: HellhoundEntity, state: WolfRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.isAngry = true
        state.texture = texture
        state.wetShade = 1.0f
        state.tailAngle = (java.lang.Math.PI / 5).toFloat()
    }

    override fun getShadowRadius(state: WolfRenderState): Float = super.getShadowRadius(state) * 0.9f
}
