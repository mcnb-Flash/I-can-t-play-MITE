package name.icpm.mixin;

import name.icpm.item.ICPMSilverArmor;
import net.minecraft.world.entity.LivingEntity;
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
 * ICPM 银制工具特殊效果 Mixin（R196 对齐）
 *
 * 银制工具对亡灵生物有 +2.5 亡灵杀手 I 等价伤害（R196：银武器对亡灵/影系增伤，
 * EntityPlayer#attackEntityAsMob 中 held_item.hasMaterial(silver) 且 target isEntityUndead
 * → damage ×1.25；此处用与 1.21 Smite I 一致的 +2.5 绝对加成近似）。
 *
 * 注：旧实现「银盔甲对亡灵攻击 5%/件 减伤」并非 R196（R196 银甲是对毒时长/吸血吸取/影
 * 减伤，见 SilverPoisonResistR196Mixin 等），已按 R196 移除。
 */
@Mixin(LivingEntity.class)
public class ICPMSilverCombatMixin {

    /**
     * 亡灵杀手 I 提供的额外伤害（与原版 1.21.x Smite I 一致：每级 +2.5 伤害）
     */
    @Unique
    private static final float SICPM_I_DAMAGE_BONUS = 2.5f;

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

        // 银制工具对亡灵生物的强制 Smite I 伤害加成（R196 银武器 vs 亡灵）
        args.set(2, icpm$applySilverSmiteBonus(amount, source, self));
    }

    /**
     * 当攻击者使用银制工具攻击亡灵生物时，附加 +2.5 伤害（Smite I 等价效果）
     */
    @Unique
    private float icpm$applySilverSmiteBonus(float amount, DamageSource source, LivingEntity victim) {
        // 攻击者必须是 LivingEntity
        var attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return amount;
        }

        // 攻击者主手必须是银制工具
        ItemStack heldItem = attacker.getMainHandItem();
        if (heldItem.isEmpty()) {
            return amount;
        }
        Item held = heldItem.getItem();
        if (!isSilverTool(held)) {
            return amount;
        }

        // 目标必须是亡灵生物
        if (!isUndead(victim)) {
            return amount;
        }

        return amount + SICPM_I_DAMAGE_BONUS;
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
