package name.icpm.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.monster.slime.SlimeModel
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.state.SlimeRenderState
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import name.icpm.entity.monster.GelatinousCubeEntity

/**
 * 黏液族渲染器：复用原版 SlimeModel/SlimeRenderState，按实体贴图渲染。
 */
class GelatinousCubeRenderer(context: EntityRendererProvider.Context, private val texture: Identifier) :
    MobRenderer<GelatinousCubeEntity, SlimeRenderState, SlimeModel>(
        context,
        SlimeModel(context.bakeLayer(ModelLayers.SLIME)),
        0.25f
    ) {

    init {
        addLayer(SlimeOuterLayer(this, context.modelSet))
    }

    override fun getShadowRadius(renderState: SlimeRenderState): Float =
        renderState.size * 0.25f

    override fun scale(renderState: SlimeRenderState, poseStack: PoseStack) {
        poseStack.scale(0.999f, 0.999f, 0.999f)
        poseStack.translate(0.0f, 0.001f, 0.0f)
        val g = renderState.size
        val h = renderState.squish / (g * 0.5f + 1.0f)
        val i = 1.0f / (h + 1.0f)
        poseStack.scale(i * g, 1.0f / i * g, i * g)
    }

    override fun getTextureLocation(renderState: SlimeRenderState): Identifier = texture

    override fun createRenderState(): SlimeRenderState = SlimeRenderState()

    override fun extractRenderState(slime: GelatinousCubeEntity, renderState: SlimeRenderState, partialTick: Float) {
        super.extractRenderState(slime, renderState, partialTick)
        renderState.squish = Mth.lerp(partialTick, slime.oSquish, slime.squish)
        renderState.size = slime.size
    }
}
