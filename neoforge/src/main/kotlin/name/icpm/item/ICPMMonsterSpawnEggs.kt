package name.icpm.item

import name.icpm.ICPM
import name.icpm.entity.ICPMEntities
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.SpawnEggItem

/**
 * ICPM 怪物生成蛋注册。
 *
 * 为 R196 中尚未拥有生成蛋的怪物补齐生成蛋：
 *   - 骷髅变种：长逝、长逝守卫、骨领主、湮灭骷髅
 *   - 蜘蛛变种：木蛛、洞窟蜘蛛、黑寡妇、相位蜘蛛、恶魔蜘蛛
 *   - 地狱犬
 */
object ICPMMonsterSpawnEggs {

    // ==================== 骷髅变种生成蛋 ====================
    val longdeadSpawnEgg: SpawnEggItem = register(
        "longdead_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("longdead_spawn_egg", ICPMEntities.LONGDEAD))
    )
    val longdeadGuardianSpawnEgg: SpawnEggItem = register(
        "longdead_guardian_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("longdead_guardian_spawn_egg", ICPMEntities.LONGDEAD_GUARDIAN))
    )
    val boneLordSpawnEgg: SpawnEggItem = register(
        "bone_lord_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("bone_lord_spawn_egg", ICPMEntities.BONE_LORD))
    )
    val annihilationSkeletonSpawnEgg: SpawnEggItem = register(
        "annihilation_skeleton_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("annihilation_skeleton_spawn_egg", ICPMEntities.ANNIHILATION_SKELETON))
    )

    // ==================== 蜘蛛变种生成蛋 ====================
    val woodSpiderSpawnEgg: SpawnEggItem = register(
        "wood_spider_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("wood_spider_spawn_egg", ICPMEntities.WOOD_SPIDER))
    )
    val caveSpiderVariantSpawnEgg: SpawnEggItem = register(
        "cave_spider_variant_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("cave_spider_variant_spawn_egg", ICPMEntities.CAVE_SPIDER_VARIANT))
    )
    val blackWidowSpawnEgg: SpawnEggItem = register(
        "black_widow_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("black_widow_spawn_egg", ICPMEntities.BLACK_WIDOW))
    )
    val phaseSpiderSpawnEgg: SpawnEggItem = register(
        "phase_spider_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("phase_spider_spawn_egg", ICPMEntities.PHASE_SPIDER))
    )
    val demonSpiderSpawnEgg: SpawnEggItem = register(
        "demon_spider_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("demon_spider_spawn_egg", ICPMEntities.DEMON_SPIDER))
    )

    // ==================== 地狱犬生成蛋 ====================
    val hellhoundSpawnEgg: SpawnEggItem = register(
        "hellhound_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("hellhound_spawn_egg", ICPMEntities.HELLHOUND))
    )

    // ==================== R196 补全怪物生成蛋（A 项） ====================
    val fireElementalSpawnEgg: SpawnEggItem = register(
        "fire_elemental_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("fire_elemental_spawn_egg", ICPMEntities.FIRE_ELEMENTAL))
    )
    val infernalCreeperSpawnEgg: SpawnEggItem = register(
        "infernal_creeper_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("infernal_creeper_spawn_egg", ICPMEntities.INFERNAL_CREEPER))
    )
    val direWolfSpawnEgg: SpawnEggItem = register(
        "dire_wolf_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("dire_wolf_spawn_egg", ICPMEntities.DIRE_WOLF))
    )
    val hoarySilverfishSpawnEgg: SpawnEggItem = register(
        "hoary_silverfish_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("hoary_silverfish_spawn_egg", ICPMEntities.HOARY_SILVERFISH))
    )

    fun init() {
        // 物品在字段初始化时即注册
    }

    private fun makeSpawnEggProperties(name: String, entityType: net.minecraft.world.entity.EntityType<out net.minecraft.world.entity.Mob>): Item.Properties {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name))
        return Item.Properties().setId(key).stacksTo(64).spawnEgg(entityType)
    }

    private fun <T : Item> register(name: String, item: T): T {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name))
        return Registry.register(BuiltInRegistries.ITEM, key, item)
    }
}
