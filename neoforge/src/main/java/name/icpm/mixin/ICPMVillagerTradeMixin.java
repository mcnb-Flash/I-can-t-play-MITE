package name.icpm.mixin;

import name.icpm.common.ICPMVillagerReputation;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * R196 村民声望：交易成功 +1（忠实移植 `EntityVillager.java:147`）。
 *
 * R196 在 villager 的 buyingList 因一次成功交易而重建时，对上次交易的玩家
 * 执行 {@code villageObj.setReputationForPlayer(lastBuyingPlayer, 1)}。
 *
 * 1.21.11 对应入口：{@code AbstractVillager.notifyTrade(MerchantOffer)} —— 每次成功交易后调用，
 * 此时 {@code getTradingPlayer()} 即交易玩家。村民（Villager）= 原版生物 → mixin 注入；
 * 流浪商人（WanderingTrader）同属 AbstractVillager，但无村庄声望，按类型排除。
 */
@Mixin(AbstractVillager.class)
public abstract class ICPMVillagerTradeMixin {

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void icpm$reputationOnTrade(MerchantOffer offer, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        // 仅村庄村民计入声望，流浪商人不计
        if (!(self instanceof Villager)) {
            return;
        }
        if (self.level().isClientSide()) {
            return;
        }
        Player customer = self.getTradingPlayer();
        if (customer != null) {
            // R196：交易成功 +1
            ICPMVillagerReputation.add(customer, 1);
        }
    }
}
