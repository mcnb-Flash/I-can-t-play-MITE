package name.icpm.mixin;

import name.icpm.common.ICPMEnchantmentHelper;
import name.icpm.common.ICPMExperience;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * ICPM 附魔台经验值消耗机制（1.6.4 ContainerEnchantment.enchantItem 移植）
 *
 * 原版：附魔只需【经验等级】达到要求（costs[i] 级），成功后扣等级（1/2/3 级）。
 * ICPM：附魔【直接消耗经验值】——经验值 = 附魔等级 × 100（R196 Enchantment.getExperienceCost），
 * 判定玩家 totalExperience 是否足够，不足则拒绝附魔。
 *
 * 注入点：EnchantmentMenu.clickMenuButton（客户端点击附魔按钮时服务端调用）。
 */
@Mixin(EnchantmentMenu.class)
public abstract class ICPMEnchantmentMenuMixin {

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

        // ICPM：经验值判定（cost = 附魔等级 × 100，R196 getExperienceCost）
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
            ItemStack result = itemStack;
            List<EnchantmentInstance> list = acc.invokeGetEnchantmentList(
                    levelAccess.registryAccess(), result, i, cost);
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
        if (stack.is(Items.GOLDEN_APPLE) || icpm$isWaterBottle(stack)) {
            for (int i = 0; i < 3; i++) {
                menu.costs[i] = 2;
                menu.enchantClue[i] = -1;
                menu.levelClue[i] = -1;
            }
            menu.broadcastChanges();
            ci.cancel();
        }
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
