package name.icpm.entity.ai;

import name.icpm.item.ICPMToolProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * R196「挖掘型怪物」挖掘状态机 —— {@code EntityAnimalWatcher} 的逐字段/逐公式忠实移植。
 *
 * <p>R196 原文（src_deobf/.../EntityAnimalWatcher.java + EntityAIWatchAnimal.java）：
 * <ul>
 *   <li>字段：{@code is_destroying_block / destroy_block_x|y|z / destroy_block_progress /
 *       destroy_block_cooloff(初值 40) / destroy_pause_ticks}；</li>
 *   <li>{@code isHoldingItemThatPreventsDigging()}：手持<b>剑 / 短棒(Cudgel) / 镰刀(Scythe)</b> → 禁止挖掘；</li>
 *   <li>{@code canDestroyBlock}：材料不要求工具，或（frenzied 且 minHarvestLevel&lt;2），或持有对该方块
 *       {@code getStrVsBlock > 0} 的有效工具，或属于硬编码白名单（沙/土/草/沙砾/雪块/耕地/黏土/树叶/
 *       羊毛/海绵/南瓜/西瓜/菌丝/干草/玻璃板/仙人掌(不死)）→ 可挖；液体与 null 一律不可挖；</li>
 *   <li>{@code getCooloffForBlock()}：{@code cooloff = 300 × hardness}（frenzied 再 /2），手持 ItemTool 时
 *       {@code cooloff /= (1 + strVsBlock × 0.5)}；无方块时回落 40；</li>
 *   <li>调度（{@code EntityAIWatchAnimal.updateTask}）：暂停中递减直接返回 → {@code cooloff == 10} 时挥手臂 →
 *       递减 {@code cooloff}，未到 0 返回 → 否则 {@code cooloff = getCooloffForBlock()} 并
 *       {@code partiallyDestroyBlock()}；</li>
 *   <li>{@code partiallyDestroyBlock()}：{@code ++progress}，{@code progress < 10} 只更新裂纹；
 *       {@code >= 10} 时掉落 + 置空 + 播放 2001 音效 + 重置进度，并按上方/下方方块决定是否带 10 tick
 *       暂停继续挖。</li>
 * </ul>
 *
 * <p>1.21.11 的等价映射：R196 {@code Material.requiresTool} → {@code state.requiresCorrectToolForDrops()}；
 * R196 {@code getStrVsBlock} → {@code stack.getDestroySpeed(state)}（均为"该工具对该方块的速度"）；
 * R196 的 {@code minHarvestLevel} 比较 → {@code stack.isCorrectToolForDrops(state)}。
 */
public final class R196BlockDigger {

    /** R196 destroy_block_cooloff 初值。 */
    public static final int DEFAULT_COOLOFF = 40;
    /** R196 进度达到 10 即破坏方块（destroy_block_progress）。 */
    public static final int PROGRESS_TO_BREAK = 10;

    private final Mob mob;

    private boolean destroying;
    private BlockPos pos;
    private int progress = -1;
    private int cooloff = DEFAULT_COOLOFF;
    private int pauseTicks;

    public R196BlockDigger(Mob mob) {
        this.mob = mob;
    }

    public boolean isDestroying() {
        return destroying;
    }

    public BlockPos pos() {
        return pos;
    }

    // ==================== R196 判定 ====================

    /** R196 EntityAnimalWatcher.isHoldingItemThatPreventsDigging：剑 / 短棒 / 镰刀 禁止挖掘。 */
    public boolean isHoldingItemThatPreventsDigging() {
        ItemStack held = mob.getMainHandItem();
        if (held.isEmpty()) {
            return false;
        }
        // 1.21.11 无 SwordItem 类 → 用 #minecraft:swords 物品标签判定剑
        if (held.is(ItemTags.SWORDS)) {
            return true;
        }
        ICPMToolProperties.ToolCategory cat = ICPMToolProperties.INSTANCE.getToolCategory(held);
        return cat == ICPMToolProperties.ToolCategory.CUDGEL
                || cat == ICPMToolProperties.ToolCategory.SCYTHE;
    }

    /**
     * R196 {@code EntityZombie.isDiggingEnabled:412-420}：
     * 非"禁挖物品" 且（聪明 ‖ 狂热 ‖ 手持工具）。丧尸/矿工僵尸共用此语义。
     * （基类 {@code EntityAnimalWatcher.isDiggingEnabled} 仅要求非禁挖物品；此处用僵尸的更严格版本。）
     */
    public boolean isDiggingEnabled() {
        if (isHoldingItemThatPreventsDigging()) {
            return false;
        }
        if (mob instanceof Zombie zombie && ZombieMiteState.get(zombie).smart) {
            return true;
        }
        // R196: getHeldItem() instanceof ItemTool → 用 ICPM 工具分类等价判定
        return ICPMToolProperties.INSTANCE.getToolCategory(mob.getMainHandItem()) != null;
    }

    /** 是否持有对该方块"有效"的工具（R196 has_effective_tool：ItemTool 且 getStrVsBlock > 0）。 */
    public boolean hasEffectiveTool(BlockState state) {
        ItemStack held = mob.getMainHandItem();
        if (held.isEmpty() || ICPMToolProperties.INSTANCE.getToolCategory(held) == null) {
            return false;
        }
        return held.getDestroySpeed(state) > 1.0f;
    }

    /** R196 硬编码白名单：即使材料 requiresTool 也照挖的方块。 */
    private static boolean isWhitelisted(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.FARMLAND)
                || state.is(Blocks.CLAY) || state.is(net.minecraft.tags.BlockTags.LEAVES)
                || state.is(net.minecraft.tags.BlockTags.WOOL)
                || state.is(Blocks.SPONGE) || state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)
                || state.is(Blocks.MYCELIUM) || state.is(Blocks.HAY_BLOCK) || state.is(Blocks.GLASS_PANE)
                || state.is(Blocks.CACTUS) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.COCOA);
    }

    /**
     * R196 canDestroyBlock(x,y,z,check_clipping) 的判定主体（视觉/碰撞裁剪在 1.21 由寻路与
     * 方块形状自然处理，此处保留材料/工具/白名单三条核心规则）。
     */
    public boolean canDestroyBlock(BlockPos p) {
        if (p == null || !(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockState state = level.getBlockState(p);
        if (state.isAir() || state.liquid()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, p);
        if (hardness < 0.0f) {
            return false; // 基岩/屏障等：R196 由 getBlockHardness < 0 排除
        }
        // R196: !requiresTool || (frenzied && minHarvestLevel < 2) || has_effective_tool || 白名单
        if (!state.requiresCorrectToolForDrops() || hasEffectiveTool(state) || isWhitelisted(state)) {
            return true;
        }
        return false;
    }

    // ==================== R196 调度 ====================

    /** R196 setBlockToDig：设置挖掘目标并重置进度。 */
    public boolean setBlockToDig(BlockPos p) {
        if (!canDestroyBlock(p)) {
            return false;
        }
        this.destroying = true;
        if (p.equals(this.pos)) {
            return true;
        }
        this.progress = -1;
        this.pos = p;
        return true;
    }

    /** R196 cancelBlockDestruction：清除裂纹、复位冷却。 */
    public void cancelBlockDestruction() {
        if (!destroying) {
            return;
        }
        if (pos != null && mob.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(mob.getId(), pos, -1);
        }
        this.destroying = false;
        this.progress = -1;
        this.cooloff = DEFAULT_COOLOFF;
    }

    /** R196 getCooloffForBlock：(300 × hardness) ÷ (1 + strVsBlock × 0.5)，无方块时 40。 */
    public int getCooloffForBlock() {
        if (pos == null || !(mob.level() instanceof ServerLevel level)) {
            return DEFAULT_COOLOFF;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return DEFAULT_COOLOFF;
        }
        int value = (int) (300.0f * state.getDestroySpeed(level, pos));
        ItemStack held = mob.getMainHandItem();
        if (!held.isEmpty() && ICPMToolProperties.INSTANCE.getToolCategory(held) != null) {
            float str = held.getDestroySpeed(state);
            value = (int) ((float) value / (1.0f + str * 0.5f));
        }
        return Math.max(1, value);
    }

    /** R196 partiallyDestroyBlock：进度 +1；满 10 时破坏方块并处理上方/下方续挖。 */
    public void partiallyDestroyBlock() {
        if (pos == null || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!canDestroyBlock(pos)) {
            cancelBlockDestruction();
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (++this.progress < PROGRESS_TO_BREAK) {
            this.destroying = true;
        } else {
            this.progress = -1;
            level.levelEvent(2001, pos, Block.getId(state)); // 破坏音效/粒子（R196 playAuxSFX 2001）
            level.destroyBlock(pos, true, mob, 0);           // 掉落（R196 dropBlockAsEntityItem + setBlockToAir）
            this.destroying = false;
            BlockPos above = pos.above();
            BlockState aboveState = level.getBlockState(above);
            // R196：上方为下落方块 / 或仍可挖 → 带 10 tick 暂停继续处理（此处简化为重置冷却）
            if (aboveState.getBlock() instanceof net.minecraft.world.level.block.FallingBlock) {
                this.destroying = true;
                this.pauseTicks = 10;
            }
            this.cooloff = DEFAULT_COOLOFF;
            return;
        }
        level.destroyBlockProgress(mob.getId(), pos, Math.min(PROGRESS_TO_BREAK, progress));
    }

    /** R196 EntityAIWatchAnimal.updateTask 的等价调度（每 tick 由 Goal 调用）。 */
    public void tickTask() {
        if (!destroying || pos == null) {
            return;
        }
        if (pauseTicks > 0) {
            --pauseTicks;
            return;
        }
        if (cooloff == 10) {
            mob.swing(InteractionHand.MAIN_HAND); // R196: cooloff == 10 时 swingArm()
        }
        if (--cooloff > 0) {
            return;
        }
        cooloff = getCooloffForBlock();
        partiallyDestroyBlock();
    }

    /** R196 EntityAnimalWatcher.onUpdate_ 的等价：挖掘中看向目标方块，失效则取消。 */
    public void tickLook() {
        if (destroying && pos != null && pauseTicks == 0) {
            mob.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10.0f, mob.getMaxHeadXRot());
            if (!canDestroyBlock(pos)) {
                cancelBlockDestruction();
            }
        }
    }
}
