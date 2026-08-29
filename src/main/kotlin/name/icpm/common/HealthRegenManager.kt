package name.icpm.common

import net.minecraft.world.entity.player.Player

/**
 * ICPM 血量恢复管理器
 *
 * 血量恢复机制：
 * - 正常：每1280游戏刻（64现实秒）回复1点血量
 * - 睡觉：8倍速度（每160游戏刻回复1点）
 */
object HealthRegenManager {
    // 64现实秒 = 64 * 20 = 1280游戏刻
    const val TICKS_PER_HEALTH_NORMAL = 1280
    // 睡觉时8倍速度 = 1280 / 8 = 160游戏刻
    const val TICKS_PER_HEALTH_SLEEPING = 160

    // 半个游戏日 = 6000游戏刻
    const val TICKS_PER_FOOD_DRAIN = 6000

    // 困难模式饱和度消耗倍率 (原版困难为4.0，ICPM为原版困难的8倍 = 32)
    const val SATURATION_EXHAUSTION_MULTIPLIER = 32f

    /**
     * 检查是否应该恢复血量
     * @param player 玩家
     * @param ticksSinceLastRegen 上次恢复以来的tick数
     * @return 是否应该恢复
     */
    @JvmStatic
    fun shouldRegenHealth(player: Player, ticksSinceLastRegen: Int): Boolean {
        val regenInterval = if (player.isSleeping) {
            TICKS_PER_HEALTH_SLEEPING
        } else {
            TICKS_PER_HEALTH_NORMAL
        }
        return ticksSinceLastRegen >= regenInterval
    }

    /**
     * 获取恢复间隔（游戏刻）
     */
    @JvmStatic
    fun getRegenInterval(player: Player): Int {
        return if (player.isSleeping) {
            TICKS_PER_HEALTH_SLEEPING
        } else {
            TICKS_PER_HEALTH_NORMAL
        }
    }
}