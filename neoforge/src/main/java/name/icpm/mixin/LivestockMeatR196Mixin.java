package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import name.icpm.entity.LivestockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
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
 * A4 · 牛/猪/羊掉落 R196 全量对齐 —— R196 EntityCow/EntityPig/EntitySheep.dropFewItems 忠实移植：
 * <pre>
 *   Cow：皮革 rand(3)+1 = 1..3 恒掉（不受健康/屠宰影响）
 *        肉：仅 isWell(健康) 时 = rand(3)+1 + rand(1+屠宰级)；总数为 1 时 50% +1；燃烧→熟牛肉
 *   Pig：肉：仅 isWell 时 = rand(3)+1 + rand(1+屠宰级)（**无 50% +1**、无皮革）；燃烧→熟猪排
 *   Sheep：羊毛：未剪且未燃时 50% 掉 1（本羊毛色，R196 织布）
 *         肉：仅 isWell 时 = rand(2)+rand(1+屠宰级)，&lt;1 强制 1；燃烧→熟羊肉
 *         皮革：50% 掉 1
 * </pre>
 * 取代 vanilla 常掉 1..3 肉（R196：**不健康动物不掉肉**，这才是"全量"缺口）。
 * 1.21 MushroomCow 继承 Cow，天然覆盖。牛/猪/羊已从屠宰附魔 mixin 剔除避免叠加。
 *
 * 注：dropFromLootTable 声明于 LivingEntity（父类），故 @Mixin(LivingEntity) + instanceof 守卫
 * （ICPM 铁律：继承方法注入必须 @Mixin 声明类）。
 * 鸡/兔按 R196 豁免（不受屠宰影响、不掉健康肉）。
 */
@Mixin(LivingEntity.class)
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
            dropLeatherR196(cow, level); // 皮革 1..3 恒掉
            if (!isWell(self)) {
                ci.cancel(); // 不健康牛：只有皮革
                return;
            }
            int count = cowMeatCount(level, damageSource);
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
            int count = pigMeatCount(level, damageSource);
            ItemStack meat = new ItemStack(pig.isOnFire() ? Items.COOKED_PORKCHOP : Items.PORKCHOP, count);
            pig.spawnAtLocation(level, meat);
            ci.cancel();
            return;
        }
        if (self instanceof Sheep sheep) {
            if (sheep.isBaby()) {
                ci.cancel();
                return;
            }
            // 羊毛：未剪且未燃时 50%（R196 rand(2)==0 → 掉；cloth 带羊毛色）
            if (!sheep.isSheared() && !sheep.isOnFire() && level.random.nextInt(2) == 0) {
                Item wool = woolForColor(sheep);
                if (wool != null) {
                    sheep.spawnAtLocation(level, new ItemStack(wool, 1));
                }
            }
            if (isWell(self)) {
                int count = sheepMeatCount(level, damageSource);
                ItemStack meat = new ItemStack(sheep.isOnFire() ? Items.COOKED_MUTTON : Items.MUTTON, count);
                sheep.spawnAtLocation(level, meat);
            }
            // 皮革：50%（R196 rand(2)==0 → 掉 1）
            if (level.random.nextInt(2) == 0) {
                sheep.spawnAtLocation(level, new ItemStack(Items.LEATHER, 1));
            }
            ci.cancel();
        }
    }

    /** R196 Cow：皮革 = rand(3)+1 → 1..3 恒掉（独立于健康/屠宰）。 */
    private static void dropLeatherR196(Cow cow, ServerLevel level) {
        int n = level.random.nextInt(3) + 1;
        cow.spawnAtLocation(level, new ItemStack(Items.LEATHER, n));
    }

    /** R196 Cow 肉量 = rand(3)+1 + rand(1+屠宰)；总数为 1 时 50% +1 */
    private static int cowMeatCount(ServerLevel level, DamageSource damageSource) {
        int butcher = butcheringLevel(level, damageSource);
        int count = level.random.nextInt(3) + 1 + level.random.nextInt(butcher + 1);
        if (count == 1 && level.random.nextInt(2) == 0) {
            count++;
        }
        return count;
    }

    /** R196 Pig 肉量 = rand(3)+1 + rand(1+屠宰)；**无 50% +1 规则**（与 Cow 不同） */
    private static int pigMeatCount(ServerLevel level, DamageSource damageSource) {
        int butcher = butcheringLevel(level, damageSource);
        return level.random.nextInt(3) + 1 + level.random.nextInt(butcher + 1);
    }

    /** R196 Sheep 肉量 = rand(2)+rand(1+屠宰)，&lt;1 强制 1 */
    private static int sheepMeatCount(ServerLevel level, DamageSource damageSource) {
        int butcher = butcheringLevel(level, damageSource);
        int count = level.random.nextInt(2) + level.random.nextInt(butcher + 1);
        if (count < 1) {
            count = 1;
        }
        return count;
    }

    /** 羊毛物品：按 Sheep 羊毛色（DyeColor → <color>_wool；兜底 white_wool）。 */
    private static Item woolForColor(Sheep sheep) {
        try {
            String name = sheep.getColor().getName();
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", name + "_wool"));
            return item == Items.AIR ? Items.WHITE_WOOL : item;
        } catch (Exception e) {
            return Items.WHITE_WOOL;
        }
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
