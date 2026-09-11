package name.icpm.common

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player

/**
 * ICPM 玩家属性管理器
 *
 * 血量与饱食度上限机制：
 * - 初始上限：6点（3颗心/3个鸡腿）
 * - 每升高5级经验等级增加2点上限
 * - 最高上限：20点（10颗心/10个鸡腿）
 */
object PlayerStatsManager {
    const val MIN_MAX_HEALTH = 6.0f
    const val MAX_MAX_HEALTH = 20.0f
    const val MIN_MAX_FOOD = 6
    const val MAX_MAX_FOOD = 20
    const val LEVELS_PER_INCREASE = 5
    const val INCREASE_AMOUNT = 2

    /**
     * 根据经验等级计算最大生命值
     */
    @JvmStatic
    fun calculateMaxHealth(experienceLevel: Int): Float {
        val increases = experienceLevel / LEVELS_PER_INCREASE
        val maxHealth = MIN_MAX_HEALTH + increases * INCREASE_AMOUNT
        return maxHealth.coerceIn(MIN_MAX_HEALTH, MAX_MAX_HEALTH)
    }

    /**
     * 根据经验等级计算最大饱食度
     */
    @JvmStatic
    fun calculateMaxFood(experienceLevel: Int): Int {
        val increases = experienceLevel / LEVELS_PER_INCREASE
        val maxFood = MIN_MAX_FOOD + increases * INCREASE_AMOUNT
        return maxFood.coerceIn(MIN_MAX_FOOD, MAX_MAX_FOOD)
    }

    /**
     * 更新玩家的属性上限
     */
    @JvmStatic
    fun updatePlayerStats(player: Player) {
        val maxHealth = calculateMaxHealth(player.experienceLevel)
        val maxFood = calculateMaxFood(player.experienceLevel)

        // 更新最大生命值
        val attribute = player.getAttribute(Attributes.MAX_HEALTH)
        if (attribute != null) {
            val currentValue = attribute.baseValue
            if (currentValue.toFloat() != maxHealth) {
                attribute.baseValue = maxHealth.toDouble()
                // 使用 setHealth 而非直接赋值，确保客户端/服务端正确同步
                if (player.health > maxHealth) {
                    player.setHealth(maxHealth)
                }
            }
        }

        // 限制当前饱食度（setFoodLevel 会触发 FoodDataMixin 自动钳制饱和度）
        val foodData = player.foodData
        if (foodData.foodLevel > maxFood) {
            foodData.setFoodLevel(maxFood)
        }
    }

    /**
     * 判断玩家是否可以进食普通食物
     * 
     * 规则：
     * - 如果饱食度 < 最大值，允许进食
     * - 如果饱和度 < 当前饱食度（饱和度未满），允许进食
     *   （ICPM 设计：饱食度满后仍可通过进食积累饱和度）
     * - 否则拒绝进食普通食物
     * 
     * 注意：金苹果等特殊物品通过 ignoreHunger=true 绕过此检查
     */
    @JvmStatic
    fun canEat(player: Player): Boolean {
        val maxFood = calculateMaxFood(player.experienceLevel)
        val foodData = player.foodData
        // 饱食度未满，或饱和度尚未达到当前饱食度上限
        return foodData.foodLevel < maxFood || foodData.saturationLevel < foodData.foodLevel.toFloat()
    }
}