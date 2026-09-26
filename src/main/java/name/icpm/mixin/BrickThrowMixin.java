package name.icpm.mixin;

import name.icpm.entity.ICPMEntities;
import name.icpm.entity.projectile.BrickEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 ItemBrick.onItemRightClick：右键砖/下界砖掷出 EntityBrick 抛射物。
 * 消耗手持 1 个（创造模式除外），继承玩家朝向与初速。
 * 仅拦截砖/下界砖；其余物品走原版 use 逻辑。
 */
@Mixin(Item.class)
public abstract class BrickThrowMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void icpm$brickThrow(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if ((Object) this != (Object) Items.BRICK && (Object) this != (Object) Items.NETHER_BRICK) {
            return;
        }
        if (level.isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        ItemStack thrown = new ItemStack((Item) (Object) this);
        BrickEntity entity = new BrickEntity(ICPMEntities.INSTANCE.getBRICK(), player, level, thrown);
        entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        level.addFreshEntity(entity);
        if (!player.isCreative()) {
            player.getItemInHand(hand).shrink(1);
        }
        cir.setReturnValue(InteractionResult.CONSUME);
        cir.cancel();
    }
}
