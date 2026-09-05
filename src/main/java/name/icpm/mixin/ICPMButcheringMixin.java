package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ICPM 屠宰附魔（R196 EnchantmentButchering 移植）：小刀/匕首杀死动物时额外掉落肉（最多 3 级）。
 */
@Mixin(LivingEntity.class)
public abstract class ICPMButcheringMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void icpm$butchering(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity victim = (LivingEntity) (Object) this;
        if (victim.level().isClientSide()) {
            return;
        }
        Entity attacker = damageSource.getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }
        int lvl = ICPMEnchantEffects.level(victim.level(), player.getMainHandItem(), "butchering");
        if (lvl <= 0) {
            return;
        }
        // 蜘蛛眼：R196 掉率 = 1 - 2/(3(n+1))（无附魔天然 1/3 已由 vanilla 掉落近似覆盖）
        if (victim instanceof net.minecraft.world.entity.monster.spider.Spider) {
            float eyeChance = 1.0f - 2.0f / (3.0f * (lvl + 1.0f));
            if (victim.level().random.nextFloat() < eyeChance
                    && victim.level() instanceof ServerLevel serverLevelEye) {
                victim.spawnAtLocation(serverLevelEye, new ItemStack(Items.SPIDER_EYE));
            }
            return;
        }
        // R196：鸡不受屠宰影响（其余被动动物受影响）；牛/猪已由 LivestockMeatR196Mixin 全量公式出肉（含屠宰加成）
        if (!(victim instanceof Animal)
                || victim instanceof Chicken
                || victim instanceof Rabbit
                || victim instanceof Cow
                || victim instanceof MushroomCow
                || victim instanceof Pig) {
            return;
        }
        Item meat = meatFor(victim);
        if (meat == null) {
            return;
        }
        // R196 附加肉量近似：+rand(0..n)（基础 1+(0..2) 部分由原版掉落提供）
        int add = victim.level().random.nextInt(lvl + 1);
        if (add <= 0) {
            return;
        }
        if (victim.level() instanceof ServerLevel serverLevel) {
            Item cooked = victim.isOnFire() ? cookedFor(meat) : meat;
            victim.spawnAtLocation(serverLevel, new ItemStack(cooked, add));
        }
    }

    private static Item cookedFor(Item meat) {
        if (meat == Items.BEEF) return Items.COOKED_BEEF;
        if (meat == Items.PORKCHOP) return Items.COOKED_PORKCHOP;
        if (meat == Items.MUTTON) return Items.COOKED_MUTTON;
        if (meat == Items.CHICKEN) return Items.COOKED_CHICKEN;
        if (meat == Items.RABBIT) return Items.COOKED_RABBIT;
        return meat;
    }

    private static Item meatFor(LivingEntity entity) {
        if (entity instanceof MushroomCow || entity instanceof Cow) {
            return Items.BEEF;
        }
        if (entity instanceof Pig) {
            return Items.PORKCHOP;
        }
        if (entity instanceof Sheep) {
            return Items.MUTTON;
        }
        if (entity instanceof Chicken) {
            return Items.CHICKEN;
        }
        if (entity instanceof Rabbit) {
            return Items.RABBIT;
        }
        return null;
    }
}
