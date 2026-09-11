package name.icpm.mixin;

import name.icpm.entity.monster.GelatinousCubeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 史莱姆/果冻腐蚀 —— R196 忠实移植（EntityGelatinousCube#405 / EntityCubic#361,381 /
 * InventoryPlayer#878,896 / Item#1707）。
 *
 * <pre>
 * 类型倍率 M：绿史莱姆 1 / 黄果冻 2 / 红团块 3 / 灰软泥 3 / 黑布丁 4
 * 1) 你近战打到史莱姆 → 腐蚀手持物：每次扣耐久 100 × M
 * 2) 史莱姆打到你：
 *    - 主背包每件触发概率 0.05 × S（S=史莱姆尺寸）
 *    - 护甲每件触发概率 0.25 × S
 *    - 触发后工具扣 100 × S × M；护甲扣 2 × S × M
 * </pre>
 */
@Mixin(LivingEntity.class)
public abstract class SlimeCorrosionMixin {

    @Unique
    private static int typeMultiplier(LivingEntity cube) {
        if (cube instanceof GelatinousCubeEntity g) {
            return g.getAttackStrengthMultiplier();
        }
        return 1; // vanilla 绿史莱姆
    }

    @Unique
    private static boolean damageable(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem();
    }

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void icpm$slimeCorrosion(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity victim = (LivingEntity) (Object) this;
        if (level.isClientSide()) {
            return;
        }
        // ===== 1) 近战打史莱姆 → 腐蚀手持物 =====
        if (victim instanceof Slime slimeVictim && source.getDirectEntity() instanceof Player attacker) {
            ItemStack held = attacker.getMainHandItem();
            if (damageable(held)) {
                held.hurtAndBreak(100 * typeMultiplier(victim), attacker, EquipmentSlot.MAINHAND);
            }
            return;
        }
        // ===== 2) 史莱姆打到你 → 腐蚀背包与护甲 =====
        if (victim instanceof Player player
                && source.getDirectEntity() instanceof Slime slime) {
            int size = slime.getSize();
            int m = typeMultiplier(slime);
            if (size <= 0) {
                return;
            }
            Inventory inv = player.getInventory();
            // 主背包（0..35）：每件 0.05×S 概率，触发扣 100×S×M
            for (int i = 0; i < 36; i++) {
                ItemStack stack = inv.getItem(i);
                if (!damageable(stack)) {
                    continue;
                }
                if (level.random.nextFloat() < 0.05f * size) {
                    stack.hurtAndBreak(100 * size * m, player, EquipmentSlot.MAINHAND);
                }
            }
            // 护甲：每件 0.25×S 概率，触发扣 2×S×M
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!damageable(stack)) {
                    continue;
                }
                if (level.random.nextFloat() < 0.25f * size) {
                    stack.hurtAndBreak(2 * size * m, player, slot);
                }
            }
        }
    }
}
