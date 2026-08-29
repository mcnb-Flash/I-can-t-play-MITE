package name.icpm.mixin;

import name.icpm.common.BurningCookingHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;

/**
 * 物品实体 tick 时若携带生食且位于燃烧方块/火焰/熔岩上，按 R196 逻辑累计烹饪进度，满额转成熟食。
 *
 * <p>R196 的 EntityItem 在火/熔岩中不会因"燃烧"而立即消失（它有独立的 health 系统），
 * 而现代 MC 的物品实体一旦接触火/熔岩会被直接销毁，导致无法"烧多次"。这里让"正在被烹饪的生/熟食物"
 * 临时 {@code fireImmune()}，等价于 R196 的"受伤但不死、累计烹饪进度"语义。</p>
 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Unique
    private float icpmCookingProgress = 0.0F;
    /**
     * 一旦开始受热烹饪即标记为"烹饪物"，烤熟后仍保持免疫，避免熟食被火/熔岩销毁而"夹生消失"。
     */
    @Unique
    private boolean icpmCooking = false;
    /**
     * 当前这次连续受热窗口已贡献过进度，需离开热源再放回才会再次累计（还原 R196 的"反复点燃"）。
     */
    @Unique
    private boolean icpmBurnConsumed = false;

    /**
     * 让受热中的生/熟食物临时防火，避免被火/熔岩直接销毁（R196 等价行为）。
     */
    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void icpm$fireImmune(CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        if (stack.isEmpty()) {
            return;
        }
        // 生食 或 已进入烹饪态的物品，只要位于燃烧方块/火/熔岩上即免疫销毁。
        // 切勿在此调用 self.isOnFire()/isInLava()：isOnFire 会回环到 fireImmune 包装器导致无限递归崩溃。
        if ((BurningCookingHandler.isRawFood(stack.getItem()) || icpmCooking)
                && BurningCookingHandler.isOnHeat(self.level(), self.blockPosition())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void icpm$tickCooking(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) {
            return;
        }

        ItemStack stack = self.getItem();
        if (stack.isEmpty()) {
            return;
        }

        BlockPos pos = self.blockPosition();
        boolean onHeat = BurningCookingHandler.isOnHeat(level, pos);

        if (!onHeat) {
            // 离开热源：允许下次受热重新开始一段"燃烧"，对应 R196 的再次点燃。
            icpmBurnConsumed = false;
            return;
        }

        if (BurningCookingHandler.getCooked(stack.getItem()) == null) {
            // 已经是熟食且仍在热源上：保持免疫即可，不再累计。
            return;
        }

        icpmCooking = true;

        // 每次连续受热窗口只贡献一段进度；窗口结束后必须离开热源再放回才能继续（还原"点 4 次"）。
        if (!icpmBurnConsumed) {
            icpmBurnConsumed = true;
            icpmCookingProgress += BurningCookingHandler.COOK_UNIT;
            if (icpmCookingProgress >= BurningCookingHandler.COOK_THRESHOLD) {
                icpmCookingProgress = 0.0F;
                BurningCookingHandler.completeCook(self);
            }
        }
    }
}
