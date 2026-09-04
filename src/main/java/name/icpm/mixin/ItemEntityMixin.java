package name.icpm.mixin;

import name.icpm.common.BurningCookingHandler;
import name.icpm.item.ICPMBuckets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 火焰烧肉 —— 物品实体在火焰中的持续烹饪（R196 忠实移植，重构版）。
 *
 * <p>R196 语义：生食掉落物只要持续受火焰灼烧，cooking_progress 就连续累计
 * （+火伤×3/tick），累计满 100 即烤熟 —— 没有"点火 N 次 / 离开热源窗口"。
 * 现代 MC 物品会被火直接销毁且无 R196 的独立 health，故在受热/刚熟期间临时防火，
 * 使"持续灼烧 → 熟"完整成立。
 *
 * <p>本轮重构移除的混元3 自创状态机：{@code icpmBurnConsumed}（"每次连续受热窗口只贡献
 * 25 进度、必须离火再放回才能继续、点 4 次熟"）、熟食永久防火。原因：R196 不存在窗口模型；
 * 熟食永久防火会让熟食在火中永不损毁，同样违背原文（原文熟食继续受火会再次累计直至消失）。
 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    /** 烹饪进度（R196 cooking_progress）。 */
    @Unique
    private float icpmCookingProgress = 0.0F;
    /** 刚烤熟（仍在热源上），短暂防火以免熟食被现代火焰秒毁（R196 熟食尚有短暂存活期）。 */
    @Unique
    private boolean icpmJustCooked = false;

    /** 受热烹饪中的生食（与刚烤熟的熟食）临时防火，避免被火/熔岩直接销毁。 */
    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void icpm$fireImmune(CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        if (stack.isEmpty()) {
            return;
        }
        // 切勿在 isOnHeat 内调用 self.isOnFire()（会回环 fireImmune → 无限递归）。
        if (BurningCookingHandler.isOnHeat(self.level(), self.blockPosition())
                && (BurningCookingHandler.isRawFood(stack.getItem()) || icpmJustCooked)) {
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
            icpmJustCooked = false; // 离开热源：刚熟标记失效，之后交给火焰正常规则
            return;
        }

        // 生食持续受热：连续累计（R196 每 tick +damage×3）
        if (BurningCookingHandler.isRawFood(stack.getItem())) {
            icpmCookingProgress += BurningCookingHandler.COOK_RATE;
            if (icpmCookingProgress >= BurningCookingHandler.COOK_THRESHOLD) {
                icpmCookingProgress = 0.0F;
                icpmJustCooked = true;
                BurningCookingHandler.completeCook(self);
            }
        }
    }

    /**
     * R196 忠实移植（EntityItem.spentTickInWater）：
     * 岩浆桶掉落物入水 → 嘶嘶蒸汽并冷却成石桶。
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void icpm$coolLavaBucketInWater(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide() || !self.isInWater()) {
            return;
        }
        ItemStack stack = self.getItem();
        if (stack.isEmpty()) {
            return;
        }
        Item stone = ICPMBuckets.stoneBucketFromLavaBucket(stack.getItem());
        if (stone == null) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 p = self.position();
        serverLevel.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.4, p.z, 5, 0.2, 0.3, 0.2, 0.02);
        serverLevel.playSound(null, self.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.7F);
        self.setItem(new ItemStack(stone));
    }
}
