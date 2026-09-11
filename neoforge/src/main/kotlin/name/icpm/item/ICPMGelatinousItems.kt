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
 * ICPM 凝胶球物品注册（R196 acd GelatinousSphere）。
 * subtype: 0=史莱姆 1=胶体/琥珀 2=血块 3=酸 4=布丁
 */
object ICPMGelatinousItems {

    val slimeSphere: Item = register(
        "slime_sphere",
        GelatinousSphereItem(makeProperties("slime_sphere"), 0, 1.0f, 1.5f)
    )

    val ochreJelly: Item = register(
        "ochre_jelly",
        GelatinousSphereItem(makeProperties("ochre_jelly"), 1, 2.0f, 1.6f)
    )

    val crimsonBlob: Item = register(
        "crimson_blob",
        GelatinousSphereItem(makeProperties("crimson_blob"), 2, 3.0f, 1.7f)
    )

    val ooze: Item = register(
        "ooze",
        GelatinousSphereItem(makeProperties("ooze"), 3, 3.0f, 1.7f)
    )

    val pudding: Item = register(
        "pudding",
        GelatinousSphereItem(makeProperties("pudding"), 4, 4.0f, 1.8f)
    )

    // 生成蛋（R196: 每种凝胶方块都有对应生成蛋）
    val jellySpawnEgg: SpawnEggItem = register(
        "jelly_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("jelly_spawn_egg", ICPMEntities.JELLY))
    )

    val blobSpawnEgg: SpawnEggItem = register(
        "blob_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("blob_spawn_egg", ICPMEntities.BLOB))
    )

    val oozeSpawnEgg: SpawnEggItem = register(
        "ooze_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("ooze_spawn_egg", ICPMEntities.OOZE))
    )

    val puddingSpawnEgg: SpawnEggItem = register(
        "pudding_spawn_egg",
        SpawnEggItem(makeSpawnEggProperties("pudding_spawn_egg", ICPMEntities.PUDDING))
    )

    fun init() {
        // 物品在字段初始化时即注册
    }

    private fun makeProperties(name: String): Item.Properties {
        val key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name))
        return Item.Properties().setId(key).stacksTo(16)
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
