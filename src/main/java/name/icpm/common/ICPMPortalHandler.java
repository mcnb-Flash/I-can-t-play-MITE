package name.icpm.common;

import name.icpm.ICPM;
import name.icpm.block.BlockRunestone;
import name.icpm.block.HellPortalBlock;
import name.icpm.block.ICPMBlocks;
import name.icpm.block.ICPMPortalShape;
import name.icpm.block.ReturnPortalBlock;
import name.icpm.block.UnderworldPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.Set;

/**
 * ICPM 传送门传送逻辑
 *
 * 传送门类型与规则：
 * - 地下世界传送门 (UnderworldPortalBlock):
 *   主世界 y<=-55 → 地下世界（÷8）
 *   地下世界 y>5 → 主世界（×8，返回上次使用的传送位置）
 *
 * - 返回传送门 (ReturnPortalBlock):
 *   主世界 y>-55 → 返回当前世界出生点
 *
 * - 地狱传送门 (HellPortalBlock):
 *   地下世界 y<=5 且下方接触地幔 → 地狱
 *   地狱 → 地下世界（无条件）
 *
 * 自动创建规则：
 * 传送时若目标位置附近无配对传送门，则自动生成黑曜石框架+传送门方块。
 * 传送门方块类型为返回出发维度的传送门。
 */
public class ICPMPortalHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("ICPM-Portal");

    public static final ResourceKey<Level> UNDERWORLD_KEY = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "underworld")
    );

    /** 主世界<->地下世界/地狱 的坐标缩放倍数 */
    private static final int NETHER_SCALE = 8;

    /** 自动配对传送门搜索半径（方块） */
    private static final int PAIRED_SEARCH_RADIUS = 16;

    public enum PortalType {
        UNDERWORLD, RETURN, HELL
    }

    private ICPMPortalHandler() {
    }

    // ===================== 符文门（R196 BlockPortal 第 8 位 runegate） =====================
    // 4 角同金属符文石的变体组合成 seed，决定同维度内传送坐标（mithril 半径 5000 / adamantium 半径 40000）。
    public enum RunegateMetal {
        MITHRIL,
        ADAMANTIUM
    }

    private static final int RUNEGATE_MITHRIL_RADIUS = 5000;
    private static final int RUNEGATE_ADAMANTIUM_RADIUS = 40000;

    /**
     * 读取传送门 4 角符文石：若 4 角均为同金属符文石，返回该金属；否则 null（R196 getRunegateType）。
     */
    public static RunegateMetal getRunegateMetal(Level level, BlockPos portalPos, Direction.Axis axis) {
        ICPMPortalShape shape = new ICPMPortalShape(level, portalPos, axis);
        if (shape.getBottomLeft() == null || shape.getWidth() < 2 || shape.getHeight() < 3) {
            return null;
        }
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bl = shape.getBottomLeft();
        int w = shape.getWidth();
        int h = shape.getHeight();
        BlockPos cornerBL = bl.relative(rightDir, -1).below();
        BlockPos cornerBR = bl.relative(rightDir, w).below();
        BlockPos cornerTL = bl.relative(rightDir, -1).above(h);
        BlockPos cornerTR = bl.relative(rightDir, w).above(h);
        BlockState sBL = level.getBlockState(cornerBL);
        BlockState sBR = level.getBlockState(cornerBR);
        BlockState sTL = level.getBlockState(cornerTL);
        BlockState sTR = level.getBlockState(cornerTR);
        if (!(sBL.getBlock() instanceof BlockRunestone) || !(sBR.getBlock() instanceof BlockRunestone)
                || !(sTL.getBlock() instanceof BlockRunestone) || !(sTR.getBlock() instanceof BlockRunestone)) {
            return null;
        }
        BlockRunestone rBL = (BlockRunestone) sBL.getBlock();
        BlockRunestone rBR = (BlockRunestone) sBR.getBlock();
        BlockRunestone rTL = (BlockRunestone) sTL.getBlock();
        BlockRunestone rTR = (BlockRunestone) sTR.getBlock();
        if (rBL.getMetal() == rBR.getMetal() && rBL.getMetal() == rTL.getMetal() && rBL.getMetal() == rTR.getMetal()) {
            return rBL.getMetal() == BlockRunestone.MetalType.MITHRIL ? RunegateMetal.MITHRIL : RunegateMetal.ADAMANTIUM;
        }
        return null;
    }

    /**
     * 4 角符文石变体组合成 seed（R196 getRunegateSeed）：BL + (BR<<4) + (TL<<8) + (TR<<12)。
     */
    private static int getRunegateSeed(Level level, BlockPos portalPos, Direction.Axis axis) {
        ICPMPortalShape shape = new ICPMPortalShape(level, portalPos, axis);
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bl = shape.getBottomLeft();
        int w = shape.getWidth();
        int h = shape.getHeight();
        BlockPos cornerBL = bl.relative(rightDir, -1).below();
        BlockPos cornerBR = bl.relative(rightDir, w).below();
        BlockPos cornerTL = bl.relative(rightDir, -1).above(h);
        BlockPos cornerTR = bl.relative(rightDir, w).above(h);
        int vBL = level.getBlockState(cornerBL).getValue(BlockRunestone.VARIANT);
        int vBR = level.getBlockState(cornerBR).getValue(BlockRunestone.VARIANT);
        int vTL = level.getBlockState(cornerTL).getValue(BlockRunestone.VARIANT);
        int vTR = level.getBlockState(cornerTR).getValue(BlockRunestone.VARIANT);
        return vBL + (vBR << 4) + (vTL << 8) + (vTR << 12);
    }

    /**
     * 计算符文门同维度内传送坐标（R196 getRunegateDestinationCoords）：
     * seed==0 → 原点 (0,0)；否则以 seed 为随机种子在金属半径内取坐标（adamantium 远离原点、避开海洋）。
     */
    private static int[] getRunegateDestinationCoords(ServerLevel world, BlockPos portalPos, Direction.Axis axis, RunegateMetal metal) {
        int seed = getRunegateSeed(world, portalPos, axis);
        int x = 0;
        int z = 0;
        if (seed == 0) {
            x = 0;
            z = 0;
        } else {
            int radius = metal == RunegateMetal.ADAMANTIUM ? RUNEGATE_ADAMANTIUM_RADIUS : RUNEGATE_MITHRIL_RADIUS;
            Random random = new Random(seed);
            // R196 getRunegateDestinationCoords：4 次尝试（mithril/adamantium 半径内），
            // adamantium 时远离原点半径一半以内则重投，遇到海洋则换一个尝试。
            for (int attempts = 0; attempts < 4; ++attempts) {
                x = random.nextInt(radius * 2) - radius;
                z = random.nextInt(radius * 2) - radius;
                while (metal == RunegateMetal.ADAMANTIUM) {
                    if (!((double) x * x + (double) z * z < (double) (radius / 2) * (radius / 2))) {
                        break;
                    }
                    x = random.nextInt(radius * 2) - radius;
                    z = random.nextInt(radius * 2) - radius;
                }
                if (!world.getBiome(new BlockPos(x, world.getMinY(), z)).is(Biomes.OCEAN)) {
                    break;
                }
            }
        }
        int[] safe = findSafeDestination(world, x, z);
        if (safe == null) {
            BlockPos p = new BlockPos(x, 64, z);
            if (!isPassable(world, x, 64, z)) {
                world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
            if (!isPassable(world, x, 65, z)) {
                world.setBlockAndUpdate(p.above(), Blocks.AIR.defaultBlockState());
            }
            return new int[]{x, 64, z};
        }
        return safe;
    }

    private static Direction.Axis getAxis(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof UnderworldPortalBlock) {
            return state.getValue(UnderworldPortalBlock.AXIS);
        }
        if (block instanceof ReturnPortalBlock) {
            return state.getValue(ReturnPortalBlock.AXIS);
        }
        if (block instanceof HellPortalBlock) {
            return state.getValue(HellPortalBlock.AXIS);
        }
        return Direction.Axis.X;
    }

    private static boolean teleportRunegate(ServerLevel level, Entity entity, BlockPos portalPos, RunegateMetal metal) {
        int[] dest = getRunegateDestinationCoords(level, portalPos, getAxis(level.getBlockState(portalPos)), metal);
        entity.setPortalCooldown();
        double dx = dest[0] + 0.5;
        double dy = dest[1] + 0.1;
        double dz = dest[2] + 0.5;
        if (entity instanceof ServerPlayer sp) {
            return sp.teleportTo(level, dx, dy, dz, Set.of(), sp.getYRot(), sp.getXRot(), false);
        }
        return entity.teleportTo(level, dx, dy, dz, Set.of(), entity.getYRot(), entity.getXRot(), false);
    }

    private static TeleportTransition createRunegateTransition(ServerLevel level, Entity entity, BlockPos portalPos, RunegateMetal metal) {
        int[] dest = getRunegateDestinationCoords(level, portalPos, getAxis(level.getBlockState(portalPos)), metal);
        double dx = dest[0] + 0.5;
        double dy = dest[1] + 0.1;
        double dz = dest[2] + 0.5;
        Vec3 pos = new Vec3(dx, dy, dz);
        // 符文门：同维度内按 seed 传送，不自动创建配对传送门（R196 不会在落点生成传送门）
        return new TeleportTransition(level, pos, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(),
                Set.of(), e -> {});
    }

    /**
     * 执行传送。返回 true 表示成功传送。
     */
    public static boolean teleport(ServerLevel currentLevel, Entity entity, BlockPos portalPos, PortalType type) {
        // 符文门优先判定（R196 第 8 位）：4 角同金属符文石 → 同维度内按 seed 传送
        RunegateMetal runegate = getRunegateMetal(currentLevel, portalPos, getAxis(currentLevel.getBlockState(portalPos)));
        if (runegate != null) {
            return teleportRunegate(currentLevel, entity, portalPos, runegate);
        }

        PortalPlan plan = computePortalPlan(currentLevel, entity, type);
        if (plan == null) {
            return false;
        }

        entity.setPortalCooldown();

        int[] safe = plan.safe;
        LOGGER.info("ICPM-Portal: {} 到 {}，落点 ({}, {}, {})",
                entity.getName().getString(), plan.targetLevel.dimension(), safe[0], safe[1], safe[2]);

        // 仅当确实需要新建配对传送门时才创建（已复用已有则不建）
        if (plan.portalToCreate != null) {
            findOrCreatePairedPortal(plan.targetLevel, safe[0], safe[1], safe[2], plan.portalToCreate);
        }

        double x = safe[0] + 0.5;
        double y = safe[1] + 0.1;
        double z = safe[2] + 0.5;

        if (entity instanceof ServerPlayer serverPlayer) {
            return serverPlayer.teleportTo(
                    plan.targetLevel, x, y, z, java.util.Set.of(),
                    serverPlayer.getYRot(), serverPlayer.getXRot(), false
            );
        }
        return entity.teleportTo(
                plan.targetLevel, x, y, z, java.util.Set.of(),
                entity.getYRot(), entity.getXRot(), false
        );
    }

    /**
     * 创建传送过渡（用于原版 Portal 接口）。
     */
    public static TeleportTransition createTeleportTransition(ServerLevel currentLevel, Entity entity, BlockPos portalPos, PortalType type) {
        // 符文门优先判定（R196 第 8 位）：4 角同金属符文石 → 同维度内按 seed 传送
        RunegateMetal runegate = getRunegateMetal(currentLevel, portalPos, getAxis(currentLevel.getBlockState(portalPos)));
        if (runegate != null) {
            entity.setPortalCooldown();
            return createRunegateTransition(currentLevel, entity, portalPos, runegate);
        }

        PortalPlan plan = computePortalPlan(currentLevel, entity, type);
        if (plan == null) {
            return null;
        }
        // 设传送冷却，避免落点恰在传送门内时被原路立刻弹回
        entity.setPortalCooldown();

        // 仅当确实需要新建配对传送门时才创建（已复用已有则不建）
        if (plan.portalToCreate != null) {
            findOrCreatePairedPortal(plan.targetLevel, plan.safe[0], plan.safe[1], plan.safe[2], plan.portalToCreate);
        }

        double x = plan.safe[0] + 0.5;
        double y = plan.safe[1] + 0.1;
        double z = plan.safe[2] + 0.5;

        Vec3 position = new Vec3(x, y, z);
        Vec3 deltaMovement = entity.getDeltaMovement();
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();

        // 复用已有传送门时不挂 portal ticket，避免落点再被原版逻辑处理
        TeleportTransition.PostTeleportTransition post;
        if (plan.portalToCreate != null) {
            post = TeleportTransition.PLACE_PORTAL_TICKET;
        } else {
            post = e -> {};
        }
        return new TeleportTransition(
                plan.targetLevel, position, deltaMovement, yRot, xRot,
                Set.of(), post
        );
    }

    /**
     * 传送计划：目标维度、安全落点、需要创建的配对传送门方块（null 表示复用已有，不创建）。
     */
    private static final class PortalPlan {
        final ServerLevel targetLevel;
        final int[] safe;            // {x, y, z}
        final Block portalToCreate;  // null => 不创建（已复用）

        PortalPlan(ServerLevel targetLevel, int[] safe, Block portalToCreate) {
            this.targetLevel = targetLevel;
            this.safe = safe;
            this.portalToCreate = portalToCreate;
        }
    }

    /**
     * 计算传送计划（落点 + 是否需要新建配对传送门），供 teleport / createTeleportTransition 共用。
     *
     * 修复点：返回上一维度（有记忆坐标）时，优先在记忆坐标（含真实 y）附近查找已存在的配对传送门。
     * 若找到则复用该传送门（落点设在已有传送门内、不再新建）；仅当确实找不到时才按维度缩放计算
     * 落点并可能新建。这样原路返回不会再在地表另建一个新传送门。
     */
    private static PortalPlan computePortalPlan(ServerLevel currentLevel, Entity entity, PortalType type) {
        ResourceKey<Level> sourceDimension = currentLevel.dimension();
        ResourceKey<Level> targetDimension = resolveTargetDimension(sourceDimension, type);
        if (targetDimension == null) {
            LOGGER.warn("ICPM-Portal: 无法确定目标维度 from {}", sourceDimension);
            return null;
        }

        ServerLevel targetLevel = currentLevel.getServer().getLevel(targetDimension);
        if (targetLevel == null) {
            LOGGER.warn("ICPM-Portal: 目标维度 {} 不存在，无法传送", targetDimension);
            return null;
        }

        int srcX = entity.blockPosition().getX();
        int srcZ = entity.blockPosition().getZ();

        // 传送前保存位置记忆（用于原路返回定位）
        savePositionOnTeleport(entity, sourceDimension, targetDimension, srcX, srcZ);

        int destX;
        int destZ;
        Block portalToCreate = null;

        if (type == PortalType.RETURN) {
            BlockPos spawnPos = targetLevel.getRespawnData().pos();
            destX = spawnPos.getX();
            destZ = spawnPos.getZ();
        } else {
            int[] remembered = getRememberedPosition(entity, sourceDimension, targetDimension);
            Block returnBlock = getReturnPortalBlock(sourceDimension, targetDimension);
            if (remembered != null && returnBlock != null) {
                // 返回上一维度：优先在记忆坐标（含真实 y）附近复用已有配对传送门
                BlockPos existing = findExistingPairedPortal(targetLevel, remembered[0], remembered[1], remembered[2], returnBlock);
                if (existing != null) {
                    // 复用：落点必须落在传送门内部（尊重真实 Y）。主世界入口常位于 y<=-55 的地下深处，
                    // 不能用 findSafeDestination 从世界顶部往下找，否则会落到地表、远离传送门。
                    // 找到本列最底部的传送门方块，玩家站立其上方的黑曜石地板。
                    int portalBottomY = existing.getY();
                    while (portalBottomY > targetLevel.getMinY()
                            && targetLevel.getBlockState(new BlockPos(existing.getX(), portalBottomY - 1, existing.getZ())).getBlock() == returnBlock) {
                        portalBottomY--;
                    }
                    int[] safe = new int[]{existing.getX(), portalBottomY, existing.getZ()};
                    LOGGER.info("ICPM-Portal: 复用已有配对传送门 at {}，落点 ({},{},{})", existing, safe[0], safe[1], safe[2]);
                    return new PortalPlan(targetLevel, safe, null);
                }
                // 记忆坐标附近无可用传送门 -> 在记忆坐标（含 y）附近落点，之后新建配对门
                int[] safe = findSafeDestinationNear(targetLevel, remembered[0], remembered[2], remembered[1]);
                if (safe == null) {
                    LOGGER.warn("ICPM-Portal: 目标维度 {} 未找到安全落点", targetDimension);
                    return null;
                }
                portalToCreate = returnBlock;
                return new PortalPlan(targetLevel, safe, portalToCreate);
            } else {
                int[] computed = computeDestinationCoords(targetDimension, sourceDimension, srcX, srcZ);
                destX = computed[0];
                destZ = computed[1];
                portalToCreate = returnBlock;
            }
        }

        int[] safe = findSafeDestination(targetLevel, destX, destZ);
        if (safe == null) {
            LOGGER.warn("ICPM-Portal: 目标维度 {} 未找到安全落点", targetDimension);
            return null;
        }
        return new PortalPlan(targetLevel, safe, portalToCreate);
    }

    /**
     * 在记忆坐标附近查找已存在的配对传送门。找到则返回传送门方块位置，未找到返回 null。
     * 搜索半径取 PAIRED_SEARCH_RADIUS（含 y），足以覆盖记忆坐标处的传送门框架。
     */
    private static BlockPos findExistingPairedPortal(ServerLevel level, int centerX, int centerY, int centerZ, Block returnPortalBlock) {
        for (BlockPos pos : BlockPos.betweenClosed(
                centerX - PAIRED_SEARCH_RADIUS, centerY - PAIRED_SEARCH_RADIUS, centerZ - PAIRED_SEARCH_RADIUS,
                centerX + PAIRED_SEARCH_RADIUS, centerY + PAIRED_SEARCH_RADIUS, centerZ + PAIRED_SEARCH_RADIUS)) {
            if (level.getBlockState(pos).getBlock() == returnPortalBlock) {
                return pos;
            }
        }
        return null;
    }

    private static ResourceKey<Level> resolveTargetDimension(ResourceKey<Level> source, PortalType type) {
        switch (type) {
            case UNDERWORLD:
                // 地下世界传送门：主世界/地狱 -> 地下世界；地下世界 -> 主世界
                if (source.equals(UNDERWORLD_KEY)) {
                    return Level.OVERWORLD;
                }
                return UNDERWORLD_KEY;
            case RETURN:
                // 返回传送门：任何维度 -> 主世界出生点
                return Level.OVERWORLD;
            case HELL:
                // 地狱传送门：地下世界 <-> 地狱
                if (source.equals(Level.NETHER)) {
                    return UNDERWORLD_KEY;
                }
                return Level.NETHER;
            default:
                return null;
        }
    }

    /**
     * 获取保存的传送位置记忆。
     * 返回 int[]{x, y, z} 或 null（无记忆）。
     */
    private static int[] getRememberedPosition(Entity entity, ResourceKey<Level> source, ResourceKey<Level> target) {
        if (!(entity instanceof ServerPlayer player)) return null;

        // 地下世界 -> 主世界：使用保存的主世界坐标
        if (source.equals(UNDERWORLD_KEY) && target.equals(Level.OVERWORLD)) {
            return PortalPositionStorage.getOverworldPosition(player);
        }
        // 地狱 -> 地下世界：使用保存的地下世界坐标
        if (source.equals(Level.NETHER) && target.equals(UNDERWORLD_KEY)) {
            return PortalPositionStorage.getUnderworldPosition(player);
        }
        return null;
    }

    /**
     * 传送前保存位置记忆。
     */
    private static void savePositionOnTeleport(Entity entity, ResourceKey<Level> source, ResourceKey<Level> target, int x, int z) {
        if (!(entity instanceof ServerPlayer player)) return;

        // 进入地下世界时保存主世界坐标
        if (source.equals(Level.OVERWORLD) && target.equals(UNDERWORLD_KEY)) {
            PortalPositionStorage.saveOverworldPosition(player, x, entity.blockPosition().getY(), z);
        }
        // 进入地狱时保存地下世界坐标
        if (source.equals(UNDERWORLD_KEY) && target.equals(Level.NETHER)) {
            PortalPositionStorage.saveUnderworldPosition(player, x, entity.blockPosition().getY(), z);
        }
    }

    /**
     * 根据出发/目标维度确定返回时应使用的传送门方块类型。
     * 返回 null 表示不需要自动创建。
     */
    private static Block getReturnPortalBlock(ResourceKey<Level> source, ResourceKey<Level> target) {
        // 主世界 <-> 地下世界：使用地下世界传送门
        if ((source.equals(Level.OVERWORLD) && target.equals(UNDERWORLD_KEY))
                || (source.equals(UNDERWORLD_KEY) && target.equals(Level.OVERWORLD))) {
            return ICPMBlocks.UNDERWORLD_PORTAL;
        }
        // 地下世界 <-> 地狱：使用地狱传送门
        if ((source.equals(UNDERWORLD_KEY) && target.equals(Level.NETHER))
                || (source.equals(Level.NETHER) && target.equals(UNDERWORLD_KEY))) {
            return ICPMBlocks.HELL_PORTAL;
        }
        return null;
    }

    /**
     * 根据 R196 缩放法则计算目标维度的 x/z。
     */
    private static int[] computeDestinationCoords(ResourceKey<Level> target, ResourceKey<Level> source, int srcX, int srcZ) {
        int destX;
        int destZ;
        if (target.equals(Level.OVERWORLD)) {
            // 小维度 -> 主世界：乘 8
            destX = srcX * NETHER_SCALE;
            destZ = srcZ * NETHER_SCALE;
        } else if (source.equals(Level.OVERWORLD)) {
            // 主世界 -> 小维度：除 8
            destX = srcX / NETHER_SCALE;
            destZ = srcZ / NETHER_SCALE;
        } else {
            // 地下世界 <-> 地狱：1:1
            destX = srcX;
            destZ = srcZ;
        }
        return new int[]{destX, destZ};
    }

    /**
     * 搜索安全落点。
     */
    private static int[] findSafeDestination(ServerLevel level, int x, int z) {
        if (level.dimension().equals(UNDERWORLD_KEY)) {
            int top = level.getMaxY() - 2;
            for (int y = top; y > 1; y--) {
                if (isGoodSpotForPlayerToAppearAt(level, x, y, z)) {
                    return new int[]{x, y, z};
                }
            }
            BlockPos pos = new BlockPos(x, 64, z);
            if (!isPassable(level, x, 64, z)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            if (!isPassable(level, x, 65, z)) {
                level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            }
            return new int[]{x, 64, z};
        }

        int maxY = level.getMaxY() - 1;
        // 有天花板维度（地狱）：基岩天花板上方是虚空，落点搜索必须从天花板下方开始，
        // 否则会找到虚空中最高可站立的 y，最终站在基岩天花板顶上。
        if (level.dimensionType().hasCeiling()) {
            for (int y = maxY; y > level.getMinY(); y--) {
                if (isSolid(level, x, y, z)) {
                    maxY = y - 1;
                    break;
                }
            }
        }
        for (int y = maxY; y > level.getMinY(); y--) {
            if (isPassable(level, x, y, z) && isPassable(level, x, y + 1, z)) {
                int groundY = y;
                while (groundY > level.getMinY() && isPassable(level, x, groundY - 1, z)) {
                    groundY--;
                }
                if (groundY <= level.getMinY()) {
                    groundY = 64;
                }
                return new int[]{x, groundY, z};
            }
        }
        BlockPos pos = new BlockPos(x, 64, z);
        if (!isPassable(level, x, 64, z)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        if (!isPassable(level, x, 65, z)) {
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        }
        return new int[]{x, 64, z};
    }

    /**
     * 在 (x,z) 附近、以 preferredY 为中心的有限范围内搜索安全落点。
     * 用于传送门返回：主世界入口常位于 y<=-55 的地下深处，落点必须尊重传送门真实 Y，
     * 不能像 findSafeDestination 那样永远从世界顶部往下找（否则落到地表、远离传送门）。
     * 附近搜不到（被堵）时回退到 findSafeDestination。
     */
    private static int[] findSafeDestinationNear(ServerLevel level, int x, int z, int preferredY) {
        int maxY = level.getMaxY() - 1;
        // 有天花板维度（地狱）：基岩天花板上方是虚空，搜索上限不能高于天花板下方
        if (level.dimensionType().hasCeiling()) {
            for (int y = maxY; y > level.getMinY(); y--) {
                if (isSolid(level, x, y, z)) {
                    maxY = y - 1;
                    break;
                }
            }
        }
        int top = Math.min(preferredY + 3, maxY);
        int bottom = Math.max(level.getMinY() + 1, preferredY - 6);
        for (int y = top; y >= bottom; y--) {
            if (isPassable(level, x, y, z) && isPassable(level, x, y + 1, z)) {
                int groundY = y;
                while (groundY > level.getMinY() && isPassable(level, x, groundY - 1, z)) {
                    groundY--;
                }
                if (groundY <= level.getMinY()) {
                    groundY = y;
                }
                return new int[]{x, groundY, z};
            }
        }
        // 附近被堵或无可站立处，回退常规搜索
        return findSafeDestination(level, x, z);
    }

    private static boolean isGoodSpotForPlayerToAppearAt(ServerLevel level, int x, int y, int z) {
        if (!isPassable(level, x, y, z)) return false;
        if (!isPassable(level, x, y + 1, z)) return false;
        if (!isSolid(level, x, y - 1, z)) return false;
        BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
        if (below.getBlock() == Blocks.LAVA) return false;
        if (below.getBlock() == Blocks.BEDROCK) return false;
        return true;
    }

    private static boolean isPassable(ServerLevel level, int x, int y, int z) {
        if (y < level.getMinY() || y > level.getMaxY()) return false;
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        if (state.isAir()) return true;
        return state.getCollisionShape(level, new BlockPos(x, y, z)).isEmpty();
    }

    private static boolean isSolid(ServerLevel level, int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return !state.getCollisionShape(level, new BlockPos(x, y, z)).isEmpty();
    }

    /**
     * 搜索目标位置附近的配对传送门，若无则创建黑曜石框架+传送门方块。
     */
    private static void findOrCreatePairedPortal(ServerLevel level, int destX, int destY, int destZ, Block returnPortalBlock) {
        // 搜索附近是否已有配对传送门
        for (BlockPos pos : BlockPos.betweenClosed(
                destX - PAIRED_SEARCH_RADIUS, destY - PAIRED_SEARCH_RADIUS, destZ - PAIRED_SEARCH_RADIUS,
                destX + PAIRED_SEARCH_RADIUS, destY + PAIRED_SEARCH_RADIUS, destZ + PAIRED_SEARCH_RADIUS)) {
            if (level.getBlockState(pos).getBlock() == returnPortalBlock) {
                LOGGER.info("ICPM-Portal: 找到配对传送门 at {}", pos);
                return;
            }
        }
        // 未找到，创建新的黑曜石框架 + 传送门
        createObsidianPortalFrame(level, new BlockPos(destX, destY, destZ), returnPortalBlock);
    }

    /**
     * 在指定位置创建标准黑曜石框架（2×3 内部空间）+ 填充传送门方块。
     * 框架朝向使用 X 轴。
     */
    private static void createObsidianPortalFrame(ServerLevel level, BlockPos center, Block portalBlock) {
        // 内部空间：2 宽（X）× 3 高（Y），左下角 = center 向左 1、向下 0
        BlockPos interiorBottomLeft = center.offset(-1, 0, 0);

        // 放置黑曜石框架（底部、顶部、左右两侧）
        Direction.Axis axis = Direction.Axis.X;
        BlockState portalState = portalBlock.defaultBlockState();
        if (portalBlock instanceof UnderworldPortalBlock) {
            portalState = portalState.setValue(UnderworldPortalBlock.AXIS, axis);
        } else if (portalBlock instanceof ReturnPortalBlock) {
            portalState = portalState.setValue(ReturnPortalBlock.AXIS, axis);
        } else if (portalBlock instanceof HellPortalBlock) {
            portalState = portalState.setValue(HellPortalBlock.AXIS, axis);
        }

        // 底部一排（y = interiorBottomLeft.y - 1）
        for (int i = -1; i <= 2; i++) {
            level.setBlockAndUpdate(interiorBottomLeft.offset(i, -1, 0), Blocks.OBSIDIAN.defaultBlockState());
        }
        // 顶部一排（y = interiorBottomLeft.y + 3）
        for (int i = -1; i <= 2; i++) {
            level.setBlockAndUpdate(interiorBottomLeft.offset(i, 3, 0), Blocks.OBSIDIAN.defaultBlockState());
        }
        // 左侧一列（x = interiorBottomLeft.x - 1）
        for (int j = 0; j < 3; j++) {
            level.setBlockAndUpdate(interiorBottomLeft.offset(-1, j, 0), Blocks.OBSIDIAN.defaultBlockState());
        }
        // 右侧一列（x = interiorBottomLeft.x + 2）
        for (int j = 0; j < 3; j++) {
            level.setBlockAndUpdate(interiorBottomLeft.offset(2, j, 0), Blocks.OBSIDIAN.defaultBlockState());
        }
        // 内部填充传送门方块
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                level.setBlockAndUpdate(interiorBottomLeft.offset(i, j, 0), portalState);
            }
        }

        LOGGER.info("ICPM-Portal: 在 {} 创建黑曜石框架 + {}", interiorBottomLeft, portalBlock);
    }
}
