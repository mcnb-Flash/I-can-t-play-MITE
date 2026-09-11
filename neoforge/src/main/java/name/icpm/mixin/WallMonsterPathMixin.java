package name.icpm.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A3：墙阻挡怪物寻路，但不影响索敌机制。
 *
 * 机制：怪物（PathfinderMob）在寻路时将墙方块（Wall）的落点判定为“不稳定/不可站立”，
 * 从而拒绝把墙顶或墙作为路径落点 —— 怪物不会尝试跳上或越过墙。
 *
 * 不影响索敌：索敌（NearestAttackableTargetGoal / 视线 / LeapAtTargetGoal）完全不经过
 * PathNavigation.isStableDestination，本注入只改变“能否走到墙顶”，怪仍能看见玩家并攻击。
 *
 * 墙的碰撞高度为 1.5 格（< 2 格），按 A3 规则“高度 < 2 时拒绝走过”全部拒绝。
 * 玩家（非 PathfinderMob）与非墙方块不受影响。
 */
@Mixin(PathNavigation.class)
public abstract class WallMonsterPathMixin {

    @Shadow
    protected Mob mob;

    @Shadow
    public Level level;

    @Inject(method = "isStableDestination", at = @At("HEAD"), cancellable = true, require = 0)
    private void icpm$blockMonsterOnWall(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // 仅对怪物生效；玩家骑乘/其他导航不受影响
        if (!(mob instanceof PathfinderMob)) {
            return;
        }
        Block block = level.getBlockState(pos).getBlock();
        // 墙方块（圆石墙/苔石墙等）：碰撞 1.5 格 < 2，拒绝怪物将其作为稳定落点
        if (block instanceof WallBlock) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
