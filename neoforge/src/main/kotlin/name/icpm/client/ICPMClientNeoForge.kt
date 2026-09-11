package name.icpm.client

import name.icpm.ICPM
import net.minecraft.resources.Identifier
import net.neoforged.bus.api.IEventBus
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.gui.GuiLayer
import net.neoforged.neoforge.client.gui.VanillaGuiLayers
import net.neoforged.neoforge.common.NeoForge

/**
 * ICPM NeoForge 客户端专用入口（仅在客户端加载，服务端不构造本类）。
 *
 * 承接原 Fabric 客户端入口点（ClientModInitializer.onInitializeClient）的职责：
 * - mod bus：RegisterKeyMappingsEvent（C 缩放 / H 配置）、RegisterMenuScreensEvent（砧/工作台）、
 *   RegisterGuiLayersEvent（营养值 HUD）、EntityRenderersEvent.RegisterRenderers（全部 ICPM 实体渲染器）
 * - game bus：ClientTickEvent.Post（疾跑锁定 / 缩放 / 配置键提示）
 *
 * 注：S2C 网络处理器注册在公共入口 ICPMNeoForge（RegisterPayloadHandlersEvent）中完成，
 * 客户端接收 lambda 调 NutritionSyncHandler/InventoryCraftSyncHandler.handle。
 */
@Mod(value = ICPM.MOD_ID, dist = [Dist.CLIENT])
class ICPMClientNeoForge(modBus: IEventBus) {

    init {
        ICPM.LOGGER.info("ICPM NeoForge client constructing")
        // 按键注册（mod bus）
        modBus.addListener(RegisterKeyMappingsEvent::class.java) { evt ->
            ICPMKeyBindings.registerAll(evt)
        }
        // 菜单屏幕（mod bus）
        modBus.addListener(RegisterMenuScreensEvent::class.java) { evt ->
            ICPMClient.registerScreens(evt)
        }
        // 营养值 HUD（mod bus）：1.21.11 Gui 已重构为 layerManager 分层，原 Gui.render 内
        // renderHotbarAndDecorations 调用点消失（旧 NutritionHUDMixin 因此注入 0 目标崩溃）→
        // 改用 NeoForge 原生 GuiLayer，注册在经验条层之上
        modBus.addListener(RegisterGuiLayersEvent::class.java) { evt ->
            evt.registerAbove(
                VanillaGuiLayers.EXPERIENCE_LEVEL,
                Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "nutrition_hud"),
                GuiLayer { graphics, _ ->
                    name.icpm.client.hud.NutritionHUD.render(graphics, graphics.guiWidth(), graphics.guiHeight())
                }
            )
        }
        // 实体渲染器（mod bus）
        modBus.addListener(EntityRenderersEvent.RegisterRenderers::class.java) { evt ->
            ICPMClient.registerRenderers(evt)
        }
        // 客户端逐 tick（game bus）
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post::class.java) { _ ->
            ICPMClient.onClientTick()
        }
    }
}
