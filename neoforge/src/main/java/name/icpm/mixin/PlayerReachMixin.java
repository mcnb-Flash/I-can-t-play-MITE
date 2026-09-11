package name.icpm.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ICPM 玩家交互/攻击距离 —— R196 EntityPlayer.getReach 忠实移植（1.21.11 修正版）。
 *
 * <p>R196 原文（src_deobf/.../EntityPlayer.java getReach）：
 * <pre>
 *   getReach(Block,int)          = 2.75f + Item.getReachBonus(block,metadata)  // 方块（持有效工具才加）
 *   getReach(FOR_MELEE_ATTACK)   = 1.5f  + height_advantage + Item.getReachBonus()      // 近战攻击
 *   getReach(FOR_INTERACTION)    = 2.5f  + height_advantage + Item.getReachBonus(entity) // 实体右键交互
 *   武器 reach_bonus：剑/斧/镐等 ItemTool 系 0.75（dagger/hatchet/shears 0.5、cudgel/knife 0.25、scythe 1.0）
 *   hasExtendedReach()（创造）= 5.0f
 * </pre>
 *
 * <p>1.21.11 的真实链路（已反编译核实，非猜测）：
 * <ul>
 *   <li>客户端准星：{@code GameRenderer.pick → LocalPlayer.raycastHitResult}，实体拾取与方块
 *       射线长度分别调 {@code entityInteractionRange()}/{@code blockInteractionRange()}；
 *       LocalPlayer <b>不覆写</b> 二者 → @Mixin(Player) 基类注入对客户端/服务端同时生效。</li>
 *   <li>方块挖掘/放置服务端校验同样走 blockInteractionRange；实体右键走 entityInteractionRange。</li>
 *   <li>近战攻击：客户端命中实体后发包，服务端调用 {@code Player.attack(Entity)}；
 *       攻击距离不单独走 range getter（entityAttackRange 返回的是物品 AttackRange 组件），
 *       故在 {@code Player.attack} 入口加 R196 距离闸门（1.5+武器加成，含目标 AABB 计算）。</li>
 * </ul>
 *
 * <p>映射取舍（如实记录，避免再被误判"有名无实"）：
 * <ul>
 *   <li>1.21.11 交互与准星实体拾取共用 entityInteractionRange → 客户端高亮限制取 R196 交互值
 *       （2.5+加成）而非近战值 1.5，否则拾取/右键全被压到 1.5 过于离谱（此前版本 bug 所在）。</li>
 *   <li>近战 1.5+加成由 Player.attack 服务端闸门强制（比高亮更严格，越界点击不产生伤害）。</li>
 *   <li>工具方块加成按"手持含 TOOL 组件的工具 +0.75"近似（无法按方块材质精确）；无工具 2.75。</li>
 *   <li>武器识别 = 主手含正 ATTACK_DAMAGE 属性修饰（兼容 Properties.sword()/AxeItem 等，无需类名）。</li>
 *   <li>height_advantage 垂直差折算未移植（影响小，避免 1.21 判定复杂度）。</li>
 *   <li>R196 无潜行加成（"潜行延长"为 MITE:Equilibrium 自改）。</li>
 * </ul>
 */
@Mixin(Player.class)
public abstract class PlayerReachMixin {

    /** R196 方块 reach 基础 */
    private static final double BLOCK_REACH_LIMIT = 2.75;
    /** R196 实体右键交互 reach 基础 */
    private static final double INTERACT_REACH_LIMIT = 2.5;
    /** R196 近战攻击 reach 基础 */
    private static final double MELEE_REACH_LIMIT = 1.5;
    /** R196 ItemTool 系（剑/斧/镐等）reach bonus */
    private static final double WEAPON_REACH_BONUS = 0.75;

    private static boolean creativeOrSpectator(Player self) {
        Abilities abilities = self.getAbilities();
        return abilities.instabuild || self.isSpectator();
    }

    /** 主手是否携带正攻击伤害修饰（兼容 1.21 Properties.sword() 与 AxeItem 等）。 */
    private static boolean hasWeaponBonus(Player self) {
        ItemStack stack = self.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.attribute() == Attributes.ATTACK_DAMAGE && e.modifier().amount() > 0.0) {
                return true;
            }
        }
        return false;
    }

    /** 是否手持可挖掘工具（含 TOOL 组件）。 */
    private static boolean hasToolBonus(Player self) {
        ItemStack stack = self.getMainHandItem();
        return !stack.isEmpty() && stack.get(DataComponents.TOOL) != null;
    }

    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void icpm$clampBlockReach(CallbackInfoReturnable<Double> cir) {
        Player self = (Player) (Object) this;
        if (creativeOrSpectator(self)) {
            return;
        }
        double limit = BLOCK_REACH_LIMIT + (hasToolBonus(self) ? WEAPON_REACH_BONUS : 0.0);
        double original = cir.getReturnValue();
        if (original > limit) {
            cir.setReturnValue(limit);
        }
    }

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void icpm$clampEntityInteractReach(CallbackInfoReturnable<Double> cir) {
        Player self = (Player) (Object) this;
        if (creativeOrSpectator(self)) {
            return;
        }
        double limit = INTERACT_REACH_LIMIT + (hasWeaponBonus(self) ? WEAPON_REACH_BONUS : 0.0);
        double original = cir.getReturnValue();
        if (original > limit) {
            cir.setReturnValue(limit);
        }
    }

    /** R196 近战距离闸门：Player.attack 入口按视线点到目标 AABB 最近距离 ≤ 1.5+武器加成。 */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void icpm$gateMeleeReach(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (creativeOrSpectator(self) || !(target instanceof LivingEntity)) {
            return; // 创造/旁观不限制；仅对生物近战判定（R196 实体近战语境）
        }
        double reach = MELEE_REACH_LIMIT + (hasWeaponBonus(self) ? WEAPON_REACH_BONUS : 0.0);
        Vec3 eye = self.getEyePosition(1.0f);
        AABB box = target.getBoundingBox();
        double nx = Math.max(box.minX - eye.x, Math.max(0.0, eye.x - box.maxX));
        double ny = Math.max(box.minY - eye.y, Math.max(0.0, eye.y - box.maxY));
        double nz = Math.max(box.minZ - eye.z, Math.max(0.0, eye.z - box.maxZ));
        double distSq = nx * nx + ny * ny + nz * nz;
        if (distSq > reach * reach) {
            ci.cancel(); // 越界：不产生攻击（R196 canReachEntity 语义）
        }
    }
}
