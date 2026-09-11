package name.icpm.mixin;

import name.icpm.block.HellPortalBlock;
import name.icpm.block.ICPMBlocks;
import name.icpm.block.ReturnPortalBlock;
import name.icpm.block.UnderworldPortalBlock;
import name.icpm.common.ICPMPortalHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("ICPM-Portal");

    private static final int MAX_WIDTH = 21;
    private static final int MIN_WIDTH = 2;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 21;

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void icpm$onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.isClientSide()) {
            return;
        }

        if (!level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
            icpm$handleSpark(context, cir);
            return;
        }

        // The fire candidate cell (where vanilla would place fire) = clicked obsidian's clicked-face neighbor
        BlockPos seed = pos.relative(context.getClickedFace());

        // Find a valid obsidian frame enclosing the clicked obsidian
        PortalFrame frame = icpm$findPortalFrame(level, pos, seed);
        if (frame == null) {
            return;
        }

        Block portalBlock = icpm$determinePortalBlock(level, frame);
        if (portalBlock == null) {
            // Not an ICPM portal condition; let vanilla handle (e.g. overworld Y >= -55 nether portal)
            return;
        }

        icpm$fillPortal(level, frame, portalBlock);

        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() != null) {
            stack.hurtAndBreak(1, context.getPlayer(), EquipmentSlot.MAINHAND);
        }

        LOGGER.info("ICPM-Portal: created {} at {} axis={}", portalBlock, frame.minPos, frame.axis);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    /**
     * Determine which portal block to create.
     * - Overworld Y < -55 -> Underworld portal (purple, to the Underground)
     * - Overworld Y >= -55 -> Return portal (cyan, back to Overworld)
     * - Underworld (any Y) WITHOUT both conditions -> Underworld portal (return to Overworld, purple)
     *   - 地下世界传送门（返回主世界）在地下世界维度任意 Y 都可创建。
     * - Underworld WITH frame bottom touching mantle AND Y < -55 -> Hell portal (red, to the Nether)
     *   - 地狱传送门必须同时满足"下方接触地幔"与"Y < -55"两个条件；仅达成其中之一一律建地下世界传送门。
     * - Nether -> Hell portal (no condition check)
     */
    private Block icpm$determinePortalBlock(Level level, PortalFrame frame) {
        ResourceKey<Level> dim = level.dimension();
        int bottomY = frame.minPos.getY();

        if (dim.equals(Level.OVERWORLD)) {
            if (bottomY < -55) {
                LOGGER.info("ICPM-Portal: overworld Y={} (<-55), underworld portal", bottomY);
                return ICPMBlocks.UNDERWORLD_PORTAL;
            }
            LOGGER.info("ICPM-Portal: overworld Y={} (>= -55), return portal", bottomY);
            return ICPMBlocks.RETURN_PORTAL;
        } else if (dim.equals(ICPMPortalHandler.UNDERWORLD_KEY)) {
            // 地下世界：仅当“接触地幔 且 Y < -55”才建地狱传送门；其余情形（含仅达成其一）一律建地下世界传送门。
            if (icpm$isOnMantle(level, frame) && bottomY < -55) {
                LOGGER.info("ICPM-Portal: underworld Y={} (< -55) on mantle, hell portal", bottomY);
                return ICPMBlocks.HELL_PORTAL;
            }
            LOGGER.info("ICPM-Portal: underworld Y={} (not both conditions), underworld return portal at any Y", bottomY);
            return ICPMBlocks.UNDERWORLD_PORTAL;
        } else if (dim.equals(Level.NETHER)) {
            LOGGER.info("ICPM-Portal: nether, hell portal (no condition check)");
            return ICPMBlocks.HELL_PORTAL;
        }
        return null;
    }

    /**
     * Check whether the bottom layer of the obsidian frame contacts mantle directly beneath it.
     * Note: frame.minPos is the bottom-left INTERIOR cell, so the obsidian bottom bar is at
     * minPos.below() and the mantle would be two blocks below minPos.
     */
    private boolean icpm$isOnMantle(Level level, PortalFrame frame) {
        if (ICPMBlocks.MANTLE == null) {
            return false;
        }
        BlockPos a = frame.minPos.below().below();
        BlockPos b = frame.minPos.relative(frame.rightDir, frame.width - 1).below().below();
        BlockPos center = frame.minPos.relative(frame.rightDir, frame.width / 2).below().below();
        return level.getBlockState(a).is(ICPMBlocks.MANTLE)
                || level.getBlockState(b).is(ICPMBlocks.MANTLE)
                || level.getBlockState(center).is(ICPMBlocks.MANTLE);
    }

    /**
     * Fill the interior of the frame with the given portal block (interior rectangle only).
     */
    private void icpm$fillPortal(Level level, PortalFrame frame, Block portalBlock) {
        BlockState portalState = portalBlock.defaultBlockState();
        if (portalBlock instanceof UnderworldPortalBlock) {
            portalState = portalState.setValue(UnderworldPortalBlock.AXIS, frame.axis);
        } else if (portalBlock instanceof ReturnPortalBlock) {
            portalState = portalState.setValue(ReturnPortalBlock.AXIS, frame.axis);
        } else if (portalBlock instanceof HellPortalBlock) {
            portalState = portalState.setValue(HellPortalBlock.AXIS, frame.axis);
        }
        BlockPos corner = frame.minPos.relative(Direction.UP, frame.height - 1).relative(frame.rightDir, frame.width - 1);
        for (BlockPos fillPos : BlockPos.betweenClosed(frame.minPos, corner)) {
            level.setBlockAndUpdate(fillPos, portalState);
        }
    }

    // ===================== R196 ItemFlintAndSteel.onItemRightClick（火花） =====================

    /**
     * R196 忠实语义（ItemFlintAndSteel.onItemRightClick）：
     * <pre>
     * if (rc.getBlockHit() == Block.tnt)                     → 引爆 TNT
     * else if (rc.isNeighborAirBlock() &amp;&amp; 玩家可编辑该相邻格) → 放置 Block.spark
     * else                                                   → 无事发生（返回 false）
     * 走 spark/TNT 分支时：播放 fire.ignite 音效 + 手持物扣 1 点耐久
     * </pre>
     * 火花（{@link name.icpm.block.ICPMSparkBlock}）自行在 2 tick 内决定「变火 or 消失」，
     * 因此这里不做任何可燃性判断 —— 与 R196 一致：放在任何空气格都行，烧不起来就白烧一次耐久。
     *
     * <p>TNT：现代由 {@code TntBlock.useItemOn} 在方块层处理（先于物品 useOn），无需在此重复。
     * <p>现代补充（1.6.4 尚无这些方块，不可照搬火花逻辑）：营火/蜡烛点燃交回原版，
     * 否则右键未点燃的营火会被火花抢走而永远点不着。
     */
    private void icpm$handleSpark(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);

        // 营火/蜡烛（1.6.4 不存在）→ 交回原版 FlintAndSteelItem.useOn
        if (CampfireBlock.canLight(clicked) || CandleBlock.canLight(clicked) || CandleCakeBlock.canLight(clicked)) {
            return;
        }

        // R196: rc.isNeighborAirBlock()
        BlockPos sparkPos = pos.relative(context.getClickedFace());
        if (!level.getBlockState(sparkPos).isAir()) {
            return;
        }

        // R196: rc.canPlayerEditNeighborOfBlockHit(player, heldItem)
        Player player = context.getPlayer();
        if (player != null
                && !(level.mayInteract(player, sparkPos)
                     && player.mayUseItemAt(sparkPos, context.getClickedFace(), context.getItemInHand()))) {
            return;
        }

        // R196: rc.setNeighborBlock(Block.spark)（仅服务端；客户端已在方法入口提前返回）
        level.setBlock(sparkPos, name.icpm.ICPM.SPARK.defaultBlockState(), 11);

        // R196: playSoundAtEntity(player, "fire.ignite", ...) + tryDamageHeldItem(DamageSource.generic, 1)
        level.playSound(null, sparkPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                level.getRandom().nextFloat() * 0.4F + 0.8F);
        ItemStack stack = context.getItemInHand();
        if (player != null) {
            stack.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    // ===================== PortalShape-style frame detection (ported, FRAME = obsidian) =====================

    private static boolean icpm$isEmpty(BlockState s) {
        return s.isAir() || s.is(BlockTags.FIRE)
                || s.getBlock() instanceof UnderworldPortalBlock
                || s.getBlock() instanceof ReturnPortalBlock
                || s.getBlock() instanceof HellPortalBlock
                || s.is(Blocks.NETHER_PORTAL);
    }

    private static boolean icpm$isFrame(BlockState s) {
        return s.is(Blocks.OBSIDIAN);
    }

    private static int icpm$distanceUntilEdge(Level level, BlockPos pos, Direction dir) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i <= MAX_WIDTH; i++) {
            m.set(pos).move(dir, i);
            BlockState s = level.getBlockState(m);
            if (!icpm$isEmpty(s)) {
                return icpm$isFrame(s) ? i : 0;
            }
            BlockState below = level.getBlockState(m.move(Direction.DOWN));
            if (!icpm$isFrame(below)) {
                break;
            }
        }
        return 0;
    }

    private static BlockPos icpm$calculateBottomLeft(Level level, BlockPos pos, Direction rightDir) {
        int i = Math.max(level.getMinY(), pos.getY() - 21);
        BlockPos p = pos;
        while (p.getY() > i && icpm$isEmpty(level.getBlockState(p.below()))) {
            p = p.below();
        }
        Direction opposite = rightDir.getOpposite();
        int j = icpm$distanceUntilEdge(level, p, opposite) - 1;
        if (j < 0) {
            return null;
        }
        return p.relative(opposite, j);
    }

    private static int icpm$calculateWidth(Level level, BlockPos bottomLeft, Direction rightDir) {
        int i = icpm$distanceUntilEdge(level, bottomLeft, rightDir);
        return i >= MIN_WIDTH && i <= MAX_WIDTH ? i : 0;
    }

    private static boolean icpm$hasTopFrame(Level level, BlockPos bottomLeft, Direction rightDir, int width, int height) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int k = 0; k < width; k++) {
            m.set(bottomLeft).move(Direction.UP, height).move(rightDir, k);
            if (!icpm$isFrame(level.getBlockState(m))) {
                return false;
            }
        }
        return true;
    }

    private static int icpm$calculateHeight(Level level, BlockPos bottomLeft, Direction rightDir, int width) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int j = 0; j < MAX_HEIGHT; j++) {
            m.set(bottomLeft).move(Direction.UP, j).move(rightDir, -1);
            if (!icpm$isFrame(level.getBlockState(m))) {
                return j;
            }
            m.set(bottomLeft).move(Direction.UP, j).move(rightDir, width);
            if (!icpm$isFrame(level.getBlockState(m))) {
                return j;
            }
            for (int k = 0; k < width; k++) {
                BlockPos ip = bottomLeft.relative(Direction.UP, j).relative(rightDir, k);
                if (!icpm$isEmpty(level.getBlockState(ip))) {
                    return j;
                }
            }
        }
        return MAX_HEIGHT;
    }

    private static PortalFrame icpm$findShapeAt(Level level, BlockPos seed, Direction.Axis axis) {
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bottomLeft = icpm$calculateBottomLeft(level, seed, rightDir);
        if (bottomLeft == null) {
            return null;
        }
        int width = icpm$calculateWidth(level, bottomLeft, rightDir);
        if (width == 0) {
            return null;
        }
        int height = icpm$calculateHeight(level, bottomLeft, rightDir, width);
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            return null;
        }
        if (!icpm$hasTopFrame(level, bottomLeft, rightDir, width, height)) {
            return null;
        }
        return new PortalFrame(axis, rightDir, bottomLeft, width, height);
    }

    /**
     * The clicked obsidian must belong to the found frame (i.e. the seed, an interior cell directly
     * behind the clicked obsidian, lies within the interior bounding box of the shape).
     */
    private static boolean icpm$seedTouchesFrame(PortalFrame frame, BlockPos seed, BlockPos clickedObsidian) {
        BlockPos corner = frame.minPos.relative(frame.rightDir, frame.width - 1).relative(Direction.UP, frame.height - 1);
        int minX = Math.min(frame.minPos.getX(), corner.getX());
        int maxX = Math.max(frame.minPos.getX(), corner.getX());
        int minY = Math.min(frame.minPos.getY(), corner.getY());
        int maxY = Math.max(frame.minPos.getY(), corner.getY());
        int minZ = Math.min(frame.minPos.getZ(), corner.getZ());
        int maxZ = Math.max(frame.minPos.getZ(), corner.getZ());
        // seed must be inside the interior box
        if (seed.getX() < minX || seed.getX() > maxX) return false;
        if (seed.getY() < minY || seed.getY() > maxY) return false;
        if (seed.getZ() < minZ || seed.getZ() > maxZ) return false;
        // the clicked obsidian must border the interior box (it is one of the frame bars)
        if (clickedObsidian.getX() < minX - 1 || clickedObsidian.getX() > maxX + 1) return false;
        if (clickedObsidian.getY() < minY - 1 || clickedObsidian.getY() > maxY + 1) return false;
        if (clickedObsidian.getZ() < minZ - 1 || clickedObsidian.getZ() > maxZ + 1) return false;
        return true;
    }

    private PortalFrame icpm$findPortalFrame(Level level, BlockPos clickedObsidian, BlockPos primarySeed) {
        // Try the primary fire-candidate seed first
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            PortalFrame f = icpm$findShapeAt(level, primarySeed, axis);
            if (f != null && icpm$seedTouchesFrame(f, primarySeed, clickedObsidian)) {
                return f;
            }
        }
        // Fallback: try every empty neighbor of the clicked obsidian
        for (Direction dir : Direction.values()) {
            BlockPos candidate = clickedObsidian.relative(dir);
            if (!icpm$isEmpty(level.getBlockState(candidate))) {
                continue;
            }
            for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                PortalFrame f = icpm$findShapeAt(level, candidate, axis);
                if (f != null && icpm$seedTouchesFrame(f, candidate, clickedObsidian)) {
                    return f;
                }
            }
        }
        return null;
    }

    private static final class PortalFrame {
        final Direction.Axis axis;
        final Direction rightDir;
        final BlockPos minPos;
        final int width;
        final int height;

        PortalFrame(Direction.Axis axis, Direction rightDir, BlockPos minPos, int width, int height) {
            this.axis = axis;
            this.rightDir = rightDir;
            this.minPos = minPos;
            this.width = width;
            this.height = height;
        }
    }
}
