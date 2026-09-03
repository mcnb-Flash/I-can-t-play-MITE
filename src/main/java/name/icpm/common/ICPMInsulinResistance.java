package name.icpm.common;

import name.icpm.ICPM;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ICPM 胰岛素抵抗 / 糖尿病 —— R196 EntityPlayerMP.setInsulinResistance / addInsulinResistance /
 * decrementInsulinResistance 忠实移植（与蛋白质/植物营养素同构的营养类机制，非药水状态）。
 *
 * <p>R196 判决要点（src_deobf 实证）：
 * <ul>
 *   <li>状态 = 玩家胰岛素抵抗值 {@code insulin_resistance}（int，0..192000）+ 等级
 *       (mild/moderate/severe)；进食高糖食物时累加，每 tick −1 自然代谢。</li>
 *   <li><b>阈值定级（非 100%）</b>：抵抗值 ≥ 48000 → 轻度，≥ 96000 → 中度，≥ 144000 → 重度
 *       （EnumInsulinResistanceLevel.threshold）。</li>
 *   <li><b>等级回落规则</b>（setInsulinResistance）：归零 → 痊愈(null)；高于 0 但跌破轻度阈值时
 *       保留轻度（得病不轻易自愈）；升级只升不降、降档单步（经 null 段回退到轻度）。</li>
 *   <li><b>副作用</b>：处于胰岛素抵抗期每次进食高糖 → 立即中恶心 400t（强度=等级 ordinal）；
 *       重度额外中毒 duration=max(本次IR/48,100)。中度以上不能代谢食物糖 → 饱食收益
 *       按 sugar/1000 打折（R196 Item.getSatiation 折扣）。</li>
 * </ul>
 *
 * <p>存储差异说明：R196 持久化在玩家 NBT（insulin_resistance/insulin_resistance_level）；
 * 本移植存服务端表并按 PlayerNutritionManager 模式经玩家 NBT 落盘。
 */
public final class ICPMInsulinResistance {

    public static final int CAP = 192000;
    public static final int MILD = 48000;
    public static final int MODERATE = 96000;
    public static final int SEVERE = 144000;

    /** 等级 int：0=无，1=mild，2=moderate，3=severe。 */
    private static final int L_NONE = 0, L_MILD = 1, L_MODERATE = 2, L_SEVERE = 3;

    private static final String TAG_VALUE = "IcpmInsulinResistance";
    private static final String TAG_LEVEL = "IcpmInsulinLevel";

    /** 服务端在线表：玩家 UUID → {resistance, level}。 */
    private static final Map<UUID, int[]> STATE = new HashMap<>();

    private ICPMInsulinResistance() {
    }

    // ==================== 查询 ====================

    public static int getResistance(Player player) {
        int[] s = STATE.get(player.getUUID());
        return s == null ? 0 : s[0];
    }

    public static boolean isInsulinResistant(Player player) {
        int[] s = STATE.get(player.getUUID());
        return s != null && s[1] > L_NONE;
    }

    public static int level(Player player) {
        int[] s = STATE.get(player.getUUID());
        return s == null ? L_NONE : s[1];
    }

    private static int levelFromValue(int v) {
        if (v >= SEVERE) return L_SEVERE;
        if (v >= MODERATE) return L_MODERATE;
        if (v >= MILD) return L_MILD;
        return L_NONE;
    }

    /** R196 等级平移规则（setInsulinResistance 内）；notifier 非空且等级上升时发送提示。 */
    private static void applyValue(UUID uuid, int value, ServerPlayer notifier) {
        int v = Math.max(0, Math.min(CAP, value));
        int[] s = STATE.computeIfAbsent(uuid, k -> new int[]{0, L_NONE});
        int oldLevel = s[1];
        int newLevel;
        if (v == 0) {
            newLevel = L_NONE;
        } else {
            int fromValue = levelFromValue(v);
            if (fromValue == L_NONE) {
                newLevel = oldLevel == L_NONE ? L_NONE : L_MILD; // 跌破轻度阈值仍保留轻度
            } else if (oldLevel == L_NONE) {
                newLevel = fromValue;
            } else if (oldLevel < fromValue) {
                newLevel = fromValue; // 升级
            } else if (oldLevel > fromValue) {
                newLevel = fromValue == L_SEVERE ? L_SEVERE : fromValue + 1; // 降档单步
            } else {
                newLevel = oldLevel;
            }
        }
        s[0] = v;
        s[1] = newLevel;
        if (notifier != null && newLevel > oldLevel) {
            notifier.sendSystemMessage(Component.translatable("insulin.level." + newLevel));
        }
    }

    // ==================== 进食（R196 addFoodValue → addInsulinResistance） ====================

    public static void onFoodEaten(Player player, ItemStack stack) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) {
            return;
        }
        int ir = InsulinFoods.insulinResponse(stack);
        if (ir <= 0) {
            return;
        }
        applyValue(player.getUUID(), getResistance(player) + ir, sp);
        // 胰岛素抵抗者吃糖立即中恶心（强度=等级），重度另加中毒（R196 addInsulinResistance 副作用）
        if (isInsulinResistant(player)) {
            int lvl = level(player);
            sp.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 400, lvl - 1, false, true, true));
            if (lvl == L_SEVERE) {
                int poisonTicks = Math.max((int) (ir / 48.0f), 100);
                sp.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTicks, 0, false, true, true));
            }
            // 中度以上不能代谢糖：饱食收益按 sugar/1000 打折（R196 Item.getSatiation 折扣）
            if (lvl >= L_MODERATE) {
                int sugar = InsulinFoods.sugarOf(stack);
                int discount = sugar < 1000 ? 1 : sugar / 1000;
                int cur = ICPMFoodStats.INSTANCE.getSatiation(sp);
                if (cur > 0) {
                    ICPMFoodStats.INSTANCE.addSatiation(sp, -Math.min(discount, cur));
                }
            }
        }
    }

    /** 每 tick 自然代谢（R196 FoodStats.onUpdate → decrementInsulinResistance，每 tick −1）。 */
    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int v = getResistance(player);
            if (v > 0 && !player.getAbilities().instabuild) {
                applyValue(player.getUUID(), v - 1, null);
            }
        }
    }

    // ==================== NBT（随玩家存档，R196 同键语义） ====================

    public static void save(Player player, ValueOutput tag) {
        if (player.level().isClientSide()) {
            return;
        }
        int[] s = STATE.get(player.getUUID());
        if (s == null) {
            return;
        }
        tag.putInt(TAG_VALUE, s[0]);
        tag.putInt(TAG_LEVEL, s[1]);
    }

    public static void load(Player player, ValueInput tag) {
        if (player.level().isClientSide()) {
            return;
        }
        int v = tag.getInt(TAG_VALUE).orElse(0);
        int lvl = tag.getInt(TAG_LEVEL).orElse(0);
        STATE.put(player.getUUID(), new int[]{Math.max(0, Math.min(CAP, v)), Math.max(0, Math.min(3, lvl))});
    }
}
