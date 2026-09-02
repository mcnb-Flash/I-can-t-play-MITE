package name.icpm.mixin;

import name.icpm.item.ICPMItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 僵尸系：稀有掉落（R196 dropFewItems 的 rare drop 分支）。
 *
 * <p>挂 {@code dropCustomDeathLoot} 的声明类 {@link LivingEntity}（铁律 2026-08-19：
 * dropCustomDeathLoot 声明于 LivingEntity，Zombie 自身不重写），用 instanceof 过滤。
 * 村民僵尸（1.21.11 为独立实体 ZombieVillager，仍 extends Zombie）掉种子类；标准掉铜/银/金/铁粒。
 *
 * <p>⚠️ 掉落池必须是方法内局部数组，不能是 static final 字段：本 mixin 目标 LivingEntity，
 * 会在 EntityType/Items 静态初始化链上被加载；static 数组初始化若访问 {@code ICPMItems.SILVER_NUGGET}
 * 会触发 ICPMItems.<clinit>，此时注册表未就绪 → Item.Properties() 构造 NPE 崩溃
 * （2026-08-19 实测：class_9323.iterator() null）。方法内引用则等到运行时（注册表已就绪）。
 */
@Mixin(LivingEntity.class)
public abstract class ZombieMiteDropMixin {

    @Inject(method = "dropCustomDeathLoot", at = @At("RETURN"))
    private void icpm$rareDrop(ServerLevel level, DamageSource source, boolean recentlyHit, CallbackInfo ci) {
        if (!((Object) this instanceof Zombie self)) {
            return;
        }
        if (!recentlyHit) {
            return;
        }
        // 基础概率：村民僵尸 1/50，标准 1/200（R196 dropRareDrop 的 rand.nextInt(200)==0 语义）
        boolean villager = self instanceof ZombieVillager;
        int base = villager ? 50 : 200;
        // ⚠️ 修复：原实现 nextInt(base)>=5 返回 → 概率变为 5/base（1/40、1/10），
        // 与注释/R196 差 5 倍。改为 nextInt(base)==0 才是 1/base。
        if (self.getRandom().nextInt(base) != 0) {
            return;
        }
        Item[] pool = villager ? VILLAGER_DROPS() : STANDARD_DROPS();
        Item drop = pool[self.getRandom().nextInt(pool.length)];
        self.spawnAtLocation(level, drop);
    }

    private static Item[] STANDARD_DROPS() {
        return new Item[]{
                Items.COPPER_NUGGET, Items.IRON_NUGGET, Items.GOLD_NUGGET,
                ICPMItems.SILVER_NUGGET
        };
    }

    private static Item[] VILLAGER_DROPS() {
        return new Item[]{
                Items.WHEAT_SEEDS, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS,
                Items.CARROT, Items.POTATO, ICPMItems.ONION
        };
    }
}
