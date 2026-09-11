package name.icpm.mixin;

import name.icpm.common.ICPMMoonPhase;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 血月天气与月光亮度（MITE 移植，infx 开发计划忠实参考）
 *
 * A1 血月雷电 ×5：ServerLevel.tickThunder 内雷电判定 nextInt(100000) 在血月日改 nextInt(20000)
 *    （对齐 MITE WorldServer.java 雷电滚动频率；bound==100000 精确只对雷电那次生效，
 *     其余 nextInt 调用不受影响，安全降级）。
 * A3 血月雷暴修复：血月日从 tick 6000 起强制雷暴（含雨），持续覆盖整夜
 *    （对齐 MITE World.java:8675）。在 advanceWeatherCycle 之后续期/启动雷暴，
 *    确保不死族白天免烧、全群系降雨（配合 A2 群系绕过）整夜生效。
 * A4 月光亮度表：getMoonBrightness 覆盖原版相位表，按 MITE 定制
 *    （血月 0.6 / 丰收月 1.0 / 蓝月 1.1 / 其余 月相因子×0.5+0.75）。
 */
@Mixin(ServerLevel.class)
public abstract class ICPMWeatherMixin {

    @Redirect(method = "tickThunder",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int icpm$bloodMoonThunder(RandomSource random, int bound) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (bound == 100000 && ICPMMoonPhase.isBloodMoonDay(self)) {
            return random.nextInt(20000);
        }
        return random.nextInt(bound);
    }

    @Inject(method = "getMoonBrightness", at = @At("RETURN"), cancellable = true)
    private void icpm$miteMoonBrightness(CallbackInfoReturnable<Float> cir) {
        Level self = (Level) (Object) this;
        cir.setReturnValue(ICPMMoonPhase.miteMoonBrightness(self));
    }

    /**
     * A3 血月雷暴修复：在自动天气推进之后强制/续期雷暴。
     * 触发窗口：血月日午后（tick%24000 >= 6000）起，直至次日清晨（isBloodMoonNight 覆盖
     * 当日 16000~24000 与次日 0~6000）。仅在主世界生效。
     * 续期条件：当前未雷暴，或雷暴计时即将耗尽（<20 tick），避免每 tick 重置导致
     * 血月结束后仍久打雷。
     */
    @Inject(method = "advanceWeatherCycle", at = @At("RETURN"))
    private void icpm$bloodMoonForceThunder(CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (self.dimension() != Level.OVERWORLD) {
            return;
        }
        long t = self.getDayTime();
        long phase = t % 24000L;
        boolean force = (ICPMMoonPhase.isBloodMoonDay(self) && phase >= 6000L)
                || ICPMMoonPhase.isBloodMoonNight(self);
        if (!force) {
            return;
        }
        ServerLevelData data = (ServerLevelData) self.getLevelData();
        if (!self.isThundering() || data.getThunderTime() < 20) {
            // (clearDuration, weatherDuration, raining, thundering)
            self.setWeatherParameters(0, 13000, true, true);
        }
    }
}
