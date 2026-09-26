package name.icpm.mixin;

import name.icpm.common.ICPMDevMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 服务端强制锁困难：任何对 {@code MinecraftServer.setDifficulty} 的调用都归一为 {@link Difficulty#HARD}，
 * 保证即便通过暂停菜单或其它路径尝试修改，世界难度始终是困难（MITE R196 行为：难度锁死且不可降）。
 *
 * <p>界面侧的灰化见 {@code ICPMWorldDifficultyGateMixin}（创建世界界面难度按钮）。本 mixin 是功能兜底，
 * 让“强制锁困难”不依赖 UI 是否被正确渲染。
 *
 * <p>dev 模式开启时不做任何限制。
 */
@Mixin(MinecraftServer.class)
public abstract class ICPMHardDifficultyMixin {

    @ModifyVariable(method = "setDifficulty", at = @At("HEAD"), argsOnly = true)
    private Difficulty icpm$forceHard(Difficulty difficulty) {
        if (ICPMDevMode.isEnabled()) {
            return difficulty;
        }
        return Difficulty.HARD;
    }
}
