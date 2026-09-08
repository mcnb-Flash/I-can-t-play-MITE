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
    /** 女巫低吟：永久诅咒的 curse.id（-1 = 无）。独立于普通诅咒槽，随玩家 NBT 保存。 */
    private static final String TAG_WHISPER = "icpm_whisper_id";

    /** 女巫施咒延迟：ICPM 调整为立即诅咒（&lt;=0 即时生效）。R196 原版为 6000 tick（5 分钟）。 */
    public static final int CURSE_DELAY_TICKS = 0;

    private static final Map<UUID, CurseEntry> ENTRIES = new HashMap<>();
    /** 已"学会"效果的玩家（服务端会话内）；防重复发送 desc 提示。 */
    private static final Set<UUID> LEARNED = new HashSet<>();

    /** 女巫低吟（witchWhisper 选项）的永久诅咒 id 表：uuid → curse.id；独立于普通诅咒单槽。 */
    private static final Map<UUID, Integer> WHISPER_CURSE = new HashMap<>();

    private ICPMCurseManager() {
    }

    // ==================== 查询（效果本体为准） ====================

    /** 是否正被【任一】女巫诅咒命中（含女巫低吟的永久诅咒）。 */
    public static boolean hasAnyCurse(Entity entity) {
        if (entity instanceof LivingEntity le && le.hasEffect(ICPM.WITCH_CURSE_HOLDER)) {
            return true;
        }
        return isWhispered(entity);
    }

    /** 是否正被普通（非低吟）诅咒命中——效果槽判定。 */
    public static boolean hasRegularCurse(Entity entity) {
        return entity instanceof LivingEntity le && le.hasEffect(ICPM.WITCH_CURSE_HOLDER);
    }

    /** 普通诅咒槽是否空闲（用于女巫再次施咒：低吟诅咒不占普通槽，不阻挡新诅咒）。 */
    public static boolean curseSlotFree(ServerPlayer player) {
        return !hasRegularCurse(player) && !hasPending(player);
    }

    /** 是否被女巫低吟永久诅咒。 */
    public static boolean isWhispered(Entity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return false;
        }
        return WHISPER_CURSE.containsKey(entity.getUUID());
    }

    /** 女巫低吟的诅咒类型（无则 null）。 */
    public static ICPMCurse getWhisperCurse(Entity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return null;
        }
        Integer id = WHISPER_CURSE.get(entity.getUUID());
        return id == null ? null : ICPMCurse.fromId(id);
    }

    /** 是否正被指定诅咒命中（变体 = amplifier）。effect 检查点以 learnEffect=true 请求时
     *  首次命中发送"学会效果"提示（R196 hasCurse(curse, true)）。
     *  普通诅咒命中或女巫低吟永久诅咒命中都视为命中。 */
    public static boolean isCursed(Entity entity, ICPMCurse curse, boolean learnEffect) {
        if (!(entity instanceof LivingEntity le) || le.level().isClientSide()) {
            return false;
        }
        MobEffectInstance inst = le.getEffect(ICPM.WITCH_CURSE_HOLDER);
        boolean hit = inst != null && inst.getAmplifier() + 1 == curse.id();
        if (!hit) {
            // 女巫低吟的永久诅咒同样生效（不可祛除/变更类型）
            Integer w = WHISPER_CURSE.get(entity.getUUID());
            hit = w != null && w == curse.id();
        }
        if (hit && learnEffect && entity instanceof ServerPlayer player && LEARNED.add(player.getUUID())) {
            player.sendSystemMessage(Component.translatable(curse.descKey()));
        }
        return hit;
    }

    public static boolean isCursed(Entity entity, ICPMCurse curse) {
        return isCursed(entity, curse, false);
    }

    /** 当前生效的诅咒变体（普通优先，其次女巫低吟；无则 null）。 */
    public static ICPMCurse getActive(Entity entity) {
        if (!(entity instanceof LivingEntity le)) {
            return null;
        }
        MobEffectInstance inst = le.getEffect(ICPM.WITCH_CURSE_HOLDER);
        ICPMCurse regular = inst == null ? null : ICPMCurse.fromId(inst.getAmplifier() + 1);
        if (regular != null) {
            return regular;
        }
        return getWhisperCurse(entity);
    }

    /** 是否有尚未 realize 的诅咒（防叠加：R196 hasCursePending）。 */
    public static boolean hasPending(ServerPlayer player) {
        CurseEntry e = ENTRIES.get(player.getUUID());
        return e != null && !e.realized;
    }

    // ==================== 施咒 / realize / 解除 ====================

    /** 女巫对玩家施咒（R196 WorldServer.addCurse 的守卫语义）。
     *  玩家已有【普通】诅咒（含已生效/未生效）时直接拒绝，不再尝试叠加；
     *  女巫低吟的永久诅咒不占普通槽，不阻挡新诅咒（可叠加、不覆盖旧诅咒）。
     *  @param delayTicks &lt;=0 表示立即诅咒（ICPM 调整：不再等待 6000 tick，施咒即生效）。 */
    public static void curse(ServerPlayer player, Entity witch, ICPMCurse curse, int delayTicks) {
        if (witch != null && !witch.isAlive()) {
            return;
        }
        if (!curseSlotFree(player)) {
            return;
        }
        CurseEntry e = new CurseEntry();
        e.curse = curse;
        e.witchUuid = witch == null ? null : witch.getUUID();
        e.realizeAt = ((ServerLevel) player.level()).getGameTime() + Math.max(1, delayTicks);
        e.realized = delayTicks <= 0; // 立即诅咒：无需等 realize
        ENTRIES.put(player.getUUID(), e);
        if (e.realized) {
            applyRealized(player, e);
        }
    }

    /** realize 后施加效果本体 + realize 提示（R196 checkCurses realize + onCurseRealized）。 */
    private static void applyRealized(ServerPlayer player, CurseEntry e) {
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

    /** 每服务端 tick 检查 pending 到期并施加效果本体（R196 checkCurses realize 部分），
     *  并同步"女巫低吟"永久诅咒与 config/icpm.json 开关。 */
    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CurseEntry e = ENTRIES.get(player.getUUID());
            if (e != null && !e.realized) {
                if (((ServerLevel) player.level()).getGameTime() >= e.realizeAt) {
                    e.realized = true;
                    applyRealized(player, e);
                }
            }
            syncWhisper(player);
        }
    }

    /** 女巫低吟：config 开启且该玩家尚无永久诅咒 → 随机定一枚并持久化（仅一次，永不更换）；
     *  config 关闭 → 移除（效果随 NBT 清除）。普通诅咒槽不受影响。 */
    private static void syncWhisper(ServerPlayer player) {
        UUID uuid = player.getUUID();
        boolean enabled = name.icpm.common.ICPMConfig.isWitchWhisperEnabled();
        Integer cur = WHISPER_CURSE.get(uuid);
        if (enabled) {
            if (cur == null) {
                ICPMCurse curse = ICPMCurse.getRandom(player.getRandom());
                WHISPER_CURSE.put(uuid, curse.id());
                LEARNED.add(uuid);
                player.sendSystemMessage(Component.translatable("message.icpm.whisper_curse",
                        Component.translatable(curse.titleKey())));
                player.sendSystemMessage(Component.translatable(curse.descKey()));
            }
        } else {
            if (cur != null) {
                WHISPER_CURSE.remove(uuid);
            }
        }
    }

    /** 饮用去咒药水等解除【普通】诅咒（R196 removeCursesFromPlayer）。
     *  女巫低吟的永久诅咒不受影响——若仍在则追加提示。 */
    public static void lift(ServerPlayer player) {
        player.removeEffect(ICPM.WITCH_CURSE_HOLDER);
        ENTRIES.remove(player.getUUID());
        if (isWhispered(player)) {
            player.sendSystemMessage(Component.translatable("message.icpm.whisper_remains"));
        } else {
            player.sendSystemMessage(Component.translatable("curse.lifted"));
        }
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
        if (e != null) {
            tag.putInt(TAG_ID, e.curse.id());
            tag.putString(TAG_WITCH, e.witchUuid == null ? "" : e.witchUuid.toString());
            tag.putLong(TAG_REALIZE_AT, e.realizeAt);
            tag.putInt(TAG_REALIZED, e.realized ? 1 : 0);
            tag.putInt(TAG_KNOWN, e.effectKnown ? 1 : 0);
        }
        // 女巫低吟永久诅咒独立持久化（-1 = 无）
        Integer w = WHISPER_CURSE.get(player.getUUID());
        tag.putInt(TAG_WHISPER, w == null ? -1 : w);
    }

    public static void load(Player player, ValueInput tag) {
        if (player.level().isClientSide()) {
            return;
        }
        UUID uuid = player.getUUID();
        if (tag.getInt(TAG_ID).isPresent()) {
            CurseEntry e = new CurseEntry();
            e.curse = ICPMCurse.fromId(tag.getInt(TAG_ID).orElse(0));
            if (e.curse != null) {
                String w = tag.getString(TAG_WITCH).orElse("");
                e.witchUuid = w.isEmpty() ? null : UUID.fromString(w);
                e.realizeAt = tag.getLong(TAG_REALIZE_AT).orElse(-1L);
                e.realized = tag.getInt(TAG_REALIZED).orElse(0) != 0;
                e.effectKnown = tag.getInt(TAG_KNOWN).orElse(0) != 0;
                if (e.effectKnown) {
                    LEARNED.add(uuid);
                }
                ENTRIES.put(uuid, e);
            }
        }
        // 女巫低吟永久诅咒恢复
        if (tag.getInt(TAG_WHISPER).isPresent()) {
            int w = tag.getInt(TAG_WHISPER).orElse(-1);
            if (w >= 1 && ICPMCurse.fromId(w) != null) {
                WHISPER_CURSE.put(uuid, w);
                LEARNED.add(uuid);
            } else {
                WHISPER_CURSE.remove(uuid);
            }
        }
    }
}
