package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import name.icpm.entity.LivestockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A4 · 牛/猪掉落 R196 全量对齐 —— R196 EntityCow/EntityPig.dropFewItems 忠实移植：
 * <pre>
 *   Cow：皮革 0..2 恒掉（不受健康/屠宰影响）
 *        肉：仅 isWell(健康) 时 = rand(3)+1 + rand(1+屠宰级)；总数为 1 时 50% +1；燃烧→熟牛肉
 *   Pig：肉：仅 isWell 时同公式（无皮革）；燃烧→熟猪排
 * </pre>
 * 取代 vanilla 常掉 1..3 肉（R196：**不健康动物不掉肉**，这才是"全量"缺口）。
 * 1.21 MushroomCow 继承 Cow，天然覆盖。牛/猪已从屠宰附魔 mixin 剔除避免叠加。
 * 羊/鸡/兔：鸡兔按 R196 豁免，羊保持近似（vanilla 肉 + 屠宰 extra；健康门控为已知偏差）。
 */
@Mixin({Cow.class, Pig.class})
public abstract class LivestockMeatR196Mixin {

    @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void icpm$r196LivestockMeat(ServerLevel level, DamageSource damageSource, boolean recentlyHitByPlayer, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Cow cow) {
            if (cow.isBaby()) {
                ci.cancel(); // 幼崽不掉落（R196 同）
                return;
            }
            dropLeatherIfAny(cow, level);
            if (!isWell(self)) {
                ci.cancel(); // 不健康牛：只有皮革
                return;
            }
            int count = meatCount(level, damageSource);
            ItemStack meat = new ItemStack(cow.isOnFire() ? Items.COOKED_BEEF : Items.BEEF, count);
            cow.spawnAtLocation(level, meat);
            ci.cancel();
            return;
        }
        if (self instanceof Pig pig) {
            if (pig.isBaby()) {
                ci.cancel();
                return;
            }
            if (!isWell(self)) {
                ci.cancel(); // 不健康猪：无任何掉落（R196）
                return;
            }
            int count = meatCount(level, damageSource);
            ItemStack meat = new ItemStack(pig.isOnFire() ? Items.COOKED_PORKCHOP : Items.PORKCHOP, count);
            pig.spawnAtLocation(level, meat);
            ci.cancel();
        }
    }

    /** R196：皮革 0..2（rand(3)），独立于健康/屠宰 */
    private static void dropLeatherIfAny(Cow cow, ServerLevel level) {
        int n = level.random.nextInt(3);
        if (n > 0) {
            cow.spawnAtLocation(level, new ItemStack(Items.LEATHER, n));
        }
    }

    /** R196 肉量 = rand(3)+1 + rand(1+屠宰)；总数为 1 时 50% +1 */
    private static int meatCount(ServerLevel level, DamageSource damageSource) {
        int butcher = butcheringLevel(level, damageSource);
        int count = level.random.nextInt(3) + 1 + level.random.nextInt(butcher + 1);
        if (count == 1 && level.random.nextInt(2) == 0) {
            count++;
        }
        return count;
    }

    private static int butcheringLevel(ServerLevel level, DamageSource damageSource) {
        Entity attacker = damageSource == null ? null : damageSource.getEntity();
        if (attacker instanceof Player player) {
            return ICPMEnchantEffects.level(level, player.getMainHandItem(), "butchering");
        }
        return 0;
    }

    /** R196 isWell = ICPM 牲畜健康状态（食物/饮水/自由三维，初始 1.0 视为健康） */
    private static boolean isWell(Entity self) {
        return LivestockState.get(self).isWell();
    }
}
