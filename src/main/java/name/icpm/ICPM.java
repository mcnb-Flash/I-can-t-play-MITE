package name.icpm;

import name.icpm.block.ICPMBlockGroup;
import name.icpm.block.ICPMBlocks;
import name.icpm.component.CraftPreviewComponent;
import name.icpm.component.NutritionComponent;
import name.icpm.component.QualityComponent;
import name.icpm.entity.LivestockState;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.entity.animal.Animal;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import name.icpm.common.CraftingTimeHelper;
import name.icpm.common.EnumQuality;
import name.icpm.common.ICPMInventoryCraftingState;
import name.icpm.network.InventoryCraftSyncPacket;
import name.icpm.network.NutritionSyncPacket;
import name.icpm.network.WorkbenchCraftPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ICPM implements ModInitializer {
    public static final String MOD_ID = "icpm";

    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 品质数据组件类型
    public static final DataComponentType<QualityComponent> QUALITY_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("quality"),
            DataComponentType.<QualityComponent>builder()
                    .persistent(QualityComponent.CODEC)
                    .networkSynchronized(QualityComponent.STREAM_CODEC)
                    .build()
    );

    // 营养值数据组件类型（蛋白质 + 植物营养素）
    public static final DataComponentType<NutritionComponent> NUTRITION_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("nutrition"),
            DataComponentType.<NutritionComponent>builder()
                    .persistent(NutritionComponent.CODEC)
                    .networkSynchronized(NutritionComponent.STREAM_CODEC)
                    .build()
    );

    // 金属币分解返还经验标记（coin_xp 组件，取走时 CoinXpRefundMixin 读取返还）
    public static final DataComponentType<Integer> COIN_XP_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("coin_xp"),
            DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                    .build()
    );

    // 符文石变体标记（runestone_variant 组件，0..15 = Nul..Sanct，决定符文门 seed）
    public static final DataComponentType<Integer> RUNESTONE_VARIANT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("runestone_variant"),
            DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                    .build()
    );

    // 合成预览标记（craft_preview 组件，仅挂在工作台结果槽预览物品上，用于 tooltip 展示经验消耗/可切换品质）
    public static final DataComponentType<CraftPreviewComponent> CRAFT_PREVIEW_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("craft_preview"),
            DataComponentType.<CraftPreviewComponent>builder()
                    .persistent(CraftPreviewComponent.CODEC)
                    .networkSynchronized(CraftPreviewComponent.STREAM_CODEC)
                    .build()
    );

    // 装盾标记（shield_attached 组件，布尔）：工具在对应等级工作台与盾牌合成后获得，
    // 持此工具右键可格挡，格挡效果与 R196 相同（伤害减半 + 工具扣耐久）。盾牌仅消耗 25% 耐久并可继续用/再合成。
    public static final DataComponentType<Boolean> SHIELD_ATTACHED = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            id("shield_attached"),
            DataComponentType.<Boolean>builder()
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL)
                    .build()
    );

    // 金属砧菜单类型
    public static final MenuType<name.icpm.inventory.MetalAnvilMenu> METAL_ANVIL_MENU = Registry.register(
            BuiltInRegistries.MENU,
            id("metal_anvil"),
            new ExtendedScreenHandlerType<name.icpm.inventory.MetalAnvilMenu, net.minecraft.core.BlockPos>(
                    name.icpm.inventory.MetalAnvilMenu.Companion::create,
                    name.icpm.inventory.MetalAnvilMenu.Companion.streamCodec()
            )
    );

    // ICPM 工作台菜单类型
    public static final MenuType<name.icpm.inventory.ICPMWorkbenchMenu> ICPM_WORKBENCH_MENU = Registry.register(
            BuiltInRegistries.MENU,
            id("icpm_workbench"),
            new ExtendedScreenHandlerType<name.icpm.inventory.ICPMWorkbenchMenu, net.minecraft.core.BlockPos>(
                    name.icpm.inventory.ICPMWorkbenchMenu.Companion::create,
                    name.icpm.inventory.ICPMWorkbenchMenu.Companion.streamCodec()
            )
    );

    // 营养不良效果（图标自动从 assets/icpm/textures/mob_effect/malnutrition.png 加载）
    // 当蛋白或植物营养素归零时施加（PlayerMixin tick 检查），图标/提示与回血减半/饥饿消耗×4 配合。
    // 1.21.11 MobEffect 构造器签名变化/非公开，强制使用子类化。
    public static final MobEffect MALNUTRITION = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            id("malnutrition"),
            new MobEffect(MobEffectCategory.HARMFUL, 0x8B4513) {
                @Override
                public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
                    return true; // 每 tick 应用（用于持续检查并刷新）
                }
            }
    );
    public static final net.minecraft.core.Holder<MobEffect> MALNUTRITION_HOLDER = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MALNUTRITION);

    // 女巫诅咒（R196 Curse）：单一 MobEffect + amplifier 变体（curse.id-1）编码 16 类诅咒。
    // 玩家至多一个诅咒 → 效果槽天然唯一；检测统一走 ICPMCurseManager.isCursed。
    public static final MobEffect WITCH_CURSE = Registry.register(
            BuiltInRegistries.MOB_EFFECT,
            id("witch_curse"),
            new MobEffect(MobEffectCategory.HARMFUL, 0x4B0082) {
                @Override
                public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
                    return true;
                }
            }
    );
    public static final net.minecraft.core.Holder<MobEffect> WITCH_CURSE_HOLDER = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(WITCH_CURSE);

    // 存储注册名 - 用于重复注册
    private static final List<RegisteredBlock> REGISTERED_BLOCKS = new ArrayList<>();

    private static class RegisteredBlock {
        String name;
        Block block;
        BlockItem blockItem;

        RegisteredBlock(String name, Block block, BlockItem blockItem) {
            this.name = name;
            this.block = block;
            this.blockItem = blockItem;
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("ICPM mod loaded");

        // 启动卡死探测器（监控渲染线程 + 服务端线程栈稳定性；客户端 mixin 也会触发，此处确保专用服务器也启动）
        FreezeDetector.ensureStarted();

        // 注册营养值同步网络包
        PayloadTypeRegistry.playS2C().register(NutritionSyncPacket.TYPE, NutritionSyncPacket.CODEC);

        // 注册工作台合成操作网络包（C2S）
        PayloadTypeRegistry.playC2S().register(WorkbenchCraftPacket.TYPE, WorkbenchCraftPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WorkbenchCraftPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                if (player.containerMenu instanceof name.icpm.inventory.ICPMWorkbenchMenu workbenchMenu) {
                    switch (payload.action()) {
                        case START_CRAFT -> workbenchMenu.startCrafting(player);
                        case CYCLE_QUALITY -> workbenchMenu.cycleQuality(player);
                        case TAKE_RESULT -> workbenchMenu.takeResult(player);
                    }
                }
            });
        });

        // 注册金属砧命名框同步包（C2S）：客户端命名框输入 → 服务端 setItemName（R196 MC|ItemName）
        PayloadTypeRegistry.playC2S().register(name.icpm.network.AnvilRenamePacket.TYPE, name.icpm.network.AnvilRenamePacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(name.icpm.network.AnvilRenamePacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                if (player.containerMenu instanceof name.icpm.inventory.MetalAnvilMenu menu) {
                    menu.setItemName(payload.name());
                }
            });
        });

        // 注册背包合成进度同步网络包（S2C）
        // 注：背包 2x2 合成的「开始/取走」时间门控直接由服务端
        // ICPMInventoryCraftingMixin 在 InventoryMenu.clicked 上完成，
        // 不再走 C2S 网络包（避免客户端拦截鼠标点击的脆弱实现）。
        PayloadTypeRegistry.playS2C().register(InventoryCraftSyncPacket.TYPE, InventoryCraftSyncPacket.CODEC);

        // 初始化ICPM材质定义（按1.6.4-ICPM R196原版顺序）
        name.icpm.common.ICPMMaterials.registerAll();

        // 注册 ICPM 指令（/day /xp）
        name.icpm.common.ICPMCommands.init();

        // 注册成就系统代码触发器（R196 AchievementList 移植）
        name.icpm.common.ICPMAchievementTriggers.init();

        // 注册自定义配方序列化器（金属币分解等）
        name.icpm.recipe.ICPMRecipes.init();

        // 注册方块
        ICPMBlocks.init();
        registerAllBlocks();

        // 注册方块实体类型
        name.icpm.blockentity.ICPMBlockEntities.INSTANCE.init();

        // 注册物品 - 通过Kotlin object静态调用
        name.icpm.item.ICPMItems.INSTANCE.init();
        name.icpm.item.ICPMGelatinousItems.INSTANCE.init();
        name.icpm.item.ICPMEarthElementalItems.INSTANCE.init();
        name.icpm.item.ICPMMonsterSpawnEggs.INSTANCE.init();

        // 注册实体
        name.icpm.entity.ICPMEntities.INSTANCE.init();

        // 注册ICPM专属物品列表
        name.icpm.item.ICPMItemGroup.register();

        // 注册ICPM专属方块列表
        ICPMBlockGroup.register();

        // 注册 ICPM 矿石生成（data-driven，绑定 placed_feature 到主世界群系）
        name.icpm.world.ICPMOreGenerator.register();

        // 地下世界底层基岩山脉 / 地幔盆地生成（R196 忠实移植，逐列噪声二值豁口，替代 surface_rule 平滑梯度）
        name.icpm.world.ICPMUnderworldBedrock.register();

        // 玩家登录时更新属性并同步营养值到客户端。
        // 营养值 / 传送门记忆的加载已改由 PlayerMixin.readAdditionalSaveData（玩家自身 NBT）完成，
        // 不再单独读写 playerdata 文件，避免与原版玩家保存互相覆盖导致旧存档数据丢失。
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            name.icpm.common.PlayerStatsManager.updatePlayerStats(player);
            name.icpm.common.PlayerNutritionManager.onPlayerJoin(player);
        });

        // 死亡捕获（R196 重生下限）：用 ServerLivingEntityEvents.ALLOW_DEATH 在死亡处理最开头、
        // 经验被清零之前捕获玩家死亡前经验。这是服务端事件、零方法注入风险，且能拿到完整 XP，
        // 比在 mixin 上挂 die 注入（依赖沿父类解析 LivingEntity.die）更稳健。
        // 捕获到的下限由下方 AFTER_RESPAWN 落地到重生后的玩家。
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayer player) {
                java.util.UUID uuid = player.getUUID();
                int cur = player.totalExperience;
                // 死亡前下限：优先静态表（同会话内上一次死亡保留），否则由 readAdditionalSaveData
                // 从 NBT 恢复的持久化下限（重启后首死兜底）。
                int prevFloor = name.icpm.common.ICPMExperience.getRespawnFloor(uuid);
                // 开启「死亡不掉落」(keepInventory) 的玩家不施加负等级惩罚：applyPenalty=false
                // 时 computeRespawnFloor 返回 0，既不会把经验压到负下限、也不会弹惩罚提示。
                // 1.21.11 的 GameRules 已重构：KEEP_INVENTORY 为 GameRule 原始类型，getValue 经
                // get(GameRule) 以 Object 形式返回（布尔规则实际为 Boolean），用 Boolean.TRUE.equals 安全取值。
                boolean keepInventory = java.lang.Boolean.TRUE.equals(
                        player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY));
                int newFloor = name.icpm.common.ICPMExperience.recordDeath(uuid, cur, prevFloor, !keepInventory);
                LOGGER.info("[ICPM] ALLOW_DEATH: uuid={} cur={} prevFloor={} newFloor={} keepInventory={}", uuid, cur, prevFloor, newFloor, keepInventory);
            }
            return true;
        });

        // 牲畜受攻击惊吓（R196 attackEntityFrom → considerFleeing）：牲畜真实受伤时立即 spook，
        // 驱动 ICPMFleeWhenSpooked 目标使动物四散奔逃并向同伴传染。对齐 R196 在受伤方法里直接触发惊吓，
        // 比轮询 LivingEntity.hurtTime 上升沿更稳健（不会漏触发，且覆盖任意伤害来源）。
        // tickLogic 中的 hurtTime 上升沿检测保留为次级兜底。
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, originalAmount, blocked) -> {
            if (!blocked && entity instanceof Animal animal && LivestockState.isLivestock(animal)) {
                LivestockState.get(animal).spook(entity.level().getGameTime() + 400L + (long) animal.getRandom().nextInt(400));
            }
            // 僵尸受玩家伤害后变聪明（R196 attackEntityFrom 中 is_smart = true）。
            // 原 ZombieMiteMixin 的 hurt RETURN 注入迁移至此：hurt 声明于 LivingEntity 且 1.21.11
            // 签名已变（铁律：不要 inject hurt/hurtServer），改用 Fabric 事件更稳健。
            if (entity instanceof net.minecraft.world.entity.monster.zombie.Zombie zombie
                    && source.getDirectEntity() instanceof net.minecraft.world.entity.player.Player) {
                name.icpm.entity.ai.ZombieMiteState.get(zombie).smart = true;
            }
        });

        // 骷髅免疫仙人掌伤害（R196 EntitySkeleton.canBeDamagedByCacti()=false）：ALLOW_DAMAGE 返回 false。
        // 原 SkeletonFamilyMixin 的 hurt HEAD cancel 迁移至此（hurt 声明于 LivingEntity 且 1.21.11 签名已变，
        // 不用 mixin 注入，用 Fabric 事件覆盖原版 Skeleton 系与 ICPM 全部骷髅变体）。
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton
                    && source.is(net.minecraft.world.damagesource.DamageTypes.CACTUS)) {
                return false;
            }
            return true;
        });

        // 玩家重生时落地负等级惩罚 + 更新属性 + 损失提示。
        // 落地点：Fabric 的 AFTER_RESPAWN 在重生流程末尾（原版所有经验同步/死亡清零之后）触发，
        // 因此把 totalExperience 设为持久化的惩罚下限，之后不会被任何代码覆盖。
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            java.util.UUID uuid = newPlayer.getUUID();
            // 仅当存在负惩罚（floor < 0）时覆盖经验；floor==0 时原版死亡已清零，无需处理。
            int floor = name.icpm.common.ICPMExperience.getRespawnFloor(uuid);
            LOGGER.info("[ICPM] AFTER_RESPAWN: uuid={} floor={} alive={} oldTotal={} newTotalBefore={}", uuid, floor, alive, oldPlayer.totalExperience, newPlayer.totalExperience);
            if (floor < 0) {
                name.icpm.common.ICPMExperience.applyRespawnExperience(newPlayer, floor);
            }
            // 损失提示取自死亡时记录的经验差值（独立于下限应用）
            int penalty = name.icpm.common.ICPMExperience.getDeathPenalty(uuid);
            name.icpm.common.ICPMExperience.clearDeathState(uuid);
            name.icpm.common.PlayerStatsManager.updatePlayerStats(newPlayer);
            name.icpm.common.PlayerNutritionManager.onPlayerJoin(newPlayer);
            if (penalty > 0) {
                newPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c你死亡损失了 " + penalty + " 点经验（负等级惩罚）"));
            }
        });

        // 创建新存档 / 世界加载时自动开启 lava_source_conversion（使岩浆像旧版一样自然形成
        // source 方块，1.21 默认 false）。直接用命令设置（绕开 1.21 重构后不可直接引用的
        // gamerule value 类型名），仅在当前为 false 时执行，避免重复广播。
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!java.lang.Boolean.TRUE.equals(
                    world.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.LAVA_SOURCE_CONVERSION))) {
                var source = server.createCommandSourceStack().withLevel(world).withSuppressedOutput();
                var parse = server.getCommands().getDispatcher()
                        .parse("gamerule lava_source_conversion true", source);
                server.getCommands().performCommand(parse, "gamerule lava_source_conversion true");
            }
        });

        // 维度卸载时清理病害/肥力内存数据，避免泄漏
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            name.icpm.common.ICPMPlantDisease.clearDimension(world.dimension());
            name.icpm.common.ICPMFarmlandFertility.clearDimension(world.dimension());
        });

        // B3 方块被破坏时清理作物病害追踪（患病作物被挖掉后表中位置失效，需即时清理避免残留）
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            name.icpm.common.ICPMPlantDisease.onBlockRemoved(world.dimension(), pos);
            // 耕地被锄回泥土/破坏/被推动时，下方耕地肥力失效需即时清理（否则同坐标重新锄地会"复活"旧肥力）
            if (!world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
                name.icpm.common.ICPMFarmlandFertility.onBlockRemoved(world.dimension(), pos);
            }
        });

        // 女巫诅咒引擎：pending 诅咒到期 realize（R196 WorldServer.checkCurses）
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(
                name.icpm.curse.ICPMCurseManager::onServerTick);
        // 女巫召狼：被玩家打伤后的倒计时与刷新（R196 summonWolves）
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(
                name.icpm.curse.WitchSummonManager::onServerTick);

        // 月相机制：血月强制降雨 + 月相变化广播（R196 World.isBloodMoon/isBlueMoon/isHarvestMoon）
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getAllLevels().forEach(level -> {
                if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
                    return;
                }
                long dayTime = level.getDayTime();
                long day = dayTime / 24000L + 1L;
                // 注：血月降雨/雷暴改由 ICPMWeatherMixin.advanceWeatherCycle 注入统一驱动
                // （infx 移植：血月日从 tick 6000 起强制雷暴、全群系降雨，不再强制晴空）。
                // 月相变化每天广播一次
                if (day != lastAnnouncedMoonDay) {
                    lastAnnouncedMoonDay = day;
                    if (name.icpm.common.ICPMMoonPhase.isBloodMoon(dayTime)) {
                        broadcastMoonMessage(server, "message.icpm.blood_moon", "§c血月降临！今夜怪物狂暴，作物将大量染病，无法入睡！");
                    } else if (name.icpm.common.ICPMMoonPhase.isBlueMoon(dayTime)) {
                        broadcastMoonMessage(server, "message.icpm.blue_moon", "§9蓝月升起！动物重新繁衍，钓鱼与作物加速。");
                    } else if (name.icpm.common.ICPMMoonPhase.isHarvestMoon(dayTime)) {
                        broadcastMoonMessage(server, "message.icpm.harvest_moon", "§6丰收之月！作物生长加速。");
                    }
                }
                // 季节变化广播（春/夏/秋/冬，每季 32 天）
                name.icpm.common.ICPMSeason.Season season = name.icpm.common.ICPMSeason.getSeason(dayTime);
                if (season != lastAnnouncedSeason) {
                    lastAnnouncedSeason = season;
                    broadcastMoonMessage(server,
                            name.icpm.common.ICPMSeason.messageKey(season),
                            switch (season) {
                                case SPRING -> "§a春天来了！作物加速生长。";
                                case SUMMER -> "§e盛夏已至，炎热干燥。";
                                case AUTUMN -> "§6秋收时节！作物大幅加速生长。";
                                default -> "§f寒冬降临，作物停止生长。";
                            });
                }
            });
        });
    }

    /** 月相广播辅助 */
    private static long lastAnnouncedMoonDay = -1;

    /** 季节广播辅助 */
    private static name.icpm.common.ICPMSeason.Season lastAnnouncedSeason = null;

    private static void broadcastMoonMessage(net.minecraft.server.MinecraftServer server, String key, String fallback) {
        net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.translatable(key);
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    // ==================== 背包 2x2 合成时间机制 ====================

    /**
     * 开始背包合成：校验配方、计算合成时间并下发同步包。
     * 品质固定使用 AVERAGE（背包不循环品质）。
     */
    public static void startBackpackCraft(ServerPlayer player, AbstractContainerMenu invMenu) {
        UUID uuid = player.getUUID();
        if (ICPMInventoryCraftingState.isActive(uuid)) {
            return;
        }
        Slot resultSlot = invMenu.getSlot(0);
        ItemStack result = resultSlot.getItem();
        if (result.isEmpty()) {
            return;
        }
        // 用 2x2 网格（slot 1~4）构建 CraftingInput，顺序与原版网格一致
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            grid.add(invMenu.getSlot(i).getItem());
        }
        CraftingInput input = CraftingInput.of(2, 2, grid);
        ServerLevel level = (ServerLevel) player.level();
        RecipeHolder<CraftingRecipe> holder = level.getServer().getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        if (holder == null) {
            return;
        }
        ItemStack recipeResult = holder.value().assemble(input, level.registryAccess());
        if (recipeResult.isEmpty()) {
            return;
        }
        // 配方难度：复用工作台哈希映射（25~200）
        float difficulty = 25f + (Math.abs(holder.value().toString().hashCode()) % 176);
        int duration = CraftingTimeHelper.calculateCraftingTime(EnumQuality.AVERAGE, difficulty, 0f);
        long startTick = level.getGameTime();
        ICPMInventoryCraftingState.start(uuid, startTick, duration, recipeResult);
        ServerPlayNetworking.send(player, new InventoryCraftSyncPacket(true, duration, startTick));
    }

    /**
     * 取走背包合成成品：校验完成度与配方未被改动，消耗网格材料并发放成品。
     */
    public static void takeBackpackCraft(ServerPlayer player, AbstractContainerMenu invMenu) {
        UUID uuid = player.getUUID();
        if (!ICPMInventoryCraftingState.isActive(uuid)) {
            return;
        }
        long currentTick = ((ServerLevel) player.level()).getGameTime();
        if (!ICPMInventoryCraftingState.isComplete(uuid, currentTick)) {
            return;
        }
        Slot resultSlot = invMenu.getSlot(0);
        ItemStack currentResult = resultSlot.getItem();
        ItemStack expected = ICPMInventoryCraftingState.getExpectedResult(uuid);
        // 网格被改动则取消本次取走，防止白嫖
        if (currentResult.isEmpty() || expected.isEmpty()
            || !ItemStack.isSameItemSameComponents(currentResult, expected)) {
            ICPMInventoryCraftingState.clear(uuid);
            ServerPlayNetworking.send(player, new InventoryCraftSyncPacket(false, 0, 0));
            return;
        }
        // 消耗 2x2 网格材料（每格 1 个）
        for (int i = 1; i <= 4; i++) {
            Slot s = invMenu.getSlot(i);
            ItemStack st = s.getItem();
            if (!st.isEmpty()) {
                st.shrink(1);
                s.set(st);
            }
        }
        // 发放成品到玩家背包
        ItemStack toGive = currentResult.copy();
        if (!player.getInventory().add(toGive)) {
            player.drop(toGive, false);
        }
        // 清空结果槽
        resultSlot.set(ItemStack.EMPTY);
        // 重置状态并通知客户端
        ICPMInventoryCraftingState.clear(uuid);
        ServerPlayNetworking.send(player, new InventoryCraftSyncPacket(false, 0, 0));
    }

    private void registerAllBlocks() {
        // 在这里直接使用名字注册，避免通过 BuiltInRegistries.BLOCK.getKey 反查
        for (String name : ICPMBlocks.BLOCK_NAMES) {
            Block block = ICPMBlocks.createAndRegister(name);
            // 金属砧的「裂痕/损坏」变体 id 仅为旧存档兼容保留（避免旧档直接变空气），
            // 不注册物品 -> 不会出现在创造栏 / JEI；仅主砧方块（copper_anvil 等）可被获得。
            if (name.startsWith("chipped_") || name.startsWith("damaged_")) {
                continue;
            }
            // 同时注册 BlockItem；门是双高方块，需用 DoubleHighBlockItem 处理放置
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name));
            BlockItem blockItem;
            if (block instanceof name.icpm.block.BlockRunestone) {
                // 符文石：使用自定义物品（按 runestone_variant 组件放置对应变体，显示名追加魔法名）
                blockItem = new name.icpm.item.RunestoneItem(block, new Item.Properties().setId(itemKey));
            } else if (block instanceof name.icpm.block.BlockICPMFlintWorkbench) {
                // 燧石工作台：自定义物品，按原版 block_state 组件放置/命名对应原木衍生变体
                blockItem = new name.icpm.item.FlintWorkbenchItem(block, new Item.Properties().setId(itemKey));
            } else if (block instanceof net.minecraft.world.level.block.DoorBlock) {
                blockItem = new net.minecraft.world.item.DoubleHighBlockItem(block, new Item.Properties().setId(itemKey));
            } else if (block instanceof name.icpm.block.BlockMetalAnvil) {
                // 金属砧：带耐久的物品（R196 ItemAnvilBlock），砧耐久值计入物品数据
                blockItem = new name.icpm.item.ICPMMetalAnvilItem((name.icpm.block.BlockMetalAnvil) block, new Item.Properties().setId(itemKey));
            } else {
                blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
            }
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name), blockItem);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
