package name.icpm.mixin;

import name.icpm.common.ICPMEnchantEffects;
import name.icpm.common.ICPMFarmlandFertility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 收获附魔（R196 EnchantmentHarvesting 移植）：镰刀/锄/战锄破坏成熟作物时额外掉落。
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ICPMCropHarvestMixin {

    /** 破坏前处理（方块仍在）：若为成熟作物且手持收获附魔，额外掉落（R196 收获量加成） */
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void icpm$harvestingHead(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerGameMode gameMode = (ServerPlayerGameMode) (Object) this;
        ServerPlayer player = ((ServerPlayerGameModeAccessor) gameMode).icpm$getPlayer();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockState state = level.getBlockState(blockPos);
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
            return;
        }
        int lvl = ICPMEnchantEffects.level(player.level(), player.getMainHandItem(), "harvesting");
        // (a) R196 ItemHoe.java:108-114（ItemMattock / ItemScythe 同款）：收获成熟作物时，
        //     以 getEnchantmentLevelFraction(fertility) = 等级/5 的概率把下方耕地重设为已施肥。
        int fertLvl = ICPMEnchantEffects.level(level, player.getMainHandItem(), "fertility");
        if (fertLvl > 0 && player.getRandom().nextFloat() < (float) fertLvl / 5.0f) {
            ICPMFarmlandFertility.add(level.dimension(), blockPos.below(), 1);
        }
        // (b) 收获附魔额外掉落
        if (lvl <= 0) {
            return;
        }
        // 注：R196 收获**不消耗**耕地肥力 —— 唯一的消耗点是 BlockCrops.java:124-125
        //（作物生长成功时 1/256 概率取消施肥），故此处不再扣肥力。
        int extra = 1 + player.getRandom().nextInt(lvl + 1);
        ItemStack drop = new ItemStack(((CropBlockAccessor) crop).icpm$getBaseSeedId(), extra);
        double x = blockPos.getX() + 0.5;
        double y = blockPos.getY() + 0.5;
        double z = blockPos.getZ() + 0.5;
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, drop);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }
}
