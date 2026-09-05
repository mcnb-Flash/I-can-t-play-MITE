package name.icpm.mixin;

import name.icpm.block.ICPMBlueberryBush;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 剪刀剪取植物方块（R196 ItemShears.onItemRightClick 移植，silk-harvest 语义）：
 * 手持任意剪刀（原版/ICPM 铜/金/银/远古金属/秘银/艾德曼——均为 ShearsItem 子类）
 * 右键植物类方块 → 整丛剪下、以方块物品形式掉落（可捡起带走）。
 *
 * 当前剪取对象：icpm:blueberry_bush（未来 icpm 植物方块在此追加判定）。
 * 对蓝莓丛有果(age=1)/空枝(age=0)均剪走整丛（不额外掉蓝莓——摘果请空手右键）。
 *
 * useOn 声明于 ShearsItem（vanilla 覆写存在），@Mixin(ShearsItem) 合法；
 * 该 vanilla useOn 仅用于原版剪毛剪取交互之外的默认行为，HEAD 接管植物分支后
 * 其它目标走原逻辑不受影响。
 */
@Mixin(ShearsItem.class)
public abstract class ShearsPlantCutMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void icpm$shearsCutPlant(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        // —— 剪取对象判定（ICPM 植物）——
        Block cutBlock = null;
        if (state.getBlock() == ICPMBlueberryBush.BLUEBERRY_BUSH_BLOCK) {
            cutBlock = ICPMBlueberryBush.BLUEBERRY_BUSH_BLOCK;
        }
        if (cutBlock == null) {
            return; // 非 ICPM 剪取对象，交回原逻辑
        }
        // 整丛剪下：以方块物品掉落 + 音效 + 破坏 + 耐久
        Item asItem = cutBlock.asItem();
        if (asItem == null || asItem == net.minecraft.world.item.Items.AIR) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        var player = context.getPlayer();
        // 剪下整丛（物品可放回）
        Block.popResource(serverLevel, pos, new ItemStack(asItem));
        serverLevel.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0f, 1.0f);
        serverLevel.destroyBlock(pos, false);
        // R196 剪植物损耗（getToolDecayFromBreakingBlock≈1）
        if (player != null) {
            ItemStack stack = context.getItemInHand();
            if (!stack.isEmpty()) {
                EquipmentSlot slot = context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.hurtAndBreak(1, player, slot);
            }
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
