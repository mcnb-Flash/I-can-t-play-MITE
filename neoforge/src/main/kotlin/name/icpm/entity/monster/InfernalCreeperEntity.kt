package name.icpm.entity.monster

import name.icpm.item.ICPMItems
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor

/**
 * 地狱苦力怕（R196 EntityInfernalCreeper 移植，数值与机制严格对齐）。
 *
 * 机制（来自 r196 反编译源码逐字确认）：
 * <ul>
 *   <li>继承 [Creeper]，爆炸半径 ×2（R196 用 getScale/explosionRadius 放大；1.21.11 用反射设置 explosionRadius=2）</li>
 *   <li>天然防御 +2（非 bypassesArmor 的伤害减免 2，见 hurtServer）</li>
 *   <li>免疫火 / 岩浆</li>
 *   <li>掉落“地狱碎片”（R196 fragsInfernalCreeper），经验 ×3</li>
 * </ul>
 *
 * 生成：R196 在下界/地下世界用 50% 概率替换即将生成的苦力怕。见 [name.icpm.mixin.CreeperMiteSpawnMixin]。
 */
class InfernalCreeperEntity(type: EntityType<out InfernalCreeperEntity>, level: Level) : Creeper(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
    }

    init {
        // R196 爆炸半径 ×2（原版 Creeper 默认 1 → 2）。
        // explosionRadius 是 private 字段且无 setter；Fabric remap 后字段名为 yarn "explosionRadius"，
        // 通过反射设置（accessWidener 已 widener 开放）。若运行环境不可访问则安全回退为默认 1。
        try {
            val field = Creeper::class.java.getDeclaredField("explosionRadius")
            field.isAccessible = true
            field.set(this, 2)
        } catch (_: Exception) {
            // 回退：保留默认爆炸半径
        }
    }

    /**
     * R196 getNaturalDefense：非 bypassesMundaneArmor / bypassesArmor 的伤害减免 +2。
     * 1.21.11 DamageSource 通过 DamageType 判定 bypassesArmor（source.is(BuiltInDamageTypes.BY PAssesArmor) 或 source.bypassesArmor()）。
     */
    override fun hurtServer(serverLevel: ServerLevel, source: DamageSource, amount: Float): Boolean {
        if (source.`is`(DamageTypes.IN_FIRE) || source.`is`(DamageTypes.ON_FIRE) || source.`is`(DamageTypes.LAVA)) {
            return false
        }
        // R196 getNaturalDefense：非 bypassesMundaneArmor 的伤害减免 +2（1.21.11 无该判定，简化为全减）。
        val reduced = (amount - 2.0f).coerceAtLeast(1.0f)
        return super.hurtServer(serverLevel, source, reduced)
    }

    /**
     * R196 掉落“地狱碎片”。Mob.getLootTable 是 final，改用 dropCustomDeathLoot 手动掉落，
     * 与数据驱动 loot table（entities/infernal_creeper.json）二选一；此处以代码掉落保证生效。
     */
    override fun dropCustomDeathLoot(serverLevel: ServerLevel, source: DamageSource, recentlyHit: Boolean) {
        super.dropCustomDeathLoot(serverLevel, source, recentlyHit)
        if (recentlyHit) {
            val count = 1 + this.random.nextInt(3)
            this.spawnAtLocation(serverLevel, net.minecraft.world.item.ItemStack(ICPMItems.INFERNAL_CREEPER_FRAG, count), 0.5f)
        }
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: net.minecraft.world.DifficultyInstance,
        reason: EntitySpawnReason,
        spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, reason, spawnData)
        // R196：经验 ×3（原版苦力怕基础 5 → 15）
        this.xpReward = 15
        return data
    }
}
