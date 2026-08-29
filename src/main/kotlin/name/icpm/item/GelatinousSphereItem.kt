package name.icpm.item

import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import name.icpm.entity.ICPMEntities
import name.icpm.entity.projectile.GelatinousSphereEntity

/**
 * R196 acd (GelatinousSphere) 移植：右击投掷凝胶球。
 * subtype: 0=史莱姆 1=胶体/琥珀 2=血块 3=酸 4=布丁
 */
class GelatinousSphereItem(properties: Properties, val subtype: Int, val attackDamage: Float, val projectilePower: Float) : Item(properties) {

    override fun use(level: Level, player: Player, interactionHand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(interactionHand)
        level.playSound(
            null, player.x, player.y, player.z,
            SoundEvents.SLIME_JUMP, SoundSource.NEUTRAL, 0.5f,
            0.4f / (level.random.nextFloat() * 0.4f + 0.8f)
        )
        if (level is ServerLevel) {
            val serverLevel = level as ServerLevel
            Projectile.spawnProjectileFromRotation(
                { srvLevel, livingEntity, itemStack ->
                    GelatinousSphereEntity(ICPMEntities.GELATINOUS_SPHERE, livingEntity, srvLevel, itemStack)
                },
                serverLevel, stack, player, 0.0f, projectilePower, 1.0f
            )
        }
        stack.consume(1, player)
        return InteractionResult.SUCCESS
    }
}
