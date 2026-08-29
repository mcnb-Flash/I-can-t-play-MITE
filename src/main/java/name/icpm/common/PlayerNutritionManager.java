package name.icpm.common;

import name.icpm.component.NutritionComponent;
import name.icpm.network.NutritionSyncPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ICPM 营养值管理器
 * 移植自 ICPM R196 (1.6.4) 的营养系统，适配 1.21.11 Fabric
 *
 * 原版逻辑 (ux.java / jv.java):
 * - 每 tick 消耗 1 点蛋白质/必需脂肪/植物营养素（创造模式除外）
 * - 食物恢复量 = nutrition * 8000（nutrition 为食物的营养等级 1-20）
 * - 营养不良：protein==0 OR phytonutrients==0 → 回血速度降为 25%，饥饿消耗 +50%
 * - 营养上限：160000
 */
public class PlayerNutritionManager {
    public static final int MAX_NUTRITION = NutritionComponent.MAX;

    private static final Map<UUID, NutritionComponent> PLAYER_NUTRITION = new HashMap<>();

    private static final Map<String, int[]> FOOD_NUTRITION = new HashMap<>();

    static {
        // ================================================================
        // 数值来源：ICPM R196 (1.6.4) Item.java / ItemMeat.java / ItemFood.java / ItemBowl.java
        // 恢复量 = 食物营养等级(nutrition) × 8000（营养上限 160000 = 20 级）
        // 肉(ItemMeat)：恒有蛋白质；鱼类额外有必需脂肪；其余标记见各注册行
        // ================================================================
        // ----- 生肉（ItemMeat：protein = nutrition×8000）-----
        FOOD_NUTRITION.put("minecraft:beef", new int[]{5 * 8000, 0, 0});          // beefRaw(5)
        FOOD_NUTRITION.put("minecraft:porkchop", new int[]{4 * 8000, 0, 0});      // porkRaw(4)
        FOOD_NUTRITION.put("minecraft:chicken", new int[]{3 * 8000, 0, 0});       // chickenRaw(3)
        FOOD_NUTRITION.put("minecraft:mutton", new int[]{3 * 8000, 0, 0});        // lambchopRaw(3)
        FOOD_NUTRITION.put("minecraft:rabbit", new int[]{3 * 8000, 0, 0});        // ICPM 无兔子，按小肉等级
        FOOD_NUTRITION.put("minecraft:cod", new int[]{3 * 8000, 3 * 8000, 0});    // fishRaw(3, 含脂肪)
        FOOD_NUTRITION.put("minecraft:salmon", new int[]{5 * 8000, 5 * 8000, 0}); // fishLargeRaw(5, 含脂肪)
        FOOD_NUTRITION.put("minecraft:tropical_fish", new int[]{1 * 8000, 1 * 8000, 0}); // 按小鱼推断
        FOOD_NUTRITION.put("minecraft:pufferfish", new int[]{1 * 8000, 0, 0});    // 按小鱼推断
        FOOD_NUTRITION.put("minecraft:rotten_flesh", new int[]{1 * 8000, 0, 0});  // rottenFlesh(1)
        FOOD_NUTRITION.put("minecraft:spider_eye", new int[]{1 * 8000, 0, 0});    // spiderEye(1, protein)

        // ----- 熟肉 -----
        FOOD_NUTRITION.put("minecraft:cooked_beef", new int[]{10 * 8000, 0, 0});       // beefCooked(10)
        FOOD_NUTRITION.put("minecraft:cooked_porkchop", new int[]{8 * 8000, 0, 0});    // porkCooked(8)
        FOOD_NUTRITION.put("minecraft:cooked_chicken", new int[]{6 * 8000, 0, 0});     // chickenCooked(6)
        FOOD_NUTRITION.put("minecraft:cooked_mutton", new int[]{6 * 8000, 0, 0});      // lambchopCooked(6)
        FOOD_NUTRITION.put("minecraft:cooked_rabbit", new int[]{6 * 8000, 0, 0});      // 按熟肉推断
        FOOD_NUTRITION.put("minecraft:cooked_cod", new int[]{6 * 8000, 6 * 8000, 0});  // fishCooked(6, 含脂肪)
        FOOD_NUTRITION.put("minecraft:cooked_salmon", new int[]{10 * 8000, 10 * 8000, 0}); // fishLargeCooked(10, 含脂肪)

        // ----- 植物类（ItemSeedFood / ItemFood：phyto = nutrition×8000）-----
        FOOD_NUTRITION.put("minecraft:carrot", new int[]{0, 0, 2 * 8000});        // carrot(2, phyto)
        FOOD_NUTRITION.put("minecraft:potato", new int[]{0, 0, 0});               // potato 无营养标记
        FOOD_NUTRITION.put("minecraft:beetroot", new int[]{0, 0, 1 * 8000});      // ICPM 无甜菜根，按蔬菜推断
        FOOD_NUTRITION.put("minecraft:apple", new int[]{0, 0, 1 * 8000});         // appleRed(1, phyto)
        FOOD_NUTRITION.put("minecraft:melon_slice", new int[]{0, 0, 1 * 8000});   // melon(1, phyto)
        FOOD_NUTRITION.put("minecraft:sweet_berries", new int[]{0, 0, 1 * 8000}); // 按 blueberries(1) 推断
        FOOD_NUTRITION.put("minecraft:glow_berries", new int[]{0, 0, 1 * 8000});  // 按 blueberries(1) 推断
        FOOD_NUTRITION.put("minecraft:golden_carrot", new int[]{0, 0, 2 * 8000}); // goldenCarrot(2, phyto)

        // ----- 无营养标记的原版食物（R196 中这些恢复量均为 0）-----
        FOOD_NUTRITION.put("minecraft:wheat", new int[]{0, 0, 0});                // 材料，非食物
        FOOD_NUTRITION.put("minecraft:wheat_seeds", new int[]{0, 0, 0});          // ItemSeeds 无营养
        FOOD_NUTRITION.put("minecraft:baked_potato", new int[]{0, 0, 0});         // bakedPotato 无标记
        FOOD_NUTRITION.put("minecraft:bread", new int[]{0, 0, 0});                // bread 无标记
        FOOD_NUTRITION.put("minecraft:cookie", new int[]{0, 0, 0});               // cookie 无标记
        FOOD_NUTRITION.put("minecraft:sugar", new int[]{0, 0, 0});
        FOOD_NUTRITION.put("minecraft:poisonous_potato", new int[]{0, 0, 0});
        FOOD_NUTRITION.put("minecraft:mushroom_stew", new int[]{0, 0, 0});        // bowlMushroomStew 无标记

        // ----- 特殊/混合 -----
        FOOD_NUTRITION.put("minecraft:pumpkin_pie", new int[]{6 * 8000, 0, 6 * 8000}); // pumpkinPie(6, protein+phyto)
        FOOD_NUTRITION.put("minecraft:golden_apple", new int[]{4 * 8000, 0, 4 * 8000}); // 特殊物品保留
        FOOD_NUTRITION.put("minecraft:enchanted_golden_apple", new int[]{4 * 8000, 0, 4 * 8000});
        FOOD_NUTRITION.put("minecraft:egg", new int[]{1 * 8000, 0, 0});            // 本 mod 鸡蛋可食，按蛋白食物
        FOOD_NUTRITION.put("minecraft:beetroot_soup", new int[]{0, 0, 3 * 8000});  // ICPM 无，按蔬菜汤推断
        FOOD_NUTRITION.put("minecraft:rabbit_stew", new int[]{3 * 8000, 0, 3 * 8000}); // ICPM 无，按肉汤推断
        FOOD_NUTRITION.put("minecraft:honey_bottle", new int[]{0, 0, 1 * 8000});   // ICPM 无，按糖类推断
        FOOD_NUTRITION.put("minecraft:dried_kelp", new int[]{0, 0, 1 * 8000});     // ICPM 无，按植物推断

        // ===== ICPM 新增食物（R196 精确数值，nutrition 级 ×8000）=====
        FOOD_NUTRITION.put("icpm:flour", new int[]{0, 0, 0});                 // flour 是材料，非食物
        FOOD_NUTRITION.put("icpm:dough", new int[]{0, 0, 0});                 // dough 无标记
        FOOD_NUTRITION.put("icpm:chocolate", new int[]{0, 0, 0});             // chocolate 无标记
        FOOD_NUTRITION.put("icpm:cheese", new int[]{3 * 8000, 0, 0});         // cheese(3, protein)
        FOOD_NUTRITION.put("icpm:ice_cream", new int[]{4 * 8000, 0, 0});      // bowlIceCream(4, protein)
        FOOD_NUTRITION.put("icpm:sorbet", new int[]{0, 0, 2 * 8000});         // bowlSorbet(2, phyto)
        FOOD_NUTRITION.put("icpm:mashed_potato", new int[]{8 * 8000, 0, 0});  // bowlMashedPotato(8, protein)
        FOOD_NUTRITION.put("icpm:beef_stew", new int[]{16 * 8000, 0, 16 * 8000});   // bowlBeefStew(16, protein+phyto)
        FOOD_NUTRITION.put("icpm:chicken_soup", new int[]{10 * 8000, 0, 10 * 8000}); // bowlChickenSoup(10, protein+phyto)
        FOOD_NUTRITION.put("icpm:vegetable_soup", new int[]{0, 0, 6 * 8000}); // bowlVegetableSoup(6, phyto)
        FOOD_NUTRITION.put("icpm:vegetable_soup_cream", new int[]{7 * 8000, 0, 7 * 8000}); // (7, protein+phyto)
        FOOD_NUTRITION.put("icpm:mushroom_soup_cream", new int[]{5 * 8000, 0, 0}); // (5, protein)
        FOOD_NUTRITION.put("icpm:pumpkin_soup", new int[]{0, 0, 2 * 8000});   // bowlPumpkinSoup(2, phyto)
        FOOD_NUTRITION.put("icpm:salad", new int[]{0, 0, 1 * 8000});          // bowlSalad(1, phyto)
        FOOD_NUTRITION.put("icpm:porridge", new int[]{0, 0, 2 * 8000});       // bowlPorridge(2, phyto)
        FOOD_NUTRITION.put("icpm:cereal", new int[]{2 * 8000, 0, 0});         // bowlCereal(2, protein)
        FOOD_NUTRITION.put("icpm:orange", new int[]{0, 0, 1 * 8000});         // orange(1, phyto)
        FOOD_NUTRITION.put("icpm:banana", new int[]{0, 0, 1 * 8000});         // banana(1, phyto)
        FOOD_NUTRITION.put("icpm:blueberry", new int[]{0, 0, 1 * 8000});      // blueberries(1, phyto)
        FOOD_NUTRITION.put("icpm:onion", new int[]{0, 0, 1 * 8000});          // onion(1, phyto)
        FOOD_NUTRITION.put("icpm:worm", new int[]{1 * 8000, 0, 0});           // wormRaw(1, protein)
        FOOD_NUTRITION.put("icpm:cooked_worm", new int[]{1 * 8000, 0, 0});    // wormCooked(1, protein)
        FOOD_NUTRITION.put("icpm:milk_bowl", new int[]{1 * 8000, 0, 0});      // bowlMilk(1, protein)
    }

    public static NutritionComponent getNutrition(Player player) {
        return PLAYER_NUTRITION.getOrDefault(player.getUUID(), NutritionComponent.DEFAULT);
    }

    public static void setNutrition(Player player, NutritionComponent nutrition) {
        PLAYER_NUTRITION.put(player.getUUID(), nutrition);
        syncToClient(player);
    }

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NutritionComponent nutrition = getNutrition(player);
            ServerPlayNetworking.send(serverPlayer, NutritionSyncPacket.fromComponent(nutrition));
        }
    }

    public static void addProtein(Player player, int amount) {
        NutritionComponent current = getNutrition(player);
        setNutrition(player, current.withProtein(current.protein() + amount));
    }

    public static void addEssentialFats(Player player, int amount) {
        NutritionComponent current = getNutrition(player);
        setNutrition(player, current.withEssentialFats(current.essentialFats() + amount));
    }

    public static void addPhytonutrients(Player player, int amount) {
        NutritionComponent current = getNutrition(player);
        setNutrition(player, current.withPhytonutrients(current.phytonutrients() + amount));
    }

    public static void tickNutritionDrain(Player player) {
        if (player.level().isClientSide()) return;
        if (player.isCreative()) return;

        NutritionComponent current = getNutrition(player);
        int p = Math.max(0, current.protein() - 1);
        int ef = Math.max(0, current.essentialFats() - 1);
        int ph = Math.max(0, current.phytonutrients() - 1);

        if (p != current.protein() || ef != current.essentialFats() || ph != current.phytonutrients()) {
            PLAYER_NUTRITION.put(player.getUUID(), new NutritionComponent(p, ef, ph));
            if (player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, NutritionSyncPacket.fromComponent(new NutritionComponent(p, ef, ph)));
            }
        }
    }

    public static void onFoodEaten(Player player, ItemStack food) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(food.getItem());
        if (itemId == null) return;

        String itemIdStr = itemId.toString();
        int[] nutrition = FOOD_NUTRITION.get(itemIdStr);

        if (nutrition != null) {
            addProtein(player, nutrition[0]);
            addEssentialFats(player, nutrition[1]);
            addPhytonutrients(player, nutrition[2]);
        }
    }

    public static void saveNutrition(Player player, CompoundTag tag) {
        NutritionComponent nutrition = getNutrition(player);
        tag.putInt("IcpmProtein", nutrition.protein());
        tag.putInt("IcpmEssentialFats", nutrition.essentialFats());
        tag.putInt("IcpmPhytonutrients", nutrition.phytonutrients());
    }

    public static void loadNutrition(Player player, CompoundTag tag) {
        int protein = tag.getInt("IcpmProtein").orElse(MAX_NUTRITION);
        int essentialFats = tag.getInt("IcpmEssentialFats").orElse(MAX_NUTRITION);
        int phytonutrients = tag.getInt("IcpmPhytonutrients").orElse(MAX_NUTRITION);
        protein = Math.min(MAX_NUTRITION, Math.max(0, protein));
        essentialFats = Math.min(MAX_NUTRITION, Math.max(0, essentialFats));
        phytonutrients = Math.min(MAX_NUTRITION, Math.max(0, phytonutrients));
        PLAYER_NUTRITION.put(player.getUUID(), new NutritionComponent(protein, essentialFats, phytonutrients));
    }

    public static void onPlayerJoin(Player player) {
        syncToClient(player);
    }

    public static void loadFromDisk(Player player, MinecraftServer server) {
        if (player.level().isClientSide()) return;
        try {
            java.nio.file.Path playerDataDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR);
            java.nio.file.Path playerFile = playerDataDir.resolve(player.getUUID() + ".dat");
            if (java.nio.file.Files.exists(playerFile)) {
                CompoundTag root = NbtIo.readCompressed(playerFile, NbtAccounter.unlimitedHeap());
                if (root != null && root.contains("IcpmProtein")) {
                    int protein = root.getInt("IcpmProtein").orElse(MAX_NUTRITION);
                    int essentialFats = root.getInt("IcpmEssentialFats").orElse(MAX_NUTRITION);
                    int phytonutrients = root.getInt("IcpmPhytonutrients").orElse(MAX_NUTRITION);
                    protein = Math.min(MAX_NUTRITION, Math.max(0, protein));
                    essentialFats = Math.min(MAX_NUTRITION, Math.max(0, essentialFats));
                    phytonutrients = Math.min(MAX_NUTRITION, Math.max(0, phytonutrients));
                    PLAYER_NUTRITION.put(player.getUUID(), new NutritionComponent(protein, essentialFats, phytonutrients));
                    return;
                }
            }
        } catch (Exception e) {
        }
        PLAYER_NUTRITION.put(player.getUUID(), NutritionComponent.DEFAULT);
    }

    /**
     * 随玩家 NBT 落盘（addAdditionalSaveData 调用）。
     * 不再单独写 playerdata 文件，避免与原版玩家保存互相覆盖导致数据丢失。
     */
    public static void save(Player player, ValueOutput tag) {
        NutritionComponent nutrition = getNutrition(player);
        tag.putInt("IcpmProtein", nutrition.protein());
        tag.putInt("IcpmEssentialFats", nutrition.essentialFats());
        tag.putInt("IcpmPhytonutrients", nutrition.phytonutrients());
    }

    /**
     * 从玩家 NBT 读取（readAdditionalSaveData 调用）。
     * 若玩家 NBT 中尚无常量（旧存档），尝试从旧 playerdata 文件迁移一次；都没有则回到默认满值。
     */
    public static void load(Player player, ValueInput tag) {
        int protein = tag.getInt("IcpmProtein").orElse(-1);
        int essentialFats = tag.getInt("IcpmEssentialFats").orElse(-1);
        int phytonutrients = tag.getInt("IcpmPhytonutrients").orElse(-1);
        if (protein >= 0 || essentialFats >= 0 || phytonutrients >= 0) {
            PLAYER_NUTRITION.put(player.getUUID(), new NutritionComponent(
                    Math.min(MAX_NUTRITION, Math.max(0, protein < 0 ? MAX_NUTRITION : protein)),
                    Math.min(MAX_NUTRITION, Math.max(0, essentialFats < 0 ? MAX_NUTRITION : essentialFats)),
                    Math.min(MAX_NUTRITION, Math.max(0, phytonutrients < 0 ? MAX_NUTRITION : phytonutrients))));
            return;
        }
        // 旧存档迁移：从旧的 playerdata 文件读取（仅一次），之后会以玩家 NBT 为准
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            loadFromDisk(player, server);
        } else {
            PLAYER_NUTRITION.put(player.getUUID(), NutritionComponent.DEFAULT);
        }
    }
}
