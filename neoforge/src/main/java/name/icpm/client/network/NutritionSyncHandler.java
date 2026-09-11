package name.icpm.client.network;

import name.icpm.client.hud.NutritionHUD;
import name.icpm.component.NutritionComponent;
import name.icpm.network.NutritionSyncPacket;

/**
 * 营养值同步处理器
 * 处理服务端发送的营养值同步包
 *
 * NeoForge：由 ICPMNeoForge 的 RegisterPayloadHandlersEvent 中 playToClient lambda 调用。
 */
public class NutritionSyncHandler {

    private NutritionSyncHandler() {
    }

    /** 客户端处理（NeoForge payload handler 已包 enqueueWork，此处直接执行）。 */
    public static void handle(NutritionSyncPacket payload) {
        NutritionComponent nutrition = new NutritionComponent(
            payload.protein(), payload.essentialFats(), payload.phytonutrients()
        );
        NutritionHUD.INSTANCE.setClientNutrition(nutrition);
    }
}
