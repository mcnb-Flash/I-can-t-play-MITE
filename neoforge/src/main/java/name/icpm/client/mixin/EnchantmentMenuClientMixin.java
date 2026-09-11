package name.icpm.client.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 阻止【客户端】本地执行 vanilla 附魔词条计算（客户端专用）。
 *
 * 1.21.11 EnchantmentMenu 的词条计算已内联在 slotsChanged（旧版独立的 method_17411 已消失），
 * 服务端版本由 ICPMEnchantmentMenuMixin 在 TAIL 用 R196 词条覆盖并广播 DataSlot；
 * 若客户端同时本地计算 vanilla 词条写入 clue，会与服务端推送产生竞态/后写覆盖，
 * 造成「UI 显示 vanilla 词条、点击产出 R196 词条」。本客户端 mixin 直接跳过客户端计算，
 * 三档 costs/enchantClue/levelClue 完全由服务端 DataSlot 驱动（显示与产出 100% 同源）。
 *
 * 仅注册于 icpm.client.mixins.json，专用服务器不会加载。
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuClientMixin {

    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void icpm$skipClientVanillaCalc(Container container, CallbackInfo ci) {
        ci.cancel();
    }
}
