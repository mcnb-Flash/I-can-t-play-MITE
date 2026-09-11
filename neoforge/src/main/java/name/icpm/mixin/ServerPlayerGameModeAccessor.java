package name.icpm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 暴露 ServerPlayerGameMode 的 protected player/level 字段 */
@Mixin(ServerPlayerGameMode.class)
public interface ServerPlayerGameModeAccessor {
    @Accessor("player")
    ServerPlayer icpm$getPlayer();

    @Accessor("level")
    ServerLevel icpm$getLevel();
}
