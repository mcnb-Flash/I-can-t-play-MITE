package name.icpm.client.mixin;

import java.util.function.Consumer;

import name.icpm.item.ICPMBucketItem;
import name.icpm.item.ICPMBucketRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * R196 桶 tooltip（ItemBucket.addInformation 移植，客户端渲染时生效）：
 * <ul>
 *   <li>Shift：岩浆桶显示"当被盛装时" + 红色熔化概率（仅概率&gt;0，艾德曼不显示）；</li>
 *   <li>Shift：水/岩浆桶且（创造或经验≥100）显示蓝色/红色"Ctrl+右键 放置源头(100 XP)"。</li>
 * </ul>
 * 仅作用原版铁桶三件套与 ICPM 金属桶。
 */
@Mixin(BucketItem.class)
public abstract class BucketTooltipMixin {

    @Inject(method = "appendHoverText(Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/item/Item$TooltipContext;"
            + "Lnet/minecraft/world/item/component/TooltipDisplay;"
            + "Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At("HEAD"))
    private void icpm$bucketTooltip(ItemStack stack, Item.TooltipContext context,
                                    TooltipDisplay display, Consumer<Component> tooltip,
                                    TooltipFlag flag, CallbackInfo ci) {
        if (!(stack.getItem() instanceof BucketItem bucket)) {
            return;
        }
        boolean icpm = bucket instanceof ICPMBucketItem;
        if (!(bucket.getClass() == BucketItem.class || icpm)) {
            return;
        }
        if (!Minecraft.getInstance().hasShiftDown()) {
            return;
        }
        Fluid content = bucket.getContent();
        boolean lava = content == Fluids.LAVA;
        boolean water = content == Fluids.WATER;
        if (!lava && !water) {
            return;
        }
        String metal = ICPMBucketRules.metalOf(stack);

        // R196 addInformation：岩浆桶熔化概率（whenBucketFilled + chanceOfBucketMelting）
        if (lava) {
            int chance = (int) (ICPMBucketRules.meltChance(metal) * 100.0f);
            if (chance > 0) {
                tooltip.accept(Component.empty());
                tooltip.accept(Component.translatable("bucket.icpm.when_filled")
                        .withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.accept(Component.translatable("bucket.icpm.melt_chance", chance)
                        .withStyle(ChatFormatting.RED));
            }
        }

        // R196 addInformation：经验≥100 提示可放置源头
        Player viewer = Minecraft.getInstance().player;
        if (viewer != null && (viewer.getAbilities().instabuild || viewer.totalExperience >= 100)) {
            tooltip.accept(Component.translatable("bucket.icpm.place_source_hint")
                    .withStyle(water ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
    }
}
