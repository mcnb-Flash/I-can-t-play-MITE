package name.icpm;

import name.icpm.network.InventoryCraftSyncPacket;
import name.icpm.network.NutritionSyncPacket;
import name.icpm.network.WorkbenchCraftPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge 入口：@Mod 构造器完成内容注册编排 + 事件订阅。
 *
 * Fabric 端原 onInitialize 的职责在此按 NeoForge 模型分派：
 * - 内容注册（方块/物品/实体/效果等）：全部经 mod bus RegisterEvent 按注册表解冻窗口分发
 * - 网络 payload：挂 RegisterPayloadHandlersEvent（mod bus）
 * - 游戏事件：挂 NeoForge.EVENT_BUS（server tick / 玩家加入重生 / 死亡 / 世界加载卸载 / 方块破坏）
 */
@Mod(ICPM.MOD_ID)
public final class ICPMNeoForge {

    public ICPMNeoForge(IEventBus modBus) {
        ICPM.LOGGER.info("ICPM NeoForge constructing");
        // 触发 ICPM 静态字段注册（DataComponent/MobEffect/MenuType 等直写 BuiltInRegistries）
        name.icpm.ICPM.init();
        // 注册网络 payload（mod bus 事件）
        modBus.addListener((RegisterPayloadHandlersEvent evt) -> {
            PayloadRegistrar registrar = evt.registrar("1");
            // S2C
            registrar.playToClient(NutritionSyncPacket.TYPE, NutritionSyncPacket.CODEC,
                    (p, ctx) -> ctx.enqueueWork(() -> name.icpm.client.network.NutritionSyncHandler.handle(p)));
            registrar.playToClient(InventoryCraftSyncPacket.TYPE, InventoryCraftSyncPacket.CODEC,
                    (p, ctx) -> ctx.enqueueWork(() -> name.icpm.client.network.InventoryCraftSyncHandler.handle(p)));
            // C2S
            registrar.playToServer(WorkbenchCraftPacket.TYPE, WorkbenchCraftPacket.CODEC, ICPMPackets::handleWorkbenchCraft);
            registrar.playToServer(name.icpm.network.AnvilRenamePacket.TYPE, name.icpm.network.AnvilRenamePacket.CODEC,
                    ICPMPackets::handleAnvilRename);
            registrar.playToServer(name.icpm.network.BucketSourcePacket.TYPE, name.icpm.network.BucketSourcePacket.CODEC,
                    ICPMPackets::handleBucketSource);
        });
        // 实体属性注册（原 FabricDefaultAttributeRegistry → mod bus 事件）
        modBus.addListener((EntityAttributeCreationEvent evt) ->
                name.icpm.entity.ICPMEntities.INSTANCE.registerAttributes(evt));
        // 实体生成放置规则（1.21.11 SpawnPlacements.register 私有化 → mod bus 事件）
        modBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent evt) ->
                name.icpm.entity.ICPMEntities.INSTANCE.registerSpawnPlacements(evt));
        // 配方序列化器 + 全部内容注册：注册表在 mod 构造期已冻结（Block 构造器即写 intrusive holder），
        // 必须经 RegisterEvent 在各注册表解冻窗口内注册。触发顺序（NeoForge GameData）：
        // ATTRIBUTE → DATA_COMPONENT_TYPE → PARTICLE_TYPE → (vanilla 声明序)
        // MOB_EFFECT → BLOCK → ENTITY_TYPE → ITEM → BLOCK_ENTITY_TYPE → MENU → RECIPE_SERIALIZER
        // → … → CREATIVE_MODE_TAB，依赖方向（方块→方块物品、实体→生成蛋、方块→BE）全部满足。
        modBus.addListener((net.neoforged.neoforge.registries.RegisterEvent evt) -> {
            net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> key = evt.getRegistryKey();
            if (key == net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE) {
                ICPM.registerDataComponents();
            } else if (key == net.minecraft.core.registries.Registries.MOB_EFFECT) {
                ICPM.registerMobEffects();
            } else if (key == net.minecraft.core.registries.Registries.BLOCK) {
                ICPM.registerBlocks();
                name.icpm.block.ICPMBlueberryBush.INSTANCE.registerBlock();
            } else if (key == net.minecraft.core.registries.Registries.ENTITY_TYPE) {
                // Kotlin object 类加载时 val 字段直写 ENTITY_TYPE（此时已解冻）
                name.icpm.entity.ICPMEntities.INSTANCE.init();
            } else if (key == net.minecraft.core.registries.Registries.ITEM) {
                ICPM.registerBlockItems();
                name.icpm.block.ICPMBlueberryBush.INSTANCE.registerItem();
                name.icpm.item.ICPMItems.INSTANCE.init();
                name.icpm.item.ICPMGelatinousItems.INSTANCE.init();
                name.icpm.item.ICPMEarthElementalItems.INSTANCE.init();
                name.icpm.item.ICPMMonsterSpawnEggs.INSTANCE.init();
            } else if (key == net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE) {
                name.icpm.blockentity.ICPMBlockEntities.INSTANCE.init();
            } else if (key == net.minecraft.core.registries.Registries.MENU) {
                ICPM.registerMenus();
            } else if (key == net.minecraft.core.registries.Registries.RECIPE_SERIALIZER) {
                name.icpm.recipe.ICPMRecipes.registerAll();
            } else if (key == net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB) {
                name.icpm.item.ICPMItemGroup.register();
                name.icpm.block.ICPMBlockGroup.register();
            }
        });
        // 游戏事件订阅：ICPMEventHandlers 以 @EventBusSubscriber(bus=GAME) 自动挂载
        ICPM.LOGGER.info("ICPM NeoForge initialized");
    }
}
