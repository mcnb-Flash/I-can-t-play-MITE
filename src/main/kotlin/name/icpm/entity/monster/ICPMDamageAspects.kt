package name.icpm.entity.monster

import name.icpm.item.ICPMSilverArmor
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.item.ItemStack

/**
 * 复制 R196 [net.minecraft.src.EntityDamageSource] 构造时的"伤害属性"判定。
 *
 * MITE R196 中，特殊怪物通过 [net.minecraft.src.Entity.isImmuneTo] 决定是否吃伤害，
 * 而伤害来源是否带有 银 / 魔法 属性，由攻击者手中武器（或射出的箭）的
 * 材质与附魔决定：
 *   - 银属性(silver)：武器材质为银，或发射者使用银制武器
 *   - 魔法属性(magic)：武器已被附魔（含弓被附魔），或原版药水 / 魔法类伤害
 *
 * 1.21 原版有一个关键差异：附魔武器攻击**不会**生成 [DamageTypes.MAGIC] 伤害类型
 * （该类型仅限药水、潮涌、守卫者等）。因此若用 [DamageSource.is] 直接判断
 * [DamageTypes.MAGIC]，等价于"只有药水能伤害这些怪物"，而玩家手持**附魔武器**攻击时
 * 伤害来源仍是普通的 mob_attack / player 伤害类型 → 怪物对所有武器免疫。这正是
 * "灰/黑史莱姆、暗影、夜翼、尸妖无法被攻击"的根因。
 *
 * 本工具类直接检查武器 / 箭发射者手中武器本身是否被附魔、是否为银制，
 * 从而正确还原 R196 的判定。（注：火焰附加也是一种附魔，已被"魔法属性"涵盖。）
 */
object ICPMDamageAspects {

    /**
     * 魔法属性：原版魔法类伤害，或攻击者使用已附魔的武器 / 弓。
     */
    fun hasMagicAspect(source: DamageSource): Boolean {
        if (source.`is`(DamageTypes.MAGIC) || source.`is`(DamageTypes.INDIRECT_MAGIC)) return true
        return anyAttackerItem(source) { it.isEnchanted }
    }

    /**
     * 银属性：攻击者使用银制武器，或银箭的发射者使用银制武器。
     */
    fun hasSilverAspect(source: DamageSource): Boolean {
        return anyAttackerItem(source) { isSilverStack(it) }
    }

    // ===================== 内部辅助 =====================

    /**
     * 遍历"造成伤害的武器 / 箭发射者武器"物品（近战取攻击者主 / 副手；
     * 远程取箭发射者的主 / 副手），对任一物品命中 [predicate] 即返回 true。
     */
    private inline fun anyAttackerItem(source: DamageSource, predicate: (ItemStack) -> Boolean): Boolean {
        val direct = source.directEntity
        if (direct is AbstractArrow) {
            // 箭本身的物品为 protected，无法在类外读取；但 R196 中箭的魔法 / 银属性
            // 来自发射者(launcher)的武器附魔 / 银材质，故检查发射者手中武器即可。
            val owner = direct.owner
            if (owner is LivingEntity && (predicate(owner.mainHandItem) || predicate(owner.offhandItem))) {
                return true
            }
            return false
        }
        val attacker = source.entity
        if (attacker is LivingEntity) {
            return predicate(attacker.mainHandItem) || predicate(attacker.offhandItem)
        }
        return false
    }

    private fun isSilverStack(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return ICPMSilverArmor.isSilverTool(stack.item) ||
            stack.item.builtInRegistryHolder().key().identifier().path.contains("silver")
    }
}
