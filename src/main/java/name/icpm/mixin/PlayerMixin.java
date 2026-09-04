package name.icpm.mixin;

import name.icpm.ICPM;
import name.icpm.common.ICPMExperience;
import name.icpm.common.ICPMFoodStats;
import name.icpm.common.ICPMHealProgressManager;
import name.icpm.common.PlayerNutritionManager;
import name.icpm.common.ICPMInsulinResistance;
import name.icpm.common.PlayerStatsManager;
import name.icpm.common.PortalPositionStorage;
import name.icpm.item.ICPMBuckets;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 玩家属性修改：
 * 1. 饥饿值 > 0 时允许疾跑
 * 2. 根据经验等级调整血量/饱食度上限
 * 3. 自定义血量恢复机制（每64秒1点，睡觉时8倍）
 * 4. 饱食度自动消耗（每半个游戏日1点）
 * 5. 构造器/经验变化时更新属性
 * 6. 饱和度满时拒绝食用普通食物（金苹果等例外）
 */
@Mixin(Player.class)
public class PlayerMixin {
    @Unique
    private int icpm$lastCheckedLevel = -1;
    @Unique
    private int icpm$nutritionWarnCounter = 0;

    /**
     * R196 重生经验下限（respawn_experience），持久化到玩家 NBT("icpm_respawn_experience")。
     * 死亡时按 ICPMExperience.computeRespawnFloor 更新；重生时玩家经验被重置为该值。
     */
    @Unique
    private int icpm$respawnExperience = 0;

    /**
     * 覆盖原版经验流入：所有给玩家经验的地方（杀怪、破坏方块、烧炼、经验瓶等）
     * 都会调用 giveExperiencePoints，统一汇入 ICPM 带符号经验体系（存储于 totalExperience）。
     */
    @Overwrite
    public void giveExperiencePoints(int amount) {
        ICPMExperience.addExperience((Player) (Object) this, amount);
    }

    /**
     * 覆盖原版 giveExperienceLevels：改为"给予相当于 n 级所需的经验"（支持负 n 降级）。
     */
    @Overwrite
    public void giveExperienceLevels(int levels) {
        ICPMExperience.addExperienceLevels((Player) (Object) this, levels);
    }

    /**
 * 读档后：totalExperience 已由原版从 NBT(XpTotal) 恢复，这里按 ICPM 曲线重算等级/进度
 * 并同步显示字段与属性上限（icpm 等级体系与原版不同，必须重新换算）。
 * 同时恢复持久化的重生经验下限字段（供「重启后、本次会话首死」当 prevFloor 兜底）。
 * 重生经验下限的落地由 ServerPlayerEvents.AFTER_RESPAWN 完成（Fabric 保证在重生流程末尾触发，
 * 原版所有同步/清零逻辑之后，对时序免疫）。
 */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void icpm$readExperience(ValueInput tag, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        int floor = tag.getInt("icpm_respawn_experience").orElse(0);
        this.icpm$respawnExperience = floor;
        // 把持久化下限灌进静态表：使「重启后首死」也能拿到正确的 prevFloor，
        // 无需再依赖本实体 @Unique 字段（重生新建实体该字段会重置为 0）。
        ICPMExperience.setRespawnFloor(player.getUUID(), floor);
        ICPMExperience.syncToVanilla(player, player.totalExperience);
        PlayerStatsManager.updatePlayerStats(player);
    }

    /**
     * 写档：持久化重生经验下限，重启后仍能保留负等级惩罚态。
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void icpm$writeExperience(ValueOutput tag, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        int floor = ICPMExperience.getRespawnFloor(player.getUUID());
        if (floor == 0) {
            floor = this.icpm$respawnExperience;
        }
        tag.putInt("icpm_respawn_experience", floor);
    }

    /**
     * ICPM 玩家数据（营养值 / 传送门记忆）随玩家自身 NBT 持久化。
     *
     * 旧实现曾在 DISCONNECT 时直接读写 playerdata/<uuid>.dat，而原版 PlayerList.save 在停服时
     * 会再次覆盖同一文件，导致 ICPM 写入的数据被冲掉（旧存档玩家数据"卡掉"），且单独的
     * 文件 I/O 在退出保存时增加不确定性。改为挂在玩家 addAdditionalSaveData /
     * readAdditionalSaveData 上，随原版玩家保存一起落盘，彻底消除覆盖与竞态。
     * 旧存档若仍保留了 playerdata 文件里的旧数据，load 时会自动迁移一次。
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void icpm$readIcpmPlayerData(ValueInput tag, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        PlayerNutritionManager.load(player, tag);
        ICPMFoodStats.load(player, tag);
        PortalPositionStorage.load(player, tag);
        ICPMInsulinResistance.load(player, tag);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void icpm$writeIcpmPlayerData(ValueOutput tag, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        PlayerNutritionManager.save(player, tag);
        ICPMFoodStats.save(player, tag);
        PortalPositionStorage.save(player, tag);
        ICPMInsulinResistance.save(player, tag);
    }

    /**
     * 构造器注入：玩家创建时设置正确的初始属性
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void icpm$onConstruct(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }
        // 更新血量/饱食度上限
        PlayerStatsManager.updatePlayerStats(player);
        // 重置饱食度到初始值
        FoodData foodData = player.getFoodData();
        int maxFood = PlayerStatsManager.calculateMaxFood(player.experienceLevel);
        foodData.setFoodLevel(maxFood);
        // 同步 ICPM 带符号经验到原版显示字段（新建玩家总经验默认为 0）
        ICPMExperience.syncToVanilla(player, player.totalExperience);
    }

    /**
     * 拦截进食判断：
     * - 如果 ignoreHunger=true（金苹果等特殊物品），直接放行
     * - 如果 ignoreHunger=false（普通食物），检查饱食度/饱和度
     * - 饱食度满且饱和度满时，拒绝进食
     * 
     * 注意：在 1.21.11 中，方法名可能为 canEat 或类似。
     * 如果方法不存在，此注入会被忽略（编译通过但运行时不生效）。
     */
    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true, require = 0)
    private void icpm$onCanEat(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        // 金苹果等特殊物品（ignoreHunger=true）不受限制
        if (!ignoreHunger && !PlayerStatsManager.canEat(player)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 在 vanilla tick 之前钳制饱食度，防止原版自然回血触发。
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void icpm$preTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide()) {
            return;
        }

        // 每 tick 钳制饱食度到上限
        FoodData foodData = player.getFoodData();
        int maxFood = PlayerStatsManager.calculateMaxFood(player.experienceLevel);
        if (foodData.getFoodLevel() > maxFood) {
            foodData.setFoodLevel(maxFood);
        }

        // 营养不良效果施加/移除：蛋白或植物营养素归零时附加 MALNUTRITION（每 tick 刷新 600 tick=30s 模拟无限）
        // 效果本身不施加惩罚（惩罚在 icpm$tickHealthRegen / icpm$tickFoodDrain 中按 isMalnourished() 实现），
        // 仅作 HUD 图标/tooltip 显示。
        if (PlayerNutritionManager.getNutrition(player).isMalnourished()) {
            player.addEffect(new MobEffectInstance(ICPM.MALNUTRITION_HOLDER, 600, 0, true, true, true));
        } else if (player.hasEffect(ICPM.MALNUTRITION_HOLDER)) {
            player.removeEffect(ICPM.MALNUTRITION_HOLDER);
        }
    }

    /**
     * 饥饿值 > 0 时允许疾跑（ICPM 特性）
     */
    @Inject(method = "hasEnoughFoodToDoExhaustiveManoeuvres", at = @At("RETURN"), cancellable = true)
    private void icpm$allowSprintWhenHungry(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!cir.getReturnValue() && player.getFoodData().getFoodLevel() > 0) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 每 tick 检查（仅服务端执行）：
     * 1. 经验等级变化，更新属性上限
     * 2. 血量恢复（使用 ICPMHealProgressManager）
     * 3. 饱食度消耗
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void icpm$onTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (player.level().isClientSide()) {
            return;
        }

        // 检查生命值
        if (!player.isAlive()) {
            return;
        }

        int currentLevel = player.experienceLevel;

        // 经验等级变化时更新属性
        if (currentLevel != icpm$lastCheckedLevel) {
            icpm$lastCheckedLevel = currentLevel;
            PlayerStatsManager.updatePlayerStats(player);
        }

        // R196 饱食度系统（satiation/nutrition 双槽）：消耗 + 自然回血 + 饥饿伤害
        ICPMFoodStats.tick(player);

        // ICPM 营养值消耗逻辑
        PlayerNutritionManager.tickNutritionDrain(player);

        // ICPM 营养不良提示
        icpm$tickNutritionWarning(player);

        // R196：身上岩浆桶遇水冷却 → 石桶（tickPlayerInventory 的 steam_and_hiss）
        icpm$tickLavaBucketCooling(player);
    }

    /**
     * R196 忠实移植（EntityPlayerMP.tickPlayerInventory）：
     * 玩家身体浸入水中且背包带岩浆桶 → 整桶嘶嘶冷却成石桶（每个岩浆桶独立转换），
     * 节流每 8 tick 检查一次；仅服务端。
     */
    @Unique
    private void icpm$tickLavaBucketCooling(Player player) {
        if (!player.isAlive() || !player.isInWater()) {
            return;
        }
        if ((player.tickCount & 0b111) != 0) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean any = false;
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) {
                continue;
            }
            Item stone = ICPMBuckets.stoneBucketFromLavaBucket(slot.getItem());
            if (stone == null) {
                continue;
            }
            inventory.setItem(i, new ItemStack(stone, 1));
            any = true;
        }
        if (any) {
            ServerLevel serverLevel = (ServerLevel) player.level();
            var p = player.position();
            serverLevel.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 1.2, p.z, 6, 0.2, 0.4, 0.2, 0.02);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
    }

    /**
     * ICPM 营养不良提示（1.6.4 客户端 is_malnourished 标记）
     * 每 400 游戏刻（20 现实秒）在 actionbar 提示一次
     */
    @Unique
    private void icpm$tickNutritionWarning(Player player) {
        if (!PlayerNutritionManager.getNutrition(player).isMalnourished()) {
            icpm$nutritionWarnCounter = 0;
            return;
        }
        icpm$nutritionWarnCounter++;
        if (icpm$nutritionWarnCounter >= 400) {
            icpm$nutritionWarnCounter = 0;
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.icpm.malnourished"), true);
        }
    }
}
