package name.icpm.mixin;

import name.icpm.common.ICPMMoonPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 血月全群系降雨（MITE 移植，infx 开发计划参考）。
 *
 * A2 血月日全群系降雨：Level.precipitationAt(BlockPos) 在血月日（主世界）绕过热群系
 * "无降水"门控——沙漠/恶地等群系降水原本为 Biome.Precipitation.NONE，血月日强制改 RAIN。
 * 由此衍生的统一效果（均读同一方法自动跟随）：
 *  - 不死族白天免烧（Level.isRainingAt 基于 precipitationAt==RAIN 判断）；
 *  - 作物获水在全群系生效；
 *  - 客户端雨粒子渲染读同一方法，自动跟随。
 *
 * 注意：本 mixin 注入抽象类 Level，ServerLevel/ClientLevel 均继承，故服务端逻辑与
 * 客户端渲染同步生效。仅在主世界血月日生效，下界/末地不受影响。
 */
@Mixin(Level.class)
public abstract class ICPMPrecipitationMixin {

    @Inject(method = "precipitationAt", at = @At("RETURN"), cancellable = true)
    private void icpm$bloodMoonAllBiomesRain(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        Level self = (Level) (Object) this;
        if (self.dimension() == Level.OVERWORLD
                && ICPMMoonPhase.isBloodMoonDay(self)
                && cir.getReturnValue() == Biome.Precipitation.NONE) {
            cir.setReturnValue(Biome.Precipitation.RAIN);
        }
    }
}
