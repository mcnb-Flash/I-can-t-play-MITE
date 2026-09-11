package name.icpm.entity.monster

import net.minecraft.world.DifficultyInstance
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import org.jspecify.annotations.Nullable

/**
 * 巨型僵尸（R196 EntityGiantZombie 移植，数值严格对齐）。
 *
 * <p>背景：巨型僵尸原本是 Mojang 废案（原版 Giant 实体未被使用）。本实现将其作为
 * 「血月之夜地表自然刷新的普通僵尸」的 1/200 稀有替换体（见 [name.icpm.mixin.ZombieMiteSpawnMixin]）。
 *
 * <p>设计：继承原版 [Zombie]，故拥有完整僵尸 AI（近战 / 索敌 / 破门 / 装备等），
 * 仅覆盖属性与尺寸。数值来自 r196：
 * <ul>
 *   <li>最大生命 100（= 50 颗心）</li>
 *   <li>移动速度 0.5</li>
 *   <li>攻击伤害 50</li>
 *   <li>碰撞箱 ×6（3.6 × 11.7，在 EntityType 构建时设置）</li>
 * </ul>
 */
class GiantZombieEntity(type: EntityType<out GiantZombieEntity>, level: Level) : Zombie(type, level) {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Zombie.createAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5)
            .add(Attributes.ATTACK_DAMAGE, 50.0)
            .add(Attributes.FOLLOW_RANGE, 40.0)
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        reason: EntitySpawnReason,
        spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        val data = super.finalizeSpawn(level, difficulty, reason, spawnData)
        // 巨型僵尸不接受幼年形态（R196 无 baby 概念）
        this.setBaby(false)
        // 巨型怪物给予高额经验
        this.xpReward = 50
        return data
    }
}
