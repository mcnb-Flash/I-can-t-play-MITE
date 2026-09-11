package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import name.icpm.common.ICPMFarmlandFertility;
import name.icpm.item.ICPMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 鸭嘴锄 (Mattock) 多功能工具 Mixin
 *
 * 基于 ICPM R196 源码: ItemMattock extends ItemShovel
 * 鸭嘴锄拥有铲子+锄头的双重功能:
 * - 作为铲子: 挖掘泥土、沙子、沙砾等方块有加成
 * - 作为锄头: 右键泥土/草方块可以将其变成耕地
 *
 * 这个 Mixin 拦截 ShovelItem.useOn，当使用鸭嘴锄右键时执行锄头的耕地逻辑
 */
@Mixin(ShovelItem.class)
public class MattockHoeMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void icpm$mattockHoeAction(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = context.getItemInHand();
        if (!isMattock(stack)) {
            return;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction direction = context.getClickedFace();

        if (level.isClientSide() || player == null) {
            return;
        }

        BlockState state = level.getBlockState(pos);

        // 只有当右键泥土或草方块时才执行锄头耕地逻辑
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
            // 检查是否对着方块的上面（否则铲子会制作草径）
            if (direction != Direction.UP) {
                return;
            }

            // 模拟锄头的耕地行为
            BlockState farmlandState = Blocks.FARMLAND.defaultBlockState();

            // 在服务器端执行
            if (level instanceof ServerLevel serverLevel) {
                level.setBlockAndUpdate(pos, farmlandState);

                // 肥沃附魔：战锄锄地成功后给耕地增加肥力（与锄头一致）
                int fertilityLvl = ICPMEnchantEffects.level(level, stack, "fertility");
                if (fertilityLvl > 0) {
                    ICPMFarmlandFertility.add(level.dimension(), pos, fertilityLvl);
                }

                // ICPM规则：锄地一次消耗50点耐久
                stack.hurtAndBreak(50, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);

                // 返回成功
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

    /**
     * 判断物品是否为鸭嘴锄
     */
    private static boolean isMattock(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        return item == ICPMItems.SILVER_MATTOCK
            || item == ICPMItems.ANCIENT_METAL_MATTOCK
            || item == ICPMItems.MITHRIL_MATTOCK
            || item == ICPMItems.ADAMANTIUM_MATTOCK;
    }
}