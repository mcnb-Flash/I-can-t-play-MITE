package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 弓附魔（R196 EntityArrow 移植）：
 * 中毒（箭命中附加中毒）、回收（命中概率返还箭矢）、真飞行（箭矢精准）、迅捷（箭矢加速）。
 */
@Mixin(AbstractArrow.class)
public abstract class ICPMArrowEnchantMixin {

    /** 命中实体：中毒 + 回收 */
    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void icpm$arrowEffects(EntityHitResult entityHitResult, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (!(arrow.level() instanceof ServerLevel)) {
            return;
        }
        Entity target = entityHitResult.getEntity();
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) {
            return;
        }

        // 中毒：概率 level/10，中毒 160+level*240 tick（R196）
        int poison = ICPMEnchantEffects.level(arrow.level(), weapon, "poison");
        if (poison > 0 && target instanceof LivingEntity living && arrow.level().random.nextFloat() < poison / 10.0f) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 160 + poison * 240, 0));
        }

        // 回收：概率 level*0.15，返还一支箭到发射者背包（R196 arrow_recovery）
        int recovery = ICPMEnchantEffects.level(arrow.level(), weapon, "arrow_recovery");
        if (recovery > 0 && arrow.level().random.nextFloat() < recovery * 0.15f) {
            Entity owner = arrow.getOwner();
            if (owner instanceof Player player && !player.getInventory().add(new ItemStack(Items.ARROW))) {
                player.drop(new ItemStack(Items.ARROW), false);
            }
        }
    }

    /**
     * 真飞行（精准） + 迅捷（加速）：改写传给 Projectile.shoot 的 velocity(索引3) / inaccuracy(索引4)。
     * 注意：@ModifyArgs 只能改写方法体内部的「方法调用」参数，不能改 shoot 自身参数。
     * AbstractArrow.shoot 内部调用 super.shoot(d,e,f,g,h)（即 Projectile.shoot），
     * 这里拦截该调用改写 g(速度)/h(散布)。
     * shoot(double d, double e, double f, float g, float h) → args [3]=g(速度) [4]=h(散布)
     */
    @ModifyArgs(method = "shoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"))
    private void icpm$modifyShootArgs(Args args) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) {
            return;
        }
        float velocity = (float) args.get(3);
        float inaccuracy = (float) args.get(4);

        // 真飞行：散布每级 -15%（最小保留 5%）
        int flight = ICPMEnchantEffects.level(arrow.level(), weapon, "true_flight");
        if (flight > 0) {
            inaccuracy = inaccuracy * Math.max(0.05f, 1.0f - flight * 0.15f);
        }
        // 迅捷：箭速 +6%/级
        int quick = ICPMEnchantEffects.level(arrow.level(), weapon, "quickness");
        if (quick > 0) {
            velocity = velocity * (1.0f + quick * 0.06f);
        }

        args.set(3, velocity);
        args.set(4, inaccuracy);
    }
}
