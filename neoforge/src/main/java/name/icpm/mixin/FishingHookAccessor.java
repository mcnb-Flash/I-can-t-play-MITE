package name.icpm.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 暴露 FishingHook.timeUntilLured（上钩等待时间），供蓝月钓鱼加速使用 */
@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor("timeUntilLured")
    int icpm$getTimeUntilLured();

    @Accessor("timeUntilLured")
    void icpm$setTimeUntilLured(int value);
}
