package name.icpm.common

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.UUID

/**
 * R196 FoodStats 饱食度系统忠实移植（satiation 饱腹 + nutrition 营养 + hunger 饥饿累积）。
 *
 * 与原版 foodLevel/saturation/exhaustion 模型的区别：
 * - **双槽**：nutrition（营养，主显示条 = foodLevel）与 satiation（饱腹，保护层 = saturation），
 *   上限随等级 `getNutritionLimit = clamp(6 + level/5*2, 6, 20)`（与 PlayerStatsManager.calculateMaxFood 一致）。
 * - **消耗**：hunger 每 tick 固定累积 0.002（× 湿/营养不良乘数；睡眠 ×20），每累计 4.0
 *   消耗 1 单位——先消耗 satiation；satiation 耗尽 或 "只计营养的饥饿"（hunger_for_nutrition_only ≥ 4.0
 *   且 nutrition>0）时消耗 nutrition。消耗速率不随活动变化（与 R196 一致，原版 exhaustion 全禁）。
 * - **自然回血**：healProgress 每 tick 累积 (0.0004 + nutrition×0.00002) ×（营养不良×0.25）（睡眠×4），
 *   ≥1.0 回 1 血并 +1.0 hunger（nutrition 越高回血越快）。
 * - **饥饿伤害**：isStarving（satiation==0，R196 EntityPlayer.isStarving()=getSatiation()==0）时
 *   starveProgress 累积 0.002/tick，每 1.0 按难度扣 1 血（血>10 或 困难 或 血>1且普通）。
 *
 * 载体：每玩家状态存于 Player NBT（load/save，由 PlayerMixin 挂钩）。
 * GUI 显示：foodLevel=nutrition、saturationLevel=satiation（FoodData 仅作显示层，
 * 其消耗/回血/饥饿逻辑已由 FoodDataMixin 全部禁用）。
 */
object ICPMFoodStats {

    const val HUNGER_PER_TICK = 0.002f          // R196 getHungerPerTick
    const val HUNGER_PER_FOOD_UNIT = 4.0f       // R196 getHungerPerFoodUnit
    const val SLEEP_HUNGER_MULTIPLIER = 20f     // R196 床上 hunger ×20
    const val STARVE_PROGRESS_PER_TICK = 0.002f // R196 starve_progress 速率
    const val HEAL_BASE = 0.0004f               // R196 heal_progress 基础
    const val HEAL_PER_NUTRITION = 0.00002f     // R196 nutrition 回血系数

    class State {
        var satiation = 0
        var nutrition = 0
        var hunger = 0f
        var hungerForNutritionOnly = 0f
        var healProgress = 0f
        var starveProgress = 0f
    }

    private val states = java.util.WeakHashMap<UUID, State>()

    private fun get(player: Player): State =
        states.computeIfAbsent(player.uuid) {
            val s = State()
            val limit = PlayerStatsManager.calculateMaxFood(player.experienceLevel)
            s.satiation = limit
            s.nutrition = limit
            s
        }

    @JvmStatic
    fun getSatiation(player: Player): Int = get(player).satiation

    @JvmStatic
    fun getNutrition(player: Player): Int = get(player).nutrition

    @JvmStatic
    fun getNutritionLimit(player: Player): Int = PlayerStatsManager.calculateMaxFood(player.experienceLevel)

    fun addSatiation(player: Player, amount: Int) {
        val s = get(player)
        s.satiation = (s.satiation + amount).coerceAtMost(getNutritionLimit(player))
    }

    fun addNutrition(player: Player, amount: Int) {
        val s = get(player)
        s.nutrition = (s.nutrition + amount).coerceAtMost(getNutritionLimit(player))
    }

    // ===== R196 食物数值表（Item.java setFoodValue(satiation, nutrition, ...) 提取） =====
    private val FOOD_VALUES: Map<String, Pair<Int, Int>> = mapOf(
        "apple" to (2 to 1),
        "golden_apple" to (2 to 1),
        "bread" to (8 to 2),
        "porkchop" to (4 to 4),
        "cooked_porkchop" to (8 to 8),
        "cod" to (3 to 3),
        "cooked_cod" to (6 to 6),
        "salmon" to (3 to 3),
        "cooked_salmon" to (10 to 10),
        "beef" to (5 to 5),
        "cooked_beef" to (10 to 10),
        "chicken" to (3 to 3),
        "cooked_chicken" to (6 to 6),
        "cookie" to (3 to 1),
        "melon_slice" to (1 to 1),
        "carrot" to (1 to 2),
        "golden_carrot" to (1 to 2),
        "potato" to (3 to 1),
        "baked_potato" to (6 to 2),
        "poisonous_potato" to (2 to 0),
        "mushroom_stew" to (2 to 4),
        "rotten_flesh" to (2 to 1),
        "beetroot" to (2 to 1),
        "beetroot_soup" to (2 to 2),
        "pumpkin_pie" to (4 to 2),
        "sweet_berries" to (1 to 1),
        "glow_berries" to (1 to 1),
        "dried_kelp" to (1 to 1),
        "honey_bottle" to (2 to 1),
        "suspicious_stew" to (3 to 2),
        "tropical_fish" to (1 to 1),
        "pufferfish" to (1 to 0),
    )

    /** 进食：R196 addFoodValue（satiation + nutrition），不引入原版 foodLevel/saturation 数值 */
    @JvmStatic
    fun onFoodEaten(player: Player, stack: ItemStack) {
        val path = BuiltInRegistries.ITEM.getKey(stack.item)?.path ?: return
        val (sat, nut) = FOOD_VALUES[path] ?: defaultValues(stack)
        addSatiation(player, sat)
        addNutrition(player, nut)
    }

    private fun defaultValues(stack: ItemStack): Pair<Int, Int> {
        val base = stack.get(DataComponents.FOOD)?.nutrition() ?: 0
        return base to maxOf(1, base / 2)
    }

    /** R196 FoodStats.onUpdate 每 tick 调用（仅服务端；由 PlayerMixin.tick 驱动） */
    @JvmStatic
    fun tick(player: Player) {
        if (!player.isAlive) return
        val s = get(player)
        // 饥饿累积乘数：R196 getWetnessAndMalnourishmentHungerMultiplier —— ICPM 以营养不良 ×1.5 近似
        val malnourished = PlayerNutritionManager.getNutrition(player).isMalnourished()
        val hungerFactor = if (malnourished) 1.5f else 1.0f
        addHunger(player, s, HUNGER_PER_TICK * hungerFactor)
        // 非创造：饥饿只计营养（保证 nutrition 最终也会消耗）
        if (!player.abilities.instabuild) {
            s.hungerForNutritionOnly += HUNGER_PER_TICK * 0.25f
        }
        // 每累计 4.0 hunger 消耗 1 单位（先 satiation，再 nutrition）
        if (s.hunger >= HUNGER_PER_FOOD_UNIT) {
            s.hunger -= HUNGER_PER_FOOD_UNIT
            if (s.satiation > 0 || s.nutrition > 0) {
                if (s.satiation < 1 || (s.hungerForNutritionOnly + 0.001f >= HUNGER_PER_FOOD_UNIT && s.nutrition > 0)) {
                    s.nutrition--
                    s.hungerForNutritionOnly = 0f
                } else {
                    s.satiation--
                }
            }
        }
        // 睡眠：饥饿消耗 ×20
        if (player.isSleeping) {
            addHunger(player, s, HUNGER_PER_TICK * SLEEP_HUNGER_MULTIPLIER)
        }
        // ===== 饥饿伤害：R196 isStarving = satiation==0（EntityPlayer.isStarving） =====
        if (s.satiation <= 0) {
            s.healProgress = 0f
            s.starveProgress += STARVE_PROGRESS_PER_TICK
            if (s.starveProgress >= 1f) {
                val difficulty = player.level().difficulty
                if (player.health > 10f || difficulty == Difficulty.HARD
                    || (player.health > 1f && difficulty == Difficulty.NORMAL)) {
                    player.hurtServer(player.level() as net.minecraft.server.level.ServerLevel, player.level().damageSources().starve(), 1f)
                }
                s.starveProgress -= 1f
                s.hungerForNutritionOnly = 0f
            }
        } else {
            // ===== 自然回血（R196 heal_progress，nutrition 驱动） =====
            s.healProgress += (HEAL_BASE + s.nutrition * HEAL_PER_NUTRITION) *
                (if (malnourished) 0.25f else 1f) *
                (if (player.isSleeping) 4f else 1f) // R196: inBed ? 4.0f : 1.0f
            s.starveProgress = 0f
            // shouldHeal：血量 > 0 且未满
            if (player.health > 0f && player.health < player.maxHealth) {
                if (s.healProgress >= 1f) {
                    // 标记 ICPM 回血，绕过 DisableVanillaHealingMixin 拦截
                    ICPMHealProgressManager.beginHealing()
                    player.heal(1f)
                    ICPMHealProgressManager.endHealing()
                    addHunger(player, s, 1f)
                    s.healProgress -= 1f
                }
            } else {
                s.healProgress = 0f
            }
        }
        // 同步 GUI 显示层
        syncToFoodData(player, s)
    }

    private fun addHunger(player: Player, s: State, amount: Float) {
        if (player.abilities.instabuild || player.abilities.invulnerable) return
        s.hunger = minOf(s.hunger + amount, 40f)
    }

    /** 同步显示：foodLevel=nutrition、saturationLevel=satiation */
    private fun syncToFoodData(player: Player, s: State) {
        val foodData = player.foodData
        if (foodData.getFoodLevel() != s.nutrition) {
            foodData.setFoodLevel(s.nutrition)
        }
        if (foodData.getSaturationLevel() != s.satiation.toFloat()) {
            foodData.setSaturation(s.satiation.toFloat())
        }
    }

    // ===== NBT（PlayerMixin readAdditionalSaveData / addAdditionalSaveData 挂钩） =====

    @JvmStatic
    fun load(player: Player, tag: net.minecraft.world.level.storage.ValueInput) {
        val s = get(player)
        val limit = getNutritionLimit(player)
        s.satiation = tag.getInt("icpm_satiation").orElse(limit).coerceIn(0, limit)
        s.nutrition = tag.getInt("icpm_nutrition").orElse(limit).coerceIn(0, limit)
        s.hunger = tag.getFloatOr("icpm_hunger", 0f)
        s.hungerForNutritionOnly = tag.getFloatOr("icpm_hunger_for_nutrition_only", 0f)
        s.healProgress = tag.getFloatOr("icpm_heal_progress", 0f)
        s.starveProgress = tag.getFloatOr("icpm_starve_progress", 0f)
    }

    @JvmStatic
    fun save(player: Player, tag: net.minecraft.world.level.storage.ValueOutput) {
        val s = get(player)
        tag.putInt("icpm_satiation", s.satiation)
        tag.putInt("icpm_nutrition", s.nutrition)
        tag.putFloat("icpm_hunger", s.hunger)
        tag.putFloat("icpm_hunger_for_nutrition_only", s.hungerForNutritionOnly)
        tag.putFloat("icpm_heal_progress", s.healProgress)
        tag.putFloat("icpm_starve_progress", s.starveProgress)
    }
}
