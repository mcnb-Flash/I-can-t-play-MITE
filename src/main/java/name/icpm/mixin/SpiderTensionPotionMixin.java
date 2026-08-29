package name.icpm.mixin;

import name.icpm.common.ICPMTension;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * G3：蜘蛛药水概率吃张力（R196 EntitySpider.onSpawnWithEgg + SpiderEffectsGroupData 忠实移植）。
 *
 * <p>R196 原文（EntitySpider.java 第 45-53 行）：
 * <pre>
 *   if (par1EntityLivingData1 == null) {
 *       par1EntityLivingData1 = new SpiderEffectsGroupData();
 *       if (worldObj.difficultySetting &gt; 2 &amp;&amp; worldObj.rand.nextFloat() &lt; 0.1f * getLocationTensionFactor(...)) {
 *           ((SpiderEffectsGroupData)par1EntityLivingData1).func_111104_a(rand);
 *       }
 *   }
 *   ... addPotionEffect(new PotionEffect(effect_id, Integer.MAX_VALUE))
 * </pre>
 * 药水四选一（SpiderEffectsGroupData）：速度 / 力量 / 再生 / 隐身，无限时长。
 * 仅困难难度（difficultySetting &gt; 2 对应 1.21.11 的 Difficulty.HARD），概率 = 10% × 张力。
 *
 * <p>注入 {@link Spider#finalizeSpawn} TAIL：原版 Spider 与 ICPM 全部蜘蛛变体
 * （{@code ICPMSpiderVariant} 子类）都会经由 {@code Spider.finalizeSpawn}，故一次注入全覆盖。
 */
@Mixin(Spider.class)
public class SpiderTensionPotionMixin {

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void icpm$tensionPotion(ServerLevelAccessor level, DifficultyInstance difficulty,
                                    EntitySpawnReason reason, SpawnGroupData spawnData,
                                    CallbackInfoReturnable<SpawnGroupData> cir) {
        Spider self = (Spider) (Object) this;
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        // R196：仅困难难度（difficultySetting > 2）
        if (sl.getDifficulty() != Difficulty.HARD) {
            return;
        }
        // R196：概率 = 10% × 张力因子
        float tension = ICPMTension.getTension(sl, self.blockPosition());
        if (self.getRandom().nextFloat() >= 0.10f * tension) {
            return;
        }
        // R196 SpiderEffectsGroupData.func_111104_a：0-3 → 速度/力量/再生/隐身（无限时长）
        // 1.21.11 命名：MobEffects.SPEED(=移动速度)/STRENGTH(=力量)，均为 Holder<MobEffect>
        Holder<MobEffect> effect;
        int roll = self.getRandom().nextInt(4);
        if (roll == 0) {
            effect = MobEffects.SPEED;
        } else if (roll == 1) {
            effect = MobEffects.STRENGTH;
        } else if (roll == 2) {
            effect = MobEffects.REGENERATION;
        } else {
            effect = MobEffects.INVISIBILITY;
        }
        self.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0));
    }
}
