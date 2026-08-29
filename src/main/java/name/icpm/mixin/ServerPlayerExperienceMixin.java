package name.icpm.mixin;

import name.icpm.common.ICPMExperience;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * ICPM 经验机制（移植自 MITE R196）：覆盖 ServerPlayer.setExperiencePoints。
 *
 * 背景：`/xp set <数值>`（不带单位时的 points 模式）在 1.21.11 调用的是
 * ServerPlayer.setExperiencePoints（注意它不是 Player 的方法，Player 只有
 * giveExperiencePoints / giveExperienceLevels）。原版该方法会把经验钳为非负、且
 * 不会保留负经验，导致"压到负等级"完全无效——服务端写不进负值、客户端也收不到，
 * HUD 画不出负等级、负等级惩罚也不生效（表现为"完全没有加载"）。
 *
 * 这里改走 ICPM 带符号体系：以"差量"方式调用 addExperience（内部 clamp 到
 * MIN_EXPERIENCE(-800) 并 syncToVanilla 向客户端发包），使 `/xp set -800` 这类
 * 命令能可靠地把玩家压到 -40 级并在客户端显示 "-40"。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerExperienceMixin {

    @Overwrite
    public void setExperiencePoints(int amount) {
        Player player = (Player) (Object) this;
        // 以差量方式设置：delta = 目标 - 当前；addExperience 内部 clamp 下限 + 同步客户端，
        // 且 suppressHealing/suppressSound=true，避免"set"语义下误触升级回血/音效。
        ICPMExperience.addExperience(player, amount - player.totalExperience, true, true);
    }

    /**
     * 覆盖原版 setExperienceLevels：对应命令 `/xp set <数值>L`（设置等级模式）。
     * 原版该方法会把等级钳为非负，导致"设置到负等级"完全无效。
     * 这里改走 ICPM 带符号体系：以"目标等级 - 当前等级"作为差量交给 addExperienceLevels
     * （内部按 ICPM 曲线换算经验、clamp 到 [MIN_LEVEL, MAX_LEVEL] 并 syncToVanilla 发包），
     * 使 `/xp set -40L` 这类命令能可靠地把玩家压到 -40 级并在客户端显示 "-40"。
     */
    @Overwrite
    public void setExperienceLevels(int levels) {
        Player player = (Player) (Object) this;
        int curLevel = ICPMExperience.getExperienceLevel(player.totalExperience);
        // 差量 = 目标等级 - 当前等级；addExperienceLevels 内部已 suppressHealing/suppressSound。
        ICPMExperience.addExperienceLevels(player, levels - curLevel);
    }
}
