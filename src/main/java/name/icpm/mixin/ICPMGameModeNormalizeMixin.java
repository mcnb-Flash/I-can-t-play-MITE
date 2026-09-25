package name.icpm.mixin;

import name.icpm.common.ICPMDevMode;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 游戏模式归一（R196 语义）—— 堵住**所有**“变更为非生存”的路径。
 *
 * <p>R196 {@code CommandGameMode.java:40}：
 * {@code if (!Minecraft.inDevMode() && var3 != EnumGameType.SURVIVAL)} ⇒ {@code /gamemode} 只允许切生存。
 *
 * <p>1.21.11 中所有游戏模式变更最终都汇聚到
 * {@code ServerPlayerGameMode.setGameModeForPlayer(GameType, GameType)}——包括
 * {@code /gamemode}、第三方 mod，以及**从存档恢复玩家数据**（HMCL 等外部工具改档的入口）。
 *
 * <p>因此在收敛点把目标模式归一为 {@code SURVIVAL}，可一次性覆盖全部路径：
 * 外部把 {@code playerGameType} 改成创造后，玩家一进世界就会被改回生存；
 * 归一后的值会被原版存档逻辑（{@code ServerPlayer.storeGameTypes}）写回，外部改动即被洗掉。
 *
 * <p>dev 模式开启时**不干预玩家模式**（开发者可自由切换创造调试）。
 */
@Mixin(ServerPlayerGameMode.class)
public class ICPMGameModeNormalizeMixin {

    /** {@code setGameModeForPlayer(GameType target, GameType previous)} —— ordinal 0 = 目标模式。 */
    @ModifyVariable(method = "setGameModeForPlayer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private GameType icpm$normalizeToSurvival(GameType gameType) {
        if (ICPMDevMode.isEnabled()) {
            return gameType; // dev 模式：不修改玩家模式
        }
        return gameType == GameType.SURVIVAL ? gameType : GameType.SURVIVAL;
    }
}
