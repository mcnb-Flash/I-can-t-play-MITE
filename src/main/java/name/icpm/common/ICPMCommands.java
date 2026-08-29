package name.icpm.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import name.icpm.common.ICPMExperience;

/**
 * ICPM 指令（R196）：
 * /day —— 输出当前游戏日（世界天数，(dayTime+4800)/24000 + 1；+4800 使天数边界落在“游戏日 00:01”，即午夜 +1 分钟，每过午夜 1 分钟天数 +1）；
 * /xp  —— 输出当前玩家经验值（覆盖原版 /xp 的 add/set 语义，R196 百科：通过 /xp 查看经验值）。
 */
public final class ICPMCommands {

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // /day：输出当前游戏日
            dispatcher.register(Commands.literal("day")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        // 世界天数 = dayTime/24000 + 1（与季节/月相同源的累计世界刻）。
                        // 天数边界按需求设在“游戏日 00:01”（午夜 + 1 分钟）：Minecraft 午夜对应
                        // timeOfDay=18000，00:01 = 18000 + 1200 = 19200；公式 (dayTime + 4800)/24000
                        // 的翻转点恰好落在 timeOfDay=19200，故每过午夜 1 分钟，天数 +1。
                        // （此前 +1000 是把边界对齐到 mod 的 5:00 唤醒 / 早晨，现改为午夜起点。）
                        long day = (player.level().getDayTime() + 4800L) / 24000L + 1L;
                        ctx.getSource().sendSuccess(() ->
                                Component.literal("当前游戏日: 第 " + day + " 天"), false);
                        return 1;
                    }));

            // /xp：输出当前玩家经验值（R196 带符号经验体系）
            dispatcher.register(Commands.literal("xp")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        int xp = ICPMExperience.getExperience(player);
                        ctx.getSource().sendSuccess(() ->
                                Component.literal("You now have " + xp + " experience points"), false);
                        return 1;
                    })
                    .then(Commands.literal("set")
                            .then(Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(ctx -> {
                                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                                        int target = IntegerArgumentType.getInteger(ctx, "amount");
                                        int cur = ICPMExperience.getExperience(player);
                                        int delta = target - cur;
                                        // 负数走 addExperience 确保下限钳制与同步；正数直接给
                                        ICPMExperience.addExperience(player, delta, true, true);
                                        int xp = ICPMExperience.getExperience(player);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("You now have " + xp + " experience points"), false);
                                        return 1;
                                    })))
                    .then(Commands.literal("add")
                            .then(Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(ctx -> {
                                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                        ICPMExperience.addExperience(player, amount, true, true);
                                        int xp = ICPMExperience.getExperience(player);
                                        ctx.getSource().sendSuccess(() ->
                                                Component.literal("You now have " + xp + " experience points"), false);
                                        return 1;
                                    }))));
        });
    }

    private ICPMCommands() {
    }
}
