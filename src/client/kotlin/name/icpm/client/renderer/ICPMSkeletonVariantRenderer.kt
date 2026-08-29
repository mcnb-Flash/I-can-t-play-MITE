package name.icpm.client.renderer

import name.icpm.entity.monster.ICPMSkeletonVariant
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.AbstractSkeletonRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.SkeletonRenderState
import net.minecraft.resources.Identifier

/**
 * 骷髅变种渲染器：同一模型（原版骷髅模型+护甲层），按实体类型选择纹理。
 */
class ICPMSkeletonVariantRenderer(
    context: EntityRendererProvider.Context,
    private val texture: Identifier
) : AbstractSkeletonRenderer<ICPMSkeletonVariant, SkeletonRenderState>(
    context,
    ModelLayers.SKELETON,
    ModelLayers.SKELETON_ARMOR
) {

    override fun getTextureLocation(state: SkeletonRenderState): Identifier = texture

    override fun createRenderState(): SkeletonRenderState = SkeletonRenderState()
}
