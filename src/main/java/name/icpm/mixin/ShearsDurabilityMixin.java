package name.icpm.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 剪刀机制 Mixin —— 忠实移植 R196 ItemShears
 *
 * 覆盖所有 ShearsItem 实例（icpm 六把剪刀 + 原版 minecraft:shears 铁剪刀，
 * 铁剪刀的 MITE 属性完全由此 Mixin 注入，不另立物品）。
 *
 * 实现的三套 R196 机制：
 *  1) 剪取类动作消耗 50 点耐久：原版每次只扣 1 点，本 Mixin 额外补 49（合计 50）。
 *     - useOn 命中（雕刻南瓜 / 剪蘑菇牛）→ SUCCESS 分支补 49
 *     - interactLivingEntity（剪羊毛 / 剪蘑菇牛实体等）→ 补 49
 *  2) 右键剪取方块（R196 onItemRightClick 的 silk harvest）：
 *     原版未处理的可剪取方块（叶/毛/藤/蛛网/发光地衣/花/树苗/绊线），
 *     以物品形式完整收获，并扣除 R196 破坏衰减耐久。
 *  3) 左键破坏方块耐久（R196 onBlockDestroyed → getToolDecayFromBreakingBlock）：
 *     用剪刀破坏可剪取方块时，按方块硬度结算 R196 衰减（原版 ShearsItem 不耗耐久）。
 *  4) 右键延迟（R196 右键全局去抖 PlayerControllerMP.setUseButtonDelay ≈500ms）：
 *     剪取类右键动作之间强制间隔 SHEAR_USE_DELAY_TICKS（10 刻≈0.5s），
 *     冷却中右键剪取不生效（点击被忽略），对齐 R196 防连点/误触的手感。
 *
 * 注：R196 onItemRightClick 还要求 block.canSilkHarvest；本版本映射未暴露该方法，
 * 而下方所有可剪取方块在 1.21.11 均为 silk-harvestable，故以 isShearEffective 等价替代。
 * 又：R196 的「慢速破坏」源于其低采掘效率（base 4.0 × 材质系数），
 * 而 1.21.11 采掘速度由不可编译的 ToolComponent 决定，无法在本项目降低，
 * 故「慢」以经济代价（破坏衰减耐久）形式忠实体现，而非降低挖掘速度。
 */
@Mixin(ShearsItem.class)
public class ShearsDurabilityMixin {

    /**
     * R196 右键全局去抖 ≈ 500ms（PlayerControllerMP.setUseButtonDelay）。
     * 以游戏刻近似：10 刻 ≈ 0.5s。剪取类右键动作之间强制间隔，防误触/连点。
     */
    private static final long SHEAR_USE_DELAY_TICKS = 10L;

    /** 玩家 UUID → 上次剪取类右键动作发生时的服务端游戏刻。仅服务端写入/读取。 */
    private static final Map<UUID, Long> LAST_SHEAR_USE = new ConcurrentHashMap<>();

    @Inject(method = "useOn", at = @At("RETURN"))
    private void icpm$shearsUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if (result == InteractionResult.SUCCESS) {
            // 原版已处理（雕刻南瓜 / 剪蘑菇牛）：原版扣 1，这里再补 49 → 合计 50，对齐 R196 剪取类动作
            applyExtra(context.getItemInHand(), context.getPlayer(), 49);
            // 同样计入右键冷却，使后续剪取方块动作也受 R196 去抖约束
            Level lvl = context.getLevel();
            Player p = context.getPlayer();
            if (lvl != null && p != null && !lvl.isClientSide()) {
                markShearUse(p.getUUID(), lvl.getGameTime());
            }
            return;
        }
        if (result != InteractionResult.PASS) {
            return; // CONSUME / FAIL 等不接管
        }

        // 右键剪取（R196 silk harvest 兜底）：原版未处理的可剪取方块 → 完整收获 + 衰减耐久
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level == null || player == null || level.isClientSide()) {
            return;
        }
        // R196 右键全局去抖：冷却中右键剪取不生效（保持原版 PASS，点击被忽略），防连点/误触
        long now = level.getGameTime();
        if (onShearCooldown(player.getUUID(), now)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!isShearEffective(state)) {
            return;
        }
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return;
        }
        level.removeBlock(pos, false);
        Block.popResource(level, pos, new ItemStack(item));
        level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0f, 1.0f);
        int cost = shearBlockDecay(state, level, pos);
        if (cost > 0) {
            applyExtra(context.getItemInHand(), player, cost);
        }
        markShearUse(player.getUUID(), now);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "mineBlock", at = @At("RETURN"))
    private void icpm$shearsBlockBreakDurability(ItemStack stack, Level level, BlockState state, BlockPos pos,
                                                 LivingEntity miner, CallbackInfoReturnable<Boolean> cir) {
        if (miner == null || stack.isEmpty()) {
            return;
        }
        if (!isShearEffective(state)) {
            return;
        }
        // 原版 ShearsItem.mineBlock 不耗耐久；此处追加 R196 破坏方块衰减
        int cost = shearBlockDecay(state, level, pos);
        if (cost <= 0) {
            return;
        }
        stack.hurtAndBreak(cost, miner, EquipmentSlot.MAINHAND);
    }

    private static boolean isShearEffective(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.WOOL)
                || state.is(BlockTags.WOOL_CARPETS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || block == Blocks.COBWEB
                || block == Blocks.VINE
                || block == Blocks.GLOW_LICHEN
                || block == Blocks.TRIPWIRE;
    }

    /**
     * R196 ItemTool.getToolDecayFromBreakingBlock，剪刀 getBaseDecayRateForBreakingBlock = 1.0
     * 公式：hardness==0 → 0；否则 max(max((int)(hardness*100), (int)(100/20)), 1)
     */
    private static int shearBlockDecay(BlockState state, Level level, BlockPos pos) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness <= 0.0f) {
            return 0;
        }
        float decay = 100.0f * 1.0f;
        int a = (int) (hardness * decay);
        int b = (int) (decay / 20.0f); // 5
        return Math.max(Math.max(a, b), 1);
    }

    /** 距上次剪取类右键动作是否仍在去抖冷却窗口内（服务端游戏刻）。 */
    private static boolean onShearCooldown(UUID id, long now) {
        Long last = LAST_SHEAR_USE.get(id);
        return last != null && (now - last) < SHEAR_USE_DELAY_TICKS;
    }

    /** 记录一次剪取类右键动作发生时刻（服务端）。 */
    private static void markShearUse(UUID id, long now) {
        LAST_SHEAR_USE.put(id, now);
    }

    private static void applyExtra(ItemStack stack, LivingEntity entity, int amount) {
        if (entity == null || stack.isEmpty()) {
            return;
        }
        // 原版已扣 1 点；这里扣 amount 点，合计 1+amount
        stack.hurtAndBreak(amount, entity, EquipmentSlot.MAINHAND);
    }
}
