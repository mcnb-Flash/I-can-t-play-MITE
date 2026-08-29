package name.icpm.mixin;

import name.icpm.ICPM;
import name.icpm.common.ICPMPortalHandler;
import name.icpm.entity.ICPMEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * 原版简单地牢（monster_room）的地下世界变体（R196 移植，用户要求：地牢用原版结构）。
 *
 * 主世界地牢保持原版逻辑（僵尸/骷髅/蜘蛛刷怪笼 + simple_dungeon 箱子，该箱子表已被
 * simple_dungeon.json 覆盖注入 ICPM 物品池）。
 *
 * 地下世界：原版 MonsterRoomFeature.place 对近实心地形的门控会让几乎所有尝试失败；R196 的
 * WorldGenDungeons 用天然空腔门控（var9∈[1,5] + 整层实心地板/天花板），依赖主世界丰富的洞穴
 * 系统才能生成。ICPM 地下世界近实心、仅 nether_cave 产生少量细长隧道，严格门控下概率趋近 0，
 * 实际一个都不生成。因此在 HEAD 拦截地下世界维度，改用 icpm$generateUnderworldDungeon：
 *   - 完整复刻原版 monster_room 结构：圆石墙（地板 1/4 苔石）+ 挖空 + 中央刷怪笼 + 贴墙箱子
 *   - 门控收紧为"洞穴壁/隧道交汇处"：中心可放刷怪笼、有地板支撑、有天花板、中心 5×5 空腔≥40%、
 *     外围 7×7×5 实心比例≥30%。避免之前"中心 60% 空腔"在大洞穴里每试必成、导致遍地刷怪笼。
 *   - 刷新 Y 区间 [52,80]（地下世界靠中间的干燥层，避开底部熔岩/水洞、也避开顶部怪物密集上层，
 *     玩家挖矿必经、方便探索；128 高世界的中段）
 *   - placed_feature 每区块尝试次数从 16 降到 2，控制密度。
 *   - 刷怪笼刷 古尸（1/6 概率古尸守卫），普通箱子内为远古金属池 icpm:chests/underworld_dungeon
 */
@Mixin(MonsterRoomFeature.class)
public abstract class ICPMMonsterRoomFeatureMixin {

    /** 地下世界古尸地牢箱子战利品表 */
    private static final ResourceKey<LootTable> UNDERWORLD_DUNGEON_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, ICPM.id("chests/underworld_dungeon"));

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void icpm$placeUnderworld(FeaturePlaceContext<NoneFeatureConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        if (context.level().getLevel().dimension().identifier().equals(ICPMPortalHandler.UNDERWORLD_KEY.identifier())) {
            cir.setReturnValue(icpm$generateUnderworldDungeon(context));
        }
    }

    private boolean icpm$generateUnderworldDungeon(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        Predicate<BlockState> predicate = Feature.isReplaceable(BlockTags.FEATURES_CANNOT_REPLACE);
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        BlockPos center = origin; // R196：地牢就建在尝试坐标 (x,y,z) 上，不强行落地到地表

        // 房间尺寸（与原版 / R196 WorldGenDungeons.generate 逐格一致）
        int var6 = 3;                  // 房间高度：y-1(地板) .. y+4(天花板)
        int j = random.nextInt(2) + 2; // var7：X 半宽 (2~3)
        int o = random.nextInt(2) + 2; // var8：Z 半宽 (2~3)

        // ===== 地下世界地牢门控（平衡"能找到"与"不泛滥"）=====
        // 之前"中心区域 60% 空腔"在大洞穴里太容易被满足，导致每区块 16 次尝试几乎全部成功，
        // 地下世界遍地刷怪笼。收紧门控：只在"洞穴壁/隧道交汇处"生成，不占领大洞穴中央：
        //   ① 中心格可放刷怪笼（空气或可替换方块）
        //   ② 中心正下方 1 格必须实心（地板支撑）
        //   ③ 中心正上方 2 格内至少 1 格实心（天花板，防露天）
        //   ④ 中心 5×5 区域有 ≥40% 空腔（能挖出房间）
        //   ⑤ 7×7×5 评估区域内实心方块比例 ≥30%（防止在大空洞中央生成）
        // 同时 placed_feature 的 count 从 16 降到 2，避免同一区块反复成功。
        if (!level.isEmptyBlock(center) && !predicate.test(level.getBlockState(center))) {
            return false; // 中心不是可替换/空气，无法放刷怪笼
        }
        if (!level.getBlockState(center.below()).isSolid()) {
            return false; // 无地板支撑
        }
        boolean hasCeiling = false;
        for (int dy = 1; dy <= 2; dy++) {
            if (level.getBlockState(center.above(dy)).isSolid()) {
                hasCeiling = true;
                break;
            }
        }
        if (!hasCeiling) {
            return false; // 无天花板，露天洞穴中央
        }
        int openSpace = 0;
        for (int dx = -j; dx <= j; dx++) {
            for (int dz = -o; dz <= o; dz++) {
                BlockPos p = center.offset(dx, 0, dz);
                if (level.isEmptyBlock(p) || predicate.test(level.getBlockState(p))) {
                    openSpace++;
                }
            }
        }
        int roomArea = (j * 2 + 1) * (o * 2 + 1);
        if (openSpace * 10 < roomArea * 4) {
            return false; // 中心区域空腔不足 40%
        }
        // 评估更外围：7×7×5 区域里实心比例需 ≥30%，确保是在洞穴壁/隧道交汇处而非大空洞中央
        int solidCount = 0;
        int evalCount = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    evalCount++;
                    if (level.getBlockState(p).isSolid()) {
                        solidCount++;
                    }
                }
            }
        }
        if (solidCount * 10 < evalCount * 3) {
            return false; // 太空旷，大洞穴中央
        }

        // ===== 门控通过：复刻原版 monster_room 结构 =====
        int k = -j - 1;
        int l = j + 1;
        int p = -o - 1;
        int q = o + 1;

        // 挖空 + 建墙（完整复刻原版 monster_room 结构：圆石墙、地板 1/4 苔石）
        for (int s = k; s <= l; s++) {
            for (int t = 3; t >= -1; t--) {
                for (int u = p; u <= q; u++) {
                    BlockPos pos = center.offset(s, t, u);
                    BlockState state = level.getBlockState(pos);
                    if (s == k || t == -1 || u == p || s == l || t == 4 || u == q) {
                        if (pos.getY() >= level.getMinY() && !level.getBlockState(pos.below()).isSolid()) {
                            level.setBlock(pos, air, 2);
                        } else if (state.isSolid() && !state.is(Blocks.CHEST)) {
                            if (t == -1 && random.nextInt(4) != 0) {
                                icpm$safeSetBlock(level, pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), predicate);
                            } else {
                                icpm$safeSetBlock(level, pos, Blocks.COBBLESTONE.defaultBlockState(), predicate);
                            }
                        }
                    } else if (!state.is(Blocks.CHEST) && !state.is(Blocks.SPAWNER)) {
                        icpm$safeSetBlock(level, pos, air, predicate);
                    }
                }
            }
        }

        // 贴墙箱子（1~2 个尝试，普通箱子），战利品 = 远古金属池
        for (int s = 0; s < 2; s++) {
            for (int t = 0; t < 3; t++) {
                int u = center.getX() + random.nextInt(j * 2 + 1) - j;
                int v = center.getY();
                int w = center.getZ() + random.nextInt(o * 2 + 1) - o;
                BlockPos pos = new BlockPos(u, v, w);
                if (level.isEmptyBlock(pos)) {
                    int x = 0;
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (level.getBlockState(pos.relative(direction)).isSolid()) {
                            x++;
                        }
                    }
                    if (x == 1) {
                        icpm$safeSetBlock(level, pos, StructurePiece.reorient(level, pos, Blocks.CHEST.defaultBlockState()), predicate);
                        RandomizableContainer.setBlockEntityLootTable(level, random, pos, UNDERWORLD_DUNGEON_LOOT);
                        break;
                    }
                }
            }
        }

        // 中央刷怪笼：古尸，1/6 概率古尸守卫（与 R196 pickMobSpawner 一致）
        icpm$safeSetBlock(level, center, Blocks.SPAWNER.defaultBlockState(), predicate);
        if (level.getBlockEntity(center) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(random.nextInt(6) == 0
                    ? ICPMEntities.INSTANCE.getLONGDEAD_GUARDIAN()
                    : ICPMEntities.INSTANCE.getLONGDEAD(), random);
        }
        return true;
    }

    private void icpm$safeSetBlock(WorldGenLevel level, BlockPos pos, BlockState state, Predicate<BlockState> predicate) {
        if (predicate.test(level.getBlockState(pos))) {
            level.setBlock(pos, state, 2);
        }
    }
}
