package name.icpm.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 骷髅家族：凋零骷髅生成时装备铁剑（R196 EntitySkeleton 类型1 swordIron）。
 *
 * <p>挂 {@code finalizeSpawn} 的声明类 {@link Mob}（铁律 2026-08-19：AbstractSkeleton 不重写
 * finalizeSpawn），用 instanceof 过滤只对 WitherSkeleton 生效；保留弓手骷髅的弓。
 */
@Mixin(Mob.class)
public abstract class SkeletonFamilySpawnMixin {

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void icpm$witherIronSword(ServerLevelAccessor level, DifficultyInstance difficulty,
                                      EntitySpawnReason reason, SpawnGroupData spawnData,
                                      CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!((Object) this instanceof WitherSkeleton self)) {
            return;
        }
        ItemStack held = self.getMainHandItem();
        if (held.isEmpty() || held.getItem() != Items.BOW) {
            self.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }
}
