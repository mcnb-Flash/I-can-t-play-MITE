package name.icpm.common;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ICPM 食物数值定义（移植自 1.18.2-ICPM / IFW 移植与 1.6.4 ICPM 合成表）
 *
 * 1.21.11 食物机制说明：
 * - FOOD 组件 (FoodProperties) 只存 nutrition/saturation/canAlwaysEat；
 * - 进食行为由 CONSUMABLE 组件驱动（Item.use 检查 CONSUMABLE → startConsuming）；
 * - 进食效果（中毒等）通过 Consumable.onConsumeEffects 的 ApplyStatusEffectsConsumeEffect；
 * - 碗返还通过 Item.Properties.usingConvertsTo()（即 USE_REMAINDER 组件）。
 *
 * 数值说明：nutrition 采用 ICPM 原版数值；saturationModifier 换算为原版兼容值
 * （本模组饱食度上限 6-20 且 FoodDataMixin 将饱和度钳制到饱食度内，ICPM 1.6.4
 * 的巨大 satiation 数值无意义，故按"饱和度≈营养值"的宽松比例设置）。
 */
public final class ICPMFoodProperties {

    private ICPMFoodProperties() {
    }

    // ==================== 构建辅助 ====================

    /** 基础食物（默认进食动画/时长/声音） */
    public static FoodProperties food(int nutrition, float satMod) {
        return food(nutrition, satMod, false);
    }

    public static FoodProperties food(int nutrition, float satMod, boolean alwaysEdible) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(satMod);
        if (alwaysEdible) {
            builder.alwaysEdible();
        }
        return builder.build();
    }

    /** 带进食效果的食物组件（用于原版食物注入与 ICPM 新食物） */
    public static Consumable consumableWithEffects(List<MobEffectInstance> effects, float probability) {
        return Consumable.builder()
                .consumeSeconds(1.6F)
                .animation(ItemUseAnimation.EAT)
                .sound(SoundEvents.GENERIC_EAT)
                .hasConsumeParticles(true)
                .onConsume(new ApplyStatusEffectsConsumeEffect(effects, probability))
                .build();
    }

    /** 纯进食（无额外效果）的默认 Consumable：让原版非食物变可食用 */
    public static Consumable defaultConsumable() {
        return Consumable.builder()
                .consumeSeconds(1.6F)
                .animation(ItemUseAnimation.EAT)
                .sound(SoundEvents.GENERIC_EAT)
                .hasConsumeParticles(true)
                .build();
    }

    /**
     * ICPM r196 中可食用、但原版 1.21.11 无 CONSUMABLE 的物品：补默认进食行为使其可吃。
     * - 小麦种子：food(1,0.5f) = +1 饥饿 + 1.0 饱和（"半格饱和度"），普通食物机制，必然可见。
     * - 鸡蛋：默认右键吃、潜行右键扔（见 ICPMEdibleUseMixin），需此处补 CONSUMABLE 才能走完进食动画并回数值。
     * - 其余为 ICPM 原版种子/糖/地狱疣，原版不可吃。
     * 注：brown_mushroom / red_mushroom / beetroot_seeds 在 ICPM r196 中并非食物（源码无注册），不在此列。
     */
    public static final Set<String> DEFAULT_CONSUMABLE_ITEMS = Set.of(
            "wheat_seeds", "pumpkin_seed", "melon_seed", "nether_wart", "sugar", "egg"
    );

    /** 可种植的种子（右键耕地时走种植而非进食，避免破坏 farming）。仅这些种子在 ICPMEdibleUseMixin 中排除耕地判定。 */
    public static final Set<String> PLANTABLE_SEED_ITEMS = Set.of(
            "wheat_seeds", "pumpkin_seed", "melon_seed"
    );

    // ==================== 原版食物 → ICPM 数值（mixin 注入用） ====================
    // key = 原版物品路径（无命名空间，如 "apple"）

    /** 原版已有食物：替换其 FOOD 组件（保留原 CONSUMABLE，如蜂蜜瓶清除中毒效果） */
    public static final Map<String, FoodProperties> VANILLA_FOODS = Map.ofEntries(
            Map.entry("apple", food(1, 0.3f)),
            Map.entry("golden_apple", food(1, 0.3f)),
            Map.entry("enchanted_golden_apple", food(1, 0.3f, true)),
            Map.entry("bread", food(2, 0.6f)),
            Map.entry("cookie", food(1, 0.3f)),
            Map.entry("melon_slice", food(1, 0.3f)),
            Map.entry("pumpkin_pie", food(6, 0.8f)),
            Map.entry("carrot", food(2, 0.3f)),
            Map.entry("golden_carrot", food(2, 0.3f)),
            Map.entry("potato", food(1, 0.3f)),
            Map.entry("baked_potato", food(2, 0.6f)),
            Map.entry("poisonous_potato", food(0, 0.2f)),
            Map.entry("beetroot", food(1, 0.3f)),
            Map.entry("beef", food(5, 0.5f)),
            Map.entry("cooked_beef", food(10, 0.8f)),
            Map.entry("porkchop", food(4, 0.5f)),
            Map.entry("cooked_porkchop", food(8, 0.6f)),
            Map.entry("chicken", food(3, 0.5f)),
            Map.entry("cooked_chicken", food(6, 0.6f)),
            Map.entry("mutton", food(3, 0.5f)),
            Map.entry("cooked_mutton", food(6, 0.6f)),
            Map.entry("rabbit", food(2, 0.5f)),
            Map.entry("cooked_rabbit", food(4, 0.6f)),
            Map.entry("cod", food(2, 0.3f)),
            Map.entry("cooked_cod", food(5, 0.5f)),
            Map.entry("salmon", food(3, 0.3f)),
            Map.entry("cooked_salmon", food(6, 0.6f)),
            Map.entry("tropical_fish", food(1, 0.3f)),
            Map.entry("pufferfish", food(1, 0.2f)),
            Map.entry("rotten_flesh", food(2, 0.2f)),
            Map.entry("spider_eye", food(1, 0.2f)),
            Map.entry("dried_kelp", food(0, 0.5f)),
            Map.entry("sweet_berries", food(1, 0.3f)),
            Map.entry("glow_berries", food(0, 0.3f)),
            Map.entry("honey_bottle", food(3, 0.5f)),
            Map.entry("chorus_fruit", food(2, 0.3f)),
            // ===== ICPM r196 可食用的原版非食物（数值取自 r196 源码 satiation/nutrition）=====
            // 映射：vanilla nutrition = ICPM satiation；saturationModifier = ICPM nutrition / (2 * satiation)
            // 小麦种子：ICPM r196(satiation=1,nutrition=0)。用普通食物 food(1,0.5f) = +1 饥饿 + 1.0 饱和（"半格饱和度"），取代原脆弱的 food(0,0)+addSaturationOnly 方案
            Map.entry("wheat_seeds", food(1, 0.5f)),
            // 鸡蛋 ICPM(satiation=1, nutrition=3) → 原版不可吃，由 ICPMEdibleUseMixin 触发进食（默认吃/潜行扔）
            Map.entry("egg", food(1, 1.5f)),
            // 南瓜种子 ICPM(satiation=1, nutrition=2)
            Map.entry("pumpkin_seed", food(1, 1.0f)),
            // 西瓜种子 ICPM(satiation=1, nutrition=1)
            Map.entry("melon_seed", food(1, 0.5f)),
            // 地狱疣(Nether Stalk Seeds) ICPM(satiation=1, nutrition=1)
            Map.entry("nether_wart", food(1, 0.5f)),
            // 糖 ICPM(satiation=1, nutrition=0)
            Map.entry("sugar", food(1, 0.0f))
    );

    /** 原版食物需要 ICPM 进食效果（中毒等）：同时替换 FOOD 与 CONSUMABLE */
    public static final Map<String, Consumable> VANILLA_CONSUMABLES = Map.ofEntries(
            Map.entry("chicken", consumableWithEffects(
                    List.of(new MobEffectInstance(MobEffects.POISON, 150, 0)), 0.3f)),
            Map.entry("poisonous_potato", consumableWithEffects(
                    List.of(new MobEffectInstance(MobEffects.POISON, 100, 0)), 0.6f)),
            Map.entry("pufferfish", consumableWithEffects(
                    List.of(new MobEffectInstance(MobEffects.POISON, 1200, 1),
                            new MobEffectInstance(MobEffects.HUNGER, 300, 2),
                            new MobEffectInstance(MobEffects.NAUSEA, 1200, 0)), 1.0f)),
            Map.entry("rotten_flesh", consumableWithEffects(
                    List.of(new MobEffectInstance(MobEffects.HUNGER, 600, 0),
                            new MobEffectInstance(MobEffects.POISON, 300, 0)), 0.8f)),
            Map.entry("spider_eye", consumableWithEffects(
                    List.of(new MobEffectInstance(MobEffects.POISON, 300, 0)), 1.0f))
    );

    // ==================== ICPM 新增食物数值（注册新物品用） ====================

    // 面粉不可直接食用（仅合成材料）
    public static final FoodProperties DOUGH = food(2, 0.4f);
    public static final FoodProperties CHEESE = food(3, 0.6f);
    public static final FoodProperties CHOCOLATE = food(3, 0.6f);
    public static final FoodProperties ICE_CREAM = food(5, 0.8f);
    public static final FoodProperties SORBET = food(4, 0.8f);
    public static final FoodProperties MASHED_POTATO = food(12, 0.8f);
    public static final FoodProperties BEEF_STEW = food(16, 0.8f);
    public static final FoodProperties CHICKEN_SOUP = food(10, 0.8f);
    public static final FoodProperties VEGETABLE_SOUP = food(7, 0.6f);
    public static final FoodProperties VEGETABLE_SOUP_CREAM = food(7, 0.8f);
    public static final FoodProperties MUSHROOM_SOUP_CREAM = food(3, 0.8f);
    public static final FoodProperties PUMPKIN_SOUP = food(1, 0.5f);
    public static final FoodProperties SALAD = food(1, 0.5f);
    public static final FoodProperties PORRIDGE = food(4, 0.6f);
    public static final FoodProperties CEREAL = food(4, 0.6f);
    public static final FoodProperties ORANGE = food(2, 0.3f);
    public static final FoodProperties BANANA = food(2, 0.3f);
    public static final FoodProperties BLUEBERRY = food(1, 0.3f);
    public static final FoodProperties ONION = food(1, 0.3f);
    public static final FoodProperties WORM = food(1, 0.2f);
    public static final FoodProperties COOKED_WORM = food(1, 0.4f);
    public static final FoodProperties MILK_BOWL = food(1, 0.3f);
    public static final FoodProperties WATER_BOWL = food(0, 0.0f);
}
