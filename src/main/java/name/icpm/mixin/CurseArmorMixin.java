package name.icpm.mixin;

import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 诅咒：无法穿戴盔甲 —— R196 SlotArmor/ItemArmor.onArmorEquip 语义移植。
 * <p>ArmorSlot 在 1.21.11 是非 public 类，必须用 targets 字符串定位。
 * realize 瞬间自动脱甲的 dropAllArmor 在 ICPMCurseManager.onServerTick 内完成。
 */
@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public abstract class CurseArmorMixin {

    @Unique
    private LivingEntity icpm$slotOwner;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void icpm$captureOwner(Container container, LivingEntity owner, EquipmentSlot slot,
                                   int x, int y, int z, Identifier background, CallbackInfo ci) {
        this.icpm$slotOwner = owner;
    }

    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    private void icpm$blockArmor(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty() || cir.getReturnValue() == Boolean.FALSE) {
            return;
        }
        if (!(icpm$slotOwner instanceof Player player)) {
            return;
        }
        // 1.21.11 装备重构后无 ArmorItem 类：cannot_wear_armor = 4 个护甲槽一律拒绝非空放入
        //（与 realize 的 dropAllArmor 槽位语义一致）
        if (ICPMCurseManager.isCursed(player, ICPMCurse.CANNOT_WEAR_ARMOR, true)) {
            cir.setReturnValue(false);
        }
    }
}
