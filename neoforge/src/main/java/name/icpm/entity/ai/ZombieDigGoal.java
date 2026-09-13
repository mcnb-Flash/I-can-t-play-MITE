package name.icpm.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * R196 挖掘型怪物「挖开挡路方块」AI —— {@code EntityAIWatchAnimal} 的忠实移植。
 *
 * <p>R196 的挖掘不是"独立的矿工怪"专属能力，而是 {@code EntityAnimalWatcher}（僵尸等）的通用机制：
 * 当<b>追不到目标</b>（有攻击目标、距离合适、却无路径可接近）时，挖开挡在路径上的方块——
 * 典型表现是挖掉玩家脚下支撑柱、或挖穿墙壁。
 *
 * <p>执行条件（逐条对应 R196 {@code EntityAIWatchAnimal.shouldExecute()}）：
 * <ol>
 *   <li>手持剑 / 短棒 / 镰刀 → 不挖（{@code isHoldingItemThatPreventsDigging}）；</li>
 *   <li>必须有攻击目标，且与目标不在同一方块位置；</li>
 *   <li>已在挖掘且目标方块仍有效 → 继续；否则未挖掘时每 tick 仅 <b>1/20</b> 概率发起；</li>
 *   <li>距离 &gt; 16 格 → 放弃；</li>
 *   <li>距离 &gt; √2 时先尝试挖<b>目标所在方块柱</b>（自目标脚部向下到自身脚部高度）；</li>
 *   <li>距离 &gt; 8 格 → 放弃；视线通畅时上限 8 格，否则 4 格（R196 frenzied 为 6）；</li>
 *   <li>若寻路能接近目标（有路径）→ 不挖，交给移动；</li>
 *   <li>否则挖视线上第一个阻挡方块（自其上方逐格向下尝试）。</li>
 * </ol>
 *
 * <p>进度/冷却全部由 {@link R196BlockDigger} 按 R196 原公式处理（cooloff = 300×硬度 ÷ (1+工具速度×0.5)、
 * 每次 +1 进度、满 10 破坏）。
 */
public class ZombieDigGoal extends Goal {

    /** R196 发起挖掘的概率分母（rand.nextInt(20) == 0）。 */
    private static final int DIG_CHANCE_DENOMINATOR = 20;

    private final Mob mob;
    private final R196BlockDigger digger;

    public ZombieDigGoal(Mob mob) {
        this.mob = mob;
        this.digger = new R196BlockDigger(mob);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /** 供宿主实体查询（例如渲染/调试）。 */
    public R196BlockDigger digger() {
        return digger;
    }

    @Override
    public boolean canUse() {
        if (digger.isHoldingItemThatPreventsDigging()) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return false;
        }
        // R196 EntityAIWatchAnimal.shouldExecute:37 ——
        //   非「可挖掘」（聪明/狂热/持工具）时，必须能看见目标，否则不挖。
        if (!digger.isDiggingEnabled() && !mob.hasLineOfSight(target)) {
            return false;
        }
        if (mob.blockPosition().equals(target.blockPosition())) {
            return false;
        }
        // 已在挖且目标方块仍可挖 → 直接继续
        if (digger.isDestroying() && digger.pos() != null && digger.canDestroyBlock(digger.pos())) {
            return true;
        }
        // R196：未在挖掘时每 tick 仅 1/20 概率发起
        if (mob.getRandom().nextInt(DIG_CHANCE_DENOMINATOR) != 0) {
            return false;
        }
        double distSqr = mob.distanceToSqr(target);
        if (distSqr > 256.0) {
            return false; // > 16 格
        }
        // ① 目标所在方块柱：从目标脚部向下到自身脚部高度逐格尝试
        if (distSqr > 2.0) {
            int footY = mob.blockPosition().getY();
            BlockPos t = target.blockPosition();
            for (int y = target.getBlockY(); y >= footY; y--) {
                if (digger.setBlockToDig(new BlockPos(t.getX(), y, t.getZ()))) {
                    return true;
                }
            }
        }
        if (distSqr > 64.0) {
            return false; // > 8 格
        }
        // ② 视距上限：视线通畅 8 格，否则 4 格
        double maxDist = mob.hasLineOfSight(target) ? 8.0 : 4.0;
        if (distSqr > maxDist * maxDist) {
            return false;
        }
        // ③ 有路可走就不挖（R196: navigator 有路径 → false）
        if (!mob.getNavigation().isDone()) {
            return false;
        }
        // ④ 挖视线上的阻挡方块：自目标上方逐格向下尝试到自身脚部
        int footY = mob.blockPosition().getY();
        BlockPos t = target.blockPosition();
        for (int y = target.getBlockY() + 1; y >= footY; y--) {
            if (digger.setBlockToDig(new BlockPos(t.getX(), y, t.getZ()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (digger.isHoldingItemThatPreventsDigging()) {
            return false;
        }
        if (!digger.isDestroying() || digger.pos() == null) {
            return false;
        }
        if (!digger.canDestroyBlock(digger.pos())) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return false;
        }
        // R196：与目标同格 → 停止（可直接攻击了）
        return !mob.blockPosition().equals(target.blockPosition());
    }

    @Override
    public void stop() {
        digger.cancelBlockDestruction();
    }

    @Override
    public void tick() {
        digger.tickLook();
        digger.tickTask();
    }
}
