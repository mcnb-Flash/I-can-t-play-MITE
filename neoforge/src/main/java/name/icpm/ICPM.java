package name.icpm;

import name.icpm.block.ICPMBlockGroup;
import name.icpm.block.ICPMBlocks;
import name.icpm.component.CraftPreviewComponent;
import name.icpm.component.NutritionComponent;
import name.icpm.component.QualityComponent;
import name.icpm.entity.LivestockState;
import name.icpm.network.InventoryCraftSyncPacket;
import name.icpm.network.NutritionSyncPacket;
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
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ICPM 内容注册中心（NeoForge 版）。
 *
 * 本类保留静态注册字段（DataComponent/MobEffect/MenuType 等直写 BuiltInRegistries，
 * 于 mod 构造期首次引用本类时完成，先于注册表冻结）+ 内容注册编排 + 背包合成辅助。
 * 服务端事件与网络分别收敛于 {@link ICPMEventHandlers} / {@link ICPMPackets}。
 */
public final class ICPM {

    public static final String MOD_ID = "icpm";

    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ============ 内容注册（NeoForge：注册表在 mod 构造期已冻结，
    // 必须经 mod bus RegisterEvent 在对应注册表解冻窗口内赋值，见 ICPMNeoForge 分发器） ============

    public static DataComponentType<QualityComponent> QUALITY_COMPONENT;
    public static DataComponentType<NutritionComponent> NUTRITION_COMPONENT;
    public static DataComponentType<Integer> COIN_XP_COMPONENT;
    public static DataComponentType<Integer> RUNESTONE_VARIANT;
    public static DataComponentType<CraftPreviewComponent> CRAFT_PREVIEW_COMPONENT;
    public static DataComponentType<Boolean> SHIELD_ATTACHED;

    public static MenuType<name.icpm.inventory.MetalAnvilMenu> METAL_ANVIL_MENU;
    public static MenuType<name.icpm.inventory.ICPMWorkbenchMenu> ICPM_WORKBENCH_MENU;

    public static net.minecraft.world.effect.MobEffect MALNUTRITION;
    public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> MALNUTRITION_HOLDER;
    public static net.minecraft.world.effect.MobEffect WITCH_CURSE;
    public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> WITCH_CURSE_HOLDER;
    public static net.minecraft.world.effect.MobEffect WITCH_WHISPER;
    public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> WITCH_WHISPER_HOLDER;

    /** R196 Block.spark（火花）：燧石打火的中间态方块（无对应物品）。 */
    public static Block SPARK;

    /** R196 枯死作物：旱死（未成熟）与疫病致死的目标方块（无对应物品）。 */
    public static Block DEAD_CROP;

    /** RegisterEvent(DATA_COMPONENT_TYPE)：注册数据组件 */
    public static void registerDataComponents() {
        QUALITY_COMPONENT = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE, id("quality"),
                DataComponentType.<QualityComponent>builder().persistent(QualityComponent.CODEC)
                        .networkSynchronized(QualityComponent.STREAM_CODEC).build());
        NUTRITION_COMPONENT = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE, id("nutrition"),
                DataComponentType.<NutritionComponent>builder().persistent(NutritionComponent.CODEC)
                        .networkSynchronized(NutritionComponent.STREAM_CODEC).build());
        COIN_XP_COMPONENT = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE, id("coin_xp"),
                DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT)
                        .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());
        RUNESTONE_VARIANT = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE, id("runestone_variant"),
                DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT)
                        .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());
        CRAFT_PREVIEW_COMPONENT = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE, id("craft_preview"),
                DataComponentType.<CraftPreviewComponent>builder().persistent(CraftPreviewComponent.CODEC)
                        .networkSynchronized(CraftPreviewComponent.STREAM_CODEC).build());
        SHIELD_ATTACHED = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE, id("shield_attached"),
                DataComponentType.<Boolean>builder().persistent(com.mojang.serialization.Codec.BOOL)
                        .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());
    }

    /** RegisterEvent(MENU)：注册菜单类型（工厂经 RegistryFriendlyByteBuf 传 BlockPos） */
    public static void registerMenus() {
        METAL_ANVIL_MENU = Registry.register(
                BuiltInRegistries.MENU, id("metal_anvil"),
                net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                        (syncId, inv, buf) -> name.icpm.inventory.MetalAnvilMenu.Companion.create(syncId, inv, buf.readBlockPos())));
        ICPM_WORKBENCH_MENU = Registry.register(
                BuiltInRegistries.MENU, id("icpm_workbench"),
                net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                        (syncId, inv, buf) -> name.icpm.inventory.ICPMWorkbenchMenu.Companion.create(syncId, inv, buf.readBlockPos())));
    }

    /** RegisterEvent(MOB_EFFECT)：注册状态效果 */
    public static void registerMobEffects() {
        MALNUTRITION = Registry.register(
                BuiltInRegistries.MOB_EFFECT, id("malnutrition"),
                new net.minecraft.world.effect.MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x8B4513) {
                    @Override public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) { return true; }
                });
        MALNUTRITION_HOLDER = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MALNUTRITION);

        WITCH_CURSE = Registry.register(
                BuiltInRegistries.MOB_EFFECT, id("witch_curse"),
                new net.minecraft.world.effect.MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x4B0082) {
                    @Override public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) { return true; }
                });
        WITCH_CURSE_HOLDER = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(WITCH_CURSE);

        WITCH_WHISPER = Registry.register(
                BuiltInRegistries.MOB_EFFECT, id("witch_whisper"),
                new net.minecraft.world.effect.MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x3A0066) {
                    @Override public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) { return true; }
                });
        WITCH_WHISPER_HOLDER = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(WITCH_WHISPER);
    }

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

    /** 由 NeoForge 入口在 mod 构造时调用：非注册表初始化（注册表写入一律走 RegisterEvent 分发器）。 */
    public static void init() {
        LOGGER.info("ICPM mod loaded (NeoForge)");

        // 启动卡死探测器（监控渲染线程 + 服务端线程栈稳定性；客户端 mixin 也会触发，此处确保专用服务器也启动）
        FreezeDetector.ensureStarted();

        // 初始化ICPM材质定义（按1.6.4-ICPM R196原版顺序；非 MC 注册表，自定义材质映射）
        name.icpm.common.ICPMMaterials.registerAll();

        // 初始化全局配置（config/icpm.json → enableCreativeMode 等）
        name.icpm.common.ICPMConfig.init();

        // 注册成就系统代码触发器（R196 AchievementList 移植；仅挂 GAME bus 事件监听）
        name.icpm.common.ICPMAchievementTriggers.init();

        // 以下内容注册全部由 ICPMNeoForge 的 RegisterEvent 分发器驱动：
        // DataComponent/Menu/MobEffect/Block/BlockItem/Item/EntityType/BlockEntityType/
        // CreativeModeTab/RecipeSerializer（各注册表仅在自身 RegisterEvent 窗口解冻）
        LOGGER.info("ICPM early init done (NeoForge)");
    }

    // ==================== 背包 2x2 合成时间机制 ====================

    public static void startBackpackCraft(ServerPlayer player, AbstractContainerMenu invMenu) {
        UUID uuid = player.getUUID();
        if (name.icpm.common.ICPMInventoryCraftingState.isActive(uuid)) {
            return;
        }
        Slot resultSlot = invMenu.getSlot(0);
        ItemStack result = resultSlot.getItem();
        if (result.isEmpty()) {
            return;
        }
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
        float difficulty = 25f + (Math.abs(holder.value().toString().hashCode()) % 176);
        int duration = name.icpm.common.CraftingTimeHelper.calculateCraftingTime(
                name.icpm.common.EnumQuality.AVERAGE, difficulty, 0f);
        long startTick = level.getGameTime();
        name.icpm.common.ICPMInventoryCraftingState.start(uuid, startTick, duration, recipeResult);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new InventoryCraftSyncPacket(true, duration, startTick));
    }

    public static void takeBackpackCraft(ServerPlayer player, AbstractContainerMenu invMenu) {
        UUID uuid = player.getUUID();
        if (!name.icpm.common.ICPMInventoryCraftingState.isActive(uuid)) {
            return;
        }
        long currentTick = ((ServerLevel) player.level()).getGameTime();
        if (!name.icpm.common.ICPMInventoryCraftingState.isComplete(uuid, currentTick)) {
            return;
        }
        Slot resultSlot = invMenu.getSlot(0);
        ItemStack currentResult = resultSlot.getItem();
        ItemStack expected = name.icpm.common.ICPMInventoryCraftingState.getExpectedResult(uuid);
        if (currentResult.isEmpty() || expected.isEmpty()
            || !ItemStack.isSameItemSameComponents(currentResult, expected)) {
            name.icpm.common.ICPMInventoryCraftingState.clear(uuid);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new InventoryCraftSyncPacket(false, 0, 0));
            return;
        }
        for (int i = 1; i <= 4; i++) {
            Slot s = invMenu.getSlot(i);
            ItemStack st = s.getItem();
            if (!st.isEmpty()) {
                st.shrink(1);
                s.set(st);
            }
        }
        ItemStack toGive = currentResult.copy();
        if (!player.getInventory().add(toGive)) {
            player.drop(toGive, false);
        }
        resultSlot.set(ItemStack.EMPTY);
        name.icpm.common.ICPMInventoryCraftingState.clear(uuid);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new InventoryCraftSyncPacket(false, 0, 0));
    }

    /** RegisterEvent(BLOCK)：注册全部方块（BlockItem 延迟到 ITEM 窗口） */
    public static void registerBlocks() {
        // R196 Block.spark（火花）：燧石打火的中间态方块 —— 放置 2 tick 后若邻格可燃则变火，否则消失。
        // 无对应 BlockItem（R196 同，玩家不可获得），故不进 BLOCK_NAMES / 不注册物品。
        SPARK = Registry.register(BuiltInRegistries.BLOCK, id("spark"),
                new name.icpm.block.ICPMSparkBlock(
                        net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                                .mapColor(net.minecraft.world.level.material.MapColor.FIRE)
                                .replaceable()
                                .noCollision()
                                .instabreak()
                                .lightLevel(state -> 15)
                                .sound(net.minecraft.world.level.block.SoundType.WOOL)
                                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                                .setId(ResourceKey.create(Registries.BLOCK, id("spark")))));

        // R196 枯死作物（BlockCropsDead 等忠实移植）：旱死（未成熟）与疫病致死的目标方块。
        // 无 BlockItem、无随机刻、骨粉无效、破坏无掉落（空 loot_table）。纹理源自 MITE RP 1.6.41。
        DEAD_CROP = Registry.register(BuiltInRegistries.BLOCK, id("dead_crop"),
                name.icpm.block.ICPMDeadCropBlock.INSTANCE = new name.icpm.block.ICPMDeadCropBlock(
                        net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                                .mapColor(net.minecraft.world.level.material.MapColor.PLANT)
                                .noCollision()
                                .instabreak()
                                .sound(net.minecraft.world.level.block.SoundType.CROP)
                                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                                .setId(ResourceKey.create(Registries.BLOCK, id("dead_crop")))));

        REGISTERED_BLOCKS.clear();
        for (String name : ICPMBlocks.BLOCK_NAMES) {
            Block block = ICPMBlocks.createAndRegister(name);
            REGISTERED_BLOCKS.add(new RegisteredBlock(name, block, null));
        }
    }

    /** RegisterEvent(ITEM)：注册方块物品（BLOCK 窗口已建好方块实例） */
    public static void registerBlockItems() {
        for (RegisteredBlock rb : REGISTERED_BLOCKS) {
            String name = rb.name;
            Block block = rb.block;
            if (name.startsWith("chipped_") || name.startsWith("damaged_")) {
                continue;
            }
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name));
            BlockItem blockItem;
            if (block instanceof name.icpm.block.BlockRunestone) {
                blockItem = new name.icpm.item.RunestoneItem(block, new Item.Properties().setId(itemKey));
            } else if (block instanceof name.icpm.block.BlockICPMFlintWorkbench) {
                blockItem = new name.icpm.item.FlintWorkbenchItem(block, new Item.Properties().setId(itemKey));
            } else if (block instanceof net.minecraft.world.level.block.DoorBlock) {
                blockItem = new net.minecraft.world.item.DoubleHighBlockItem(block, new Item.Properties().setId(itemKey));
            } else if (block instanceof name.icpm.block.BlockMetalAnvil) {
                blockItem = new name.icpm.item.ICPMMetalAnvilItem((name.icpm.block.BlockMetalAnvil) block, new Item.Properties().setId(itemKey));
            } else {
                blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
            }
            Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name), blockItem);
            rb.blockItem = blockItem;
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
