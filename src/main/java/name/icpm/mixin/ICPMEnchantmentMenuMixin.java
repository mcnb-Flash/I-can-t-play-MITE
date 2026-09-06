package name.icpm.mixin;

import name.icpm.common.ICPMEnchantDifficulty;
import name.icpm.common.ICPMEnchantmentHelper;
import name.icpm.common.ICPMExperience;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ICPM 附魔台经验值消耗机制（1.6.4 ContainerEnchantment.enchantItem 移植）
 *
 * 原版：附魔只需【经验等级】达到要求（costs[i] 级），成功后扣等级（1/2/3 级）。
 * ICPM：附魔【直接消耗经验值】——经验值 = 附魔等级 × 100（R196 Enchantment.getExperienceCost），
 * 判定玩家 totalExperience 是否足够，不足则拒绝附魔。
 *
 * <p>「所见即所得」：展示层三档词条（enchantClue/levelClue，由 slotsChanged 服务端计算）与点击产出
 * 使用同一份 R196 词条缓存——放入物品时预生成并存 {@link #icpm$slotLists}，点击按钮直接应用缓存，
 * 因此 UI 显示的词条就是实际会得到的词条。词条预算 = 该档位难度（menu.costs[i]，书架+物品附魔能力
 * 驱动），不再使用玩家总经验；产出过程带互斥剔除（exclusive_set），杜绝 保护+爆炸保护 等原版不可能组合。
 *
 * 注入点：EnchantmentMenu.clickMenuButton（客户端点击附魔按钮时服务端调用）。
 */
@Mixin(EnchantmentMenu.class)
public abstract class ICPMEnchantmentMenuMixin {

    /** 三档缓存词条（服务端 slotsChanged 时生成，点击时应用） */
    @Unique
    private List<EnchantmentInstance>[] icpm$slotLists;

    /** 缓存对应的物品快照：物品变化需重算 */
    @Unique
    private ItemStack icpm$cachedStack = ItemStack.EMPTY;

    /** 诊断：服务端生成/产出词条日志（不一致排查用） */
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("ICPM-Enchant");

    /** 获取附魔菜单访问器 */
    @Unique
    private EnchantmentMenuAccessor icpm$accessor() {
        return (EnchantmentMenuAccessor) this;
    }

    /**
     * 重写点击附魔按钮逻辑：经验值判定 + 经验值消耗
     */
    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void icpm$enchantWithXpCost(Player player, int i, CallbackInfoReturnable<Boolean> cir) {
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        EnchantmentMenuAccessor acc = icpm$accessor();
        if (i < 0 || i >= menu.costs.length) {
            cir.setReturnValue(false);
            return;
        }

        Container enchantSlots = acc.getEnchantSlots();
        ItemStack itemStack = enchantSlots.getItem(0);
        ItemStack lapis = enchantSlots.getItem(1);
        int level = i + 1;

        // R196 特殊附魔：金苹果 → 附魔金苹果；水瓶 → 附魔之瓶。
        // 固定等级 2（R196 calcEnchantmentLevelsForSlot），只消耗经验值、不检查青金石。
        if (itemStack.is(Items.GOLDEN_APPLE) || icpm$isWaterBottle(itemStack)) {
            int goldenCost = ICPMEnchantmentHelper.experienceCost(2);
            if (ICPMExperience.getExperience(player) < goldenCost && !player.hasInfiniteMaterials()) {
                cir.setReturnValue(false);
                return;
            }
            if (!player.hasInfiniteMaterials()) {
                player.giveExperiencePoints(-goldenCost);
            }
            enchantSlots.setItem(0, itemStack.is(Items.GOLDEN_APPLE)
                    ? new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)
                    : new ItemStack(Items.EXPERIENCE_BOTTLE));
            enchantSlots.setChanged();
            menu.slotsChanged(enchantSlots);
            cir.setReturnValue(true);
            return;
        }

        // 青金石检查（与原版一致：每个等级消耗 1 个青金石）
        if ((lapis.isEmpty() || lapis.getCount() < level) && !player.hasInfiniteMaterials()) {
            cir.setReturnValue(false);
            return;
        }

        // ICPM：经验值判定（cost = 该档位难度 × 100，R196 getExperienceCost）
        int cost = menu.costs[i];
        if (cost <= 0 || itemStack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        int xpCost = ICPMEnchantmentHelper.experienceCost(cost);
        if (ICPMExperience.getExperience(player) < xpCost && !player.hasInfiniteMaterials()) {
            cir.setReturnValue(false);
            return;
        }

        ContainerLevelAccess access = acc.getAccess();
        access.execute((levelAccess, blockPos) -> {
            if (levelAccess.isClientSide()) {
                return;
            }
            ServerLevel serverLevel = (ServerLevel) levelAccess;
            Registry<Enchantment> reg = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ItemStack result = itemStack;
            // R196 词条产出：应用 slotsChanged 时预生成的该档缓存（所见即所得，带互斥剔除）。
            // 缓存与当前物品不符（极端时序）则即时重算（预算=该档难度 cost）。
            List<EnchantmentInstance> list = icpm$getSlotList(serverLevel, i, cost, result);
            if (list.isEmpty()) {
                list = acc.invokeGetEnchantmentList(
                        levelAccess.registryAccess(), result, i, cost);
            }
            if (list.isEmpty()) {
                return;
            }

            // ICPM：直接扣除经验值（原版是扣经验等级）
            if (!player.hasInfiniteMaterials()) {
                player.giveExperiencePoints(-xpCost);
            }
            // 更新玩家附魔种子（原版 onEnchantmentPerformed 附带行为；传 0 不扣等级）
            player.onEnchantmentPerformed(result, 0);

            // 书 → 附魔书
            if (result.is(Items.BOOK)) {
                result = itemStack.transmuteCopy(Items.ENCHANTED_BOOK);
                enchantSlots.setItem(0, result);
            }

            for (EnchantmentInstance instance : list) {
                result.enchant(instance.enchantment(), instance.level());
            }
            if (player instanceof ServerPlayer) {
                StringBuilder sb = new StringBuilder();
                for (EnchantmentInstance ins : list) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(reg.getKey(ins.enchantment().value()));
                    sb.append(' ').append(ins.level());
                }
                LOGGER.info("CLICK slot={} cost={} applied to {} -> [{}] (clueId={})",
                        i, cost, result.getItem(), sb, menu.enchantClue[i]);
            }

            // 消耗青金石
            lapis.consume(level, player);
            if (lapis.isEmpty()) {
                enchantSlots.setItem(1, ItemStack.EMPTY);
            }

            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                net.minecraft.advancements.CriteriaTriggers.ENCHANTED_ITEM
                        .trigger(serverPlayer, result, level);
            }

            enchantSlots.setChanged();
            acc.getEnchantmentSeed().set(player.getEnchantmentSeed());
            menu.slotsChanged(enchantSlots);
            if (levelAccess instanceof Level l) {
                l.playSound(null, blockPos, SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 1.0F, l.random.nextFloat() * 0.1F + 0.9F);
            }
        });
        cir.setReturnValue(true);
    }

    /**
     * 阻止【客户端】本地执行 vanilla 词条计算（method_17411 写 costs/enchantClue/levelClue）。
     *
     * 关键时序缺陷：客户端放物品也会触发 slotsChanged → 客户端 method_17411 用 vanilla 算法
     * （基于同步来的 enchantmentSeed）算出"锋利"等词条写入本地 clue 并显示；而服务端 TAIL 用
     * R196 生成的是另一套词条（如"亡灵杀手"）并经 DataSlot 广播。客户端本地写入与服务端
     * DataSlot 推送存在竞态/后写覆盖 → UI 显示 vanilla 词条、点击产出 R196 词条 = "名不副实"。
     *
     * 修复：客户端跳过 method_17411 —— 三档 costs/enchantClue/levelClue 全部由服务端 DataSlot
     * 广播驱动（服务端 TAIL 已写 R196 词条并 broadcastChanges），显示与产出 100% 同源。
     */
    @Inject(method = "method_17411", at = @At("HEAD"), cancellable = true)
    private void icpm$skipClientVanillaCalc(ItemStack itemStack, net.minecraft.world.level.Level level,
                                            net.minecraft.core.BlockPos blockPos, CallbackInfo ci) {
        if (level.isClientSide()) {
            ci.cancel();
        }
    }

    /**
     * R196 calcEnchantmentLevelsForSlot：金苹果 / 水瓶放上附魔台时固定显示等级 2
     * （三槽位均为 2 → 成本 200 XP），并清除附魔名/等级线索。
     */
    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void icpm$specialEnchantmentLevels(Container container, CallbackInfo ci) {
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        EnchantmentMenuAccessor acc = icpm$accessor();
        if (container != acc.getEnchantSlots()) {
            return;
        }
        ItemStack stack = container.getItem(0);
        if (stack == null) {
            return; // 容器内容同步瞬时空槽(null)——让 vanilla 自行处理，避免 NPE
        }
        if (stack.is(Items.GOLDEN_APPLE) || icpm$isWaterBottle(stack)) {
            for (int i = 0; i < 3; i++) {
                menu.costs[i] = 2;
                menu.enchantClue[i] = -1;
                menu.levelClue[i] = -1;
            }
            menu.broadcastChanges();
            ci.cancel();
            return;
        }
        // 普通物品：服务端在 vanilla 计算完成后 TAIL 用 R196 词条覆盖 clue/levelClue，
        // 使 UI 三档显示 = 点击实际产出。此处仅标记缓存失效（物品已变化）。
        // icpm$cachedStack 可能为 null（@Unique 字段初始化器不保证注入所有构造器）→ 惰性判空。
        ItemStack cached = icpm$cachedStack;
        if (cached == null || !ItemStack.isSameItemSameComponents(stack, cached)) {
            icpm$slotLists = null;
            icpm$cachedStack = stack.copy();
        }
    }

    /**
     * slotsChanged TAIL（服务端）：物品放入后立即按 R196 预生成三档词条并缓存，
     * 同时把每档第一个词条写入 enchantClue/levelClue —— 客户端 UI 悬停/渲染看到的就是
     * 点击后实际会附上的词条（所见即所得）。预算 = 该档位难度 menu.costs[i]（vanilla 已算好）。
     */
    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void icpm$generateSlotLists(Container container, CallbackInfo ci) {
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        EnchantmentMenuAccessor acc = icpm$accessor();
        if (container != acc.getEnchantSlots()) {
            return;
        }
        ItemStack stack = container.getItem(0);
        if (stack == null || stack.isEmpty() || stack.is(Items.GOLDEN_APPLE) || icpm$isWaterBottle(stack)) {
            return; // 金苹果/水瓶由 HEAD 分支处理；空/空槽物品无需生成
        }
        acc.getAccess().execute((levelAccess, blockPos) -> {
            if (!(levelAccess instanceof ServerLevel serverLevel)) {
                return;
            }
            if (icpm$slotLists != null && ItemStack.isSameItemSameComponents(stack, icpm$cachedStack)) {
                // 已由 TAIL 生成过且物品未变——但 vanilla TAIL 可能覆盖 clue，需重填。
                // 为稳妥直接重算（预算同，随机不同但每次放入只触发一次，无碍）。
            }
            icpm$cachedStack = stack.copy();
            icpm$slotLists = new List[3];
            Registry<Enchantment> reg = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            boolean book = stack.is(Items.BOOK);
            for (int i = 0; i < 3; i++) {
                int cost = menu.costs[i];
                menu.enchantClue[i] = -1;
                menu.levelClue[i] = -1;
                if (cost <= 0) {
                    icpm$slotLists[i] = List.of();
                    continue;
                }
                List<EnchantmentInstance> list = icpm$genList(serverLevel, reg, stack, cost, book);
                icpm$slotLists[i] = list;
                if (!list.isEmpty()) {
                    EnchantmentInstance first = list.get(0);
                    menu.enchantClue[i] = reg.asHolderIdMap().getId(first.enchantment());
                    menu.levelClue[i] = first.level();
                    LOGGER.info("slot[{}] GEN cost={} -> {} {}", i, cost,
                            reg.getKey(first.enchantment().value()), first.level());
                } else {
                    LOGGER.info("slot[{}] GEN cost={} -> (empty)", i, cost);
                }
            }
            menu.broadcastChanges();
        });
    }

    /**
     * 读取槽位缓存；若缓存与物品不符则即时重算（极端时序兜底）。
     */
    @Unique
    private List<EnchantmentInstance> icpm$getSlotList(ServerLevel level, int slot, int cost, ItemStack item) {
        if (icpm$slotLists == null || icpm$cachedStack == null
                || !ItemStack.isSameItemSameComponents(item, icpm$cachedStack)) {
            icpm$cachedStack = item.copy();
            icpm$slotLists = new List[3];
            Registry<Enchantment> reg = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            boolean book = item.is(Items.BOOK);
            for (int i = 0; i < 3; i++) {
                icpm$slotLists[i] = cost > 0 ? icpm$genList(level, reg, item, cost, book) : List.of();
            }
        }
        if (slot < 0 || slot >= icpm$slotLists.length) {
            return List.of();
        }
        return icpm$slotLists[slot];
    }

    /**
     * R196 词条生成（带互斥剔除）：候选 = 对目标可用且非诅咒的全部附魔；
     * 预算 = 该档位难度 cost（档位驱动，R196 buildEnchantmentList 语义）；
     * 选择过程逐轮剔除与已选词条 exclusive 冲突的候选（R196 removeEnchantmentsFromMapThatConflict）。
     */
    @Unique
    private List<EnchantmentInstance> icpm$genList(ServerLevel level, Registry<Enchantment> reg,
                                                   ItemStack item, int cost, boolean book) {
        Map<Identifier, Integer> pool = new LinkedHashMap<>();
        Map<Identifier, Holder<Enchantment>> holderById = new HashMap<>();
        for (Identifier id : reg.keySet()) {
            Enchantment enchant = reg.getValue(id);
            if (enchant == null || !enchant.isSupportedItem(item) || !enchant.canEnchant(item)) {
                continue;
            }
            if (id.getPath().endsWith("_curse")) {
                continue; // 附魔台不给诅咒（vanilla 语义）
            }
            pool.put(id, enchant.getMaxLevel());
            holderById.put(id, reg.wrapAsHolder(enchant));
        }
        if (pool.isEmpty() || cost < 1) {
            return List.of();
        }
        // 互斥判定：R196 canApplyTogether → 1.21.11 Enchantment.areCompatible（exclusive_set）
        java.util.function.BiPredicate<Identifier, Identifier> conflicts = (a, b) -> {
            Holder<Enchantment> ha = holderById.get(a);
            Holder<Enchantment> hb = holderById.get(b);
            if (ha == null || hb == null) {
                return false;
            }
            return !Enchantment.areCompatible(ha, hb);
        };
        List<ICPMEnchantDifficulty.Instance> chosen =
                ICPMEnchantDifficulty.buildList(level.random, cost, pool, book, conflicts);
        List<EnchantmentInstance> out = new ArrayList<>();
        for (ICPMEnchantDifficulty.Instance ins : chosen) {
            Holder<Enchantment> h = holderById.get(ins.enchant);
            if (h != null) {
                out.add(new EnchantmentInstance(h, ins.level));
            }
        }
        return out;
    }

    /** 是否为原版水瓶（Potion = water，无自定义效果时视为水瓶） */
    @Unique
    private static boolean icpm$isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.potion().map(holder -> holder.is(Potions.WATER)).orElse(false);
    }
}
