package name.icpm.mixin;

import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 Chicken.eggTime，用于禁用原版计时下蛋（改由健康产出驱动）。
 */
@Mixin(Chicken.class)
public interface ICPMChickenAccessor {

    @Accessor("eggTime")
    void icpm$setEggTime(int value);

    @Accessor("eggTime")
    int icpm$getEggTime();
}
