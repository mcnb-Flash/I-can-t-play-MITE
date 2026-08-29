package name.icpm.mixin;

import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 怪物上限（MITE 移植，infx 开发计划参考）。
 *
 * A5 怪物上限改为 MITE 的每玩家 50 只（原版 MONSTER 为 70）。
 * 注入 MobCategory.getMaxInstancesPerChunk 的返回值，仅对 MONSTER 类别改为 50，
 * 其余类别（CREATURE/AMBIENT/AXOLOTL/WATER/AQUATIC 等）保持原版。
 * 该方法被 NaturalSpawner 的怪物容量计算（每玩家比例）读取，全局生效。
 */
@Mixin(MobCategory.class)
public abstract class ICPMMobCapMixin {

    @Inject(method = "getMaxInstancesPerChunk", at = @At("RETURN"), cancellable = true)
    private void icpm$monsterCap50(CallbackInfoReturnable<Integer> cir) {
        MobCategory self = (MobCategory) (Object) this;
        if (self == MobCategory.MONSTER) {
            cir.setReturnValue(50);
        }
    }
}
