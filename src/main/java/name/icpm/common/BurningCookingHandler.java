package name.icpm.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 火焰烧肉 —— R196 EntityItem 火焰灼烧烹饪的忠实移植（重构版）。
 *
 * <p>R196 源码判决（src_deobf/EntityItem.java attackEntityFrom fire 分支 + BlockFire
 * tryExtinguishByItems）：
 * <pre>
 *   1) 生食（含可转熟与可还原的熟食）掉落物持续受火焰灼烧时：
 *          cooking_progress += fireDamage × 3
 *      累计满 100 → 经 getItemProducedWhenDestroyed 转成熟食（数量不变），
 *      ItemMeat 播放滋滋声，并按熟食 experienceReward 分裂生成经验球。
 *   2) 同一时刻给周边 3×3 的火方块排程 10 tick 后的 tryExtinguishByItems：
 *      统计该火格上「仍可烹饪的生食」总堆叠数 count——
 *       count < 2 → 不灭（少量肉火不灭）；count ≥ 2 → 以 0.01×2^count 概率灭；
 *       count > 15 → 强制灭。（大量肉会把火压灭 / 肉多火自然熄。）
 * </pre>
 *
 * <p>与 R196 的偏离说明（现代 MC 差异）：
 * <ul>
 *   <li>现代物品实体接触火会被销毁且没有 health 体系 → 由 ItemEntityMixin 在受热期间提供
 *       临时防火（等效 R196"受伤但继续累计"），烤熟瞬间起仍保留短暂防火直至离开热源
 *       （否则熟食刚落地就被现代火焰秒毁，R196 中熟食尚有约 1 秒存活）。</li>
 *   <li>火焰伤害换算：每 tick 等效 fireDamage ≈ 1 → progress += 3/tick，约 34 tick（~1.7s）烤熟。</li>
 * </ul>
 */
public class BurningCookingHandler {

    /** R196: progress += damage×3（damage≈1/tick）。 */
    public static final float COOK_RATE = 3.0F;
    /** 烤熟所需累计进度（R196 threshold 100）。 */
    public static final float COOK_THRESHOLD = 100.0F;
    /** 灭火排程延迟（R196 (time/10+1)*10 → 10 tick 后）。 */
    private static final long EXTINGUISH_DELAY_TICKS = 10L;

    /** 生食 → 熟食（对应 R196 配置了 cookedItem 的烹饪集合）。 */
    private static final Map<Item, Item> RAW_TO_COOKED = Map.ofEntries(
            Map.entry(Items.BEEF, Items.COOKED_BEEF),
            Map.entry(Items.PORKCHOP, Items.COOKED_PORKCHOP),
            Map.entry(Items.CHICKEN, Items.COOKED_CHICKEN),
            Map.entry(Items.RABBIT, Items.COOKED_RABBIT),
            Map.entry(Items.MUTTON, Items.COOKED_MUTTON),
            Map.entry(Items.COD, Items.COOKED_COD),
            Map.entry(Items.SALMON, Items.COOKED_SALMON),
            Map.entry(Items.POTATO, Items.BAKED_POTATO),
            Map.entry(Items.KELP, Items.DRIED_KELP)
    );

    /** 烤熟掉落经验（R196 cooked experienceReward）。 */
    private static final Map<Item, Integer> RAW_TO_XP = Map.ofEntries(
            Map.entry(Items.BEEF, 4),
            Map.entry(Items.PORKCHOP, 3),
            Map.entry(Items.CHICKEN, 3),
            Map.entry(Items.RABBIT, 0),
            Map.entry(Items.MUTTON, 2),
            Map.entry(Items.COD, 3),
            Map.entry(Items.SALMON, 4),
            Map.entry(Items.POTATO, 0),
            Map.entry(Items.KELP, 0)
    );

    /** 待执行灭火的（维度#坐标 → 到期时间）。R196 用世界排程 10t 后执行。 */
    private static final Map<String, Long> PENDING_EXTINGUISH = new HashMap<>();

    private BurningCookingHandler() {
    }

    public static Item getCooked(Item raw) {
        return RAW_TO_COOKED.get(raw);
    }

    /** 是否为可被火烤熟的生食。 */
    public static boolean isRawFood(Item item) {
        return RAW_TO_COOKED.containsKey(item);
    }

    private static int getXp(Item raw) {
        return RAW_TO_XP.getOrDefault(raw, 0);
    }

    /** 是否处于/紧邻燃烧（火焰）方块 —— R196"持续受到火焰伤害"的等效检测。 */
    public static boolean isOnHeat(Level level, BlockPos pos) {
        if (isBurningAt(level, pos)) {
            return true;
        }
        for (Direction d : Direction.values()) {
            if (isBurningAt(level, pos.relative(d))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBurningAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ICPMTags.BURNING_BLOCKS);
    }

    /** 烤熟：转熟食、滋滋声、掉经验、并给四周火方块排程灭火（R196）。 */
    public static void completeCook(ItemEntity entity) {
        Level level = entity.level();
        ItemStack stack = entity.getItem();
        Item cooked = getCooked(stack.getItem());
        if (cooked == null) {
            return;
        }

        ItemStack result = new ItemStack(cooked, stack.getCount());
        entity.setItem(result);

        // R196：仅 ItemMeat 类播放 sizzle
        if (cooked != Items.BAKED_POTATO && cooked != Items.DRIED_KELP) {
            level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.4F + 0.8F);
        }

        // R196：掉落经验球（按熟食 experienceReward 拆分）
        int xp = getXp(stack.getItem());
        if (xp > 0) {
            ExperienceOrb orb = new ExperienceOrb(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), xp);
            ((ServerLevel) level).addFreshEntity(orb);
        }

        // R196：给附近（本层 ±1×±1 及下一层）火方块排程 10t 后 tryExtinguishByItems
        for (int dy = -1; dy <= 0; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = entity.blockPosition().offset(dx, dy, dz);
                    if (level.getBlockState(p).is(Blocks.FIRE)) {
                        scheduleExtinguish((ServerLevel) level, p);
                    }
                }
            }
        }
    }

    private static void scheduleExtinguish(ServerLevel level, BlockPos firePos) {
        String key = level.dimension() + "_" + firePos.asLong();
        PENDING_EXTINGUISH.put(key, level.getGameTime() + EXTINGUISH_DELAY_TICKS);
    }

    /** 每服务端 tick：到期执行 R196 tryExtinguishByItems（按该火上可烹饪生食堆叠数概率灭火）。 */
    public static void onServerTick(MinecraftServer server) {
        if (PENDING_EXTINGUISH.isEmpty()) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            String dim = level.dimension().toString();
            long now = level.getGameTime();
            Iterator<Map.Entry<String, Long>> it = PENDING_EXTINGUISH.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> e = it.next();
                if (!e.getKey().startsWith(dim + "_")) {
                    continue;
                }
                if (e.getValue() > now) {
                    continue;
                }
                it.remove();
                BlockPos firePos = BlockPos.of(Long.parseLong(e.getKey().substring(dim.length() + 1)));
                if (!level.getBlockState(firePos).is(Blocks.FIRE)) {
                    continue; // 火已灭/被换
                }
                tryExtinguishByItems(level, firePos);
            }
        }
    }

    /** R196 BlockFire.tryExtinguishByItems：统计该火格上仍可烹饪的生食堆叠数并概率灭火。 */
    private static void tryExtinguishByItems(ServerLevel level, BlockPos firePos) {
        int x = firePos.getX(), y = firePos.getY(), z = firePos.getZ();
        AABB box = new AABB(x - 0.125, y, z - 0.125, x + 1.125, y + 1, z + 1.125);
        int count = 0;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            ItemStack s = itemEntity.getItem();
            if (!s.isEmpty() && isRawFood(s.getItem())) {
                count += s.getCount();
            }
        }
        if (count < 2) {
            return; // R196：不足 2 份不灭火（少量肉火继续烧）
        }
        if (count > 15 || level.getRandom().nextFloat() < 0.01f * Math.pow(2.0, count)) {
            level.setBlock(firePos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
