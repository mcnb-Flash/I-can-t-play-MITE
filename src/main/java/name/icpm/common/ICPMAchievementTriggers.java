package name.icpm.common;

import name.icpm.ICPM;
import name.icpm.item.ICPMItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

/**
 * ICPM 成就系统代码触发器（以 FTB Quests「ICPM主线」任务链为蓝本）。
 *
 * 大部分成就用 inventory_changed / enchanted_item 触发器自动达成；
 * 以下特殊条件无法用原版触发器表达，在此以 minecraft:impossible + 手动 award 实现：
 *   - 穿戴全套铜甲 / 铜链甲 / 艾德曼甲
 *   - 进入地下世界（我们需要再深入些）/ 进入末地（结束了？）
 *   - 击杀任意怪物（战胜恐惧）
 *   - 死亡一次（困难的世界）
 *   - 到达末地要塞（旅途之路）
 */
public final class ICPMAchievementTriggers {

    private ICPMAchievementTriggers() {
    }

    public static void init() {
        // 每 20 tick 批量检查状态类成就
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickCheck(player);
            }
        });

        // 击杀任意怪物（战胜恐惧）；死亡一次（困难的世界）
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                grant(player, "hard_world");
                return;
            }
            if (entity instanceof Monster && source.getEntity() instanceof ServerPlayer killer) {
                grant(killer, "face_fear");
            }
        });
    }

    private static void tickCheck(ServerPlayer p) {
        // 穿戴全套护甲（铜甲 / 铜链甲 / 艾德曼甲）
        Item head = p.getItemBySlot(EquipmentSlot.HEAD).getItem();
        Item chest = p.getItemBySlot(EquipmentSlot.CHEST).getItem();
        Item legs = p.getItemBySlot(EquipmentSlot.LEGS).getItem();
        Item feet = p.getItemBySlot(EquipmentSlot.FEET).getItem();

        if (isCopper(head) && isCopper(chest) && isCopper(legs) && isCopper(feet)) {
            grant(p, "copper_armor");
        }
        if (isCopperChainmail(head) && isCopperChainmail(chest) && isCopperChainmail(legs) && isCopperChainmail(feet)) {
            grant(p, "copper_chainmail");
        }
        if (isAdamantium(head) && isAdamantium(chest) && isAdamantium(legs) && isAdamantium(feet)) {
            grant(p, "adamantium_armor");
        }

        // 维度：进入地下世界 / 末地
        ResourceKey<Level> dim = p.level().dimension();
        if (dim == ICPMPortalHandler.UNDERWORLD_KEY) {
            grant(p, "go_deeper");
        } else if (dim == Level.END) {
            grant(p, "the_end");
        }

        // 旅途之路：到达末地要塞（主世界）
        if (p.level() instanceof ServerLevel sl) {
            var start = sl.structureManager()
                .getStructureWithPieceAt(p.blockPosition(), holder -> holder.is(BuiltinStructures.STRONGHOLD));
            if (start != null && start.isValid()) {
                grant(p, "journey");
            }
        }
    }

    /** 原版铜甲（1.21.11 原版含铜装备） */
    private static boolean isCopper(Item item) {
        return item == Items.COPPER_HELMET || item == Items.COPPER_CHESTPLATE
            || item == Items.COPPER_LEGGINGS || item == Items.COPPER_BOOTS;
    }

    /** ICPM 铜链甲 */
    private static boolean isCopperChainmail(Item item) {
        return item == ICPMItems.COPPER_CHAINMAIL_HELMET || item == ICPMItems.COPPER_CHAINMAIL_CHESTPLATE
            || item == ICPMItems.COPPER_CHAINMAIL_LEGGINGS || item == ICPMItems.COPPER_CHAINMAIL_BOOTS;
    }

    /** 艾德曼系护甲（板甲或链甲，注册名以 adamantium_ 开头） */
    private static boolean isAdamantium(Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item);
        return key != null && key.getPath().startsWith("adamantium_")
            && (key.getPath().endsWith("helmet") || key.getPath().endsWith("chestplate")
                || key.getPath().endsWith("leggings") || key.getPath().endsWith("boots"));
    }

    /** 手动授予 impossible 型成就 */
    private static void grant(ServerPlayer player, String id) {
        var server = ((ServerLevel) player.level()).getServer();
        var holder = server.getAdvancements().get(ICPM.id(id));
        if (holder != null) {
            player.getAdvancements().award(holder, "impossible");
        }
    }
}
