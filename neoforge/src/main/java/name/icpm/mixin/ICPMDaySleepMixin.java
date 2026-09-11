package name.icpm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ICPM 睡眠机制调整（R196 百科）：
 * 1. 白天小睡：白天睡觉不跳时间（仅躺床回血，可主动起床）；
 * 2. 度过夜晚后醒来时间 = 第二个游戏日早上 5:00（原版为 0:00/6:00）。
 */
@Mixin(ServerLevel.class)
public abstract class ICPMDaySleepMixin {

    /**
     * 重定向第一个 areEnoughSleeping（跳时间判定）：
     * - 白天(<13000)返回 false → 不跳时间（仅躺床回血）；
     * - 到达/经过 5:00(>=23000)后也返回 false → 避免到达次日 5:00 后每 tick 反复跳、把时间卡在 5:00；
     *   此时正常时间流逝恢复，玩家可继续卧床。
     * - 仅当“夜晚且尚未到 5:00”时(13000<=now<23000)才允许原版跳时间逻辑。
     */
    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z",
                    ordinal = 0))
    private boolean icpm$noDaytimeSkip(SleepStatus sleepStatus, int i) {
        ServerLevel level = (ServerLevel) (Object) this;
        long now = level.getDayTime() % 24000L;
        if (now < 13000L || now >= 23000L) {
            return false;
        }
        return sleepStatus.areEnoughSleeping(i);
    }

    /**
     * 跳过夜晚后醒来时间 = 第二天 5:00（原版跳至 0:00 = 6:00，提前 1000 tick = 1 小时）。
     *
     * 重要：1.21.11 ServerLevel.tick 里有【两处】setDayTime 调用——
     *   ordinal=0: 所有玩家睡觉后的跳跃分支 setDayTime(l - l%24000)（跳至 6:00）
     *   ordinal=1: 每 tick 正常时间流逝 setDayTime(dayTime + 1)
     * 若不加 ordinal，两处都会被 -1000，导致正常流逝变成 dayTime - 999（时间每 tick 倒退），
     * 世界时间变负、月相/昼夜全乱、无法正常加载。这里只重定向跳跃分支。
     */
    @Redirect(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V", ordinal = 0))
    private void icpm$wakeAtDawn(ServerLevel level, long time) {
        level.setDayTime(time - 1000L);
    }

    /**
     * 度过夜晚到达次日 5:00 后，取消原版“跳时间后强制全体起床(wakeUpAllPlayers)”的逻辑，
     * 让玩家不被强制弹起，可自行选择继续卧床；时间从 5:00 起恢复正常流逝。
     */
    @Redirect(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;wakeUpAllPlayers()V"))
    private void icpm$keepInBed(ServerLevel level) {
        // 故意留空：跳过 wakeUpAllPlayers，玩家保持卧床
    }
}
