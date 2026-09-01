package name.icpm.common

import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ICPM 经验机制（移植自 MITE R196 EntityPlayer 的自定义经验体系）
 *
 * 与 1.21.11 原版经验系统的根本区别：
 *  - 经验是一个**带符号的整数** [MIN_EXPERIENCE, +∞)，可为负（负经验 = 负等级 = 惩罚态）。
 *  - 等级是**派生值**，不单独存储；由 experience 通过曲线换算。
 *  - 等级范围：负等级 [MIN_LEVEL=-40, -1]，正等级 [0, MAX_LEVEL=200]。
 *
 * 经验曲线（正等级，与 R196 完全一致）：
 *   初始 xp=0, increase=20；对 level=1..200：increase+=10, xp+=increase。
 *   即 experience_for_level[1]=30, [2]=70, [3]=120 ...
 * 负等级需求：level<0 时 getExperienceRequired(level) = level*20（level -1 需 -20，... -40 需 -800）。
 *
 * 客户端同步：原版 Player 的 experienceLevel/experienceProgress/totalExperience 字段不支持负等级，
 * 这里把"非负部分"反映到这些字段（负等级时 experienceLevel=0, progress=0），并手动发
 * ClientboundSetExperiencePacket，使客户端经验条与等级数字正常显示。
 */
object ICPMExperience {

    private val LOGGER = LoggerFactory.getLogger("ICPM-Exp")

    const val MAX_LEVEL = 200
    const val MIN_LEVEL = -40
    const val MIN_EXPERIENCE = -800 // getExperienceRequired(MIN_LEVEL) = -40*20

    /**
     * ICPM 死亡/重生经验模型（对应 EntityPlayerMP.onDeath + ServerConfigurationManager.respawn）：
     * 每个玩家维护一个**持久化**的"重生经验下限"(respawn_experience)，存于玩家 NBT。
     *  - 死亡时（onDeath / ALLOW_DEATH）：仅当死亡前经验 <= 0（已耗尽或已进入负等级），
     *    才把下限下调一个「负等级档位」（下限 = 经验 - |getExperienceRequired(-1)| = 经验 - 20），
     *    并 clamp 到 MIN_EXPERIENCE(-800)；若死亡前经验 > 0，则不受任何惩罚（下限记为非负哨兵 0，
     *    绝不沿用陈旧负下限），重生时原版死亡清零照常生效。
     *  - 重生时（respawn / AFTER_RESPAWN）：仅当持久化下限 < 0 时，玩家经验被强制重置为该负下限。
     * 反复在经验 <= 0 时死亡 → 下限一路降到 -800（对应 -40 级），即"负等级惩罚态"。
     * 一旦经验回正（>0）后死亡，惩罚态即被清除，不会再把玩家拉回负等级。
     */

    /** experience_for_level[level] = 到达 level 级所需的累计经验（level 0..MAX_LEVEL） */
    private val experienceForLevel = IntArray(MAX_LEVEL + 1)

    init {
        var xp = 0
        var increase = 20
        for (level in 1..MAX_LEVEL) {
            increase += 10
            xp += increase
            experienceForLevel[level] = xp
        }
    }

    /**
     * 到达指定等级所需的累计经验。
     * - 负等级：level * 20（线性，-1 → -20，... -40 → -800）
     * - 正等级（≤200）：查表
     * - >200：Integer.MAX_VALUE（封顶）
     */
    @JvmStatic
    fun getExperienceRequired(level: Int): Int {
        return if (level < 0) {
            level * 20
        } else if (level > MAX_LEVEL) {
            Int.MAX_VALUE
        } else {
            experienceForLevel[level]
        }
    }

    /**
     * 由经验值换算等级（与 R196 getExperienceLevel 完全一致）。
     * - 负经验：Math.max(-((-exp - 1) / 20 + 1), MIN_LEVEL)
     * - 非负经验：线性查表找最高满足等级
     */
    @JvmStatic
    fun getExperienceLevel(experience: Int): Int {
        if (experience < 0) {
            return maxOf(-((-experience - 1) / 20 + 1), MIN_LEVEL)
        }
        var level = 0
        // 上限钳制：getExperienceRequired(level>MAX_LEVEL) 恒返回 Int.MAX_VALUE，
        // 若 experience 达到 Int.MAX_VALUE（如 /xp set 2147483647），原实现会 level++ 永远
        // 递增 → 服务端死循环卡死（保存/退出时尤甚）。必须限制 level < MAX_LEVEL。
        while (level < MAX_LEVEL && getExperienceRequired(level + 1) <= experience) {
            level++
        }
        return level
    }

    /**
     * 经验条进度（0~1），用于客户端显示。负等级时返回 0。
     */
    @JvmStatic
    fun getLevelProgress(experience: Int): Float {
        val level = getExperienceLevel(experience)
        val base = getExperienceRequired(level)
        val next = getExperienceRequired(level + 1)
        if (next <= base) return 0f
        return (experience - base).toFloat() / (next - base).coerceAtLeast(1)
    }

    /**
     * 由等级计算血量/饱食度上限（R196 getHealthLimit）：
     *   6 + level/5*2，钳制到 [6, 20]。负等级时为 6（下限）。
     */
    @JvmStatic
    fun getHealthLimit(level: Int): Int {
        return maxOf(minOf(6 + level / 5 * 2, 20), 6)
    }

    /** 等级加成类型（对应 R196 EnumLevelBonus，仅取本模组需要用到的） */
    enum class LevelBonus {
        HARVESTING, CRAFTING, MELEE_DAMAGE
    }

    /**
     * 由等级决定的"最低合成品质"（R196 getMinCraftingQuality 的等级部分）：
     *   quality_ordinal = clamp(average.ordinal() + level/10, wretched.ordinal(), average.ordinal())
     * 即每 10 级提升一个品质下限，最高不超过 average（R196 在未指定技能组时不会高于 average）。
     * 负等级时 level/10 为负，下限下调（R196 中负等级玩家更易做出低品质物品）。
     * 调用方需传入品质枚举的 ordinal 区间（wretched=0, average=2）。
     *
     * @param averageOrdinal  average 品质的 ordinal（本模组 EnumQuality.AVERAGE = 2）
     * @param minOrdinal      最低品质 ordinal（wretched = 0）
     */
    @JvmStatic
    fun getMinCraftingQualityOrdinal(level: Int, averageOrdinal: Int, minOrdinal: Int): Int {
        val raw = averageOrdinal + level / 10
        return raw.coerceIn(minOrdinal, averageOrdinal)
    }

    /**
     * 等级对属性/伤害的修正系数（R196 getLevelModifier）：
     *   - 正等级且为近战伤害：level * 0.005
     *   - 其它（含负等级惩罚）：level * 0.02
     */
    @JvmStatic
    fun getLevelModifier(level: Int, kind: LevelBonus): Float {
        return if (level > 0 && kind == LevelBonus.MELEE_DAMAGE) {
            level * 0.005f
        } else {
            level * 0.02f
        }
    }

    /**
     * R196 CraftingResult.getQualityAdjustedDifficulty：
     * 品质每高于 average 一档，难度翻倍；每低于 average 一档，难度减半。
     *   quality_adjusted = difficulty * 2^(qualityOrdinal - averageOrdinal)
     */
    @JvmStatic
    fun getQualityAdjustedDifficulty(difficulty: Float, qualityOrdinal: Int, averageOrdinal: Int): Float {
        val diff = qualityOrdinal - averageOrdinal
        return (difficulty * Math.pow(2.0, diff.toDouble())).toFloat()
    }

    /**
     * R196 EntityPlayer.getCraftingExperienceCost：
     * 合成高于 average 品质时消耗的额外经验 = round(quality_adjusted_difficulty / 5)。
     * R196 中 clumsiness 诅咒会翻倍，本模组无诅咒故系数为 1。
     */
    @JvmStatic
    fun getCraftingExperienceCost(qualityAdjustedDifficulty: Float): Int {
        return Math.round(qualityAdjustedDifficulty / 5.0f)
    }

    /**
     * R196 EntityPlayer.getMaxCraftingQuality：玩家在当前经验下能为该配方合成出的最高品质 ordinal。
     *   - 经验 <= 0（含负等级）：只能合成到最低品质（最高 = 最低），形成负等级惩罚
     *   - 否则从最高品质往 average+1 遍历，第一个"经验成本 <= 当前经验"的品质即最高可合成品质
     *   - 若 average 以上都负担不起，退回最低品质
     * 返回值保证落在 [minOrdinal, maxQualityOrdinal] 且 >= 等级下限 minOrdinal。
     *
     * @param experience         玩家带符号经验值（totalExperience，负数等级时为负）
     * @param difficulty         配方未调整难度（unmodified difficulty）
     * @param maxQualityOrdinal  该物品允许的最高品质 ordinal（本模组用 LEGENDARY）
     * @param averageOrdinal     average 品质 ordinal
     * @param minOrdinal         由 getMinCraftingQualityOrdinal 算出的等级下限 ordinal
     */
    @JvmStatic
    fun getMaxCraftingQualityOrdinal(
        experience: Int,
        difficulty: Float,
        maxQualityOrdinal: Int,
        averageOrdinal: Int,
        minOrdinal: Int
    ): Int {
        if (experience <= 0) {
            return minOrdinal
        }
        for (q in maxQualityOrdinal downTo averageOrdinal + 1) {
            val cost = getCraftingExperienceCost(getQualityAdjustedDifficulty(difficulty, q, averageOrdinal))
            if (cost <= experience) return q
        }
        return minOrdinal
    }

    // ===== 玩家经验状态（带符号 experience 整数，复用原版 totalExperience 字段存储） =====
    // 原版 Player 已将 totalExperience 持久化到 NBT（XpTotal），故无需自定义字段与读写。

    /** 读取玩家当前带符号经验值（即原版 totalExperience） */
    @JvmStatic
    fun getExperience(player: Player): Int {
        return player.totalExperience
    }

    /**
     * 重生下限计算（对应 EntityPlayerMP.onDeath 中对 respawn_experience 的处理）：
     *  - 仅当死亡前经验 <= 0（已耗尽或已进入负等级）：
     *        新下限 = 经验 - |getExperienceRequired(-1)|（= 经验 - 20，下调一个负等级档位 / 降低一级），
     *        并 clamp 到 [MIN_EXPERIENCE, +∞)；
     *  - 否则（经验 > 0）：完全不受惩罚，返回非负哨兵 0（绝不返回 currentFloor）。
     * 反复在经验 <= 0 时死亡会把下限一路压到 -800（=-40 级），形成负等级惩罚态。
     *
     * ⚠️ 关键：经验 > 0 时**必须返回 0（非负）**，绝不能返回持久化下限 currentFloor。
     * 否则一旦进入过负等级（currentFloor 已为 -20 之类），之后即便经验回正、死亡时经验 > 0，
     * 仍会被这个陈旧负下限强行拉回 -20（实测"死亡后强行降至 -1 级"的根因）。
     * 返回 0 后，AFTER_RESPAWN 的 `floor < 0` 判断不成立、不覆盖经验，原版死亡清零照常生效。
     *
     * @param currentExperience 死亡前的经验值
     * @param currentFloor     玩家已持久化的重生经验下限（仅在惩罚分支使用）
     */
    @JvmStatic
    fun computeRespawnFloor(currentExperience: Int, currentFloor: Int, applyPenalty: Boolean = true): Int {
        // 开启死亡不掉落（keepInventory）时，不施加任何负等级惩罚：返回非负哨兵 0，
        // 使 AFTER_RESPAWN 既不强行把经验压到负下限、也不弹惩罚提示。
        if (!applyPenalty) return 0
        // 仅当死亡前经验 <= 0（已耗尽或已进入负等级）才施加惩罚：每次死亡下调一个负等级档位（经验 - 20，降低一级）。
        // 经验 > 0 时完全不受惩罚：返回非负哨兵 0，使 AFTER_RESPAWN 不会强行把经验拉到负下限。
        return if (currentExperience <= 0) {
            val f = currentExperience - Math.abs(getExperienceRequired(-1))
            if (f < MIN_EXPERIENCE) MIN_EXPERIENCE else f
        } else {
            0
        }
    }

    // ===== 重生状态暂存（按 UUID，跨"死亡→重生"实体重建存活）=====
    // 关键：玩家死亡与重生发生在两个不同的 ServerPlayer 实体上——死亡的是旧实体，
    // 重生的是新实体（PlayerMixin 的 @Unique 字段 icpm$respawnExperience 在新实体上重置为 0）。
    // 因此不能在 per-entity 字段里传递下限，必须用按 UUID 的静态表暂存；
    // 同时 PlayerMixin 把下限写入玩家 NBT(icpm_respawn_experience) 以跨服务器重启保留。
    private val respawnFloorByUuid = ConcurrentHashMap<UUID, Int>()
    private val deathExperienceByUuid = ConcurrentHashMap<UUID, Int>()
    /** 本次死亡损失的经验值（用于重生提示），= 死亡前经验 - 重生下限 */
    private val deathPenaltyByUuid = ConcurrentHashMap<UUID, Int>()

    /**
     * 玩家死亡时调用：计算新的重生经验下限并暂存（同时由 PlayerMixin 写入 NBT 持久化）。
     * @return 计算后的下限（已 clamp 到 [MIN_EXPERIENCE, +∞)）
     */
    @JvmStatic
    fun recordDeath(uuid: UUID, currentExperience: Int, currentFloor: Int, applyPenalty: Boolean = true): Int {
        val newFloor = computeRespawnFloor(currentExperience, currentFloor, applyPenalty)
        respawnFloorByUuid[uuid] = newFloor
        deathExperienceByUuid[uuid] = currentExperience
        deathPenaltyByUuid[uuid] = currentExperience - newFloor
        LOGGER.info("[ICPM-Exp] recordDeath: uuid={} cur={} floorBefore={} newFloor={} penalty={} applyPenalty={}", uuid, currentExperience, currentFloor, newFloor, currentExperience - newFloor, applyPenalty)
        return newFloor
    }

    /** 读取重生经验下限（死亡→重生之间由 recordDeath 暂存） */
    @JvmStatic
    fun getRespawnFloor(uuid: UUID): Int = respawnFloorByUuid[uuid] ?: 0

    /**
     * 把持久化下限写回静态表（readAdditionalSaveData 从 NBT 恢复时调用），
     * 使「重启后首死」也能拿到正确的 prevFloor，无需再依赖 per-entity @Unique 字段。
     */
    @JvmStatic
    fun setRespawnFloor(uuid: UUID, floor: Int) {
        respawnFloorByUuid[uuid] = floor
    }


    /** 读取死亡前经验（仅用于兼容/调试） */
    @JvmStatic
    fun getDeathExperience(uuid: UUID): Int = deathExperienceByUuid[uuid] ?: 0

    /** 读取本次死亡损失的经验值（用于重生提示） */
    @JvmStatic
    fun getDeathPenalty(uuid: UUID): Int = deathPenaltyByUuid[uuid] ?: 0

/**
 * 重生完成后清理「提示用」暂存（死亡前经验、损失值）。
 * 重生经验下限 respawnFloorByUuid 不清——它作为「当前惩罚态」保留，供再次死亡时当 prevFloor、
 * 以及写盘 NBT；下次死亡由 recordDeath 覆盖。
 */
    @JvmStatic
    fun clearDeathState(uuid: UUID) {
        deathExperienceByUuid.remove(uuid)
        deathPenaltyByUuid.remove(uuid)
    }

    /**
     * 玩家重生时调用：把经验重置为持久化重生下限，并同步显示字段与客户端。
     * 实际下限的持久化与读取由 PlayerMixin 完成（icpm$respawnExperience 字段 + NBT）。
     */
    @JvmStatic
    fun applyRespawnExperience(player: Player, floor: Int) {
        val before = player.totalExperience
        player.totalExperience = floor
        syncToVanilla(player, floor)
        LOGGER.info("[ICPM-Exp] applyRespawnExperience: uuid={} before={} after={} isServerPlayer={}", player.getUUID(), before, floor, player is ServerPlayer)
    }

    /**
     * 修改玩家经验（带符号加减）。统一入口：所有经验流入/流出都走这里。
     * 处理：clamp 下限、等级变化时的回血/钳制、同步原版显示字段、向客户端发包。
     */
    @JvmStatic
    @JvmOverloads
    fun addExperience(player: Player, amount: Int, suppressHealing: Boolean = false, suppressSound: Boolean = false) {
        val levelBefore = getExperienceLevel(player.totalExperience)
        val healthLimitBefore = getHealthLimit(levelBefore)

        var exp = player.totalExperience + amount
        if (exp < MIN_EXPERIENCE) exp = MIN_EXPERIENCE
        player.totalExperience = exp

        val levelAfter = getExperienceLevel(exp)
        val levelChange = levelAfter - levelBefore

        if (levelChange < 0) {
            // 降级：把血量钳制到新的（可能更低的）上限
            if (player.health > player.maxHealth) player.setHealth(player.maxHealth)
        } else if (levelChange > 0) {
            val healthLimitAfter = getHealthLimit(levelAfter)
            if (healthLimitAfter > healthLimitBefore && !suppressHealing) {
                player.heal((healthLimitAfter - healthLimitBefore).toFloat())
            }
            if (!suppressSound) {
                player.playSound(
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    (if (levelAfter > 30) 1.0f else levelAfter / 30.0f) * 0.75f,
                    1.0f
                )
            }
        }

        syncToVanilla(player, exp)
    }

    /**
     * 给玩家"相当于 n 级"的经验（替代原版 giveExperienceLevels）。
     * 负 n 表示降到低 n 级。
     */
    @JvmStatic
    fun addExperienceLevels(player: Player, levels: Int) {
        val cur = getExperience(player)
        val target = getExperienceLevel(cur) + levels
        val clampedTarget = target.coerceIn(MIN_LEVEL, MAX_LEVEL)
        val delta = getExperienceRequired(clampedTarget) - cur
        addExperience(player, delta, suppressHealing = true, suppressSound = true)
    }

    /**
     * 把 ICPM 带符号经验同步到原版 Player 字段并通知客户端。
     * 重要：原版 experienceLevel / experienceProgress 字段**不支持负值**，必须把等级钳为非负
     * （负等级一律写 0），否则把负值喂给原版伤害/属性流程会导致异常（实测：负等级玩家作为受害者
     * 时无法受到伤害）。真实带符号等级由客户端 HUD 从 totalExperience（带符号真值）派生显示。
     * - experienceLevel / experienceProgress / 经验包中的 level：均钳为非负（负等级→0）。
     * - totalExperience：保留带符号真值（负数），供命令/判定/HUD 使用。
     */
    @JvmStatic
    fun syncToVanilla(player: Player, experience: Int) {
        val level = getExperienceLevel(experience)
        val progress = getLevelProgress(experience).coerceIn(0f, 1f)
        // 原版字段保持非负：负等级 → 0（避免负值进入原版伤害/属性流程）
        val displayLevel = maxOf(level, 0)
        player.experienceLevel = displayLevel
        player.experienceProgress = progress
        player.totalExperience = experience
        // 构造器注入阶段 connection 尚未建立（为 null），此处必须守卫：
        // 否则新建玩家进入世界时（Player.<init> → onConstruct）会 NPE 崩溃。
        if (player is ServerPlayer && player.connection != null) {
            // 发包时 level 同样钳为非负；客户端 HUD 改用 totalExperience 推导真实带符号等级
            player.connection.send(ClientboundSetExperiencePacket(progress, displayLevel, experience))
        }
    }
}
