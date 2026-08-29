package name.icpm.mixin;

import name.icpm.item.ICPMItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 黑曜石收割判定 Mixin
 *
 * ICPM 规则：黑曜石需要镐类工具，等级 >= 3（铁镐即可）。
 * ICPMToolRulesMixin 已放行铁镐的破坏速度（等级 3.0 >= 3.0），
 * 但原版 Item.isCorrectToolForDrops 将黑曜石判给 needs_diamond_tool（钻石镐），
 * 导致铁镐能裂开黑曜石却不掉落。此处按 ICPM 等级补回掉落判定。
 */
@Mixin(Item.class)
public class ICPMToolHarvestMixin {

    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true, require = 0)
    private void icpm$allowObsidianHarvest(ItemStack stack, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        Block block = state.getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.CRYING_OBSIDIAN) {
            return;
        }
        if (icpm$isIronOrBetterPickaxe(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean icpm$isIronOrBetterPickaxe(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();

        // 原版铁/钻石/下界合金镐（等级 3+）
        if (item == Items.IRON_PICKAXE || item == Items.DIAMOND_PICKAXE ||
            item == Items.NETHERITE_PICKAXE) {
            return true;
        }

        // ICPM 远古金属/秘银/艾德曼镐（等级 3.5 / 4 / 5）
        if (item == ICPMItems.ANCIENT_METAL_PICKAXE ||
            item == ICPMItems.MITHRIL_PICKAXE ||
            item == ICPMItems.ADAMANTIUM_PICKAXE) {
            return true;
        }

        // 战锤按镐类计（远古金属 3.5 / 秘银 4 / 艾德曼 5 / 下界合金 6）
        if (item == ICPMItems.ANCIENT_METAL_WAR_HAMMER ||
            item == ICPMItems.MITHRIL_WAR_HAMMER ||
            item == ICPMItems.ADAMANTIUM_WAR_HAMMER ||
            item == ICPMItems.NETHERITE_WAR_HAMMER) {
            return true;
        }

        return false;
    }
}
