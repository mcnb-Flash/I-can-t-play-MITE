package name.icpm.mixin;

import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/**
 * 诅咒：装备加速腐坏 —— R196 ItemStack.damageItem（玩家持有诅咒时 damage ×2）。
 * 1.21.11 各受损路径汇聚到 ItemStack.hurtAndBreak(int, ServerLevel, ServerPlayer, Consumer)，
 * 此处直接把伤害值翻倍（仅该 4 参重载，避免与其它重载重复计入）。
 */
@Mixin(ItemStack.class)
public abstract class CurseDecayMixin {

    @ModifyVariable(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int icpm$doubleDurabilityLoss(int amount, ServerLevel level, ServerPlayer player,
                                          Consumer<ItemStack> onBroken) {
        if (ICPMCurseManager.isCursed(player, ICPMCurse.EQUIPMENT_DECAYS_FASTER, true)) {
            return amount * 2;
        }
        return amount;
    }
}
