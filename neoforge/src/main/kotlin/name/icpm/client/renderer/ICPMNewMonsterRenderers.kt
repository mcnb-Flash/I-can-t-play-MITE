package name.icpm.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.animal.wolf.WolfModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.monster.skeleton.SkeletonModel
import net.minecraft.client.model.monster.zombie.ZombieModel
import net.minecraft.client.model.monster.creeper.CreeperModel
import net.minecraft.client.model.monster.silverfish.SilverfishModel
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.model.ambient.BatModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.BatRenderState
import net.minecraft.client.renderer.entity.state.CreeperRenderState
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.client.renderer.entity.state.SkeletonRenderState
import net.minecraft.client.renderer.entity.state.WolfRenderState
import net.minecraft.client.renderer.entity.state.ZombieRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import name.icpm.entity.monster.*

/**
 * ICPM R196 新增怪物渲染器
 *
 * 全部复用原版模型 + RenderState，仅覆盖纹理路径。
 * 遵循 1.21.11 渲染系统：MobRenderer<Entity, State, Model>
 */

// ==================== 僵尸系（ZombieModel + ZombieRenderState） ====================

class GhoulRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<GhoulEntity, ZombieRenderState, ZombieModel<ZombieRenderState>>(
        context,
        ZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)),
        0.5f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/ghoul.png")
    override fun getTextureLocation(state: ZombieRenderState): Identifier = tex
    override fun createRenderState(): ZombieRenderState = ZombieRenderState()
}

class WightRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<WightEntity, ZombieRenderState, ZombieModel<ZombieRenderState>>(
        context,
        ZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)),
        0.5f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/wight.png")
    override fun getTextureLocation(state: ZombieRenderState): Identifier = tex
    override fun createRenderState(): ZombieRenderState = ZombieRenderState()
}

class ShadowRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<ShadowEntity, ZombieRenderState, ZombieModel<ZombieRenderState>>(
        context,
        ZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)),
        0.5f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/shadow.png")
    override fun getTextureLocation(state: ZombieRenderState): Identifier = tex
    override fun createRenderState(): ZombieRenderState = ZombieRenderState()
}

class InvisibleStalkerRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<InvisibleStalkerEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>(
        context,
        HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)),
        0.5f
    ) {
    // R196 RenderInvisibleStalker：复用 wight 人形贴图，并以 5% 透明度渲染（近乎隐形）
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/invisible_stalker.png")
    override fun getTextureLocation(state: HumanoidRenderState): Identifier = tex
    override fun createRenderState(): HumanoidRenderState = HumanoidRenderState()

    init {
        // 以半透明叠加绘制整模型，配合 5% alpha 贴图实现 R196 getModelOpacity = 0.05
        addLayer(InvisibleStalkerBodyLayer(this, tex))
    }
}

/** 半透明身体层：以 eyes 渲染类型整体绘制模型，配合 5% alpha 贴图使潜伏者呈微弱可见（近乎隐形）。 */
private class InvisibleStalkerBodyLayer(
    parent: RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>>,
    private val texture: Identifier
) : RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>>(parent) {
    override fun submit(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        i: Int,
        state: HumanoidRenderState,
        f: Float,
        g: Float
    ) {
        submitNodeCollector.order(0)
            .submitModel(
                getParentModel(), state, poseStack,
                RenderTypes.eyes(texture),
                i, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null
            )
    }
}

class RevenantRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<RevenantEntity, ZombieRenderState, ZombieModel<ZombieRenderState>>(
        context,
        ZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)),
        0.5f
    ) {
    // R196 资源包中 revenant 贴图位于 entity/zombie/revenant.png
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/zombie/revenant.png")
    override fun getTextureLocation(state: ZombieRenderState): Identifier = tex
    override fun createRenderState(): ZombieRenderState = ZombieRenderState()
}

/**
 * 矿工僵尸渲染器（复用 ICPM 僵尸贴图 + 原版 ZombieModel）
 */
class MinerZombieRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<MinerZombieEntity, ZombieRenderState, ZombieModel<ZombieRenderState>>(
        context,
        ZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)),
        0.5f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/zombie/zombie.png")
    override fun getTextureLocation(state: ZombieRenderState): Identifier = tex
    override fun createRenderState(): ZombieRenderState = ZombieRenderState()
}

/**
 * 巨型僵尸渲染器：复用原版 ZombieModel，渲染时整体放大 ×6（对齐 r196 体型 ×6 / 碰撞箱 3.6×11.7）。
 * 通过覆写 MobRenderer.scale 钩子放大（vanilla Giant 同范式），避免覆写 final 的 render。
 */
class GiantZombieRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<GiantZombieEntity, ZombieRenderState, ZombieModel<ZombieRenderState>>(
        context,
        ZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)),
        0.5f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/zombie/zombie.png")
    override fun getTextureLocation(state: ZombieRenderState): Identifier = tex
    override fun createRenderState(): ZombieRenderState = ZombieRenderState()

    override fun scale(state: ZombieRenderState, poseStack: PoseStack) {
        poseStack.scale(6.0f, 6.0f, 6.0f)
        super.scale(state, poseStack)
    }
}

// ==================== 骷髅系（SkeletonModel + SkeletonRenderState） ====================

class AncientBoneLordRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<AncientBoneLordEntity, SkeletonRenderState, SkeletonModel<SkeletonRenderState>>(
        context,
        SkeletonModel(context.bakeLayer(ModelLayers.SKELETON)),
        0.5f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/bone_lord.png")
    override fun getTextureLocation(state: SkeletonRenderState): Identifier = tex
    override fun createRenderState(): SkeletonRenderState = SkeletonRenderState()
}

// ==================== 魔像系（HumanoidModel + HumanoidRenderState） ====================
// R196 黏土魔像复用土元素人形模型（ModelInvisibleStalker）+ clay 贴图

class ClayGolemRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<ClayGolemEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>(
        context,
        HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)),
        0.5f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/earth_elemental/clay/earth_elemental_clay.png")
    override fun getTextureLocation(state: HumanoidRenderState): Identifier = tex
    override fun createRenderState(): HumanoidRenderState = HumanoidRenderState()
}

// ==================== 蝙蝠系（BatModel + BatRenderState） ====================

/**
 * 吸血蝙蝠渲染器
 * 覆写 extractRenderState 填充 resting 状态与翅膀动画（原版 BatRenderer 同款），
 * 否则翅膀动画状态为空导致模型姿态错乱。
 */
class VampireBatRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<VampireBatEntity, BatRenderState, BatModel>(
        context,
        BatModel(context.bakeLayer(ModelLayers.BAT)),
        0.3f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/bat/vampire.png")
    override fun getTextureLocation(state: BatRenderState): Identifier = tex
    override fun createRenderState(): BatRenderState = BatRenderState()

    override fun extractRenderState(entity: VampireBatEntity, state: BatRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.isResting = entity.isResting()
        state.flyAnimationState.copyFrom(entity.flyAnimationState)
        state.restAnimationState.copyFrom(entity.restAnimationState)
    }
}

/**
 * 夜翼渲染器（同吸血蝙蝠：填充 resting 与翅膀动画状态）
 */
class NightwingRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<NightwingEntity, BatRenderState, BatModel>(
        context,
        BatModel(context.bakeLayer(ModelLayers.BAT)),
        0.3f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/bat/nightwing.png")
    override fun getTextureLocation(state: BatRenderState): Identifier = tex
    override fun createRenderState(): BatRenderState = BatRenderState()

    override fun extractRenderState(entity: NightwingEntity, state: BatRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.isResting = entity.isResting()
        state.flyAnimationState.copyFrom(entity.flyAnimationState)
        state.restAnimationState.copyFrom(entity.restAnimationState)
    }
}

// ==================== R196 补全怪物（A 项：火元素 / 地狱苦力怕 / 恐狼 / 灰银鱼） ====================

/**
 * 火元素渲染器（R196 RenderFireElemental：复用 ModelInvisibleStalker 人形 64×32 布局 + fire_elemental 贴图，
 * 且 getModelOpacity = 0.0 → 模型本身近乎全透明，只靠火焰粒子与发光呈现）。
 * 这里复用 HumanoidModel（对应 R196 人形布局），以 eyes 渲染类型整体绘制模型（配合 fire_elemental.png
 * 的半透明火焰贴图）实现 R196 的"透明火焰体"观感。
 */
class FireElementalRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<FireElementalEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>(
        context,
        HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)),
        0.5f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/fire_elemental.png")
    override fun getTextureLocation(state: HumanoidRenderState): Identifier = tex
    override fun createRenderState(): HumanoidRenderState = HumanoidRenderState()

    init {
        // 以 eyes 渲染类型叠加绘制整模型（同 InvisibleStalker 手法），配合半透明火焰贴图呈现发光透明体。
        addLayer(FireElementalBodyLayer(this, tex))
    }
}

private class FireElementalBodyLayer(
    parent: RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>>,
    private val texture: Identifier
) : RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>>(parent) {
    override fun submit(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        i: Int,
        state: HumanoidRenderState,
        f: Float,
        g: Float
    ) {
        submitNodeCollector.order(0)
            .submitModel(
                getParentModel(), state, poseStack,
                RenderTypes.eyes(texture),
                i, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null
            )
    }
}

/**
 * 地狱苦力怕渲染器（R196 RenderInfernalCreeper：复用 Creeper 贴图变体 infernal_creeper，scale = getScale() = 1.0 无放大）。
 * 复用原版 CreeperModel + CreeperRenderState，仅覆盖纹理。
 */
class InfernalCreeperRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<InfernalCreeperEntity, CreeperRenderState, CreeperModel>(
        context,
        CreeperModel(context.bakeLayer(ModelLayers.CREEPER)),
        0.5f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/creeper/infernal_creeper.png")
    override fun getTextureLocation(state: CreeperRenderState): Identifier = tex
    override fun createRenderState(): CreeperRenderState = CreeperRenderState()
}

/**
 * 恐狼渲染器（R196 EntityDireWolf 继承 Wolf）：复用原版狼模型 + dire_wolf 贴图，
 * 强制 angry（敌对外观），使其呈现野生恐狼的红色眼睛姿态（同 HellhoundRenderer 手法）。
 */
class DireWolfRenderer(
    context: EntityRendererProvider.Context,
    private val texture: Identifier
) : MobRenderer<DireWolfEntity, WolfRenderState, WolfModel>(
    context,
    WolfModel(context.bakeLayer(ModelLayers.WOLF)),
    0.5f
) {
    override fun getTextureLocation(state: WolfRenderState): Identifier = texture
    override fun createRenderState(): WolfRenderState = WolfRenderState()

    override fun extractRenderState(entity: DireWolfEntity, state: WolfRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        // 强制愤怒外观（野生敌对恐狼）
        state.isAngry = true
        state.texture = texture
        state.wetShade = 1.0f
    }

    override fun getShadowRadius(state: WolfRenderState): Float = super.getShadowRadius(state) * 0.9f
}

/**
 * 灰银鱼渲染器（R196 EntityHoarySilverfish 继承 Silverfish）：复用原版 SilverfishModel，
 * 覆盖纹理为 hoary.png（灰银鱼专用贴图）。R196 源码为空，故行为与原版银鱼完全一致。
 */
class HoarySilverfishRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<HoarySilverfishEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, SilverfishModel>(
        context,
        SilverfishModel(context.bakeLayer(ModelLayers.SILVERFISH)),
        0.4f
    ) {
    private val tex = Identifier.fromNamespaceAndPath("icpm", "textures/entity/silverfish/hoary.png")
    override fun getTextureLocation(state: net.minecraft.client.renderer.entity.state.LivingEntityRenderState): Identifier = tex
    override fun createRenderState(): net.minecraft.client.renderer.entity.state.LivingEntityRenderState =
        net.minecraft.client.renderer.entity.state.LivingEntityRenderState()
}
