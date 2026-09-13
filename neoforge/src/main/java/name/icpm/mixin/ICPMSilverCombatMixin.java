package name.icpm.mixin;

import name.icpm.item.ICPMSilverArmor;
import name.icpm.item.ICPMToolProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * ICPM 银制工具 / 钝器特攻 Mixin（R196 对齐）
 *
 * R196 `EntityPlayer.calcRawMeleeDamageVs`（EntityPlayer.java:1151-1187）：
 * <pre>
 *   if (target.isEntityUndead() &amp;&amp; held_item.hasMaterial(Material.silver)) damage *= 1.25f;   // :1174-1176
 *   if (target instanceof EntitySkeleton &amp;&amp; (held instanceof ItemCudgel || ItemWarHammer)) damage *= 2.0f; // :1177-1179
 * </pre>
 *
 * 旧实现把银系用「+2.5 平加（Smite I 等价）」近似，与 R196 的 ×1.25 乘区不等价，已改为乘区；
 * 并补上 R196 的「短棍 / 战锤 vs 骷髅 ×2.0」。
 *
 * 注：旧实现「银盔甲对亡灵攻击 5%/件 减伤」并非 R196（R196 银甲是对毒时长/吸血吸取/影
 * 减伤，见 SilverPoisonResistR196Mixin 等），已按 R196 移除。
 */
@Mixin(LivingEntity.class)
public class ICPMSilverCombatMixin {

    /** R196：银质武器对亡灵 ×1.25 */
    @Unique
    private static final float SICPM_SILVER_UNDEAD_MULTIPLIER = 1.25f;

    /** R196：短棍 / 战锤对骷髅 ×2.0 */
    @Unique
    private static final float SICPM_BLUNT_SKELETON_MULTIPLIER = 2.0f;

    /**
     * 在 hurtServer 内部调用 actuallyHurt 的时机修改最终伤害（index 2）。
     *
     * 关键：@ModifyArgs 只能拦截方法体内部的「方法调用(INVOKE)」，不能修改方法自身参数。
     * hurtServer(ServerLevel, DamageSource, float) → actuallyHurt(ServerLevel, DamageSource, float)
     * hurtServer 内 actuallyHurt 有两处调用（冷却分支 / 普通分支），都会命中本拦截，效果一致。
     * args [0]=level [1]=source [2]=amount
     */
    @ModifyArgs(method = "hurtServer", expect = -1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void icpm$modifyHurtAmount(Args args) {
        LivingEntity self = (LivingEntity) (Object) this;
        // hurtServer 是服务器端专属方法，无需客户端判断
        DamageSource source = (DamageSource) args.get(1);
        if (source == null) {
            return;
        }
        float amount = (float) args.get(2);

        // 银制工具对亡灵生物的 ×1.25 / 短棍战锤对骷髅 ×2.0（R196 乘区）
        args.set(2, icpm$applyR196WeaponBonuses(amount, source, self));
    }

    /**
     * R196 近战乘区（`EntityPlayer.calcRawMeleeDamageVs` 尾部，仅玩家近战生效）：
     * ① 银质武器 vs 亡灵 → ×1.25（:1174-1176）；
     * ② 短棍 / 战锤 vs 骷髅 → ×2.0（:1177-1179）；
     * ③ 悬液近战削弱：`suspended_in_liquid && damage > 1 → 1 + (damage-1)×0.5`（:1182-1184）。
     * ①②可叠乘；③最后应用。近战判定用 `directEntity == entity`（箭矢的直击实体是弹射物，故不受①②影响）。
     */
    @Unique
    private float icpm$applyR196WeaponBonuses(float amount, DamageSource source, LivingEntity victim) {
        var attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof Player attacker)) {
            return amount;
        }
        // 仅近战：箭矢/投掷物 的 directEntity 是弹射物本身
        if (source.getDirectEntity() != attacker) {
            return amount;
        }

        ItemStack heldItem = attacker.getMainHandItem();
        float result = amount;

        if (!heldItem.isEmpty()) {
            // ① 银质武器 vs 亡灵 ×1.25
            if (isSilverTool(heldItem.getItem()) && isUndead(victim)) {
                result *= SICPM_SILVER_UNDEAD_MULTIPLIER;
            }
            // ② 短棍 / 战锤 vs 骷髅（含 ICPM 骷髅变种，均继承原版 Skeleton） ×2.0
            if (victim instanceof Skeleton && isBluntWeapon(heldItem)) {
                result *= SICPM_BLUNT_SKELETON_MULTIPLIER;
            }
        }

        // ③ 悬液近战削弱（R196 Entity.isSuspendedInLiquid：非骑乘、离地、在水/熔岩中）
        if (result > 1.0f && icpm$isSuspendedInLiquid(attacker)) {
            result = 1.0f + (result - 1.0f) * 0.5f;
        }

        return result;
    }

    /** 近似 R196 `Entity.isSuspendedInLiquid()`：非骑乘 + 未着地 + 处于水/熔岩中。 */
    @Unique
    private boolean icpm$isSuspendedInLiquid(Player player) {
        if (player.isPassenger() || player.onGround()) {
            return false;
        }
        return player.isInWater() || player.isInLava();
    }

    /**
     * 是否为 R196 的「钝器」：ItemCudgel（木短棒）或 ItemWarHammer（各材质战锤）。
     */
    @Unique
    private boolean isBluntWeapon(ItemStack stack) {
        ICPMToolProperties.ToolCategory category = ICPMToolProperties.getToolCategory(stack);
        return category == ICPMToolProperties.ToolCategory.CUDGEL
                || category == ICPMToolProperties.ToolCategory.CLUB
                || category == ICPMToolProperties.ToolCategory.WAR_HAMMER;
    }

    /**
     * 判断实体是否为亡灵生物（原版 EntityTypeTags.UNDEAD）
     */
    @Unique
    private boolean isUndead(LivingEntity entity) {
        return entity.getType().is(EntityTypeTags.UNDEAD);
    }

    /**
     * 判断物品是否为银制工具
     */
    @Unique
    private boolean isSilverTool(Item item) {
        return ICPMSilverArmor.isSilverTool(item);
    }
}
