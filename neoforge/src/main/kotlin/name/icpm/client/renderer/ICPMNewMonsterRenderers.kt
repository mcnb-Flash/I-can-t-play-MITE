package name.icpm.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import java.util.function.Function
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.animal.wolf.WolfModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.monster.skeleton.SkeletonModel
import net.minecraft.client.model.monster.zombie.ZombieModel
import net.minecraft.client.model.monster.creeper.CreeperModel
import net.minecraft.client.model.monster.silverfish.SilverfishModel
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.model.ambient.BatModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.ArmorModelSet
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.HumanoidMobRenderer
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.BatRenderState
import net.minecraft.client.renderer.entity.state.CreeperRenderState
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.client.renderer.entity.state.SkeletonRenderState
import net.minecraft.client.renderer.entity.state.WolfRenderState
import net.minecraft.client.renderer.entity.state.ZombieRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Mob
import name.icpm.entity.monster.*

/**
 * ICPM R196 新增怪物渲染器
 *
 * 全部复用原版模型 + RenderState，仅覆盖纹理路径。
 * 遵循 1.21.11 渲染系统：MobRenderer<Entity, State, Model>
 *
 * ⚠ 1.21.11 的 [MobRenderer] **不再自带**「手持物 / 护甲 / 头部装饰」渲染层：
 * 自定义实体若只继承 MobRenderer（旧写法），则手持武器与穿戴护甲在客户端**完全不显示**。
 * 正确做法是继承 [HumanoidMobRenderer]（自带 CustomHeadLayer / WingsLayer / ItemInHandLayer，
 * 且会完整抽取人形 RenderState：手持物、四件护甲、手臂姿态），并额外挂 [HumanoidArmorLayer]。
 * 下方 [ICPMHumanoidRenderer] 即为此封装，所有会穿戴装备/手持武器的人形怪物都应经它派生。
 */

// ==================== 人形渲染器基类（补齐手持物 + 护甲层） ====================

/**
 * 带「手持物 + 护甲」的人形怪物渲染器基类。
 *
 * @param model 主模型（成人/幼体共用）
 * @param texture 实体主纹理
 * @param armorLayers 护甲模型层集合（如 ZOMBIE_ARMOR / SKELETON_ARMOR / PLAYER_ARMOR）
 * @param armorModelFactory 由 ModelPart 构造护甲模型的工厂（须与主模型同类型）
 */
abstract class ICPMHumanoidRenderer<T : Mob, S : HumanoidRenderState, M : HumanoidModel<S>>(
    context: EntityRendererProvider.Context,
    model: M,
    private val texture: Identifier,
    armorLayers: ArmorModelSet<ModelLayerLocation>,
    armorModelFactory: Function<ModelPart, M>
) : HumanoidMobRenderer<T, S, M>(context, model, 0.5f) {

    override fun getTextureLocation(state: S): Identifier = texture

    init {
        // 挂接护甲渲染层（HumanoidMobRenderer 已带 ItemInHandLayer，无需重复添加）
        addLayer(
            HumanoidArmorLayer<S, M, M>(
                this,
                ArmorModelSet.bake(armorLayers, context.modelSet, armorModelFactory),
                ArmorModelSet.bake(armorLayers, context.modelSet, armorModelFactory),
                context.equipmentRenderer
            )
        )
    }
}

/**
 * 僵尸系人形渲染器基类（ZombieModel + ZombieRenderState）。
 *
 * 统一覆写 `isAggressive = true`：僵尸族恒为敌对，模型应呈 R196 标志性的「双臂前伸」姿态
 * （`AbstractZombieModel.setupAnim` → `AnimationUtils.animateZombieArms(..., isAggressive, ...)`）。
 */
abstract class ICPMZombieHumanoidRenderer<T : Mob>(
    context: EntityRendererProvider.Context,
    texture: Identifier
) : ICPMHumanoidRenderer<T, ZombieRenderState, ZombieModel<ZombieRenderState>>(
    context,
    ZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)),
    texture,
    ModelLayers.ZOMBIE_ARMOR,
    { part -> ZombieModel<ZombieRenderState>(part) }
) {

    override fun createRenderState(): ZombieRenderState = ZombieRenderState()

    override fun extractRenderState(entity: T, state: ZombieRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.isAggressive = true
    }
}

/**
 * 玩家模型人形渲染器基类（HumanoidModel + HumanoidRenderState，用 PLAYER 模型层）。
 */
abstract class ICPMPlayerHumanoidRenderer<T : Mob>(
    context: EntityRendererProvider.Context,
    texture: Identifier
) : ICPMHumanoidRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>(
    context,
    HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)),
    texture,
    ModelLayers.PLAYER_ARMOR,
    { part -> HumanoidModel<HumanoidRenderState>(part) }
) {

    override fun createRenderState(): HumanoidRenderState = HumanoidRenderState()
}

// ==================== 僵尸系（ZombieModel + ZombieRenderState） ====================

class GhoulRenderer(context: EntityRendererProvider.Context) :
    ICPMZombieHumanoidRenderer<GhoulEntity>(
        context,
        Identifier.fromNamespaceAndPath("icpm", "textures/entity/ghoul.png")
    )

class WightRenderer(context: EntityRendererProvider.Context) :
    ICPMZombieHumanoidRenderer<WightEntity>(
        context,
        Identifier.fromNamespaceAndPath("icpm", "textures/entity/wight.png")
    )

class ShadowRenderer(context: EntityRendererProvider.Context) :
    ICPMZombieHumanoidRenderer<ShadowEntity>(
        context,
        Identifier.fromNamespaceAndPath("icpm", "textures/entity/shadow.png")
    )

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
    ICPMZombieHumanoidRenderer<RevenantEntity>(
        context,
        // R196 资源包中 revenant 贴图位于 entity/zombie/revenant.png
        Identifier.fromNamespaceAndPath("icpm", "textures/entity/zombie/revenant.png")
    )

/**
 * 矿工僵尸渲染器（复用 ICPM 僵尸贴图 + 原版 ZombieModel）。
 *
 * 注：矿工僵尸**必定**手持铁镐/铁战锤并穿戴四件锁链装备，因此必须经
 * [ICPMZombieHumanoidRenderer] 派生，否则手持物与护甲在客户端不显示。
 */
class MinerZombieRenderer(context: EntityRendererProvider.Context) :
    ICPMZombieHumanoidRenderer<MinerZombieEntity>(
        context,
        Identifier.fromNamespaceAndPath("icpm", "textures/entity/zombie/zombie.png")
    )

/**
 * 巨型僵尸渲染器：复用原版 ZombieModel，渲染时整体放大 ×6（对齐 r196 体型 ×6 / 碰撞箱 3.6×11.7）。
 * 通过覆写 MobRenderer.scale 钩子放大（vanilla Giant 同范式），避免覆写 final 的 render。
 */
class GiantZombieRenderer(context: EntityRendererProvider.Context) :
    ICPMZombieHumanoidRenderer<GiantZombieEntity>(
        context,
        Identifier.fromNamespaceAndPath("icpm", "textures/entity/zombie/zombie.png")
    ) {
    override fun scale(state: ZombieRenderState, poseStack: PoseStack) {
        poseStack.scale(6.0f, 6.0f, 6.0f)
        super.scale(state, poseStack)
    }
}

// ==================== 骷髅系（SkeletonModel + SkeletonRenderState） ====================

class AncientBoneLordRenderer(context: EntityRendererProvider.Context) :
    ICPMHumanoidRenderer<AncientBoneLordEntity, SkeletonRenderState, SkeletonModel<SkeletonRenderState>>(
        context,
        SkeletonModel(context.bakeLayer(ModelLayers.SKELETON)),
        Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/bone_lord.png"),
        ModelLayers.SKELETON_ARMOR,
        { part -> SkeletonModel<SkeletonRenderState>(part) }
    ) {
    override fun createRenderState(): SkeletonRenderState = SkeletonRenderState()
}

// ==================== 魔像系（HumanoidModel + HumanoidRenderState） ====================
// R196 黏土魔像复用土元素人形模型（ModelInvisibleStalker）+ clay 贴图

class ClayGolemRenderer(context: EntityRendererProvider.Context) :
    ICPMPlayerHumanoidRenderer<ClayGolemEntity>(
        context,
        Identifier.fromNamespaceAndPath("icpm", "textures/entity/earth_elemental/clay/earth_elemental_clay.png")
    )

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
