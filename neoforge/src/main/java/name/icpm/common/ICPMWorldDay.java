package name.icpm.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * 世界天数（day of world）持有者（NeoForge 桥接）。
 *
 * Fabric 版村庄 60 天生成 mixin 直接经 FabricLoader 取 MinecraftServer 读主世界天数；
 * NeoForge 无对等的 getGameInstance 入口，改为由 ICPMEventHandlers 在每个 ServerTickEvent.Post
 * 顺手更新本静态字段，mixin 只读。语义与 R196 getDayOfWorld 一致：
 *   day = (gameTime + 6000) / 24000 + 1
 * 服务器尚未跑 tick 时返回 0（守旧行为：天数 < 60，村庄不生成，与 R196 逻辑兼容）。
 */
public final class ICPMWorldDay {

    /** 当前主世界天数（>= 1；服务器未启动前为 0）。 */
    private static volatile long overworldDay = 0L;

    private ICPMWorldDay() {
    }

    /** 每 ServerTick 调用一次，刷新主世界天数。 */
    public static void updateFromServer(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            overworldDay = (overworld.getGameTime() + 6000L) / 24000L + 1L;
        }
    }

    public static long get() {
        return overworldDay;
    }
}
