package name.icpm.client

import name.icpm.ICPM
import name.icpm.client.gui.ICPMWorkbenchScreen
import name.icpm.client.gui.MetalAnvilScreen
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
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.ThrownItemRenderer
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

/**
 * ICPM 客户端内容（NeoForge 版）。
 *
 * 原 Fabric 入口点（ClientModInitializer）在 NeoForge 下拆分为多个 mod bus /
 * game bus 事件回调，由 {@link ICPMClientNeoForge} 统一挂载：
 * - RegisterMenuScreensEvent  → 菜单 → 屏幕
 * - EntityRenderersEvent.RegisterRenderers → 实体渲染器
 * - ClientTickEvent（game bus）→ 疾跑锁定 / 缩放等逐 tick 逻辑
 */
object ICPMClient {

    /** 注册金属砧 / ICPM 工作台界面（mod bus RegisterMenuScreensEvent）。 */
    @JvmStatic
    fun registerScreens(evt: RegisterMenuScreensEvent) {
        evt.register(ICPM.METAL_ANVIL_MENU, ::MetalAnvilScreen)
        evt.register(ICPM.ICPM_WORKBENCH_MENU, ::ICPMWorkbenchScreen)
    }

    /** 注册全部实体渲染器（mod bus EntityRenderersEvent.RegisterRenderers）。 */
    @JvmStatic
    fun registerRenderers(evt: EntityRenderersEvent.RegisterRenderers) {
        // 注册黏液族渲染器
        registerGelatinousCubeRenderers(evt)

        // 注册凝胶球弹射物渲染器（R196 EntityGelatinousSphere）
        evt.registerEntityRenderer(ICPMEntities.GELATINOUS_SPHERE) { context ->
            ThrownItemRenderer(context, 0.8f, false)
        }

        // 注册 ICPM 箭矢渲染器
        evt.registerEntityRenderer(ICPMEntities.ICPM_ARROW) { context ->
            ICPMArrowRenderer(context)
        }

        // 注册骷髅变种渲染器
        registerSkeletonVariantRenderers(evt)

        // 注册蜘蛛变种渲染器
        registerSpiderVariantRenderers(evt)

        // 注册地狱犬渲染器
        evt.registerEntityRenderer(ICPMEntities.HELLHOUND) { context ->
            HellhoundRenderer(context, Identifier.fromNamespaceAndPath("icpm", "textures/entity/hellhound/hellhound.png"))
        }

        // 注册土元素渲染器（人形模型复用 ModelLayers.PLAYER，对应 R196 ModelInvisibleStalker 的 64×32 布局）
        evt.registerEntityRenderer(ICPMEntities.EARTH_ELEMENTAL) { context ->
            EarthElementalRenderer(context)
        }

        // 注册 R196 新增怪物渲染器
        registerNewMonsterRenderers(evt)
    }

    /** 每客户端 tick（game bus ClientTickEvent.Post 回调）。 */
    @JvmStatic
    fun onClientTick() {
        val client = Minecraft.getInstance()
        client.player?.let { SprintLockManager.tick(it) }
        while (ICPMKeyBindings.CONFIG.consumeClick()) {
            // NeoForge 无 malilib 配置 GUI：提示使用 /icpmconfig 命令
            val pl = client.player
            if (pl != null) {
                pl.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§e请在游戏内使用 /icpmconfig 命令修改 ICPM 配置"), false
                )
            }
        }
    }

    private fun registerSpiderVariantRenderers(evt: EntityRenderersEvent.RegisterRenderers) {
        val textures = mapOf(
            ICPMEntities.WOOD_SPIDER to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/wood_spider.png"),
            ICPMEntities.CAVE_SPIDER_VARIANT to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/cave_spider.png"),
            ICPMEntities.BLACK_WIDOW to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/black_widow.png"),
            ICPMEntities.PHASE_SPIDER to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/phase_spider.png"),
            ICPMEntities.DEMON_SPIDER to Identifier.fromNamespaceAndPath("icpm", "textures/entity/spider/demon_spider.png")
        )
        for ((type, tex) in textures) {
            evt.registerEntityRenderer(type) { context -> ICPMSpiderVariantRenderer(context, tex) }
        }
    }

    private fun registerSkeletonVariantRenderers(evt: EntityRenderersEvent.RegisterRenderers) {
        val textures = mapOf(
            ICPMEntities.LONGDEAD to Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/longdead.png"),
            ICPMEntities.LONGDEAD_GUARDIAN to Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/longdead_guardian.png"),
            ICPMEntities.BONE_LORD to Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/bone_lord.png"),
            ICPMEntities.ANNIHILATION_SKELETON to Identifier.fromNamespaceAndPath("icpm", "textures/entity/skeleton/annihilation_skeleton.png")
        )
        for ((type, tex) in textures) {
            evt.registerEntityRenderer(type) { context -> ICPMSkeletonVariantRenderer(context, tex) }
        }
    }

    private fun registerGelatinousCubeRenderers(evt: EntityRenderersEvent.RegisterRenderers) {
        val textures = mapOf(
            ICPMEntities.JELLY to Identifier.fromNamespaceAndPath("icpm", "textures/entity/slime/jelly.png"),
            ICPMEntities.BLOB to Identifier.fromNamespaceAndPath("icpm", "textures/entity/slime/blob.png"),
            ICPMEntities.OOZE to Identifier.fromNamespaceAndPath("icpm", "textures/entity/slime/ooze.png"),
            ICPMEntities.PUDDING to Identifier.fromNamespaceAndPath("icpm", "textures/entity/slime/pudding.png")
        )
        for ((type, tex) in textures) {
            evt.registerEntityRenderer(type) { context -> GelatinousCubeRenderer(context, tex) }
        }
    }

    private fun registerNewMonsterRenderers(evt: EntityRenderersEvent.RegisterRenderers) {
        // 僵尸系
        evt.registerEntityRenderer(ICPMEntities.GHOUL, ::GhoulRenderer)
        evt.registerEntityRenderer(ICPMEntities.WIGHT, ::WightRenderer)
        evt.registerEntityRenderer(ICPMEntities.SHADOW, ::ShadowRenderer)
        evt.registerEntityRenderer(ICPMEntities.INVISIBLE_STALKER, ::InvisibleStalkerRenderer)
        evt.registerEntityRenderer(ICPMEntities.REVENANT, ::RevenantRenderer)
        // 骷髅系
        evt.registerEntityRenderer(ICPMEntities.ANCIENT_BONE_LORD, ::AncientBoneLordRenderer)
        // 魔像系
        evt.registerEntityRenderer(ICPMEntities.CLAY_GOLEM, ::ClayGolemRenderer)
        // 蝙蝠系
        evt.registerEntityRenderer(ICPMEntities.VAMPIRE_BAT, ::VampireBatRenderer)
        evt.registerEntityRenderer(ICPMEntities.NIGHTWING, ::NightwingRenderer)
        // 矿工僵尸（血月机制新增）
        evt.registerEntityRenderer(ICPMEntities.MINER_ZOMBIE, ::MinerZombieRenderer)
        // 巨型僵尸（血月地表僵尸 1/200 替换体）
        evt.registerEntityRenderer(ICPMEntities.GIANT_ZOMBIE, ::GiantZombieRenderer)
        // R196 补全怪物（A 项）
        evt.registerEntityRenderer(ICPMEntities.FIRE_ELEMENTAL, ::FireElementalRenderer)
        evt.registerEntityRenderer(ICPMEntities.INFERNAL_CREEPER, ::InfernalCreeperRenderer)
        evt.registerEntityRenderer(ICPMEntities.DIRE_WOLF) { context ->
            DireWolfRenderer(context, Identifier.fromNamespaceAndPath("icpm", "textures/entity/dire_wolf/wolf_angry.png"))
        }
        evt.registerEntityRenderer(ICPMEntities.HOARY_SILVERFISH, ::HoarySilverfishRenderer)
    }
}
