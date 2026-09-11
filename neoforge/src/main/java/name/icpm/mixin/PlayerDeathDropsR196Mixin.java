package name.icpm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家死亡掉落存活 15 分钟 —— R196 EntityPlayer 死亡掉落语义移植：
 * R196 将玩家产生的掉落物 age 设为负值（-18000 → 配合 6000 tick 消失阈值，
 * 实际存活远大于原版 5 分钟）。sky 记录「玩家死亡物品消失时间 15 min」。
 *
 * dropAllDeathLoot 声明于 LivingEntity（Player 未覆写）——按项目铁律必须
 * @Mixin(声明类 LivingEntity) + instanceof Player 守卫，否则启动 target not found。
 * 在 TAIL 把死亡瞬间洒出、刚生成的掉落物 age 下调 12000 →
 * 存活 = 12000 + 6000 ≈ 18000 tick = 15 分钟。
 * 只作用于玩家死亡掉落（keepInventory 时 vanilla 不产生掉落，自动跳过），
 * 不碰平时丢出的物品（仍 5 分钟）；非玩家生物掉落不受影响。
 */
@Mixin(LivingEntity.class)
public abstract class PlayerDeathDropsR196Mixin {

    /** 刚生成判定：age 0~60 tick（死亡掉落生成瞬间） */
    private static final int FRESH_AGE = 60;
    /** age 下调量：despawn 阈值≈6000 → 存活 18000 tick = 15 分钟 */
    private static final int AGE_EXTEND = 12000;

    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void icpm$extendPlayerDeathDropLifetime(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        Object selfObj = this;
        if (!(selfObj instanceof Player self) || self.level().isClientSide()) {
            return;
        }
        AABB box = self.getBoundingBox().inflate(3.0, 2.0, 3.0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            ItemEntityAgeAccessor accessor = (ItemEntityAgeAccessor) item;
            int age = accessor.icpm$getAge();
            if (age >= 0 && age < FRESH_AGE) {
                accessor.icpm$setAge(age - AGE_EXTEND);
            }
        }
    }
}
