package name.icpm.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import name.icpm.entity.monster.EarthElementalEntity
import name.icpm.entity.monster.EarthElementalType

/**
 * 土元素渲染状态：携带实体材质类型对应的基础贴图与发光（眼睛）贴图。
 * 继承 HumanoidRenderState，使 MobRenderer 自动填充头部/肢体姿态（R196 ModelInvisibleStalker 即人形 64×32 布局）。
 */
class EarthElementalRenderState : HumanoidRenderState() {
    var texture: Identifier = Identifier.fromNamespaceAndPath("icpm", "textures/entity/earth_elemental/stone/earth_elemental_stone.png")
    var glowTexture: Identifier = Identifier.fromNamespaceAndPath("icpm", "textures/entity/earth_elemental/earth_elemental_glow.png")
}

/**
 * 土元素渲染器：人形模型（HumanoidModel，对应 R196 ModelInvisibleStalker 的 64×32 Steve 布局），
 * 按材质/熔岩态选择贴图；通过 EarthElementalEyesLayer 以发光贴图叠加（只有 (0,0) 的眼睛像素不透明）实现眼部发光。
 */
class EarthElementalRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<EarthElementalEntity, EarthElementalRenderState, HumanoidModel<EarthElementalRenderState>>(
        context,
        HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)),
        0.5f
    ) {

    init {
        addLayer(EarthElementalEyesLayer(this))
    }

    override fun getTextureLocation(state: EarthElementalRenderState): Identifier = state.texture

    override fun createRenderState(): EarthElementalRenderState = EarthElementalRenderState()

    override fun extractRenderState(entity: EarthElementalEntity, state: EarthElementalRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.texture = textureFor(entity.getElementType(), entity.isMagma())
        state.glowTexture = if (entity.isMagma()) MAGMA_GLOW else GLOW
    }

    companion object {
        private val GLOW: Identifier = Identifier.fromNamespaceAndPath("icpm", "textures/entity/earth_elemental/earth_elemental_glow.png")
        private val MAGMA_GLOW: Identifier = Identifier.fromNamespaceAndPath("icpm", "textures/entity/earth_elemental/earth_elemental_magma_glow.png")

        private fun textureFor(type: EarthElementalType, magma: Boolean): Identifier {
            val name = when (type) {
                EarthElementalType.STONE -> if (magma) "stone/earth_elemental_stone_magma" else "stone/earth_elemental_stone"
                EarthElementalType.CLAY -> "clay/earth_elemental_clay"
                EarthElementalType.CLAY_HARDENED -> "clay/earth_elemental_clay_hardened"
                EarthElementalType.END_STONE -> if (magma) "end_stone/earth_elemental_end_stone_magma" else "end_stone/earth_elemental_end_stone"
                EarthElementalType.NETHERRACK -> if (magma) "netherrack/earth_elemental_netherrack_magma" else "netherrack/earth_elemental_netherrack"
                EarthElementalType.OBSIDIAN -> if (magma) "obsidian/earth_elemental_obsidian_magma" else "obsidian/earth_elemental_obsidian"
                EarthElementalType.PLANK -> "plank/earth_elemental_plank"
            }
            return Identifier.fromNamespaceAndPath("icpm", "textures/entity/earth_elemental/$name.png")
        }
    }
}

/** 发光眼睛层：以全亮（eyes）渲染类型绘制模型，发光贴图仅在正面 (0,0) 有不透明眼睛像素。 */
private class EarthElementalEyesLayer(
    parent: RenderLayerParent<EarthElementalRenderState, HumanoidModel<EarthElementalRenderState>>
) : RenderLayer<EarthElementalRenderState, HumanoidModel<EarthElementalRenderState>>(parent) {
    override fun submit(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        i: Int,
        state: EarthElementalRenderState,
        f: Float,
        g: Float
    ) {
        submitNodeCollector.order(1)
            .submitModel(
                getParentModel(), state, poseStack,
                RenderTypes.eyes(state.glowTexture),
                i, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null
            )
    }
}
