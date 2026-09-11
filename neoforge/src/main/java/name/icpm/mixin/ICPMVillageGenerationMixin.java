package name.icpm.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 村庄 60 天生成机制（忠实移植 R196）。
 *
 * R196 源码：MapGenVillage.canSpawnStructureAtCoords 中
 *   if (this.worldObj.getDayOfWorld() < 60) return false;
 * 其中 getDayOfWorld(tick) = (tick + 6000) / 24000 + 1，min_day_for_village_generation = 60。
 *
 * 1.21.11 等价入口：ChunkGenerator.createStructures，每个结构集调用一次，
 * 末尾 ResourceKey 即当前结构集的注册键（其 identifier 为 "minecraft:villages"）。
 * 我们据此识别 villages 结构集，在世界天数 < 60 时跳过其生成
 * （已生成的区块不会回退，与原版行为一致）。
 */
@Mixin(ChunkGenerator.class)
public abstract class ICPMVillageGenerationMixin {

    private static final String VILLAGES_LOCATION = "minecraft:villages";
    private static final long MIN_DAY_FOR_VILLAGE = 60L;

    @Inject(method = "createStructures", at = @At("HEAD"), cancellable = true)
    private void icpm$gateVillage(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager,
            ResourceKey<?> resourceKey,
            CallbackInfo ci) {
        if (resourceKey == null || !resourceKey.identifier().toString().equals(VILLAGES_LOCATION)) {
            return;
        }

        // NeoForge：主世界天数由 ICPMEventHandlers 每 tick 经 ICPMWorldDay 更新（无 FabricLoader）。
        // 结构生成先于首个 tick 时读 0 → 视为未满 60 天，与 R196 早期世界一致（不会提前刷村）。
        long day = name.icpm.common.ICPMWorldDay.get();
        if (day < MIN_DAY_FOR_VILLAGE) {
            ci.cancel();
        }
    }
}
