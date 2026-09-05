package name.icpm.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * ItemEntity.age 访问器 —— R196 用「负数 age」延长玩家掉落物存活时间
 * （R196 EntityPlayer.dropPlayerItem* 设 age=-18000，despawn 阈值≈age≥6000）。
 */
@Mixin(ItemEntity.class)
public interface ItemEntityAgeAccessor {

    @Accessor("age")
    int icpm$getAge();

    @Accessor("age")
    void icpm$setAge(int age);
}
