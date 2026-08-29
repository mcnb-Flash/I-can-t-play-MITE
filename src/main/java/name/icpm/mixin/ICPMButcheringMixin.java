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
        if (victim.level().isClientSide() || !(victim instanceof Animal)) {
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
        Item meat = meatFor(victim);
        if (meat == null) {
            return;
        }
        int count = 1;
        if (victim.level().random.nextFloat() < lvl * 0.25f) {
            count++;
        }
        if (victim.level() instanceof ServerLevel serverLevel) {
            victim.spawnAtLocation(serverLevel, new ItemStack(meat, count));
        }
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
