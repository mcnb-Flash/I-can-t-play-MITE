package name.icpm.curse;

import name.icpm.ICPM;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ICPM 女巫诅咒引擎 —— R196 Curse/WorldServer/EntityPlayer 诅咒机制的服务端移植。
 *
 * <p>架构（2026-09-02 按开发者要求调整）：诅咒本体是【单一 MobEffect 状态效果】
 * {@code icpm:witch_curse}（类似夜视/抗火的效果形态），具体诅咒类型为它的【变体】，
 * 以 amplifier = curse.id-1 编码。理由：
 * <ul>
 *   <li>玩家至多一个女巫诅咒 —— 原版效果系统天然保证唯一槽；</li>
 *   <li>效果随玩家 NBT 自动持久化（离线诅咒保留），无需为 active 诅咒另做存档；</li>
 *   <li>检测 = 一次 hasEffect + amplifier 比对（isCursed），各效果点共用同一入口。</li>
 * </ul>
 *
 * <p>R196 机制（忠实映射）：
 * <ul>
 *   <li>施咒：女巫对玩家施咒（{@code WorldServer.addCurse}，携带女巫 UUID + 6000 tick 延迟），
 *       已中咒或有 pending 时拒绝叠加；</li>
 *   <li>realize：到期由 checkCurses 生效 —— 本实现为给玩家施加无限时长
 *       {@code witch_curse}（amplifier 编码变体）；cannot_wear_armor 生效瞬间自动卸掉全身甲；</li>
 *   <li>effect_known：玩家首次触发诅咒效果时学会提示（描述文本）；</li>
 *   <li>解除：杀死施咒女巫（removeCursesForWitch）或饮用去咒药水（lift/removeCursesFromPlayer）。</li>
 * </ul>
 *
 * <p>存储差异（有记录）：R196 诅咒清单存于世界存档，离线玩家诅咒在女巫死亡时同样被移除；
 * 本移植中 pending/元数据（施咒女巫 UUID）随玩家 NBT 保存，active 效果本体由原版效果栈持久化；
 * 女巫死亡仅能移除在线玩家对应诅咒 —— 记录为已知边界。
 */
public final class ICPMCurseManager {

    /** 服务端单条诅咒档案（pending + active 元数据；active 本体在玩家效果槽）。 */
    public static final class CurseEntry {
        public ICPMCurse curse;
        public UUID witchUuid;
        /** realize 目标时刻（服务器世界时间 tick）。 */
        public long realizeAt = -1;
        /** pending→realized（realized 后效果本体已施加，此表仅存元数据）。 */
        public boolean realized;
        /** 玩家是否已"学会"诅咒效果（首次触发后 true）。 */
        public boolean effectKnown;
    }

    private static final String TAG_ID = "icpm_curse_id";
    private static final String TAG_WITCH = "icpm_curse_witch";
    private static final String TAG_REALIZE_AT = "icpm_curse_realize_at";
    private static final String TAG_REALIZED = "icpm_curse_realized";
    private static final String TAG_KNOWN = "icpm_curse_known";

    /** 女巫施咒延迟：R196 addCurse(..., 6000) = 5 分钟。 */
    public static final int CURSE_DELAY_TICKS = 6000;

    private static final Map<UUID, CurseEntry> ENTRIES = new HashMap<>();
    /** 已"学会"效果的玩家（服务端会话内）；防重复发送 desc 提示。 */
    private static final Set<UUID> LEARNED = new HashSet<>();

    private ICPMCurseManager() {
    }

    // ==================== 查询（效果本体为准） ====================

    /** 是否正被【任一】女巫诅咒命中。 */
    public static boolean hasAnyCurse(Entity entity) {
        return entity instanceof LivingEntity le && le.hasEffect(ICPM.WITCH_CURSE_HOLDER);
    }

    /** 是否正被指定诅咒命中（变体 = amplifier）。effect 检查点以 learnEffect=true 请求时
     *  首次命中发送"学会效果"提示（R196 hasCurse(curse, true)）。 */
    public static boolean isCursed(Entity entity, ICPMCurse curse, boolean learnEffect) {
        if (!(entity instanceof LivingEntity le) || le.level().isClientSide()) {
            return false;
        }
        MobEffectInstance inst = le.getEffect(ICPM.WITCH_CURSE_HOLDER);
        if (inst == null || inst.getAmplifier() + 1 != curse.id()) {
            return false;
        }
        if (learnEffect && entity instanceof ServerPlayer player && LEARNED.add(player.getUUID())) {
            player.sendSystemMessage(Component.translatable(curse.descKey()));
        }
        return true;
    }

    public static boolean isCursed(Entity entity, ICPMCurse curse) {
        return isCursed(entity, curse, false);
    }

    /** 当前生效的诅咒变体（无则 null）。 */
    public static ICPMCurse getActive(Entity entity) {
        if (!(entity instanceof LivingEntity le)) {
            return null;
        }
        MobEffectInstance inst = le.getEffect(ICPM.WITCH_CURSE_HOLDER);
        return inst == null ? null : ICPMCurse.fromId(inst.getAmplifier() + 1);
    }

    /** 是否有尚未 realize 的诅咒（防叠加：R196 hasCursePending）。 */
    public static boolean hasPending(ServerPlayer player) {
        CurseEntry e = ENTRIES.get(player.getUUID());
        return e != null && !e.realized;
    }

    // ==================== 施咒 / realize / 解除 ====================

    /** 女巫对玩家施咒（R196 WorldServer.addCurse）。施咒者已死/玩家已咒或有 pending 则拒绝。 */
    public static void curse(ServerPlayer player, Entity witch, ICPMCurse curse, int delayTicks) {
        if (witch != null && !witch.isAlive()) {
            return;
        }
        if (hasAnyCurse(player) || hasPending(player)) {
            return;
        }
        CurseEntry e = new CurseEntry();
        e.curse = curse;
        e.witchUuid = witch == null ? null : witch.getUUID();
        e.realizeAt = ((ServerLevel) player.level()).getGameTime() + Math.max(1, delayTicks);
        ENTRIES.put(player.getUUID(), e);
    }

    /** 每服务端 tick 检查 pending 到期并施加效果本体（R196 checkCurses realize 部分）。 */
    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CurseEntry e = ENTRIES.get(player.getUUID());
            if (e == null || e.realized) {
                continue;
            }
            if (((ServerLevel) player.level()).getGameTime() < e.realizeAt) {
                continue;
            }
            e.realized = true;
            // 施加无限时长诅咒效果（变体 = amplifier）
            player.addEffect(new MobEffectInstance(ICPM.WITCH_CURSE_HOLDER, -1,
                    e.curse.id() - 1, false, false, false));
            player.sendSystemMessage(Component.translatable("curse.realized",
                    Component.translatable(e.curse.titleKey())));
            if (e.curse == ICPMCurse.CANNOT_WEAR_ARMOR) {
                dropAllArmor(player); // R196 onCurseRealized
                e.effectKnown = true;
                LEARNED.add(player.getUUID());
                player.sendSystemMessage(Component.translatable(e.curse.descKey()));
            }
        }
    }

    /** 饮用去咒药水等解除诅咒（R196 removeCursesFromPlayer）。 */
    public static void lift(ServerPlayer player) {
        player.removeEffect(ICPM.WITCH_CURSE_HOLDER);
        ENTRIES.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable("curse.lifted"));
    }

    /** 女巫死亡：移除其施加的全部诅咒（R196 removeCursesForWitch，含 pending 与已生效）。 */
    public static void removeForWitch(Entity witch) {
        if (witch == null || witch.level().isClientSide()) {
            return;
        }
        UUID witchUuid = witch.getUUID();
        Iterator<Map.Entry<UUID, CurseEntry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CurseEntry> en = it.next();
            CurseEntry e = en.getValue();
            if (e.witchUuid == null || !e.witchUuid.equals(witchUuid)) {
                continue;
            }
            it.remove();
            if (witch.level() instanceof ServerLevel sl) {
                Entity p = sl.getPlayerByUUID(en.getKey());
                if (p instanceof ServerPlayer sp) {
                    sp.removeEffect(ICPM.WITCH_CURSE_HOLDER); // active 也一并解除
                    sp.sendSystemMessage(Component.translatable("curse.lifted"));
                }
            }
        }
    }

    /** realize 为 cannot_wear_armor 时自动脱甲（R196 inventory.dropAllArmor）。 */
    private static void dropAllArmor(ServerPlayer player) {
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            net.minecraft.world.entity.EquipmentSlot s = slot;
            if (s != net.minecraft.world.entity.EquipmentSlot.HEAD
                    && s != net.minecraft.world.entity.EquipmentSlot.CHEST
                    && s != net.minecraft.world.entity.EquipmentSlot.LEGS
                    && s != net.minecraft.world.entity.EquipmentSlot.FEET) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            player.setItemSlot(slot, ItemStack.EMPTY);
            player.drop(stack, false, false);
        }
    }

    // ==================== NBT 持久化（pending 与元数据；效果本体由原版效果栈持久化） ====================

    public static void save(Player player, ValueOutput tag) {
        if (player.level().isClientSide()) {
            return;
        }
        CurseEntry e = ENTRIES.get(player.getUUID());
        if (e == null) {
            return;
        }
        tag.putInt(TAG_ID, e.curse.id());
        tag.putString(TAG_WITCH, e.witchUuid == null ? "" : e.witchUuid.toString());
        tag.putLong(TAG_REALIZE_AT, e.realizeAt);
        tag.putInt(TAG_REALIZED, e.realized ? 1 : 0);
        tag.putInt(TAG_KNOWN, e.effectKnown ? 1 : 0);
    }

    public static void load(Player player, ValueInput tag) {
        if (player.level().isClientSide()) {
            return;
        }
        if (!tag.getInt(TAG_ID).isPresent()) {
            return;
        }
        CurseEntry e = new CurseEntry();
        e.curse = ICPMCurse.fromId(tag.getInt(TAG_ID).orElse(0));
        if (e.curse == null) {
            return;
        }
        String w = tag.getString(TAG_WITCH).orElse("");
        e.witchUuid = w.isEmpty() ? null : UUID.fromString(w);
        e.realizeAt = tag.getLong(TAG_REALIZE_AT).orElse(-1L);
        e.realized = tag.getInt(TAG_REALIZED).orElse(0) != 0;
        e.effectKnown = tag.getInt(TAG_KNOWN).orElse(0) != 0;
        if (e.effectKnown) {
            LEARNED.add(player.getUUID());
        }
        ENTRIES.put(player.getUUID(), e);
    }
}
