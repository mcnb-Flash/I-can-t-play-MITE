package name.icpm.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 鱿鱼触碰麻痹 —— R196 EntitySquid.onCollideWithPlayer_ 忠实移植。
 *
 * <p>R196 原文（src_deobf/.../EntitySquid.java）：
 * <pre>
 *   public void onCollideWithPlayer_(EntityPlayer player) {
 *       if (!this.worldObj.isRemote && this.getDistanceToEntity(player) < 1.0f
 *               && !(player.ridingEntity instanceof EntityBoat)) {
 *           player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 200, 2));
 *       }
 *   }
 * </pre>
 * 即：服务端、玩家距鱿鱼 <1.0 格、不在船上 → 缓慢 III（200 tick）。
 *
 * <p>1.21.11 的 Squid 没有 onCollideWithPlayer_ 钩子，注入 {@link Squid#aiStep} TAIL
 * （GlowSquid 覆写 aiStep 且内部调用 super.aiStep()，故本注入同时覆盖原版鱿鱼与发光鱿鱼）。
 * 用 tickCount 节流近似"碰撞事件"（每 20 tick 检查一次，效果时长 200 tick 足够维持
 * R196 的"接触期间持续减速 + 离开后残留 10 秒"手感）。
 */
@Mixin(Squid.class)
public abstract class SquidParalysisMixin {

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void icpm$squidTouchSlow(CallbackInfo ci) {
        Squid self = (Squid) (Object) this;
        if (!(self.level() instanceof ServerLevel)) {
            return;
        }
        if (self.tickCount % 20 != 0) {
            return; // 节流：每秒检查一次
        }
        Entity near = self.level().getNearestPlayer(self, 1.2);
        if (!(near instanceof Player player)) {
            return;
        }
        if (player.isSpectator() || player.isCreative()) {
            return;
        }
        // R196：坐在船上免疫（墨水触手碰不到船内玩家）
        if (player.getVehicle() instanceof Boat) {
            return;
        }
        if (self.distanceToSqr(player) < 1.0 * 1.0) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 2));
        }
    }
}
