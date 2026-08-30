package name.icpm.item

import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.level.Level

/**
 * ICPM 钓鱼竿。
 *
 * 原版 FishingRodItem.use() 的全部行为由 `Player.fishing` 是否为 null 二分：
 *   - 非 null → 只走「收线」分支（retrieve + 扣耐久），永远不会再抛竿；
 *   - null    → 才走「抛竿」分支（new FishingHook）。
 * 一旦玩家身上残留一个已经失效的钩子引用（钩子被丢弃/换维度/实体生成失败等），
 * 右键就永远只能收线、再也抛不出去，表现为「鱼竿完全无法使用」，且不会有任何报错。
 *
 * 这里在调用原版逻辑之前先把失效引用清掉，保证鱼竿在异常情况下依然可用。
 * 其余抛竿/收线/耐久/统计全部沿用原版实现，不做重复造轮子。
 */
class ICPMFishingRodItem(properties: Properties) : FishingRodItem(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val hook = player.fishing
        if (hook != null && isStale(player, hook)) {
            player.fishing = null
        }
        return super.use(level, player, hand)
    }

    /** 钩子是否已经是「玩家收不回来、但也还在占位」的僵尸引用。 */
    private fun isStale(player: Player, hook: FishingHook): Boolean {
        if (hook.isRemoved || !hook.isAlive) return true
        // 玩家换了维度，旧钩子不可能再被收线
        if (hook.level() !== player.level()) return true
        return false
    }
}
