package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * ICPM 耐力附魔（R196）：护甲耐久损耗 ×(1-0.2×级)，4 级上限。
 *
 * 用 @ModifyArgs 拦截 hurtAndBreak(int, ServerLevel, ServerPlayer, Consumer) 内部对
 * processDurabilityChange(int, ServerLevel, ServerPlayer) 的调用，改写第一个 int 参数(损耗量)。
 * @ModifyArgs 只能拦截方法体内部的「方法调用」，不能改 hurtAndBreak 自身参数；
 * processDurabilityChange 仅在 4 参重载里被调用，天然等价于「只处理该重载」的约束。
 */
@Mixin(ItemStack.class)
public abstract class ICPMArmorDurabilityMixin {

    @ModifyArgs(method = "hurtAndBreak", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;)I"))
    private void icpm$endurance(Args args) {
        ItemStack stack = (ItemStack) (Object) this;
        // 1.21.11 无 ArmorItem，用 EQUIPPABLE 组件判断是否为可穿戴装备
        if (!stack.has(DataComponents.EQUIPPABLE)) {
            return;
        }
        // processDurabilityChange(int, ServerLevel, ServerPlayer)：args [0]=损耗量 [1]=ServerLevel
        ServerLevel serverLevel = (ServerLevel) args.get(1);
        int amount = (int) args.get(0);
        int lvl = ICPMEnchantEffects.level(serverLevel, stack, "endurance");
        if (lvl <= 0 || amount <= 1) {
            return;
        }
        int reduced = (int) (amount * (1.0f - 0.2f * lvl));
        args.set(0, Math.max(1, reduced));
    }
}
