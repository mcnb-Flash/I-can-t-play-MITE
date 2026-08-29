package name.icpm.mixin;

import name.icpm.common.IcpmCreeperExplosionCalculator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MITE 忠实移植：爬行者爆炸的方块破坏半径收窄为 ×0.715（实体伤害半径维持 ×1.1）。
 * <p>
 * 全局拦截 {@link Level#explode} 的 6 参重载（void），仅当爆炸源是 {@link Creeper} 时生效：
 * 取消原调用，改为用 9 参重载并传入 {@link IcpmCreeperExplosionCalculator}，同时把实体半径放大 ×1.1。
 * 其余爆炸（TNT、恶魂火球等）不受影响。
 */
@Mixin(net.minecraft.world.level.Level.class)
public class CreeperExplosionMixin {

    @Inject(
        method = "explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void icpm$creeperTwoRadius(Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction interaction, CallbackInfo ci) {
        if (!(source instanceof Creeper)) {
            return;
        }
        ci.cancel();
        float entityRadius = radius * 1.1F;
        // 6 参 explode 返回 void；此处改用 9 参重载触发爆炸（返回值忽略）
        source.level().explode(
            source,
            source.damageSources().explosion(source, (LivingEntity) source),
            new IcpmCreeperExplosionCalculator(),
            x, y, z,
            entityRadius,
            false,
            interaction
        );
    }
}
