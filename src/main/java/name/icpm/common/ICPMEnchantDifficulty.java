package name.icpm.common;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * R196 附魔难度系统（《附魔设定详细资料.xlsx》 + Enchantment/EnchantmentHelper 源码）。
 *
 * <p>核心公式（Enchantment.getMinEnchantmentLevelsCost）：
 * <pre>
 *   每级消耗难度 D = max(n − 10, 0) + n × (lvl − 1) + 1，n = 附魔前置难度
 *   经验值成本（附魔台三档显示）= 档位 × 100 XP
 * </pre>
 * 规则（xlsx 备注 A–F）：
 * - A 实际消耗难度 ≤ ⌊消耗经验×1.25/100⌋（10000 XP → 125）
 * - B 每多一个词条，总难度 +5（锋利5缴械4耐久5 = 41+5+31+5+41 = 123）
 * - D 精准采集等无级附魔视为 3 级 → 消耗难度 = 该难度 3 级值
 * - F 第 2 个及之后词条 50% 概率重置等级（除非无可选）
 *
 * <p>词条生成（EnchantmentHelper.buildEnchantmentList R196 算法）：
 * 预算难度 ×(0.75~1.25) 浮动 → 循环取最高可达等级词条（冲突排除）→ 扣难度与 +5 → 至多 3 词条。
 */
public final class ICPMEnchantDifficulty {

    private ICPMEnchantDifficulty() {}

    /** 前置难度表（n）。key = 附魔注册 id path（1.21.11 vanilla + icpm）。未收录 → 10 */
    private static final Map<String, Integer> DIFFICULTY = new LinkedHashMap<>();

    private static void reg(String id, int n) {
        DIFFICULTY.put(id, n);
    }

    static {
        // 保护系 / 通用（vanilla）
        reg("protection", 10); reg("fire_protection", 10); reg("feather_falling", 10);
        reg("blast_protection", 10); reg("projectile_protection", 10);
        reg("respiration", 10); reg("aqua_affinity", 30); reg("thorns", 20);
        reg("sharpness", 10); reg("smite", 10); reg("bane_of_arthropods", 10);
        reg("knockback", 10); reg("fire_aspect", 20); reg("looting", 10);
        reg("efficiency", 10); reg("silk_touch", 30); reg("unbreaking", 10);
        reg("fortune", 10); reg("power", 10); reg("punch", 10); reg("flame", 20);
        reg("infinity", 10); reg("luck_of_the_sea", 10); reg("luck_of_the_sea", 10);
        reg("mending", 10); reg("sweeping_edge", 10);
        // ICPM 附魔（xlsx 表：回收/击晕/收获/砍树/吸血/速度/再生/灵活/敏捷/精准/中毒/缴械/劈裂/杀害/屠宰/耐久 等）
        reg("arrow_recovery", 10);   // 回收
        reg("stun", 15);             // 击晕
        reg("harvesting", 10);       // 收获
        reg("tree_felling", 10);     // 砍树
        reg("vampiric", 20);         // 吸血
        reg("speed", 10);            // 速度
        reg("regeneration", 20);     // 再生
        reg("free_action", 10);      // 灵活移动
        reg("quickness", 10);        // 敏捷
        reg("endurance", 10);        // 精准(命中率)类/耐力
        reg("poison", 10);           // 中毒
        reg("disarming", 10);        // 缴械
        reg("piercing", 10);         // 劈裂
        reg("true_flight", 10);      // 杀害(实体杀伤)/飞行
        reg("butchering", 10);       // 屠宰
        reg("fertility", 10);        // 收获扩展
        reg("fishing_fortune", 10);  // 饵钓
    }

    public static int difficulty(Identifier enchant) {
        return DIFFICULTY.getOrDefault(enchant.getPath(), 10);
    }

    /** 是否分级（无等级词条：精准采集/aqua_affinity/mending 等视为 3 级） */
    public static boolean hasLevels(Identifier enchant) {
        String p = enchant.getPath();
        return !(p.equals("silk_touch") || p.equals("aqua_affinity") || p.equals("mending")
                || p.equals("infinity") || p.equals("true_flight") || p.equals("endurance"));
    }

    /**
     * R196 getMinEnchantmentLevelsCost：难度消耗。无级词条按 level=3 计。
     */
    public static int minCost(Identifier enchant, int level) {
        int n = difficulty(enchant);
        int lvl = hasLevels(enchant) ? Math.max(1, level) : 3;
        return Math.max(n - 10, 0) + n * (lvl - 1) + 1;
    }

    /**
     * R196 getExperienceCost：经验成本 = 档位(难度) × 100。
     */
    public static int experienceCost(int enchantmentLevels) {
        return enchantmentLevels * 100;
    }

    /**
     * R196 经验预算上限规则 A：可承受实际难度 = ⌊消耗经验 × 1.25 / 100⌋。
     */
    public static int difficultyBudgetFromXp(int spentXp) {
        return (int) ((long) spentXp * 125L / 10000L);
    }

    /**
     * R196 Enchantment.getLevelFromEnchantmentLevels：某档位难度下该附魔可达等级
     * （超出 0..max 或未达到门槛返回 0）。无级词条：档位在 (2n, 3n] 内视为 1。
     */
    public static int levelFromDifficulty(Identifier enchant, int difficultyLevels, int maxLevel) {
        int n = difficulty(enchant);
        if (hasLevels(enchant)) {
            if (difficultyLevels <= n - 10) {
                return 0;
            }
            int level = (difficultyLevels + n - 1) / n;
            return level < 1 || level > maxLevel ? 0 : level;
        }
        return (difficultyLevels > n * 2 && difficultyLevels <= n * 3) ? 1 : 0;
    }

    /** 词条：附魔 id + 等级 */
    public static final class Instance {
        public final Identifier enchant;
        public final int level;
        public Instance(Identifier enchant, int level) {
            this.enchant = enchant;
            this.level = level;
        }
    }

    /**
     * R196 buildEnchantmentList 忠实移植（无冲突/可附魔过滤的通用池版本）。
     *
     * @param random        随机源
     * @param budget        预算难度（adjusted 前，如 125）
     * @param pool         候选附魔（调用方已做 canEnchantItem 过滤），value=该词条最大等级
     * @param book         是否为书（只产出 1 个词条，调用方自己挑）
     * @return 随机词条列表（最多 3 个）
     */
    public static List<Instance> buildList(RandomSource random, int budget,
                                           Map<Identifier, Integer> pool, boolean book) {
        if (budget < 1 || pool == null || pool.isEmpty()) {
            return List.of();
        }
        // randomness = 1 + (rand − 0.5) × 0.5 → ±25%
        float randomness = 1.0f + (random.nextFloat() - 0.5f) * 0.5f;
        int remaining = Math.max(1, (int) (budget * randomness));
        List<Instance> picked = new ArrayList<>();
        List<Identifier> keys = new ArrayList<>(pool.keySet());

        while (remaining > 0 && !keys.isEmpty()) {
            // 收集当前预算内可达词条 → 最高可达等级
            List<Identifier> affordable = new ArrayList<>();
            List<Integer> bestLevels = new ArrayList<>();
            for (Identifier id : keys) {
                int maxLvl = pool.get(id);
                int best = 0;
                for (int lvl = maxLvl; lvl > 0; lvl--) {
                    if (minCost(id, lvl) <= remaining) {
                        best = lvl;
                        break;
                    }
                }
                if (best > 0) {
                    affordable.add(id);
                    bestLevels.add(best);
                }
            }
            if (affordable.isEmpty()) {
                break;
            }
            int idx = random.nextInt(affordable.size());
            Identifier enchant = affordable.get(idx);
            int level = bestLevels.get(idx);
            // F：第 2 个及以后词条 50% 重置等级
            if (picked.size() < 2 && affordable.size() > 1 && hasLevels(enchant) && random.nextInt(2) == 0) {
                level = random.nextInt(level) + 1;
            }
            picked.add(new Instance(enchant, level));
            remaining -= minCost(enchant, level);
            // B：每多一个词条额外 +5
            remaining -= 5;
            if (remaining < 5 || picked.size() > 2) {
                break;
            }
        }
        // 书只保留随机 1 个词条
        if (book && picked.size() > 1) {
            Instance keep = picked.get(random.nextInt(picked.size()));
            return List.of(keep);
        }
        return picked;
    }
}
