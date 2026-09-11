package name.icpm.mixin;

import name.icpm.curse.CurseFoods;
import name.icpm.curse.ICPMCurse;
import name.icpm.curse.ICPMCurseManager;
import name.icpm.ICPM;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 诅咒——玩家被动效果（R196 EntityPlayer 各接入点）：
 * <ul>
 *   <li>{@code canEat}：厌食动物 / 厌食植物 / 禁饮（食物级饮品）—— R196 isIngestionForbiddenByCurse；</li>
 *   <li>每 tick：无法屏息（空气钳制 90，R196 EntityPlayer.setAir）；</li>
 *   <li>每 10 tick：植物缠绕（脚下藤蔓/可替换植物减速，R196 EntityLivingBase.moveEntity 乘算）；</li>
 *   <li>每 40 tick：末影人敌视（附近末影人 1/3 概率锁定玩家，R196 EntityEnderman）。</li>
 * </ul>
 */
@Mixin(Player.class)
public abstract class CursePassiveMixin {

    private static final TagKey<net.minecraft.world.level.block.Block> REPLACEABLE_PLANTS =
            TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:replaceable_plants"));

    /** R196 cannot_hold_breath：setAir 上限 90。 */
    private static final int BREATH_CAP = 90;

    @Inject(method = "canEat", at = @At("RETURN"), cancellable = true)
    private void icpm$curseForbidFood(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (cir.getReturnValue() == Boolean.FALSE) {
            return;
        }
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            hand = player.getOffhandItem();
        }
        if (hand.isEmpty()) {
            return;
        }
        // R196 isIngestionForbiddenByCurse：动物源/植物源/饮品分类
        if (ICPMCurseManager.isCursed(player, ICPMCurse.CANNOT_EAT_ANIMALS, true)
                && CurseFoods.isAnimalFood(hand)) {
            cir.setReturnValue(false);
        } else if (ICPMCurseManager.isCursed(player, ICPMCurse.CANNOT_EAT_PLANTS, true)
                && CurseFoods.isPlantFood(hand)) {
            cir.setReturnValue(false);
        } else if (ICPMCurseManager.isCursed(player, ICPMCurse.CANNOT_DRINK, true)
                && CurseFoods.isDrink(hand)) {
            // 奶桶/汤等直接喝的食物级饮品在此拦（药水走 Item.use 另拦）
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void icpm$cursePassiveTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (player.tickCount % 5 == 0) {
            // 无法屏息：空气不可能超过 90（R196 setAir 上限）
            if (ICPMCurseManager.isCursed(player, ICPMCurse.CANNOT_HOLD_BREATH)
                    && player.getAirSupply() > BREATH_CAP) {
                player.setAirSupply(BREATH_CAP);
            }
        }
        if (player.tickCount % 10 == 0 && player.onGround()) {
            if (ICPMCurseManager.isCursed(player, ICPMCurse.ENTANGLEMENT, true)) {
                BlockState feet = serverLevel.getBlockState(player.blockPosition());
                BlockState below = serverLevel.getBlockState(player.blockPosition().below());
                boolean vine = feet.is(Blocks.VINE) || below.is(Blocks.VINE);
                boolean plants = feet.is(REPLACEABLE_PLANTS) || below.is(REPLACEABLE_PLANTS);
                if (vine || plants) {
                    int amp = vine ? 5 : 3; // R196 藤蔓 ×0.2 / 植物 ×0.4
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, amp, true, false, false));
                }
            }
        }
        if (player.tickCount % 40 == 0
                && ICPMCurseManager.isCursed(player, ICPMCurse.ENDERMEN_AGGRO, true)) {
            BlockPos pos = player.blockPosition();
            AABB box = new AABB(pos).inflate(16.0);
            for (EnderMan ender : serverLevel.getEntitiesOfClass(EnderMan.class, box)) {
                if (ender.isAlive() && ender.getTarget() == null
                        && ender.getRandom().nextInt(3) == 0) {
                    ender.setTarget(player);
                }
            }
        }
    }
}
