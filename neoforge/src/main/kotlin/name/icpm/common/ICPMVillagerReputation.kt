package name.icpm.common

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.UUID

/**
 * R196 村民声望系统（忠实移植 `Village.playerReputation`）。
 *
 * 判决源（R196 反编译）：
 *  - `Village.java:282` `getReputationForPlayer(String)` → 无记录默认 0
 *  - `Village.java:287` `setReputationForPlayer(String, delta)` → `clamp(rep + delta, -30, 10)`
 *  - `Village.java:294` `isPlayerReputationTooLow(String)` → `rep <= -15`
 *
 * 变更来源（`EntityVillager.java`）：
 *  - 交易成功            +1   (:147，buyingList 重建时)
 *  - 被玩家攻击          -1   (:260-264；攻击者为小孩时 -3)
 *  - 被玩家杀死          -2   (:279)
 *
 * 效果（`Village.java:218` `func_82685_c` ← `EntityAIDefendVillage.java:31`）：
 *  - 声望 <= -15 的玩家会被【铁傀儡】列入攻击目标（防御村庄 AI 取最近的"仇恨"玩家）。
 *
 * 存储差异（有记录）：R196 声望挂在【村庄】对象（`Village.playerReputation`）上，同一村庄共享；
 * 1.21.11 无村庄实体，改为按【玩家】全局持久化，随 Player NBT 存取（`PlayerMixin` 挂钩）。
 * —— 记为已知差异：多人共用一村时声望不再按村隔离，按玩家累计。
 */
object ICPMVillagerReputation {

    /** 下限（R196 MathHelper.clamp_int(..., -30, 10)）。 */
    const val MIN = -30

    /** 上限。 */
    const val MAX = 10

    /** 视为"仇恨玩家"的阈值，<= 该值时铁傀儡可主动攻击（R196 isPlayerReputationTooLow）。 */
    const val HOSTILE = -15

    private const val KEY = "icpm_village_reputation"

    private val REPUTATION = HashMap<UUID, Int>()

    @JvmStatic
    fun get(uuid: UUID): Int = REPUTATION[uuid] ?: 0

    @JvmStatic
    fun get(player: Player): Int = get(player.uuid)

    /** 返回变更后的声望值（R196 `setReputationForPlayer` 语义，已钳制）。 */
    @JvmStatic
    fun add(player: Player, delta: Int): Int {
        val v = (get(player) + delta).coerceIn(MIN, MAX)
        REPUTATION[player.uuid] = v
        return v
    }

    /** 该玩家是否被村庄视为仇恨目标（R196 `isPlayerReputationTooLow`）。 */
    @JvmStatic
    fun isHostile(player: Player): Boolean = get(player) <= HOSTILE

    @JvmStatic
    fun onPlayerDisconnect(uuid: UUID) {
        REPUTATION.remove(uuid)
    }

    // ===== NBT（PlayerMixin readAdditionalSaveData / addAdditionalSaveData 挂钩） =====

    @JvmStatic
    fun load(player: Player, tag: ValueInput) {
        REPUTATION[player.uuid] = tag.getInt(KEY).orElse(0).coerceIn(MIN, MAX)
    }

    @JvmStatic
    fun save(player: Player, tag: ValueOutput) {
        tag.putInt(KEY, get(player))
    }
}
