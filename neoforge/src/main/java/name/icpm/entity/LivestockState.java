package name.icpm.entity;

import name.icpm.item.ICPMItems;
import name.icpm.mixin.ICPMChickenAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * MITE 牲畜生理状态容器（R196 EntityLivestock 移植）。
 *
 * 之前把状态作为 mixin 字段挂在 {@code @Mixin({Cow,Pig,Sheep,Chicken})} 上，再试图用
 * 多目标 mixin 把逻辑织入继承方法（tick / setInLove / setAge / finalizeSpawn / NBT），
 * 但那些方法声明于 LivingEntity/Mob/Animal/Entity（不是 mixin 目标），导致 Mixin
 * 抛 "target class not supported" 而启动崩溃。
 *
 * 正确做法：跨类状态走独立静态表（见 MEMORY 铁律），逻辑放到以声明类祖先为目标的
 * mixin（{@code @Mixin(Animal.class)}，tick 等都在 Animal 向上继承链内），运行时按实体查表。
 */
public final class LivestockState {

    private static final Map<Entity, LivestockState> STATES = new WeakHashMap<>();

    public static boolean isLivestock(Entity e) {
        return e instanceof Cow || e instanceof Pig || e instanceof Sheep || e instanceof Chicken;
    }

    public static LivestockState get(Entity e) {
        return STATES.computeIfAbsent(e, k -> new LivestockState());
    }

    // ===================== 实例状态 =====================

    public float food = 1.0f;
    public float water = 1.0f;
    public float freedom = 1.0f;
    public int productionCounter = 0;
    public int manurePeriod = 24000;
    public int manureCountdown = 0;
    public int lastTrampledX = 0;
    public int lastTrampledY = 0;
    public int lastTrampledZ = 0;
    public boolean initialized = false;
    public boolean cowHpDone = false;
    public int milk = 0;
    public long spookedUntil = 0L;
    public boolean hasBeenSpookedByOtherAnimal = false;
    public int lastHurtTime = 0;

    // ===================== 健康 / 判定 =====================

    public boolean isWell() {
        return Math.min(freedom, Math.min(food, water)) >= 0.25f;
    }

    public boolean isHungry() {
        return food < 0.5f;
    }

    public boolean isVeryHungry() {
        return food < 0.25f;
    }

    public boolean isDesperateForFood() {
        return food < 0.05f;
    }

    public boolean isThirsty() {
        return water < 0.5f;
    }

    public boolean isVeryThirsty() {
        return water < 0.25f;
    }

    public boolean isDesperateForWater() {
        return water < 0.05f;
    }

    public void addFood(float f) {
        food = clamp(food + f);
    }

    public void addWater(float f) {
        water = clamp(water + f);
    }

    public void addFreedom(float f) {
        freedom = clamp(freedom + f);
    }

    private static float clamp(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    public boolean isSpooked(Entity self) {
        return spookedUntil > self.level().getGameTime();
    }

    public void spook(long until) {
        if (until > spookedUntil) {
            spookedUntil = until;
        }
        hasBeenSpookedByOtherAnimal = true;
    }

    // ===================== 共享静态辅助（供接口实现与 tick 逻辑复用） =====================

    public static boolean isFoodBlock(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS);
    }

    public static boolean isWaterSource(Level level, BlockPos pos) {
        if (level.getFluidState(pos).is(Fluids.WATER)) {
            return true;
        }
        BlockState bs = level.getBlockState(pos);
        return bs.is(Blocks.SNOW) || bs.is(Blocks.SNOW_BLOCK) || bs.is(Blocks.POWDER_SNOW) || bs.is(Blocks.CAULDRON);
    }

    public static boolean isCrowded(Animal a, int x, int y, int z) {
        Level level = a.level();
        if (!level.canSeeSky(new BlockPos(x, y, z))) {
            return true;
        }
        AABB aabb = new AABB(x - 2, (double) y - 0.5, z - 2, x + 2, (double) y + 0.5, z + 2);
        return level.getEntitiesOfClass(LivingEntity.class, aabb).size() > 2;
    }

    // ===================== 每 tick 主逻辑（原 icpm$tick 体） =====================

    public static void tickLogic(LivestockState s, Animal a) {
        if (!s.initialized) {
            s.initialized = true;
            s.food = 0.8f + a.getRandom().nextFloat() * 0.2f;
            s.water = 0.8f + a.getRandom().nextFloat() * 0.2f;
            s.freedom = 0.8f + a.getRandom().nextFloat() * 0.2f;
            s.manureCountdown = (int) (Math.random() * (double) s.manurePeriod);
        }

        // 牛最大血量：MITE（R196）设 20（10 颗心），原版仅 10（5 颗心）；猪/羊/鸡与 R196 一致，不改。
        // 独立于 initialized：存档牛加载时 initialized 会被读档置 true，仍须强制把最大血量拉到 20 并补满血。
        if (a instanceof Cow && !s.cowHpDone) {
            a.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
            a.setHealth(a.getMaxHealth());
            s.milk = 100;
            s.cowHpDone = true;
        }

        if (a.tickCount % 100 == 0) {
            if (a.getRandom().nextInt(10) > 0 && updateWellness(s, a) && !a.isBaby()) {
                ++s.productionCounter;
            }
            produceGoods(s, a);
        }

        // 粪肥掉落（仅成年且未濒临饿死）
        if (!a.isBaby()) {
            if (!s.isDesperateForFood() && --s.manureCountdown <= 0) {
                a.spawnAtLocation((ServerLevel) a.level(), new ItemStack(ICPMItems.MANURE, 1));
                s.manureCountdown = s.manurePeriod / 2 + a.getRandom().nextInt(s.manurePeriod);
            }

            // 踩踏：踩在耕地上时降低其湿度
            if (a.onGround()) {
                BlockPos p = a.blockPosition();
                int x = p.getX();
                int y = p.getY() - 1;
                int z = p.getZ();
                if (x != s.lastTrampledX || y != s.lastTrampledY || z != s.lastTrampledZ) {
                    s.lastTrampledX = x;
                    s.lastTrampledY = y;
                    s.lastTrampledZ = z;
                    BlockState bs = a.level().getBlockState(new BlockPos(x, y, z));
                    if (bs.is(Blocks.FARMLAND)) {
                        int moisture = bs.getValue(FarmBlock.MOISTURE);
                        if (moisture > 0) {
                            a.level().setBlock(new BlockPos(x, y, z), bs.setValue(FarmBlock.MOISTURE, moisture - 1), 2);
                        }
                    }
                }
            }
        }

        // ===== 受伤即惊吓（spook 火种）=====
        // 读 LivingEntity.hurtTime（受伤当帧被置正、逐 tick 递减），检测其上升沿即判定"刚受伤" → spook。
        int curHurt = a.hurtTime;
        if (curHurt > s.lastHurtTime) {
            s.spook(a.level().getGameTime() + 400L + (long) a.getRandom().nextInt(400));
        }
        s.lastHurtTime = curHurt;

        // ===== R196 受惊传染（spook contagion）=====
        if (a.tickCount % 20 == 0) {
            if (s.hasBeenSpookedByOtherAnimal && a.tickCount % 4000 == 0) {
                s.hasBeenSpookedByOtherAnimal = false;
            }
            if (a.isPanicking() || s.isSpooked(a)) {
                AABB box = a.getBoundingBox().inflate(8.0, 4.0, 8.0);
                for (Entity e : a.level().getEntities(a, box,
                        ent -> ent instanceof Cow || ent instanceof Pig
                            || ent instanceof Sheep || ent instanceof Chicken)) {
                    if (e == a || e.isRemoved()) {
                        continue;
                    }
                    ICPMLivestock other = (ICPMLivestock) (Object) e;
                    if (other.isSpooked()) {
                        continue;
                    }
                    if (a.hasLineOfSight(e)) {
                        other.spook(a.level().getGameTime() + 400L + (long) a.getRandom().nextInt(400));
                    }
                }
            }
        }

        // 禁用原版计时下蛋（改由健康产出驱动）；仅 Chicken
        if (a instanceof Chicken) {
            ((ICPMChickenAccessor) (Object) a).icpm$setEggTime(100000);
        }
    }

    private static boolean updateWellness(LivestockState s, Animal a) {
        BlockPos p = a.blockPosition();
        int x = p.getX();
        int y = p.getY();
        int z = p.getZ();
        float benefit = 0.1f;
        float penalty = -0.005f;

        if (isNearFood(s, a, x, y, z)) {
            s.addFood(benefit);
        } else {
            s.addFood(penalty);
        }

        if (isNearWaterSource(s, a, x, y, z)) {
            s.addWater(benefit);
        } else if (a.level().isRainingAt(p)) {
            s.addWater(benefit / 10.0f);
        } else {
            s.addWater(penalty);
        }

        if (!isCrowded(a, x, y, z)) {
            s.addFreedom(benefit);
        } else {
            s.addFreedom(penalty);
        }

        return s.isWell();
    }

    private static boolean isNearFood(LivestockState s, Animal a, int x, int y, int z) {
        Level level = a.level();
        int h = (int) Math.floor((double) a.getBbHeight());
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= h; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (isFoodBlock(level.getBlockState(new BlockPos(x + dx, y + dy, z + dz)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isNearWaterSource(LivestockState s, Animal a, int x, int y, int z) {
        Level level = a.level();
        int h = (int) Math.floor((double) a.getBbHeight());
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= h; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (isWaterSource(level, new BlockPos(x + dx, y + dy, z + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void produceGoods(LivestockState s, Animal a) {
        if (a instanceof Cow) {
            if (!a.isBaby()) {
                s.milk = s.milk + s.productionCounter;
            }
            s.productionCounter = 0;
            return;
        }
        if (a instanceof Chicken) {
            int featherThreshold = 100;
            if (s.productionCounter >= featherThreshold && a.getRandom().nextInt(featherThreshold * 5) == 0) {
                a.spawnAtLocation((ServerLevel) a.level(), new ItemStack(net.minecraft.world.item.Items.FEATHER, 1));
                s.productionCounter -= featherThreshold;
                return;
            }
            int eggThreshold = 200;
            if (s.productionCounter >= eggThreshold && a.getRandom().nextInt(20) == 0) {
                a.playSound(SoundEvents.CHICKEN_EGG, 1.0f,
                        (a.getRandom().nextFloat() - a.getRandom().nextFloat()) * 0.2f + 1.0f);
                a.spawnAtLocation((ServerLevel) a.level(), new ItemStack(net.minecraft.world.item.Items.EGG, 1));
                s.productionCounter -= eggThreshold;
            }
            return;
        }
        s.productionCounter = 0;
    }

    // ===================== NBT 持久化辅助 =====================

    public static void writeNbt(LivestockState s, net.minecraft.world.level.storage.ValueOutput output) {
        output.putInt("icpm_food", (int) Math.round(s.food * 1000.0f));
        output.putInt("icpm_water", (int) Math.round(s.water * 1000.0f));
        output.putInt("icpm_freedom", (int) Math.round(s.freedom * 1000.0f));
        output.putInt("icpm_production_counter", s.productionCounter);
        output.putInt("icpm_milk", s.milk);
        output.putInt("icpm_spooked_by_other", s.hasBeenSpookedByOtherAnimal ? 1 : 0);
    }

    public static void readNbt(LivestockState s, net.minecraft.world.level.storage.ValueInput tag) {
        s.initialized = true;
        s.food = tag.getInt("icpm_food").orElse(1000) / 1000.0f;
        s.water = tag.getInt("icpm_water").orElse(1000) / 1000.0f;
        s.freedom = tag.getInt("icpm_freedom").orElse(1000) / 1000.0f;
        s.productionCounter = tag.getInt("icpm_production_counter").orElse(0);
        s.milk = tag.getInt("icpm_milk").orElse(0);
        s.hasBeenSpookedByOtherAnimal = tag.getInt("icpm_spooked_by_other").orElse(0) != 0;
    }
}
