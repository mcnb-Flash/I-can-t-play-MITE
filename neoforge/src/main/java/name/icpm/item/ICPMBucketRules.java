package name.icpm.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * R196 水源·桶机制（忠实移植）。
 *
 * <p>判决来源：src_deobf/net/minecraft/src/ItemBucket.java + ItemVessel.java + Material.java
 * <ul>
 *   <li>接取：空桶(铁或 ICPM 金属)右键源头——生存/非 ctrl 时<strong>不删除液体块</strong>
 *       （源头不消耗）；创造才删除。接岩浆先 roll 熔化：艾德曼 0%、金 20%、
 *       其余 0.01×(秘银耐久 64÷本材质耐久)（铜/银 16%、铁 8%、远古金属 4%、秘银 1%），
 *       熔化即整桶被岩浆吞没（ItemVessel.getItemProducedWhenDestroyed harmed-by-lava → null）。</li>
 *   <li>放置：满桶(水/岩浆)默认放<strong>流动</strong>液块（R196 放 moving + schedule 1tick
 *       若仍原位且无可扩散方向则结晶为源头，等效"密闭成源/开放扩散"——本移植用服务端
 *       延迟沉降检查实现）；创造直接放源头；Ctrl+右键 消耗 100 经验放源头由
 *       {@link name.icpm.network.BucketSourcePacket} 在服务端完成。</li>
 *   <li>同液体取消：生存放置时目标命中块或邻块已是同种液体 → 不放置、桶变空
 *       （R196 防"往海里倒水造源"）。</li>
 *   <li>tooltip：Shift 时岩浆桶显示熔化概率；水/岩浆桶且经验≥100 显示 Ctrl 放源头提示。</li>
 * </ul>
 */
public final class ICPMBucketRules {

    private ICPMBucketRules() {
    }

    /** 桶的容器金属："iron" 或 ICPM 金属名（copper/silver/gold/ancient_metal/mithril/adamantium）。 */
    public static String metalOf(ItemStack stack) {
        if (stack.getItem() instanceof ICPMBucketItem b) {
            return b.getMetal();
        }
        return "iron";
    }

    public static Item emptyBucketOf(String metal) {
        return "iron".equals(metal) ? Items.BUCKET : ICPMBuckets.emptyOf(metal);
    }

    public static Item waterBucketOf(String metal) {
        return "iron".equals(metal) ? Items.WATER_BUCKET : ICPMBuckets.waterOf(metal);
    }

    public static Item lavaBucketOf(String metal) {
        return "iron".equals(metal) ? Items.LAVA_BUCKET : ICPMBuckets.lavaOf(metal);
    }

    /** 是否属于本规则管辖的桶（仅流体桶：水/岩浆/空）。 */
    public static boolean isR196Bucket(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof BucketItem bucket)) {
            return false;
        }
        Fluid c = bucket.getContent();
        if (c != Fluids.EMPTY && c != Fluids.WATER && c != Fluids.LAVA) {
            return false;
        }
        // 仅原版铁桶三件套与 ICPM 金属桶；放过鱼类/粉雪等子类桶
        return item.getClass() == BucketItem.class || item instanceof ICPMBucketItem;
    }

    /**
     * R196 getChanceOfMeltingWhenFilledWithLava：
     * adamantium 0.0；gold 0.2；其余 0.01 × (mithril.durability 64 ÷ material.durability)
     * （EnumEquipmentMaterial durability：copper/silver 4、iron 8、ancient_metal 16、mithril 64）。
     */
    public static float meltChance(String metal) {
        return switch (metal) {
            case "adamantium" -> 0.0f;
            case "gold" -> 0.2f;
            case "copper", "silver" -> 0.01f * (64.0f / 4.0f);   // 16%
            case "iron" -> 0.01f * (64.0f / 8.0f);               // 8%
            case "ancient_metal" -> 0.01f * (64.0f / 16.0f);     // 4%
            case "mithril" -> 0.01f * (64.0f / 64.0f);           // 1%
            default -> 0.01f;
        };
    }

    /** 材质是否被岩浆损毁（Material.is_harmed_by_lava 默认 true；仅艾德曼 false）。 */
    public static boolean destroyedByLava(String metal) {
        return !"adamantium".equals(metal);
    }

    /** R196 满桶 use 的统一入口（原版铁桶 mixin 与 ICPMBucketItem.use 共用）。 */
    public static InteractionResult handleUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BucketItem bucket) || !isR196Bucket(stack)) {
            return InteractionResult.PASS;
        }
        Fluid content = bucket.getContent();
        boolean empty = content == Fluids.EMPTY;
        String metal = metalOf(stack);
        boolean creative = player.getAbilities().instabuild;

        ClipContext.Fluid clip = empty ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;
        BlockHitResult hit = raycast(level, player, clip);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos hitPos = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockPos adj = hitPos.relative(face);
        if (!level.mayInteract(player, hitPos) || !player.mayUseItemAt(adj, face, stack)) {
            return InteractionResult.FAIL;
        }

        if (empty) {
            return scoop(level, player, hand, stack, bucket, metal, creative, hitPos);
        }
        return place(level, player, hand, stack, bucket, metal, creative, content, hit, hitPos, adj);
    }

    /** 玩家视线块检测（等效原版 getPlayerPOVHitResult：射线长度=blockInteractionRange）。 */
    private static BlockHitResult raycast(Level level, Player player, ClipContext.Fluid fluidMode) {
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition(1.0f);
        net.minecraft.world.phys.Vec3 look = player.getViewVector(1.0f);
        double reach = player.blockInteractionRange();
        return level.clip(new ClipContext(eye,
                eye.add(look.x * reach, look.y * reach, look.z * reach),
                ClipContext.Block.OUTLINE, fluidMode, player));
    }

    /** 空桶接取：源头不消耗（生存）；接岩浆 roll 熔化。 */
    private static InteractionResult scoop(Level level, Player player, InteractionHand hand,
                                           ItemStack stack, BucketItem bucket, String metal,
                                           boolean creative, BlockPos hitPos) {
        FluidState fs = level.getFluidState(hitPos);
        if (fs.isEmpty() || !fs.isSource()) {
            return InteractionResult.FAIL;
        }
        Fluid src = fs.getType();
        boolean isWater = src == Fluids.WATER;
        boolean isLava = src == Fluids.LAVA;
        if (!isWater && !isLava) {
            return InteractionResult.FAIL;
        }
        BlockState state = level.getBlockState(hitPos);
        // 接取仅对真实液块生效（水流/岩浆流均为 BucketPickup）；炼药锅等由原版其它路径处理
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.BucketPickup)) {
            return InteractionResult.FAIL;
        }

        // R196：仅创造（或 ctrl）删除被接液体块；生存普通接取不消耗源头
        if (creative) {
            level.setBlockAndUpdate(hitPos, Blocks.AIR.defaultBlockState());
        }

        if (creative) {
            return InteractionResult.SUCCESS; // 创造不消耗空桶
        }

        // R196 生存接岩浆熔化：roll 命中 → 整桶被岩浆吞没（源仍在）
        if (isLava) {
            float chance = meltChance(metal);
            if (chance > 0.0f && level.getRandom().nextFloat() < chance) {
                level.playSound(null, hitPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f,
                        0.8f + level.getRandom().nextFloat() * 0.4f);
                level.gameEvent(player, GameEvent.FLUID_PICKUP, hitPos);
                player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(bucket));
                ItemStack gone = ItemUtils.createFilledResult(stack, player, ItemStack.EMPTY);
                return InteractionResult.SUCCESS.heldItemTransformedTo(gone);
            }
        }

        // 接取成功：空桶 → 对应材质满桶（源不被删除）
        Item filled = isLava ? lavaBucketOf(metal) : waterBucketOf(metal);
        ItemStack result = ItemUtils.createFilledResult(stack, player, new ItemStack(filled));
        level.playSound(null, hitPos, isLava ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        level.gameEvent(player, GameEvent.FLUID_PICKUP, hitPos);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(bucket));
        return InteractionResult.SUCCESS.heldItemTransformedTo(result);
    }

    /** 满桶放置：生存普通=流动液块（密闭沉降成源）；创造=源头；同液体取消。 */
    private static InteractionResult place(Level level, Player player, InteractionHand hand,
                                           ItemStack stack, BucketItem bucket, String metal,
                                           boolean creative, Fluid content, BlockHitResult hit,
                                           BlockPos hitPos, BlockPos adj) {
        // R196 同液体取消：生存时命中块/邻块已是同种液体 → 不放置，桶变空
        if (!creative
                && (level.getFluidState(hitPos).getType() == content
                    || level.getFluidState(adj).getType() == content)) {
            ItemStack empty = ItemUtils.createFilledResult(stack, player,
                    new ItemStack(emptyBucketOf(metal)));
            return InteractionResult.SUCCESS.heldItemTransformedTo(empty);
        }

        BlockState stateAtHit = level.getBlockState(hitPos);
        boolean canIntoHit = stateAtHit.canBeReplaced(content)
                || (stateAtHit.getBlock() instanceof LiquidBlockContainer container
                    && container.canPlaceLiquid(player, level, hitPos, stateAtHit, content));
        BlockPos target = canIntoHit ? hitPos : adj;
        BlockState targetState = level.getBlockState(target);
        if (!(targetState.canBeReplaced(content)
                || (targetState.getBlock() instanceof LiquidBlockContainer container
                    && container.canPlaceLiquid(player, level, target, targetState, content)))) {
            return InteractionResult.FAIL; // 无处可放
        }

        boolean placed;
        if (creative) {
            // R196 创造：直接放源头（走原版 emptyContents 保留容器/石-岩浆交互）
            placed = bucket.emptyContents(player, level, target, new BlockHitResult(
                    hit.getLocation(), hit.getDirection(), target, false));
        } else {
            placed = placeFlowing(player, level, target, targetState, content);
        }
        if (!placed) {
            return InteractionResult.FAIL;
        }

        if (!creative) {
            ItemStack empty = ItemUtils.createFilledResult(stack, player,
                    new ItemStack(emptyBucketOf(metal)));
            return InteractionResult.SUCCESS.heldItemTransformedTo(empty);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * R196 生存放置：默认放"流动"液块而非源头（getBlockForContents = Moving）。
     * 现代 MC 无 still/moving 分块，放 level=1 的流动态等效；若其密闭无可扩散方向，
     * 由延迟沉降检查结晶为源头（R196 scheduleBlockChange 语义）。
     */
    private static boolean placeFlowing(Player player, Level level, BlockPos pos,
                                        BlockState current, Fluid content) {
        if (current.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, level, pos, current, content)) {
            FluidState flowing = ((FlowingFluid) content).getFlowing(1, false);
            container.placeLiquid(level, pos, current, flowing);
            if (!level.isClientSide()) {
                registerSettle(level, pos, content, flowing);
            }
        } else {
            FluidState flowing = ((FlowingFluid) content).getFlowing(1, false);
            BlockState placed = flowing.createLegacyBlock();
            if (!level.setBlock(pos, placed, 11)) {
                return false;
            }
            if (!level.isClientSide()) {
                registerSettle(level, pos, content, flowing);
            }
        }
        level.playSound(player, pos, content == Fluids.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA
                : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        return true;
    }

    // ============ 密闭沉降成源（R196 scheduleBlockChange moving→still） ============

    private record SettleEntry(Fluid fluid, FluidState placed, long born) {
    }

    private static final Map<ResourceKey<Level>, Map<BlockPos, SettleEntry>> PENDING = new HashMap<>();

    private static void registerSettle(Level level, BlockPos pos, Fluid fluid, FluidState placed) {
        PENDING.computeIfAbsent(level.dimension(), k -> new HashMap<>())
                .put(pos.immutable(), new SettleEntry(fluid, placed, level.getGameTime()));
    }

    /** 每服务端 tick 检查：流动液块若已无可扩散去向则结晶为源头。 */
    public static void onServerTick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        for (ResourceKey<Level> dim : List.copyOf(PENDING.keySet())) {
            ServerLevel level = server.getLevel(dim);
            if (level == null) {
                PENDING.remove(dim);
                continue;
            }
            Map<BlockPos, SettleEntry> map = PENDING.get(dim);
            if (map == null) {
                continue;
            }
            for (Map.Entry<BlockPos, SettleEntry> e : List.copyOf(map.entrySet())) {
                SettleEntry entry = e.getValue();
                long age = level.getGameTime() - entry.born();
                if (trySettle(level, e.getKey(), entry) || age > 120) {
                    map.remove(e.getKey());
                }
            }
            if (map.isEmpty()) {
                PENDING.remove(dim);
            }
        }
    }

    private static boolean trySettle(ServerLevel level, BlockPos pos, SettleEntry entry) {
        FluidState cur = level.getFluidState(pos);
        if (cur.isEmpty() || !cur.getType().isSame(entry.fluid()) || cur.isSource()) {
            return true; // 已变化/已是源 → 收尾
        }
        if (!sameLayer(cur, entry.placed())) {
            return true; // 已变薄/扩散 → 不再结晶
        }
        if (level.getFluidState(pos.below()).getType().isSame(entry.fluid())) {
            return false; // 下方仍是同种液体=补给/流柱，非密闭 → 继续等
        }
        if (spreadable(level, pos.below(), entry.fluid())
                || spreadable(level, pos.north(), entry.fluid())
                || spreadable(level, pos.south(), entry.fluid())
                || spreadable(level, pos.east(), entry.fluid())
                || spreadable(level, pos.west(), entry.fluid())) {
            return false; // 还有去向，继续等
        }
        // 密闭 → 结晶为源头（R196 schedule moving→still）
        level.setBlockAndUpdate(pos, entry.fluid().defaultFluidState().createLegacyBlock());
        return true;
    }

    private static boolean sameLayer(FluidState a, FluidState b) {
        return a.getAmount() == b.getAmount();
    }

    /** 该方块是否可被流体流入（空气/可替换非液体）。 */
    private static boolean spreadable(Level level, BlockPos pos, Fluid fluid) {
        BlockState bs = level.getBlockState(pos);
        if (bs.isAir()) {
            return true;
        }
        return bs.getFluidState().isEmpty() && bs.canBeReplaced(fluid);
    }

    /**
     * 服务端 ctrl 放源头处理器（BucketSourcePacket）。生存扣 100 经验；创造免费。
     * 物品由 setItemInHand + 槽同步包即时刷新。
     */
    public static void placeSourceAt(ServerPlayer player, ItemStack stack, InteractionHand hand,
                                     BlockPos target) {
        if (!(stack.getItem() instanceof BucketItem bucket) || !isR196Bucket(stack)) {
            return;
        }
        Fluid content = bucket.getContent();
        if (content != Fluids.WATER && content != Fluids.LAVA) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockState state = level.getBlockState(target);
        if (state.getBlock() instanceof LiquidBlockContainer
                || !state.canBeReplaced(content)
                || level.getFluidState(target).getType() == content) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable("bucket.icpm.cannot_place_source"));
            return;
        }
        if (!player.getAbilities().instabuild && player.totalExperience < 100) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable("bucket.icpm.need_xp_source"));
            return;
        }
        if (!player.getAbilities().instabuild) {
            player.giveExperiencePoints(-100);
        }
        level.setBlockAndUpdate(target, content.defaultFluidState().createLegacyBlock());
        level.playSound(null, target, content == Fluids.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA
                : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.gameEvent(player, GameEvent.FLUID_PLACE, target);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(bucket));

        String metal = metalOf(stack);
        ItemStack result = new ItemStack(emptyBucketOf(metal));
        player.setItemInHand(hand, result);
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : 40;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                -2, 0, slot, result));
    }
}
