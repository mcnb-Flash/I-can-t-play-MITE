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
 * ICPM 土元素生成蛋注册（R196 土元素对应生成蛋）。
 */
object ICPMEarthElementalItems {

    val earthElementalSpawnEgg: SpawnEggItem = register(
        "earth_elemental_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("earth_elemental_spawn_egg", ICPMEntities.EARTH_ELEMENTAL))
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
