package name.icpm.client

import name.icpm.ICPM
import name.icpm.client.gui.ICPMWorkbenchScreen
import name.icpm.client.gui.MetalAnvilScreen
import name.icpm.client.network.InventoryCraftSyncHandler
import name.icpm.client.network.NutritionSyncHandler
import name.icpm.client.renderer.GelatinousCubeRenderer
import name.icpm.client.renderer.HellhoundRenderer
import name.icpm.client.renderer.EarthElementalRenderer
import name.icpm.client.renderer.ICPMArrowRenderer
import name.icpm.client.renderer.ICPMSkeletonVariantRenderer
import name.icpm.client.renderer.ICPMSpiderVariantRenderer
import name.icpm.client.renderer.GhoulRenderer
import name.icpm.client.renderer.WightRenderer
import name.icpm.client.renderer.ShadowRenderer
import name.icpm.client.renderer.InvisibleStalkerRenderer
import name.icpm.client.renderer.RevenantRenderer
import name.icpm.client.renderer.MinerZombieRenderer
import name.icpm.client.renderer.GiantZombieRenderer
import name.icpm.client.renderer.ClayGolemRenderer
import name.icpm.client.renderer.AncientBoneLordRenderer
import name.icpm.client.renderer.VampireBatRenderer
import name.icpm.client.renderer.NightwingRenderer
import name.icpm.client.renderer.FireElementalRenderer
import name.icpm.client.renderer.InfernalCreeperRenderer
import name.icpm.client.renderer.DireWolfRenderer
import name.icpm.client.renderer.HoarySilverfishRenderer
import name.icpm.entity.ICPMEntities
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.entity.ThrownItemRenderer
import net.minecraft.resources.Identifier

object ICPMClient : ClientModInitializer {
    override fun onInitializeClient() {
        // 触发 ICPMKeyBindings 静态初始化，注册 C 键缩放
        @Suppress("UNUSED_EXPRESSION")
        ICPMKeyBindings.ZOOM

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            client.player?.let { SprintLockManager.tick(it) }
        })

        // 注册金属砧界面
        MenuScreens.register(ICPM.METAL_ANVIL_MENU, ::MetalAnvilScreen)

        // 注册 ICPM 工作台界面
        MenuScreens.register(ICPM.ICPM_WORKBENCH_MENU, ::ICPMWorkbenchScreen)
        
        // 注册营养值同步网络包处理器
        NutritionSyncHandler.register()

        // 注册背包合成进度同步网络包处理器
        InventoryCraftSyncHandler.register()

        // 注册黏液族渲染器
        registerGelatinousCubeRenderers()

        // 注册凝胶球弹射物渲染器（R196 EntityGelatinousSphere）
        EntityRendererRegistry.register(ICPMEntities.GELATINOUS_SPHERE) { context ->
            ThrownItemRenderer(context, 0.8f, false)
        }

        // 注册 ICPM 箭矢渲染器
        EntityRendererRegistry.register(ICPMEntities.ICPM_ARROW) { context ->
            ICPMArrowRenderer(context)
        }

        // 注册骷髅变种渲染器
        registerSkeletonVariantRenderers()

        // 注册蜘蛛变种渲染器
        registerSpiderVariantRenderers()

        // 注册地狱犬渲染器
        EntityRendererRegistry.register(ICPMEntities.HELLHOUND) { context ->
            HellhoundRenderer(context, Identifier.fromNamespaceAndPath("icpm", "textures/entity/hellhound/hellhound.png"))
        }

        // 注册土元素渲染器（人形模型复用 ModelLayers.PLAYER，对应 R196 ModelInvisibleStalker 的 64×32 布局）
        EntityRendererRegistry.register(ICPMEntities.EARTH_ELEMENTAL) { context ->
            EarthElementalRenderer(context)
        }

        // 注册 R196 新增怪物渲染器
        registerNewMonsterRenderers()
    }

    private fun registerSpiderVariantRenderers() {
        val textures = mapOf(
            ICPMEntities.WOOD_SPIDER to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/wood_spider.png"),
            ICPMEntities.CAVE_SPIDER_VARIANT to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/cave_spider.png"),
            ICPMEntities.BLACK_WIDOW to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/black_widow.png"),
            ICPMEntities.PHASE_SPIDER to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/phase_spider.png"),
            ICPMEntities.DEMON_SPIDER to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/demon_spider.png")
        )
        for ((type, tex) in textures) {
            EntityRendererRegistry.register(type) { context -> ICPMSpiderVariantRenderer(context, tex) }
        }
    }

    private fun registerSkeletonVariantRenderers() {
        val textures = mapOf(
            ICPMEntities.LONGDEAD to Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/longdead.png"),
            ICPMEntities.LONGDEAD_GUARDIAN to Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/longdead_guardian.png"),
            ICPMEntities.BONE_LORD to Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/bone_lord.png"),
            ICPMEntities.ANNIHILATION_SKELETON to Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/annihilation_skeleton.png")
        )
        for ((type, tex) in textures) {
            EntityRendererRegistry.register(type) { context -> ICPMSkeletonVariantRenderer(context, tex) }
        }
    }

    private fun registerGelatinousCubeRenderers() {
        val textures = mapOf(
            ICPMEntities.JELLY to Identifier.fromNamespaceAndPath("icpm", "textures/entity/slime/jelly.png"),
            ICPMEntities.BLOB to Identifier.fromNamespaceAndPath("icpm", "textures/entity/slime/blob.png"),
            ICPMEntities.OOZE to Identifier.fromNamespaceAndPath("icpm", "textures/entity/slime/ooze.png"),
            ICPMEntities.PUDDING to Identifier.fromNamespaceAndPath("icpm", "textures/entity/slime/pudding.png")
        )
        for ((type, tex) in textures) {
            EntityRendererRegistry.register(type) { context -> GelatinousCubeRenderer(context, tex) }
        }
    }

    private fun registerNewMonsterRenderers() {
        // 僵尸系
        EntityRendererRegistry.register(ICPMEntities.GHOUL, ::GhoulRenderer)
        EntityRendererRegistry.register(ICPMEntities.WIGHT, ::WightRenderer)
        EntityRendererRegistry.register(ICPMEntities.SHADOW, ::ShadowRenderer)
        EntityRendererRegistry.register(ICPMEntities.INVISIBLE_STALKER, ::InvisibleStalkerRenderer)
        EntityRendererRegistry.register(ICPMEntities.REVENANT, ::RevenantRenderer)
        // 骷髅系
        EntityRendererRegistry.register(ICPMEntities.ANCIENT_BONE_LORD, ::AncientBoneLordRenderer)
        // 魔像系
        EntityRendererRegistry.register(ICPMEntities.CLAY_GOLEM, ::ClayGolemRenderer)
        // 蝙蝠系
        EntityRendererRegistry.register(ICPMEntities.VAMPIRE_BAT, ::VampireBatRenderer)
        EntityRendererRegistry.register(ICPMEntities.NIGHTWING, ::NightwingRenderer)
        // 矿工僵尸（血月机制新增）
        EntityRendererRegistry.register(ICPMEntities.MINER_ZOMBIE, ::MinerZombieRenderer)
        // 巨型僵尸（血月地表僵尸 1/200 替换体）
        EntityRendererRegistry.register(ICPMEntities.GIANT_ZOMBIE, ::GiantZombieRenderer)
        // R196 补全怪物（A 项）
        EntityRendererRegistry.register(ICPMEntities.FIRE_ELEMENTAL, ::FireElementalRenderer)
        EntityRendererRegistry.register(ICPMEntities.INFERNAL_CREEPER, ::InfernalCreeperRenderer)
        EntityRendererRegistry.register(ICPMEntities.DIRE_WOLF) { context ->
            DireWolfRenderer(context, Identifier.fromNamespaceAndPath("icpm", "textures/entity/dire_wolf/wolf_angry.png"))
        }
        EntityRendererRegistry.register(ICPMEntities.HOARY_SILVERFISH, ::HoarySilverfishRenderer)
    }
}
