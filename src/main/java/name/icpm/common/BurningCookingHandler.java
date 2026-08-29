package name.icpm.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * 可燃物烧肉（R196 忠实移植）。
 *
 * <p>R196 中 EntityItem 受火焰伤害时累计 cooking_progress（+amount*3），满 100 时
 * 由 getItemProducedWhenDestroyed 转成熟食，并给四周火方块排程熄灭、掉落经验球。</p>
 *
 * <p>现代 MC 的物品实体不会被 FireBlock 直接伤害，因此这里在 tick 中检测实体是否处于/位于
 * 燃烧方块上，等效于"持续受到火焰伤害"，并以同样的速率累计进度。</p>
 */
public class BurningCookingHandler {
    /**
     * 每次"连续受热窗口"贡献的进度。R196 中点一次火≈烧一段，约 25；累计满 {@link #COOK_THRESHOLD} 即熟。
     * 想要"点火 N 次才熟"就调这里 / {@link #COOK_THRESHOLD}。
     */
    public static final float COOK_UNIT = 25.0F;
    /** 烤熟所需累计进度。 */
    public static final float COOK_THRESHOLD = 100.0F;

    /**
     * 生食 -> 熟食 映射。忠实覆盖 R196 的 setCookingResult 集合
     * （猪肉/牛肉/鸡肉/兔/羊/鳕鱼/鲑鱼/马铃薯/海带）。
     */
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

    /**
     * 烤熟时掉落的经验，与 R196 对齐：猪肉 3、牛肉 4、鸡肉 3、兔 0、羊肉 2、鳕鱼 3、鲑鱼 4、土豆 0、海带 0。
     */
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

    public static Item getCooked(Item raw) {
        return RAW_TO_COOKED.get(raw);
    }

    /** 是否为可烤的生食（对应 R196 的烹饪集合）。 */
    public static boolean isRawFood(Item item) {
        return RAW_TO_COOKED.containsKey(item);
    }

    public static int getXp(Item raw) {
        return RAW_TO_XP.getOrDefault(raw, 0);
    }

    /**
     * 实体是否处于/位于燃烧方块附近（等效于 R196 的"受到火焰伤害"）。
     * 覆盖六向（自身 + 上下 + 四方），使生食站在"头顶有火"的可燃方块上也能被烤熟。
     */
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

    /** 烤熟：转为熟食、播放滋滋声、熄灭四周火、掉落经验。调用前应已确认 getCooked 非空。 */
    public static void completeCook(ItemEntity entity) {
        Level level = entity.level();
        ItemStack stack = entity.getItem();
        Item cooked = getCooked(stack.getItem());

        ItemStack result = new ItemStack(cooked, stack.getCount());
        entity.setItem(result);

        // 烤肉的滋滋声（R196 仅 ItemMeat 播放 imported.random.sizzle）
        if (cooked != Items.BAKED_POTATO && cooked != Items.DRIED_KELP) {
            level.playSound(
                    null,
                    entity.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.5F,
                    level.getRandom().nextFloat() * 0.4F + 0.8F
            );
        }

        // 仅当附近没有剩余生食时才熄灭火焰，确保多份食物都能烤熟（不再夹生）。
        // 若仍有生食在火上，保留火焰让其余食物继续烹饪。
        if (!hasRawNearby(level, entity.blockPosition())) {
            extinguishAround(level, entity.blockPosition());
        }

        // 掉落经验球（R * 按 cooked_item 的 experienceReward 拆分）
        int xp = getXp(stack.getItem());
        if (xp > 0) {
            ExperienceOrb orb = new ExperienceOrb(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), xp);
            ((ServerLevel) level).addFreshEntity(orb);
        }
    }

    /**
     * 在实体所在层及下方层做 3x3 熄灭（对应 R196 的 dx/dz ∈ [-1,1]，并兼顾脚下方块）。
     */
    private static void extinguishAround(Level level, BlockPos center) {
        for (int dy = -1; dy <= 0; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(p);
                    if (state.is(Blocks.FIRE)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static boolean isBurningAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.is(ICPMTags.BURNING_BLOCKS);
    }

    /** 是否有生食位于该位置附近的火上，用于决定是否保留火焰让其余食物继续烤。 */
    private static boolean hasRawNearby(Level level, BlockPos center) {
        AABB box = new AABB(
                center.getX() - 1.0, center.getY() - 1.0, center.getZ() - 1.0,
                center.getX() + 2.0, center.getY() + 2.0, center.getZ() + 2.0);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
            ItemStack s = e.getItem();
            if (!s.isEmpty() && isRawFood(s.getItem())) {
                return true;
            }
        }
        return false;
    }
}
