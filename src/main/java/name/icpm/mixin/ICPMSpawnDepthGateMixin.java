package name.icpm.mixin;

import name.icpm.common.ICPMMoonPhase;
import name.icpm.entity.ICPMEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * R196 刷怪深度门控（`WorldServer.getSuitableCreature`，判决源 `WorldServer.java:578-682`）。
 *
 * <p>R196 在主世界（`check_depth = isOverworld()`）按 y 深度过滤候选怪物，
 * 不满足时 `continue` 重新抽取（最多 16 次）。血月夜允许部分怪物上地表：
 * <pre>
 * is_blood_moon_up             = isBloodMoon(true)
 * is_freezing_biome            = biome.isFreezing()      (temperature &lt;= 0.15)
 * is_desert_biome              = biome.isDesertBiome()   (desert / desertHills / desertRiver)
 * can_spawn_ghouls_on_surface    = is_blood_moon_up
 * can_spawn_wights_on_surface    = is_blood_moon_up &amp;&amp; is_freezing_biome
 * can_spawn_shadows_on_surface   = is_blood_moon_up &amp;&amp; is_desert_biome
 * can_spawn_revenants_on_surface = is_blood_moon_up
 *
 * EntityGhoul            : y &gt; 56 &amp;&amp; !can_spawn_ghouls_on_surface     → 跳过
 * EntityWight            : y &gt; 48 &amp;&amp; !can_spawn_wights_on_surface     → 跳过
 * EntityVampireBat       : y &gt; 48 &amp;&amp; !is_blood_moon_up                → 跳过
 * EntityRevenant         : y &gt; 44 &amp;&amp; !can_spawn_revenants_on_surface   → 跳过
 * EntityInvisibleStalker : y &gt; 40                                    → 跳过
 * EntityShadow           : y &gt; 32 &amp;&amp; !can_spawn_shadows_on_surface    → 跳过
 * EntityBlackWidowSpider : rand &lt; 0.5                                → 跳过
 * EntitySpider           : hasSkylight &amp;&amp; rand.nextInt(4)==0 &amp;&amp; isOutdoors → 跳过
 * </pre>
 *
 * <p>1.21.11 的等价时机：`NaturalSpawner.spawnCategoryForPosition` 选好 `SpawnerData` 后
 * 调用私有静态 `NaturalSpawner.getMobForSpawn(ServerLevel, EntityType)` 创建实体 ——
 * 该方法返回 `null` 即放弃本次生成（原版语义）。本 mixin Redirect 该调用：
 * 深度不满足时返回 `null`，否则用与 vanilla 相同的 `type.create(level, NATURAL)` 创建实体。
 */
@Mixin(NaturalSpawner.class)
public abstract class ICPMSpawnDepthGateMixin {

    @Redirect(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/NaturalSpawner;getMobForSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/Mob;"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Mob icpm$depthGate(ServerLevel level, EntityType type,
                                      MobCategory category, ServerLevel spawnLevel, ChunkAccess chunk,
                                      BlockPos pos, NaturalSpawner.SpawnPredicate predicate,
                                      NaturalSpawner.AfterSpawnCallback callback) {
        if (type == null) {
            return null;
        }
        // 仅主世界做深度检查（R196 check_depth = isOverworld()）
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return (Mob) type.create(level, EntitySpawnReason.NATURAL);
        }
        int y = pos.getY();
        boolean bloodMoon = ICPMMoonPhase.isBloodMoonNight(level);
        Biome biome = level.getBiome(pos).value();
        boolean freezing = biome.getBaseTemperature() <= 0.15f;
        boolean desert = level.getBiome(pos).is(Biomes.DESERT);

        ICPMEntities entities = ICPMEntities.INSTANCE;
        if (type == entities.getGHOUL() && y > 56 && !bloodMoon) {
            return null;
        }
        if (type == entities.getWIGHT() && y > 48 && !(bloodMoon && freezing)) {
            return null;
        }
        if (type == entities.getVAMPIRE_BAT() && y > 48 && !bloodMoon) {
            return null;
        }
        if (type == entities.getREVENANT() && y > 44 && !bloodMoon) {
            return null;
        }
        if (type == entities.getINVISIBLE_STALKER() && y > 40) {
            return null;
        }
        if (type == entities.getSHADOW() && y > 32 && !(bloodMoon && desert)) {
            return null;
        }
        if (type == entities.getBLACK_WIDOW() && level.random.nextFloat() < 0.5f) {
            return null;
        }
        // R196：普通蜘蛛在露天时 1/4 概率放弃
        if (type == EntityType.SPIDER && level.random.nextInt(4) == 0 && level.canSeeSky(pos)) {
            return null;
        }
        return (Mob) type.create(level, EntitySpawnReason.NATURAL);
    }
}
