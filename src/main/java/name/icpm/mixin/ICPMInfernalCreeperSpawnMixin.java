package name.icpm.mixin;

import name.icpm.entity.ICPMEntities;
import name.icpm.entity.monster.InfernalCreeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * 地狱苦力怕 50% 替换（R196 {@code WorldServer.java:595-599}）。
 *
 * <pre>
 * if (entity_class == EntityCreeper.class) {
 *     // 露天 + 夜晚：75% 直接放弃本次生成（continue）
 *     if (this.hasSkylight() &amp;&amp; !this.isDaytime() &amp;&amp; this.rand.nextInt(4) != 0 &amp;&amp; this.isOutdoors(x, y, z)) continue;
 *     // 低海拔（概率 = (40 − y) / 40）且 50% → 换成地狱苦力怕
 *     if (this.rand.nextInt(40) &gt;= y &amp;&amp; this.rand.nextFloat() &lt; 0.5f) {
 *         return EntityInfernalCreeper.class;
 *     }
 * }
 * </pre>
 *
 * <p>1.21.11 的 {@code NaturalSpawner.spawnCategoryForPosition} 在 {@code finalizeSpawn} 之后
 * 调用 {@code ServerLevel.addFreshEntityWithPassengers} 把 Mob 放入世界 —— 本 mixin
 * **包裹（wrap）该调用**（仅自然刷怪路径），在此处完成类型替换（或按 R196 放弃生成），
 * 因此不影响刷怪蛋 / 刷怪笼 / 命令等其它 SpawnReason 的苦力怕。
 *
 * <p><b>为何用 @WrapOperation 而非 @Redirect</b>：{@code @Redirect} 对同一调用点是**独占**的，
 * Carpet 等 mod 的 {@code NaturalSpawnerMixin} 也重定向此点 ⇒ 后到者被 Mixin 跳过，
 * 而对方注入是 required（0/1）⇒ 直接抛 {@code InjectionError} 令整类转换失败、世界 tick 崩。
 * {@code @WrapOperation}（MixinExtras）不占用 redirect 槽位，可与其它 mod 的 redirect 链式共存。
 */
@Mixin(NaturalSpawner.class)
public abstract class ICPMInfernalCreeperSpawnMixin {

    @WrapOperation(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void icpm$infernalCreeperReplace(ServerLevel level, Entity entity, Operation<Void> original) {
        Entity toAdd = entity;
        if (entity instanceof Creeper creeper && !(entity instanceof InfernalCreeperEntity)) {
            BlockPos pos = creeper.blockPosition();
            // R196：主世界 + 夜晚 + 露天 → 75% 放弃本次生成
            if (level.dimension().equals(Level.OVERWORLD) && level.isDarkOutside()
                    && level.random.nextInt(4) != 0 && level.canSeeSky(pos)) {
                return;
            }
            // R196：nextInt(40) >= y 的概率 = (40 − y) / 40（y ≥ 40 时为 0），再 50%
            int y = Mth.floor(creeper.getY());
            if (level.random.nextInt(40) >= y && level.random.nextFloat() < 0.5f) {
                InfernalCreeperEntity infernal = ICPMEntities.INSTANCE.getINFERNAL_CREEPER()
                        .create(level, EntitySpawnReason.NATURAL);
                if (infernal != null) {
                    infernal.snapTo(creeper.getX(), creeper.getY(), creeper.getZ(),
                            creeper.getYRot(), creeper.getXRot());
                    infernal.setYHeadRot(creeper.getYHeadRot());
                    infernal.setHealth(infernal.getMaxHealth());
                    toAdd = infernal;
                }
            }
        }
        original.call(level, toAdd);
    }
}
