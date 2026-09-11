package name.icpm;

import net.neoforged.fml.common.EventBusSubscriber;

import name.icpm.entity.LivestockState;
import name.icpm.network.InventoryCraftSyncPacket;
import name.icpm.network.NutritionSyncPacket;
import name.icpm.network.WorkbenchCraftPacket;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * ICPM 服务端游戏事件（NeoForge.EVENT_BUS 订阅）。
 *
 * 内容对齐原 Fabric ICPM.onInitialize 中挂载的事件（玩家加入/重生、死亡经验下限捕获、
 * 牲畜惊吓与僵尸变聪明、骷髅免疫仙人掌、世界加载 lava 规则、方块破坏清理、全部 per-tick
 * 引擎、月相/季节广播）与其它 Fabric 入口事件（指令注册、实体属性/生成放置、地下基岩区块）。
 */
@EventBusSubscriber(modid = ICPM.MOD_ID)  // NeoForge 21.11 默认即 GAME bus
public final class ICPMEventHandlers {

    private static long lastAnnouncedMoonDay = -1;
    private static name.icpm.common.ICPMSeason.Season lastAnnouncedSeason = null;

    private ICPMEventHandlers() {
    }

    // ==================== 指令 ====================

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        name.icpm.common.ICPMCommands.registerCommands(event.getDispatcher());
    }

    // ==================== 玩家加入 / 重生 ====================

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            name.icpm.common.PlayerStatsManager.updatePlayerStats(player);
            name.icpm.common.PlayerNutritionManager.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }
        java.util.UUID uuid = newPlayer.getUUID();
        int floor = name.icpm.common.ICPMExperience.getRespawnFloor(uuid);
        ICPM.LOGGER.info("[ICPM] RESPAWN: uuid={} floor={}", uuid, floor);
        if (floor < 0) {
            name.icpm.common.ICPMExperience.applyRespawnExperience(newPlayer, floor);
        }
        int penalty = name.icpm.common.ICPMExperience.getDeathPenalty(uuid);
        name.icpm.common.ICPMExperience.clearDeathState(uuid);
        name.icpm.common.PlayerStatsManager.updatePlayerStats(newPlayer);
        name.icpm.common.PlayerNutritionManager.onPlayerJoin(newPlayer);
        if (penalty > 0) {
            newPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c你死亡损失了 " + penalty + " 点经验（负等级惩罚）"));
        }
    }

    // ==================== 死亡 / 伤害 ====================

    /** 死亡捕获（R196 重生下限）：捕获玩家死亡前经验（death floor 持久化由原版 NBT 负责）。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            java.util.UUID uuid = player.getUUID();
            int cur = player.totalExperience;
            int prevFloor = name.icpm.common.ICPMExperience.getRespawnFloor(uuid);
            boolean keepInventory = java.lang.Boolean.TRUE.equals(
                    player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY));
            int newFloor = name.icpm.common.ICPMExperience.recordDeath(uuid, cur, prevFloor, !keepInventory);
            ICPM.LOGGER.info("[ICPM] DEATH: uuid={} cur={} prevFloor={} newFloor={} keepInventory={}",
                    uuid, cur, prevFloor, newFloor, keepInventory);
        }
    }

    /** 受击后（Pre 已放行、伤害真实生效时）牲畜惊吓 / 僵尸变聪明。 */
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        var entity = event.getEntity();
        if (entity == null) {
            return;
        }
        // 牲畜受攻击惊吓（R196 attackEntityFrom → considerFleeing）
        if (entity instanceof Animal animal && LivestockState.isLivestock(animal)) {
            LivestockState.get(animal).spook(entity.level().getGameTime() + 400L + (long) animal.getRandom().nextInt(400));
        }
        // 僵尸受玩家伤害后变聪明（R196 attackEntityFrom 中 is_smart = true）
        if (entity instanceof net.minecraft.world.entity.monster.zombie.Zombie zombie
                && event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.player.Player) {
            name.icpm.entity.ai.ZombieMiteState.get(zombie).smart = true;
        }
    }

    /** 骷髅免疫仙人掌伤害（R196 canBeDamagedByCacti=false）。 */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        var entity = event.getEntity();
        if (entity instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton
                && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.CACTUS)) {
            // NeoForge 21.11 LivingDamageEvent.Pre 不可 cancel；置伤害为 0 等效免疫
            event.setNewDamage(0.0f);
        }
    }

    // ==================== 世界加载 / 卸载 ====================

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        var server = level.getServer();
        // 新世界/世界加载时自动开启 lava_source_conversion
        if (!java.lang.Boolean.TRUE.equals(
                level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.LAVA_SOURCE_CONVERSION))) {
            var source = server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
            var parse = server.getCommands().getDispatcher()
                    .parse("gamerule lava_source_conversion true", source);
            server.getCommands().performCommand(parse, "gamerule lava_source_conversion true");
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            name.icpm.common.ICPMPlantDisease.clearDimension(level.dimension());
            name.icpm.common.ICPMFarmlandFertility.clearDimension(level.dimension());
        }
    }

    // ==================== 方块破坏清理 ====================

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        var level = event.getLevel();
        var pos = event.getPos();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        name.icpm.common.ICPMPlantDisease.onBlockRemoved(serverLevel.dimension(), pos);
        if (!serverLevel.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
            name.icpm.common.ICPMFarmlandFertility.onBlockRemoved(serverLevel.dimension(), pos);
        }
    }

    // ==================== 区块加载：地下世界底层基岩 ====================

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                && level.dimension() == name.icpm.common.ICPMPortalHandler.UNDERWORLD_KEY
                && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk) {
            name.icpm.world.ICPMUnderworldBedrock.onChunkLoad(level, chunk);
        }
    }

    // ==================== 每服务端 tick：各引擎 + 月相/季节广播 ====================

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        // 世界天数刷新（村庄 60 天 mixin 读取）
        name.icpm.common.ICPMWorldDay.updateFromServer(server);
        // 女巫诅咒 pending realize
        name.icpm.curse.ICPMCurseManager.onServerTick(server);
        // 女巫召狼
        name.icpm.curse.WitchSummonManager.onServerTick(server);
        // 胰岛素抵抗自然代谢
        name.icpm.common.ICPMInsulinResistance.onServerTick(server);
        // 火焰烧肉
        name.icpm.common.BurningCookingHandler.onServerTick(server);
        // R196 桶沉降
        name.icpm.item.ICPMBucketRules.onServerTick(server);
        // 世界恶意（噩梦时代夜锁 + 技术不佳）
        name.icpm.common.ICPMWorldEvil.onServerTick(server);
        // 月相 + 季节广播（血月/蓝月/丰收月）
        server.getAllLevels().forEach(level -> {
            if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
                return;
            }
            long dayTime = level.getDayTime();
            long day = dayTime / 24000L + 1L;
            if (day != lastAnnouncedMoonDay) {
                lastAnnouncedMoonDay = day;
                if (name.icpm.common.ICPMMoonPhase.isBloodMoon(dayTime)) {
                    broadcast(server, "message.icpm.blood_moon", "§c血月降临！今夜怪物狂暴，作物将大量染病，无法入睡！");
                } else if (name.icpm.common.ICPMMoonPhase.isBlueMoon(dayTime)) {
                    broadcast(server, "message.icpm.blue_moon", "§9蓝月升起！动物重新繁衍，钓鱼与作物加速。");
                } else if (name.icpm.common.ICPMMoonPhase.isHarvestMoon(dayTime)) {
                    broadcast(server, "message.icpm.harvest_moon", "§6丰收之月！作物生长加速。");
                }
            }
            name.icpm.common.ICPMSeason.Season season = name.icpm.common.ICPMSeason.getSeason(dayTime);
            if (season != lastAnnouncedSeason) {
                lastAnnouncedSeason = season;
                broadcast(server, name.icpm.common.ICPMSeason.messageKey(season),
                        switch (season) {
                            case SPRING -> "§a春天来了！作物加速生长。";
                            case SUMMER -> "§e盛夏已至，炎热干燥。";
                            case AUTUMN -> "§6秋收时节！作物大幅加速生长。";
                            default -> "§f寒冬降临，作物停止生长。";
                        });
            }
        });
    }

    private static void broadcast(net.minecraft.server.MinecraftServer server, String key, String fallback) {
        server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.translatable(key), false);
    }
}
