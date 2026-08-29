package name.icpm.client.network;

import name.icpm.client.hud.NutritionHUD;
import name.icpm.component.NutritionComponent;
import name.icpm.network.NutritionSyncPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * 营养值同步处理器
 * 处理服务端发送的营养值同步包
 */
public class NutritionSyncHandler {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NutritionSyncPacket.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                NutritionComponent nutrition = new NutritionComponent(
                    payload.protein(), payload.essentialFats(), payload.phytonutrients()
                );
                NutritionHUD.INSTANCE.setClientNutrition(nutrition);
            });
        });
    }
}
