package name.icpm.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * R196 ItemFlintAndSteel.tryEntityInteraction：手持打火石右键动物 → 点燃 6 秒。
 * 1.6.4 涵盖 Chicken / Sheep(!已剪毛) / Wolf（Hellhound 设目标、DireWolf 仅驯服个体；1.21 无对应原版实体，略）。
 * 仅服务端执行点燃，避免客户端重复触发。
 */
@Mixin(Player.class)
public class ICPMFlintIgniteMixin {

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void icpm$flintIgniteEntity(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof FlintAndSteelItem)) {
            return;
        }
        if (entity instanceof Chicken) {
            entity.setRemainingFireTicks(6 * 20);
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        } else if (entity instanceof Sheep sheep && !sheep.isSheared()) {
            sheep.setRemainingFireTicks(6 * 20);
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        } else if (entity instanceof Wolf wolf) {
            wolf.setRemainingFireTicks(6 * 20);
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }
    }
}
