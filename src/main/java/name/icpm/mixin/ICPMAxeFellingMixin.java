package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * ICPM 砍伐附魔（R196 EnchantmentTreeFelling 移植）：斧/战斧右键原木时连锁砍伐整棵树（最多 48 块）。
 */
@Mixin(AxeItem.class)
public abstract class ICPMAxeFellingMixin {

    private static final int MAX_LOGS = 48;
    private static final int MAX_RANGE = 12;

    @Inject(method = "useOn", at = @At("HEAD"))
    private void icpm$treeFelling(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return;
        }
        ItemStack axe = context.getItemInHand();
        int lvl = ICPMEnchantEffects.level(level, axe, "tree_felling");
        if (lvl <= 0) {
            return;
        }
        BlockPos origin = context.getClickedPos();
        BlockState originState = level.getBlockState(origin);
        if (!originState.is(BlockTags.LOGS)) {
            return;
        }

        // BFS 收集同种原木
        Set<BlockPos> logs = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        logs.add(origin);
        while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos pos = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (logs.size() >= MAX_LOGS || Math.abs(next.getX() - origin.getX()) > MAX_RANGE
                                || Math.abs(next.getY() - origin.getY()) > MAX_RANGE
                                || Math.abs(next.getZ() - origin.getZ()) > MAX_RANGE) {
                            continue;
                        }
                        if (!logs.contains(next) && level.getBlockState(next).is(originState.getBlock())) {
                            logs.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerPlayer player = context.getPlayer() instanceof ServerPlayer sp ? sp : null;
        // 先破坏所有原木（不含起点，起点由原逻辑处理）
        logs.remove(origin);
        for (BlockPos pos : logs) {
            level.destroyBlock(pos, true, player);
            if (player != null) {
                axe.hurtAndBreak(1, serverLevel, player, item -> {
                });
            }
        }
        if (!logs.isEmpty()) {
            level.playSound(null, origin, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 0.9f);
        }
    }
}
