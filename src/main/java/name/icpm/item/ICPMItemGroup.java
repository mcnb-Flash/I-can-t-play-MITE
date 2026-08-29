package name.icpm.item;

import name.icpm.ICPM;
import name.icpm.block.BlockICPMFlintWorkbench;
import name.icpm.block.ICPMBlocks;
import name.icpm.item.RunestoneItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

/**
 * ICPM 专属物品列表 (Creative Tab)
 * 图标: 燧石碎片 (flint_fragment)
 */
public class ICPMItemGroup {

    /**
     * ICPM 物品列表的 ResourceKey
     */
    public static final ResourceKey<CreativeModeTab> ICPM_GROUP_KEY =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(ICPM.MOD_ID, "icpm_items"));

    /**
     * 注册 ICPM 物品列表到 Fabric 注册表
     */
    public static void register() {
        CreativeModeTab tab = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup." + ICPM.MOD_ID + ".icpm_items"))
            .icon(() -> new ItemStack(ICPMItems.FLINT_FRAGMENT))
            .displayItems((params, output) -> {
                // 燧石系
                output.accept(ICPMItems.FLINT_FRAGMENT);
                output.accept(ICPMItems.GLASS_SHARD);
                output.accept(ICPMItems.QUARTZ_SHARD);
                output.accept(ICPMItems.OBSIDIAN_SHARD);
                output.accept(ICPMItems.EMERALD_SHARD);
                output.accept(ICPMItems.DIAMOND_SHARD);

                // 链条
                output.accept(ICPMItems.COPPER_CHAIN);
                output.accept(ICPMItems.SILVER_CHAIN);
                output.accept(ICPMItems.GOLD_CHAIN);
                output.accept(ICPMItems.IRON_CHAIN);
                output.accept(ICPMItems.MITHRIL_CHAIN);
                output.accept(ICPMItems.ADAMANTIUM_CHAIN);
                output.accept(ICPMItems.ANCIENT_METAL_CHAIN);

                // 钓鱼竿
                output.accept(ICPMItems.FLINT_FISHING_ROD);
                output.accept(ICPMItems.OBSIDIAN_FISHING_ROD);
                output.accept(ICPMItems.COPPER_FISHING_ROD);
                output.accept(ICPMItems.SILVER_FISHING_ROD);
                output.accept(ICPMItems.GOLD_FISHING_ROD);
                output.accept(ICPMItems.IRON_FISHING_ROD);
                output.accept(ICPMItems.MITHRIL_FISHING_ROD);
                output.accept(ICPMItems.ADAMANTIUM_FISHING_ROD);
                output.accept(ICPMItems.ANCIENT_METAL_FISHING_ROD);

                // 马铠
                output.accept(ICPMItems.COPPER_HORSE_ARMOR);
                output.accept(ICPMItems.SILVER_HORSE_ARMOR);
                output.accept(ICPMItems.ANCIENT_METAL_HORSE_ARMOR);
                output.accept(ICPMItems.MITHRIL_HORSE_ARMOR);
                output.accept(ICPMItems.ADAMANTIUM_HORSE_ARMOR);

                output.accept(ICPMItems.FLINT_KNIFE);
                output.accept(ICPMItems.OBSIDIAN_KNIFE);
                output.accept(ICPMItems.FLINT_SHOVEL);
                output.accept(ICPMItems.FLINT_HATCHET);
                output.accept(ICPMItems.FLINT_AXE);

                // 弓与箭矢
                output.accept(ICPMItems.BOW);
                output.accept(ICPMItems.ANCIENT_METAL_BOW);
                output.accept(ICPMItems.MITHRIL_BOW);
                output.accept(ICPMItems.FLINT_ARROW);
                output.accept(ICPMItems.OBSIDIAN_ARROW);
                output.accept(ICPMItems.COPPER_ARROW);
                output.accept(ICPMItems.SILVER_ARROW);
                output.accept(ICPMItems.GOLD_ARROW);
                output.accept(ICPMItems.IRON_ARROW);
                output.accept(ICPMItems.ANCIENT_METAL_ARROW);
                output.accept(ICPMItems.MITHRIL_ARROW);
                output.accept(ICPMItems.ADAMANTIUM_ARROW);

                // 铜制特殊工具
                output.accept(ICPMItems.COPPER_BATTLE_AXE);
                output.accept(ICPMItems.COPPER_WAR_HAMMER);
                output.accept(ICPMItems.COPPER_MATTOCK);
                output.accept(ICPMItems.COPPER_SHEARS);

                // 金制特殊工具
                output.accept(ICPMItems.GOLD_BATTLE_AXE);
                output.accept(ICPMItems.GOLD_WAR_HAMMER);
                output.accept(ICPMItems.GOLD_MATTOCK);
                output.accept(ICPMItems.GOLD_SCYTHE);
                output.accept(ICPMItems.GOLD_SHEARS);

                // 铁制特殊工具
                output.accept(ICPMItems.IRON_HATCHET);
                output.accept(ICPMItems.IRON_DAGGER);
                output.accept(ICPMItems.IRON_WAR_HAMMER);
                output.accept(ICPMItems.IRON_BATTLE_AXE);
                output.accept(ICPMItems.IRON_SCYTHE);
                output.accept(ICPMItems.IRON_MATTOCK);

                // 银系
                output.accept(ICPMItems.SILVER_NUGGET);
                output.accept(ICPMItems.SILVER_INGOT);
                output.accept(ICPMItems.SILVER_PICKAXE);
                output.accept(ICPMItems.SILVER_SHOVEL);
                output.accept(ICPMItems.SILVER_AXE);
                output.accept(ICPMItems.SILVER_HOE);
                output.accept(ICPMItems.SILVER_SWORD);
                output.accept(ICPMItems.SILVER_HATCHET);
                output.accept(ICPMItems.SILVER_DAGGER);
                output.accept(ICPMItems.SILVER_WAR_HAMMER);
                output.accept(ICPMItems.SILVER_BATTLE_AXE);
                output.accept(ICPMItems.SILVER_SCYTHE);
                output.accept(ICPMItems.SILVER_MATTOCK);
                output.accept(ICPMItems.SILVER_SHEARS);
                // 银制盔甲
                output.accept(ICPMItems.SILVER_HELMET);
                output.accept(ICPMItems.SILVER_CHESTPLATE);
                output.accept(ICPMItems.SILVER_LEGGINGS);
                output.accept(ICPMItems.SILVER_BOOTS);

                // 远古金属系
                output.accept(ICPMItems.ANCIENT_METAL_NUGGET);
                output.accept(ICPMItems.ANCIENT_METAL_INGOT);
                output.accept(ICPMItems.ANCIENT_METAL_PICKAXE);
                output.accept(ICPMItems.ANCIENT_METAL_SHOVEL);
                output.accept(ICPMItems.ANCIENT_METAL_AXE);
                output.accept(ICPMItems.ANCIENT_METAL_HOE);
                output.accept(ICPMItems.ANCIENT_METAL_SWORD);
                output.accept(ICPMItems.ANCIENT_METAL_HATCHET);
                output.accept(ICPMItems.ANCIENT_METAL_DAGGER);
                output.accept(ICPMItems.ANCIENT_METAL_WAR_HAMMER);
                output.accept(ICPMItems.ANCIENT_METAL_BATTLE_AXE);
                output.accept(ICPMItems.ANCIENT_METAL_SCYTHE);
                output.accept(ICPMItems.ANCIENT_METAL_MATTOCK);
                output.accept(ICPMItems.ANCIENT_METAL_SHEARS);
                // 远古金属制盔甲
                output.accept(ICPMItems.ANCIENT_METAL_HELMET);
                output.accept(ICPMItems.ANCIENT_METAL_CHESTPLATE);
                output.accept(ICPMItems.ANCIENT_METAL_LEGGINGS);
                output.accept(ICPMItems.ANCIENT_METAL_BOOTS);

                // 秘银系
                output.accept(ICPMItems.MITHRIL_NUGGET);
                output.accept(ICPMItems.MITHRIL_INGOT);
                output.accept(ICPMItems.MITHRIL_PICKAXE);
                output.accept(ICPMItems.MITHRIL_SHOVEL);
                output.accept(ICPMItems.MITHRIL_AXE);
                output.accept(ICPMItems.MITHRIL_HOE);
                output.accept(ICPMItems.MITHRIL_SWORD);
                output.accept(ICPMItems.MITHRIL_HATCHET);
                output.accept(ICPMItems.MITHRIL_DAGGER);
                output.accept(ICPMItems.MITHRIL_WAR_HAMMER);
                output.accept(ICPMItems.MITHRIL_BATTLE_AXE);
                output.accept(ICPMItems.MITHRIL_SCYTHE);
                output.accept(ICPMItems.MITHRIL_MATTOCK);
                output.accept(ICPMItems.MITHRIL_SHEARS);
                // 秘银制盔甲
                output.accept(ICPMItems.MITHRIL_HELMET);
                output.accept(ICPMItems.MITHRIL_CHESTPLATE);
                output.accept(ICPMItems.MITHRIL_LEGGINGS);
                output.accept(ICPMItems.MITHRIL_BOOTS);

                // 艾德曼系
                output.accept(ICPMItems.ADAMANTIUM_NUGGET);
                output.accept(ICPMItems.ADAMANTIUM_INGOT);
                output.accept(ICPMItems.ADAMANTIUM_PICKAXE);
                output.accept(ICPMItems.ADAMANTIUM_SHOVEL);
                output.accept(ICPMItems.ADAMANTIUM_AXE);
                output.accept(ICPMItems.ADAMANTIUM_HOE);
                output.accept(ICPMItems.ADAMANTIUM_SWORD);
                output.accept(ICPMItems.ADAMANTIUM_HATCHET);
                output.accept(ICPMItems.ADAMANTIUM_DAGGER);
                output.accept(ICPMItems.ADAMANTIUM_WAR_HAMMER);
                output.accept(ICPMItems.ADAMANTIUM_BATTLE_AXE);
                output.accept(ICPMItems.ADAMANTIUM_SCYTHE);
                output.accept(ICPMItems.ADAMANTIUM_MATTOCK);
                output.accept(ICPMItems.ADAMANTIUM_SHEARS);
                // 艾德曼制盔甲
                output.accept(ICPMItems.ADAMANTIUM_HELMET);
                output.accept(ICPMItems.ADAMANTIUM_CHESTPLATE);
                output.accept(ICPMItems.ADAMANTIUM_LEGGINGS);
                output.accept(ICPMItems.ADAMANTIUM_BOOTS);

                // 硬币 (ICPM 货币)
                output.accept(ICPMItems.COPPER_COIN);
                output.accept(ICPMItems.SILVER_COIN);
                output.accept(ICPMItems.GOLD_COIN);
                output.accept(ICPMItems.ANCIENT_METAL_COIN);
                output.accept(ICPMItems.MITHRIL_COIN);
                output.accept(ICPMItems.ADAMANTIUM_COIN);

                // 多级桶
                output.accept(ICPMItems.COPPER_BUCKET);
                output.accept(ICPMItems.COPPER_WATER_BUCKET);
                output.accept(ICPMItems.COPPER_LAVA_BUCKET);
                output.accept(ICPMItems.COPPER_MILK_BUCKET);
                output.accept(ICPMItems.COPPER_STONE_BUCKET);
                output.accept(ICPMItems.SILVER_BUCKET);
                output.accept(ICPMItems.SILVER_WATER_BUCKET);
                output.accept(ICPMItems.SILVER_LAVA_BUCKET);
                output.accept(ICPMItems.SILVER_MILK_BUCKET);
                output.accept(ICPMItems.SILVER_STONE_BUCKET);
                output.accept(ICPMItems.GOLD_BUCKET);
                output.accept(ICPMItems.GOLD_WATER_BUCKET);
                output.accept(ICPMItems.GOLD_LAVA_BUCKET);
                output.accept(ICPMItems.GOLD_MILK_BUCKET);
                output.accept(ICPMItems.GOLD_STONE_BUCKET);
                // 铁桶=原版 bucket 系列，不重复注册
                output.accept(ICPMItems.IRON_STONE_BUCKET);
                output.accept(ICPMItems.ANCIENT_METAL_BUCKET);
                output.accept(ICPMItems.ANCIENT_METAL_WATER_BUCKET);
                output.accept(ICPMItems.ANCIENT_METAL_LAVA_BUCKET);
                output.accept(ICPMItems.ANCIENT_METAL_MILK_BUCKET);
                output.accept(ICPMItems.ANCIENT_METAL_STONE_BUCKET);
                output.accept(ICPMItems.MITHRIL_BUCKET);
                output.accept(ICPMItems.MITHRIL_WATER_BUCKET);
                output.accept(ICPMItems.MITHRIL_LAVA_BUCKET);
                output.accept(ICPMItems.MITHRIL_MILK_BUCKET);
                output.accept(ICPMItems.MITHRIL_STONE_BUCKET);
                output.accept(ICPMItems.ADAMANTIUM_BUCKET);
                output.accept(ICPMItems.ADAMANTIUM_WATER_BUCKET);
                output.accept(ICPMItems.ADAMANTIUM_LAVA_BUCKET);
                output.accept(ICPMItems.ADAMANTIUM_MILK_BUCKET);
                output.accept(ICPMItems.ADAMANTIUM_STONE_BUCKET);

                // ===== ICPM 特有食物 =====
                output.accept(ICPMItems.FLOUR);
                output.accept(ICPMItems.DOUGH);
                output.accept(ICPMItems.CHEESE);
                output.accept(ICPMItems.CHOCOLATE);
                output.accept(ICPMItems.ICE_CREAM);
                output.accept(ICPMItems.SORBET);
                output.accept(ICPMItems.MASHED_POTATO);
                output.accept(ICPMItems.BEEF_STEW);
                output.accept(ICPMItems.CHICKEN_SOUP);
                output.accept(ICPMItems.VEGETABLE_SOUP);
                output.accept(ICPMItems.VEGETABLE_SOUP_CREAM);
                output.accept(ICPMItems.MUSHROOM_SOUP_CREAM);
                output.accept(ICPMItems.PUMPKIN_SOUP);
                output.accept(ICPMItems.SALAD);
                output.accept(ICPMItems.PORRIDGE);
                output.accept(ICPMItems.CEREAL);
                output.accept(ICPMItems.ORANGE);
                output.accept(ICPMItems.BANANA);
                output.accept(ICPMItems.BLUEBERRY);
                output.accept(ICPMItems.ONION);
                output.accept(ICPMItems.WORM);
                output.accept(ICPMItems.COOKED_WORM);
                output.accept(ICPMItems.MILK_BOWL);
                output.accept(ICPMItems.WATER_BOWL);

                // 铜锁链甲
                output.accept(ICPMItems.COPPER_CHAINMAIL_HELMET);
                output.accept(ICPMItems.COPPER_CHAINMAIL_CHESTPLATE);
                output.accept(ICPMItems.COPPER_CHAINMAIL_LEGGINGS);
                output.accept(ICPMItems.COPPER_CHAINMAIL_BOOTS);

                // 金锁链甲
                output.accept(ICPMItems.GOLD_CHAINMAIL_HELMET);
                output.accept(ICPMItems.GOLD_CHAINMAIL_CHESTPLATE);
                output.accept(ICPMItems.GOLD_CHAINMAIL_LEGGINGS);
                output.accept(ICPMItems.GOLD_CHAINMAIL_BOOTS);

                // 铁锁链甲
                output.accept(ICPMItems.IRON_CHAINMAIL_HELMET);
                output.accept(ICPMItems.IRON_CHAINMAIL_CHESTPLATE);
                output.accept(ICPMItems.IRON_CHAINMAIL_LEGGINGS);
                output.accept(ICPMItems.IRON_CHAINMAIL_BOOTS);

                // 银锁链甲
                output.accept(ICPMItems.SILVER_CHAINMAIL_HELMET);
                output.accept(ICPMItems.SILVER_CHAINMAIL_CHESTPLATE);
                output.accept(ICPMItems.SILVER_CHAINMAIL_LEGGINGS);
                output.accept(ICPMItems.SILVER_CHAINMAIL_BOOTS);

                // 远古金属锁链甲
                output.accept(ICPMItems.ANCIENT_METAL_CHAINMAIL_HELMET);
                output.accept(ICPMItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE);
                output.accept(ICPMItems.ANCIENT_METAL_CHAINMAIL_LEGGINGS);
                output.accept(ICPMItems.ANCIENT_METAL_CHAINMAIL_BOOTS);

                // 秘银锁链甲
                output.accept(ICPMItems.MITHRIL_CHAINMAIL_HELMET);
                output.accept(ICPMItems.MITHRIL_CHAINMAIL_CHESTPLATE);
                output.accept(ICPMItems.MITHRIL_CHAINMAIL_LEGGINGS);
                output.accept(ICPMItems.MITHRIL_CHAINMAIL_BOOTS);

                // 艾德曼锁链甲
                output.accept(ICPMItems.ADAMANTIUM_CHAINMAIL_HELMET);
                output.accept(ICPMItems.ADAMANTIUM_CHAINMAIL_CHESTPLATE);
                output.accept(ICPMItems.ADAMANTIUM_CHAINMAIL_LEGGINGS);
                output.accept(ICPMItems.ADAMANTIUM_CHAINMAIL_BOOTS);

                // 凝胶球（黏液族）
                output.accept(ICPMGelatinousItems.INSTANCE.getSlimeSphere());
                output.accept(ICPMGelatinousItems.INSTANCE.getOchreJelly());
                output.accept(ICPMGelatinousItems.INSTANCE.getCrimsonBlob());
                output.accept(ICPMGelatinousItems.INSTANCE.getOoze());
                output.accept(ICPMGelatinousItems.INSTANCE.getPudding());

                // 凝胶方块生成蛋（黏液族）
                output.accept(ICPMGelatinousItems.INSTANCE.getJellySpawnEgg());
                output.accept(ICPMGelatinousItems.INSTANCE.getBlobSpawnEgg());
                output.accept(ICPMGelatinousItems.INSTANCE.getOozeSpawnEgg());
                output.accept(ICPMGelatinousItems.INSTANCE.getPuddingSpawnEgg());

                // 土元素生成蛋
                output.accept(ICPMEarthElementalItems.INSTANCE.getEarthElementalSpawnEgg());

                // 骷髅变种生成蛋
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getLongdeadSpawnEgg());
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getLongdeadGuardianSpawnEgg());
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getBoneLordSpawnEgg());
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getAnnihilationSkeletonSpawnEgg());

                // 蜘蛛变种生成蛋
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getWoodSpiderSpawnEgg());
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getCaveSpiderVariantSpawnEgg());
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getBlackWidowSpawnEgg());
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getPhaseSpiderSpawnEgg());
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getDemonSpiderSpawnEgg());

                // 地狱犬生成蛋
                output.accept(ICPMMonsterSpawnEggs.INSTANCE.getHellhoundSpawnEgg());

                // ===== ICPM 工作台（按等级排列）=====
                // 燧石工作台：同一方块，按原木衍生变体逐个加入（外观由 block_state 组件区分）
                if (ICPMBlocks.FLINT_WORKBENCH != null) {
                    for (BlockICPMFlintWorkbench.WoodType wood : BlockICPMFlintWorkbench.WoodType.values()) {
                        output.accept(FlintWorkbenchItem.createStack(ICPMBlocks.FLINT_WORKBENCH, wood));
                    }
                }
                if (ICPMBlocks.COPPER_WORKBENCH != null)
                    output.accept(ICPMBlocks.COPPER_WORKBENCH.asItem().getDefaultInstance());
                if (ICPMBlocks.SILVER_WORKBENCH != null)
                    output.accept(ICPMBlocks.SILVER_WORKBENCH.asItem().getDefaultInstance());
                if (ICPMBlocks.GOLD_WORKBENCH != null)
                    output.accept(ICPMBlocks.GOLD_WORKBENCH.asItem().getDefaultInstance());
                if (ICPMBlocks.IRON_WORKBENCH != null)
                    output.accept(ICPMBlocks.IRON_WORKBENCH.asItem().getDefaultInstance());
                if (ICPMBlocks.ANCIENT_METAL_WORKBENCH != null)
                    output.accept(ICPMBlocks.ANCIENT_METAL_WORKBENCH.asItem().getDefaultInstance());
                if (ICPMBlocks.MITHRIL_WORKBENCH != null)
                    output.accept(ICPMBlocks.MITHRIL_WORKBENCH.asItem().getDefaultInstance());
                if (ICPMBlocks.ADAMANTIUM_WORKBENCH != null)
                    output.accept(ICPMBlocks.ADAMANTIUM_WORKBENCH.asItem().getDefaultInstance());

                // ===== ICPM 熔炉（粘土/硬化粘土/沙石/黑曜石/地狱岩；原石熔炉=原版熔炉）=====
                if (ICPMBlocks.CLAY_FURNACE != null)
                    output.accept(ICPMBlocks.CLAY_FURNACE.asItem().getDefaultInstance());
                if (ICPMBlocks.HARDENED_CLAY_FURNACE != null)
                    output.accept(ICPMBlocks.HARDENED_CLAY_FURNACE.asItem().getDefaultInstance());
                if (ICPMBlocks.SANDSTONE_FURNACE != null)
                    output.accept(ICPMBlocks.SANDSTONE_FURNACE.asItem().getDefaultInstance());
                if (ICPMBlocks.OBSIDIAN_FURNACE != null)
                    output.accept(ICPMBlocks.OBSIDIAN_FURNACE.asItem().getDefaultInstance());
                if (ICPMBlocks.NETHERRACK_FURNACE != null)
                    output.accept(ICPMBlocks.NETHERRACK_FURNACE.asItem().getDefaultInstance());

                // ===== ICPM 金属门（铜/铁门原版已有）=====
                if (ICPMBlocks.SILVER_DOOR != null)
                    output.accept(ICPMBlocks.SILVER_DOOR.asItem().getDefaultInstance());
                if (ICPMBlocks.GOLD_DOOR != null)
                    output.accept(ICPMBlocks.GOLD_DOOR.asItem().getDefaultInstance());
                if (ICPMBlocks.ANCIENT_METAL_DOOR != null)
                    output.accept(ICPMBlocks.ANCIENT_METAL_DOOR.asItem().getDefaultInstance());
                if (ICPMBlocks.MITHRIL_DOOR != null)
                    output.accept(ICPMBlocks.MITHRIL_DOOR.asItem().getDefaultInstance());
                if (ICPMBlocks.ADAMANTIUM_DOOR != null)
                    output.accept(ICPMBlocks.ADAMANTIUM_DOOR.asItem().getDefaultInstance());

                // ===== 绿宝石附魔台 =====
                if (ICPMBlocks.EMERALD_ENCHANTING_TABLE != null)
                    output.accept(ICPMBlocks.EMERALD_ENCHANTING_TABLE.asItem().getDefaultInstance());

                // ===== 金属箱（强箱，仅所有者可开）=====
                if (ICPMBlocks.SILVER_STRONGBOX != null)
                    output.accept(ICPMBlocks.SILVER_STRONGBOX.asItem().getDefaultInstance());
                if (ICPMBlocks.GOLD_STRONGBOX != null)
                    output.accept(ICPMBlocks.GOLD_STRONGBOX.asItem().getDefaultInstance());
                if (ICPMBlocks.IRON_STRONGBOX != null)
                    output.accept(ICPMBlocks.IRON_STRONGBOX.asItem().getDefaultInstance());
                if (ICPMBlocks.ANCIENT_METAL_STRONGBOX != null)
                    output.accept(ICPMBlocks.ANCIENT_METAL_STRONGBOX.asItem().getDefaultInstance());
                if (ICPMBlocks.MITHRIL_STRONGBOX != null)
                    output.accept(ICPMBlocks.MITHRIL_STRONGBOX.asItem().getDefaultInstance());
                if (ICPMBlocks.ADAMANTIUM_STRONGBOX != null)
                    output.accept(ICPMBlocks.ADAMANTIUM_STRONGBOX.asItem().getDefaultInstance());

                // ===== 符文石（16 变体，1.6.4 BlockRunestone 符文门框架 4 角）=====
                if (ICPMBlocks.MITHRIL_RUNESTONE != null) {
                    for (int i = 0; i < 16; i++) {
                        output.accept(RunestoneItem.createStack(ICPMBlocks.MITHRIL_RUNESTONE, i));
                    }
                }
                if (ICPMBlocks.ADAMANTIUM_RUNESTONE != null) {
                    for (int i = 0; i < 16; i++) {
                        output.accept(RunestoneItem.createStack(ICPMBlocks.ADAMANTIUM_RUNESTONE, i));
                    }
                }

                // ===== 地核 =====
                if (ICPMBlocks.CORE != null)
                    output.accept(ICPMBlocks.CORE.asItem().getDefaultInstance());

                // ===== 粪便 =====
                output.accept(ICPMItems.MANURE);
            })
            .build();

        // 注册到 Fabric 注册表
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ICPM_GROUP_KEY, tab);
    }
}
