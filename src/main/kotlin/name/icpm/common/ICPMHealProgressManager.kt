package name.icpm.common

import net.minecraft.world.entity.LivingEntity
import java.util.UUID

/**
 * ICPM 回血进度管理器
 *
 * 使用浮点进度而非计数器，实现精确的回血计时：
 * - 正常：每 1280 tick（64 现实秒）回复 1 点血量
 * - 睡觉：8 倍速度（每 160 tick 回复 1 点）
 */
object ICPMHealProgressManager {
    // 64 现实秒 = 64 * 20 = 1280 游戏刻
    private const val BASE_TICKS_PER_HEAL = 1280.0f
    // 睡觉时 8 倍速度
    private const val SLEEP_MULTIPLIER = 8.0f

    // 每个玩家的回血进度 (0.0 - 1.0)
    private val progressMap = mutableMapOf<UUID, Float>()

    // 标记当前是否正在进行 ICPM 回血（供 DisableVanillaHealingMixin 检查）
    @Volatile
    private var mitHealing = false

    @JvmStatic
    fun isMitHealing(): Boolean = mitHealing

    /**
     * ICPM 授权回血：以 mitHealing 标志包裹 heal，使 DisableVanillaHealingMixin 放行。
     * 供 ICPM 自研回血调用（再生附魔 / 吸血附魔 / 升级回血），避免被"拦截原版回血"误杀
     * （曾致再生附魔与吸血附魔有名无实——heal 被 DisableVanillaHealingMixin 无条件 cancel）。
     */
    @JvmStatic
    fun healAuthorized(entity: LivingEntity, amount: Float) {
        beginHealing()
        try {
            entity.heal(amount)
        } finally {
            endHealing()
        }
    }

    /**
     * 添加一个 tick 的回血进度
     * @param playerId 玩家 UUID
     * @param isSleeping 是否在睡觉
     * @return 本次是否应该回血（1.0 表示回 1 点）
     */
    @JvmStatic
    fun addTickProgress(playerId: UUID, isSleeping: Boolean): Float {
        val add = if (isSleeping) SLEEP_MULTIPLIER / BASE_TICKS_PER_HEAL else 1.0f / BASE_TICKS_PER_HEAL
        val current = progressMap.getOrDefault(playerId, 0.0f)
        var next = current + add
        if (next >= 1.0f) {
            next -= 1.0f
            progressMap.put(playerId, next)
            return 1.0f
        } else {
            progressMap.put(playerId, next)
            return 0.0f
        }
    }

    /**
     * 重置玩家的回血进度
     */
    @JvmStatic
    fun resetProgress(playerId: UUID) {
        progressMap.put(playerId, 0.0f)
    }

    /**
     * 标记正在进行 ICPM 回血
     */
    @JvmStatic
    fun beginHealing() {
        mitHealing = true
    }

    /**
     * 标记 ICPM 回血结束
     */
    @JvmStatic
    fun endHealing() {
        mitHealing = false
    }

    /**
     * 移除玩家的进度记录
     */
    @JvmStatic
    fun removeProgress(playerId: UUID) {
        progressMap.remove(playerId)
    }
}
