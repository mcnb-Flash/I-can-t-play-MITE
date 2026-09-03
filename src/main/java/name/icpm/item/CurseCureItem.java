package name.icpm.item;

import name.icpm.curse.ICPMCurseManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/**
 * ICPM 去咒药水 —— R196 ItemBottleOfDisenchanting 忠实移植。
 *
 * <p>R196（WorldServer.removeCursesFromPlayer）：饮用该瓶可解除身上全部女巫诅咒；
 * 且它豁免 cannot_drink 诅咒（解毒剂必须能喝）。本实现为非药水自定义物品走自身
 * use 即饮（不经 CurseDrinkMixin 药水拦截，天然豁免禁饮）；服务端解咒后发解除提示，
 * 非创造模式消耗 1 瓶。
 */
public class CurseCureItem extends Item {

    public CurseCureItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            boolean hadCurse = ICPMCurseManager.hasAnyCurse(sp) || ICPMCurseManager.hasPending(sp);
            if (hadCurse) {
                ICPMCurseManager.lift(sp); // 内部含"诅咒已被解除"提示
            }
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (!sp.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
