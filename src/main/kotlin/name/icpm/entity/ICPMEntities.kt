package name.icpm.entity

import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.BiomeTags
import net.minecraft.util.RandomSource
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import name.icpm.ICPM
import name.icpm.common.ICPMPortalHandler
import name.icpm.entity.monster.AnnihilationSkeletonEntity
import name.icpm.entity.monster.BlackWidowEntity
import name.icpm.entity.monster.BlobEntity
import name.icpm.entity.monster.BoneLordEntity
import name.icpm.entity.monster.CaveSpiderVariantEntity
import name.icpm.entity.monster.AncientBoneLordEntity
import name.icpm.entity.monster.ClayGolemEntity
import name.icpm.entity.monster.DemonSpiderEntity
import name.icpm.entity.monster.DireWolfEntity
import name.icpm.entity.monster.EarthElementalEntity
import name.icpm.entity.monster.FireElementalEntity
import name.icpm.entity.monster.GelatinousCubeEntity
import name.icpm.entity.monster.HoarySilverfishEntity
import name.icpm.entity.monster.InfernalCreeperEntity
import name.icpm.entity.monster.GhoulEntity
import name.icpm.entity.monster.HellhoundEntity
import name.icpm.entity.monster.InvisibleStalkerEntity
import name.icpm.entity.monster.JellyEntity
import name.icpm.entity.monster.LongdeadEntity
import name.icpm.entity.monster.LongdeadGuardianEntity
import name.icpm.entity.monster.MinerZombieEntity
import name.icpm.entity.monster.GiantZombieEntity
import name.icpm.entity.monster.ICPMSkeletonVariant
import name.icpm.entity.monster.ICPMSpiderVariant
import name.icpm.entity.monster.NightwingEntity
import name.icpm.entity.monster.OozeEntity
import name.icpm.entity.monster.PhaseSpiderEntity
import name.icpm.entity.monster.PuddingEntity
import name.icpm.entity.monster.RevenantEntity
import name.icpm.entity.monster.ShadowEntity
import name.icpm.entity.monster.VampireBatEntity
import name.icpm.entity.monster.WightEntity
import name.icpm.entity.monster.WoodSpiderEntity
import name.icpm.entity.projectile.GelatinousSphereEntity
import name.icpm.entity.projectile.ICPMArrowEntity

/**
 * ICPM R196 特殊怪物实体类型注册。
 * 黏液族（GelatinousCube 体系）：
 *   - JellyEntity / BlobEntity / OozeEntity / PuddingEntity（敌对凝胶方块）
 *   - GelatinousSphereEntity（投掷物）
 */
object ICPMEntities {

    // ==================== 黏液族 ====================

    val JELLY: EntityType<JellyEntity> = register(
        "jelly",
        EntityType.Builder.of(
            { type, level -> JellyEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.5f, 0.5f).clientTrackingRange(10)
    )

    val BLOB: EntityType<BlobEntity> = register(
        "blob",
        EntityType.Builder.of(
            { type, level -> BlobEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.5f, 0.5f).clientTrackingRange(10)
    )

    val OOZE: EntityType<OozeEntity> = register(
        "ooze",
        EntityType.Builder.of(
            { type, level -> OozeEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.5f, 0.5f).clientTrackingRange(10)
    )

    val PUDDING: EntityType<PuddingEntity> = register(
        "pudding",
        EntityType.Builder.of(
            { type, level -> PuddingEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.5f, 0.5f).clientTrackingRange(10)
    )

    val GELATINOUS_SPHERE: EntityType<GelatinousSphereEntity> = register(
        "gelatinous_sphere",
        EntityType.Builder.of(
            { type, level -> GelatinousSphereEntity(type, level) },
            MobCategory.MISC
        ).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10)
    )

    // ==================== 箭矢 ====================

    val ICPM_ARROW: EntityType<ICPMArrowEntity> = register(
        "icpm_arrow",
        EntityType.Builder.of(
            { type, level -> ICPMArrowEntity(type, level) },
            MobCategory.MISC
        ).sized(0.5f, 0.5f).clientTrackingRange(4).updateInterval(20)
    )

    // ==================== 骷髅变种 ====================

    val LONGDEAD: EntityType<LongdeadEntity> = register(
        "longdead",
        EntityType.Builder.of(
            { type, level -> LongdeadEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.99f).clientTrackingRange(8)
    )

    val LONGDEAD_GUARDIAN: EntityType<LongdeadGuardianEntity> = register(
        "longdead_guardian",
        EntityType.Builder.of(
            { type, level -> LongdeadGuardianEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.99f).clientTrackingRange(8)
    )

    val BONE_LORD: EntityType<BoneLordEntity> = register(
        "bone_lord",
        EntityType.Builder.of(
            { type, level -> BoneLordEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.99f).clientTrackingRange(8)
    )

    val ANNIHILATION_SKELETON: EntityType<AnnihilationSkeletonEntity> = register(
        "annihilation_skeleton",
        EntityType.Builder.of(
            { type, level -> AnnihilationSkeletonEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.99f).clientTrackingRange(8)
    )

    // ==================== 蜘蛛变种 ====================

    val WOOD_SPIDER: EntityType<WoodSpiderEntity> = register(
        "wood_spider",
        EntityType.Builder.of(
            { type, level -> WoodSpiderEntity(type, level) },
            MobCategory.MONSTER
        ).sized(1.4f, 0.9f).clientTrackingRange(8)
    )

    val CAVE_SPIDER_VARIANT: EntityType<CaveSpiderVariantEntity> = register(
        "cave_spider_variant",
        EntityType.Builder.of(
            { type, level -> CaveSpiderVariantEntity(type, level) },
            MobCategory.MONSTER
        ).sized(1.4f, 0.9f).clientTrackingRange(8)
    )

    val BLACK_WIDOW: EntityType<BlackWidowEntity> = register(
        "black_widow",
        EntityType.Builder.of(
            { type, level -> BlackWidowEntity(type, level) },
            MobCategory.MONSTER
        ).sized(1.4f, 0.9f).clientTrackingRange(8)
    )

    val PHASE_SPIDER: EntityType<PhaseSpiderEntity> = register(
        "phase_spider",
        EntityType.Builder.of(
            { type, level -> PhaseSpiderEntity(type, level) },
            MobCategory.MONSTER
        ).sized(1.4f, 0.9f).clientTrackingRange(8)
    )

    val DEMON_SPIDER: EntityType<DemonSpiderEntity> = register(
        "demon_spider",
        EntityType.Builder.of(
            { type, level -> DemonSpiderEntity(type, level) },
            MobCategory.MONSTER
        ).sized(1.4f, 0.9f).clientTrackingRange(8)
    )

    // ==================== 地狱犬 ====================

    val HELLHOUND: EntityType<HellhoundEntity> = register(
        "hellhound",
        EntityType.Builder.of(
            { type, level -> HellhoundEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 0.85f).clientTrackingRange(8)
    )

    // ==================== 土元素 ====================

    val EARTH_ELEMENTAL: EntityType<EarthElementalEntity> = register(
        "earth_elemental",
        EntityType.Builder.of(
            { type, level -> EarthElementalEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.95f).clientTrackingRange(8)
    )

    // ==================== R196 新增怪物 ====================

    val GHOUL: EntityType<GhoulEntity> = register(
        "ghoul",
        EntityType.Builder.of(
            { type, level -> GhoulEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.95f).clientTrackingRange(8)
    )

    val WIGHT: EntityType<WightEntity> = register(
        "wight",
        EntityType.Builder.of(
            { type, level -> WightEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.95f).clientTrackingRange(8)
    )

    val SHADOW: EntityType<ShadowEntity> = register(
        "shadow",
        EntityType.Builder.of(
            { type, level -> ShadowEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.95f).clientTrackingRange(8)
    )

    val INVISIBLE_STALKER: EntityType<InvisibleStalkerEntity> = register(
        "invisible_stalker",
        EntityType.Builder.of(
            { type, level -> InvisibleStalkerEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.95f).clientTrackingRange(8)
    )

    val REVENANT: EntityType<RevenantEntity> = register(
        "revenant",
        EntityType.Builder.of(
            { type, level -> RevenantEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.95f).clientTrackingRange(8)
    )

    val CLAY_GOLEM: EntityType<ClayGolemEntity> = register(
        "clay_golem",
        EntityType.Builder.of(
            { type, level -> ClayGolemEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.95f).clientTrackingRange(8)
    )

    val ANCIENT_BONE_LORD: EntityType<AncientBoneLordEntity> = register(
        "ancient_bone_lord",
        EntityType.Builder.of(
            { type, level -> AncientBoneLordEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.99f).clientTrackingRange(8)
    )

    // 注意：蝙蝠必须为 AMBIENT 类别，与下方 BiomeModifications.addSpawn 的 AMBIENT 一致。
    // 若 EntityType 类别写成 MONSTER，自然生成器会把它们计入 MONSTER 上限（约 70×区块/289），
    // 而 AMBIENT 上限检查恒为 0 → 永不封顶，地下世界蝙蝠无限刷满 → “疯狂刷怪”。
    // 改回 AMBIENT 后，AMBIENT 上限（约 15×区块/289）才会真正生效，限制总数量。
    val VAMPIRE_BAT: EntityType<VampireBatEntity> = register(
        "vampire_bat",
        EntityType.Builder.of(
            { type, level -> VampireBatEntity(type, level) },
            MobCategory.AMBIENT
        ).sized(0.5f, 0.9f).clientTrackingRange(8)
    )

    val NIGHTWING: EntityType<NightwingEntity> = register(
        "nightwing",
        EntityType.Builder.of(
            { type, level -> NightwingEntity(type, level) },
            MobCategory.AMBIENT
        ).sized(0.5f, 0.9f).clientTrackingRange(8)
    )

    // ==================== 矿工僵尸（血月机制新增） ====================

    val MINER_ZOMBIE: EntityType<MinerZombieEntity> = register(
        "miner_zombie",
        EntityType.Builder.of(
            { type, level -> MinerZombieEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.95f).clientTrackingRange(8)
    )

    // ==================== 巨型僵尸（血月地表僵尸 1/200 稀有替换体） ====================
    // 不独立刷怪：仅由 ZombieMiteSpawnMixin 在血月地表自然生成时替换普通僵尸。
    // 碰撞箱 ×6（3.6 × 11.7），数值见 GiantZombieEntity（对齐 R196 EntityGiantZombie）。

    val GIANT_ZOMBIE: EntityType<GiantZombieEntity> = register(
        "giant_zombie",
        EntityType.Builder.of(
            { type, level -> GiantZombieEntity(type, level) },
            MobCategory.MONSTER
        ).sized(3.6f, 11.7f).clientTrackingRange(16)
    )

    // ==================== R196 补全怪物（A 项：火元素 / 地狱苦力怕 / 恐狼 / 灰银鱼） ====================

    val FIRE_ELEMENTAL: EntityType<FireElementalEntity> = register(
        "fire_elemental",
        EntityType.Builder.of(
            { type, level -> FireElementalEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.95f).clientTrackingRange(8)
    )

    val INFERNAL_CREEPER: EntityType<InfernalCreeperEntity> = register(
        "infernal_creeper",
        EntityType.Builder.of(
            { type, level -> InfernalCreeperEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 1.7f).clientTrackingRange(8)
    )

    val DIRE_WOLF: EntityType<DireWolfEntity> = register(
        "dire_wolf",
        EntityType.Builder.of(
            { type, level -> DireWolfEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.6f, 0.85f).clientTrackingRange(8)
    )

    val HOARY_SILVERFISH: EntityType<HoarySilverfishEntity> = register(
        "hoary_silverfish",
        EntityType.Builder.of(
            { type, level -> HoarySilverfishEntity(type, level) },
            MobCategory.MONSTER
        ).sized(0.4f, 0.3f).clientTrackingRange(8)
    )

    // ==================== 初始化 ====================

    fun init() {
        // 黏液族属性
        FabricDefaultAttributeRegistry.register(JELLY, GelatinousCubeEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(BLOB, GelatinousCubeEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(OOZE, GelatinousCubeEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(PUDDING, GelatinousCubeEntity.createAttributes())

        // 骷髅变种属性
        FabricDefaultAttributeRegistry.register(LONGDEAD, ICPMSkeletonVariant.createAttributes())
        FabricDefaultAttributeRegistry.register(LONGDEAD_GUARDIAN, ICPMSkeletonVariant.createAttributes())
        FabricDefaultAttributeRegistry.register(BONE_LORD, ICPMSkeletonVariant.createAttributes())
        FabricDefaultAttributeRegistry.register(ANNIHILATION_SKELETON, ICPMSkeletonVariant.createAttributes())

        // 蜘蛛变种属性
        FabricDefaultAttributeRegistry.register(WOOD_SPIDER, ICPMSpiderVariant.createAttributes())
        FabricDefaultAttributeRegistry.register(CAVE_SPIDER_VARIANT, ICPMSpiderVariant.createAttributes())
        FabricDefaultAttributeRegistry.register(BLACK_WIDOW, ICPMSpiderVariant.createAttributes())
        FabricDefaultAttributeRegistry.register(PHASE_SPIDER, ICPMSpiderVariant.createAttributes())
        FabricDefaultAttributeRegistry.register(DEMON_SPIDER, ICPMSpiderVariant.createAttributes())

        // 地狱犬属性
        FabricDefaultAttributeRegistry.register(HELLHOUND, HellhoundEntity.createAttributes())

        // 土元素属性
        FabricDefaultAttributeRegistry.register(EARTH_ELEMENTAL, EarthElementalEntity.createAttributes())

        // R196 新增怪物属性
        FabricDefaultAttributeRegistry.register(GHOUL, GhoulEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(WIGHT, WightEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(SHADOW, ShadowEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(INVISIBLE_STALKER, InvisibleStalkerEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(REVENANT, RevenantEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(CLAY_GOLEM, ClayGolemEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(ANCIENT_BONE_LORD, AncientBoneLordEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(VAMPIRE_BAT, VampireBatEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(NIGHTWING, NightwingEntity.createAttributes())

        // 生成放置规则（古尸系：地下世界 或 主世界血月夜）
        net.minecraft.world.entity.SpawnPlacements.register(
            LONGDEAD,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkUnderworldOrBloodMoonSpawnRules
        )
        net.minecraft.world.entity.SpawnPlacements.register(
            LONGDEAD_GUARDIAN,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkUnderworldOrBloodMoonSpawnRules
        )
        net.minecraft.world.entity.SpawnPlacements.register(
            BONE_LORD,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkICPMVariantSpawnRules
        )
        net.minecraft.world.entity.SpawnPlacements.register(
            ANNIHILATION_SKELETON,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkICPMVariantSpawnRules
        )

        // 蜘蛛变种生成放置（支持爬墙）
        net.minecraft.world.entity.SpawnPlacements.register(
            WOOD_SPIDER,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkICPMSpiderSpawnRules
        )
        net.minecraft.world.entity.SpawnPlacements.register(
            CAVE_SPIDER_VARIANT,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkICPMSpiderSpawnRules
        )
        net.minecraft.world.entity.SpawnPlacements.register(
            BLACK_WIDOW,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkICPMSpiderSpawnRules
        )
        net.minecraft.world.entity.SpawnPlacements.register(
            PHASE_SPIDER,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkICPMSpiderSpawnRules
        )
        net.minecraft.world.entity.SpawnPlacements.register(
            DEMON_SPIDER,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkICPMSpiderSpawnRules
        )

        // 地狱犬生成放置（仅地下世界）
        net.minecraft.world.entity.SpawnPlacements.register(
            HELLHOUND,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkUnderworldOnlySpawnRules
        )

        // 土元素生成放置（R196 getCanSpawnHere：脚下须为 石头/黑曜石/地狱岩/末地石）
        net.minecraft.world.entity.SpawnPlacements.register(
            EARTH_ELEMENTAL,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkEarthElementalSpawnRules
        )

        // R196 新增怪物生成放置
        // 食尸鬼/亡魂：R196 BiomeGenUnderworld 从生成列表移除 → 非地下世界规则
        for (type in listOf(GHOUL, REVENANT)) {
            net.minecraft.world.entity.SpawnPlacements.register(
                type,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ::checkNotUnderworldSpawnRules
            )
        }
        // 尸妖/暗影/潜伏者：全维度（R196 全群系，含地下世界）
        for (type in listOf(WIGHT, SHADOW, INVISIBLE_STALKER)) {
            net.minecraft.world.entity.SpawnPlacements.register(
                type,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ::checkICPMVariantSpawnRules
            )
        }
        // 黏土魔像（R196 EntityClayGolem：仅黏土上方生成）
        net.minecraft.world.entity.SpawnPlacements.register(
            CLAY_GOLEM,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkClayGolemSpawnRules
        )
        // 远古骨王：仅地下世界（R196 BiomeGenUnderworld 专属）
        net.minecraft.world.entity.SpawnPlacements.register(
            ANCIENT_BONE_LORD,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkUnderworldOnlySpawnRules
        )
        // 蝙蝠类：放置规则（地下世界 + 洞窟天花板 + 黑暗亮度限制，见 checkICPMBatSpawnRules）
        for (type in listOf(VAMPIRE_BAT, NIGHTWING)) {
            net.minecraft.world.entity.SpawnPlacements.register(
                type,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ::checkICPMBatSpawnRules
            )
        }

        // 群系生成（R196 地下世界怪物：skeleton 系生成，古尸/守卫仅地下世界）
        val skeletonSpawn = mapOf(
            LONGDEAD to 60,
            LONGDEAD_GUARDIAN to 30,
            BONE_LORD to 5,
            ANNIHILATION_SKELETON to 10
        )
        for ((type, weight) in skeletonSpawn) {
            BiomeModifications.addSpawn(
                BiomeSelectors.all(),
                MobCategory.MONSTER,
                type,
                weight,
                1,
                4
            )
        }

        // 群系生成（蜘蛛变种）
        // 木蜘蛛：仅树林（IS_FOREST）刷新
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(BiomeTags.IS_FOREST),
            MobCategory.MONSTER,
            WOOD_SPIDER,
            20,
            1,
            4
        )
        // 其余蜘蛛变种：全群系（黑暗环境）
        val spiderSpawn = mapOf(
            CAVE_SPIDER_VARIANT to 15,
            BLACK_WIDOW to 8,
            PHASE_SPIDER to 5,
            DEMON_SPIDER to 6
        )
        for ((type, weight) in spiderSpawn) {
            BiomeModifications.addSpawn(
                BiomeSelectors.all(),
                MobCategory.MONSTER,
                type,
                weight,
                1,
                4
            )
        }

        // 地狱犬生成（地下世界）
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            HELLHOUND,
            15,
            1,
            3
        )

        // 土元素生成（R196：主世界 10 / 下界 40 / 末地 20 / 地下世界 40；生成方块限制见 checkEarthElementalSpawnRules）
        // 注：地下世界是自定义维度，BiomeSelectionContext 不暴露维度信息，无法用 foundIn* 单独匹配，
        // 故统一用 BiomeSelectors.all() + SpawnPlacements 谓词（checkEarthElementalSpawnRules 已按脚下方块
        // 允许 石头/深板岩/黑曜石/地狱岩/末地石，覆盖全部维度含地下世界）。权重取 R196 地下世界/下界值 40。
        // 不为各维度分别设权重（foundIn* 与 all() 叠加会重复计数，导致主世界/末地权重翻倍）。
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            EARTH_ELEMENTAL,
            40,
            1,
            1
        )

        // R196 新增怪物生成（全群系权重对齐 R196：10,1,1）
        val undeadSpawn = mapOf(
            GHOUL to 10,
            WIGHT to 10,
            SHADOW to 10,
            INVISIBLE_STALKER to 10,
            REVENANT to 10
        )
        for ((type, weight) in undeadSpawn) {
            BiomeModifications.addSpawn(
                BiomeSelectors.all(),
                MobCategory.MONSTER,
                type,
                weight,
                1,
                1
            )
        }

        // 黏土魔像（R196 权重 50,1,1；仅黏土上方生成）
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            CLAY_GOLEM,
            50,
            1,
            1
        )

        // 远古骨王（R196 仅地下世界，权重 5,1,1；维度限制见 checkUnderworldOnlySpawnRules）
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            ANCIENT_BONE_LORD,
            5,
            1,
            1
        )

        // 地下世界蝙蝠类（R196 spawnableCaveCreatureList：VampireBat 20,8,8 / Nightwing 4,1,4；
        // 1.21 无洞窟类别，映射为 AMBIENT。关键：EntityType 类别必须与此处一致（AMBIENT），
        // 否则 AMBIENT 上限形同虚设，蝙蝠无限刷满地下世界。
        // 总数量由 AMBIENT 上限（约 15×区块/289）硬性封顶；以下权重/组大小仅影响相对比例与刷新节奏。
        // 权重取 R196 的约 1/10（20→4、4→2），组大小 1~2 避免成团。黑暗/洞窟/维度限制见 checkICPMBatSpawnRules
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.AMBIENT,
            VAMPIRE_BAT,
            4,
            1,
            2
        )
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.AMBIENT,
            NIGHTWING,
            2,
            1,
            2
        )

        // ==================== 矿工僵尸（血月机制新增） ====================

        FabricDefaultAttributeRegistry.register(MINER_ZOMBIE, MinerZombieEntity.createAttributes())
        // 非血月仅矿洞（非露天）刷新；血月夜地面也可刷新
        net.minecraft.world.entity.SpawnPlacements.register(
            MINER_ZOMBIE,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkMinerZombieSpawnRules
        )
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            MINER_ZOMBIE,
            15,
            1,
            4
        )

        // ==================== 巨型僵尸 ====================
        // 不独立刷怪，仅由 ZombieMiteSpawnMixin 在血月地表替换普通僵尸，故无 SpawnPlacements / addSpawn。
        FabricDefaultAttributeRegistry.register(GIANT_ZOMBIE, GiantZombieEntity.createAttributes())

        // ==================== R196 补全怪物（A 项） ====================

        // 属性注册
        FabricDefaultAttributeRegistry.register(FIRE_ELEMENTAL, FireElementalEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(INFERNAL_CREEPER, InfernalCreeperEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(DIRE_WOLF, DireWolfEntity.createAttributes())
        FabricDefaultAttributeRegistry.register(HOARY_SILVERFISH, HoarySilverfishEntity.createAttributes())

        // 火元素：R196 从下界岩浆源块上方生成。Fabric 简化为下界 + 地下世界自然生成（亮度恒满足）。
        net.minecraft.world.entity.SpawnPlacements.register(
            FIRE_ELEMENTAL,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkNetherOrUnderworldSpawnRules
        )
        // 地狱苦力怕：R196 在下界/地下世界生成（50% 替换苦力怕）。简化为下界 + 地下世界独立生成。
        net.minecraft.world.entity.SpawnPlacements.register(
            INFERNAL_CREEPER,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkNetherOrUnderworldSpawnRules
        )
        // 恐狼：全维度黑暗处（夜行猛兽，对齐 R196 随狼群/蓝月生成）。
        net.minecraft.world.entity.SpawnPlacements.register(
            DIRE_WOLF,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkICPMVariantSpawnRules
        )
        // 灰银鱼：R196 在石头山地生成。简化为全维度、脚下为石头方块处生成。
        net.minecraft.world.entity.SpawnPlacements.register(
            HOARY_SILVERFISH,
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ::checkHoarySilverfishSpawnRules
        )

        // 群系生成（权重对齐 R196 夜行/洞穴猛兽）
        // 火元素 / 地狱苦力怕：下界 + 地下世界（BiomeSelectors.all + 谓词维度门控）
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            FIRE_ELEMENTAL,
            10,
            1,
            1
        )
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            INFERNAL_CREEPER,
            10,
            1,
            1
        )
        // 恐狼：全维度（黑暗判定）
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            DIRE_WOLF,
            8,
            1,
            4
        )
        // 灰银鱼：全维度（石头判定）
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            HOARY_SILVERFISH,
            6,
            1,
            4
        )
    }

    /**
     * R196 tm.isValidLightLevel 移植：
     * 1) BLV 判定（方块亮度）：若方块亮度 > rand(0..31) 则拒绝；
     * 2) 主判定：BLV <= rand(0..underOpenSky?8:5)。
     * 酸性（ooze/pudding）额外要求脚下为石头（EntityCubic.getCanSpawnHere）。
     */
    private fun checkGelatinousCubeSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false
        if (EntitySpawnReason.isSpawner(reason)) {
            return Mob.checkMobSpawnRules(type, level, reason, pos, random)
        }
        if (type === OOZE || type === PUDDING) {
            val belowState = level.getBlockState(pos.below())
            if (!belowState.`is`(Blocks.STONE)) return false
        }
        if (level.getMaxLocalRawBrightness(pos) > random.nextInt(32)) return false
        val blv = level.getMaxLocalRawBrightness(pos)
        val underOpenSky = level.canSeeSky(pos.above())
        if (blv > random.nextInt(if (underOpenSky) 8 else 5)) return false
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 骷髅变种生成规则：复用原版骷髅的生成判定（Monster.checkMonsterSpawnRules）
     * 并附加亮度限制（黑暗环境），确保地下世界洞窟/暗处生成。
     */
    private fun checkICPMVariantSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false
        if (EntitySpawnReason.isSpawner(reason)) {
            return Mob.checkMobSpawnRules(type, level, reason, pos, random)
        }
        if (level.getMaxLocalRawBrightness(pos) > random.nextInt(16)) return false
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 地下世界专属生成规则：地狱犬 / 古尸 / 古尸守卫 仅在 ICPM 地下世界维度刷新
     * （R196：这些是地下世界的标志性怪物）。
     */
    private fun checkUnderworldOnlySpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.level.dimension() != ICPMPortalHandler.UNDERWORLD_KEY) return false
        return checkICPMVariantSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 非地下世界生成规则：R196 BiomeGenUnderworld 从生成列表移除了食尸鬼/亡魂，
     * 故二者在地下世界维度一律禁止（其余维度按 ICPM 变种规则）。
     */
    private fun checkNotUnderworldSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.level.dimension() == ICPMPortalHandler.UNDERWORLD_KEY) return false
        return checkICPMVariantSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 土元素生成规则（R196 EntityEarthElemental.getCanSpawnHere）：
     * 脚下方块必须为 石头 / 黑曜石 / 地狱岩 / 末地石，并叠加原版怪物黑暗判定。
     * 下界（地狱岩/黑曜石）与末地（末地石）由同一规则覆盖。
     */
    private fun checkEarthElementalSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false
        if (EntitySpawnReason.isSpawner(reason)) {
            return Mob.checkMobSpawnRules(type, level, reason, pos, random)
        }
        val below = level.getBlockState(pos.below()).block
        // 1.21.11 深层洞穴（Y<0）地板是 DEEPSLATE 而非 STONE，必须纳入，否则土元素在主生成层被全拒
        if (below != Blocks.STONE && below != Blocks.DEEPSLATE && below != Blocks.OBSIDIAN && below != Blocks.NETHERRACK && below != Blocks.END_STONE) {
            return false
        }
        if (level.getMaxLocalRawBrightness(pos) > random.nextInt(16)) return false
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 黏土魔像生成规则（R196 EntityClayGolem.isValidBlock）：仅黏土方块上方生成。
     */
    private fun checkClayGolemSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false
        if (EntitySpawnReason.isSpawner(reason)) {
            return Mob.checkMobSpawnRules(type, level, reason, pos, random)
        }
        if (!level.getBlockState(pos.below()).`is`(Blocks.CLAY)) return false
        if (level.getMaxLocalRawBrightness(pos) > random.nextInt(16)) return false
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 矿工僵尸生成规则：
     * - 地下世界维度：始终允许（含黑暗判定）
     * - 主世界 非血月：仅矿洞（上方被遮挡 = 非露天）放行
     * - 主世界 血月之夜：仅地表（露天）放行
     * - 其他维度：禁止
     */
    private fun checkMinerZombieSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false
        if (EntitySpawnReason.isSpawner(reason)) {
            return Mob.checkMobSpawnRules(type, level, reason, pos, random)
        }
        // 地下世界：始终允许
        if (level.level.dimension() == ICPMPortalHandler.UNDERWORLD_KEY) {
            return checkICPMVariantSpawnRules(type, level, reason, pos, random)
        }
        if (level.level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return false
        val bloodMoon = name.icpm.common.ICPMMoonPhase.isBloodMoonNight(level.level)
        if (bloodMoon) {
            // 血月之夜：仅地表（露天）刷新
            if (!level.canSeeSky(pos.above())) return false
        } else {
            // 非血月：仅矿洞（非露天）刷新
            if (level.canSeeSky(pos.above())) return false
        }
        if (level.getMaxLocalRawBrightness(pos) > random.nextInt(16)) return false
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 古尸 / 古尸守卫生成规则：地下世界任意时刻放行；
     * 主世界仅在【血月之夜】（晚上8点起）放行——血月时主世界会刷新平时不能刷新的古尸。
     */
    private fun checkUnderworldOrBloodMoonSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        val dim = level.level.dimension()
        if (dim == ICPMPortalHandler.UNDERWORLD_KEY) {
            return checkICPMVariantSpawnRules(type, level, reason, pos, random)
        }
        if (dim != net.minecraft.world.level.Level.OVERWORLD) return false
        if (!name.icpm.common.ICPMMoonPhase.isBloodMoonNight(level.level)) return false
        return checkICPMVariantSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 蜘蛛变种生成规则：与骷髅类似（黑暗环境 + 原版怪物判定）。
     */
    private fun checkICPMSpiderSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false
        if (EntitySpawnReason.isSpawner(reason)) {
            return Mob.checkMobSpawnRules(type, level, reason, pos, random)
        }
        if (level.getMaxLocalRawBrightness(pos) > random.nextInt(16)) return false
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 蝙蝠类生成规则：仅 ICPM 地下世界维度刷新（洞窟，有天花板）+ R196 亮度检查。
     * 主世界 / 下界 / 末地一律禁止（R196 蝙蝠为地下世界标志性生物）。
     *
     * 亮度检查移植 R196 EntityBat.getCanSpawnHere：取生成位置的亮度 blv，
     * `blv > rand.nextInt(4)` 则拒绝 —— 只有黑暗处（blv<4）才有机会生成，
     * 亮度越高越不可能。此前缺此检查导致蝙蝠在任意亮度空间泛滥。
     */
    private fun checkICPMBatSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false
        if (EntitySpawnReason.isSpawner(reason)) {
            return Mob.checkMobSpawnRules(type, level, reason, pos, random)
        }
        // 仅在地下世界维度刷新
        if (level.level.dimension() != ICPMPortalHandler.UNDERWORLD_KEY) return false
        // 洞窟判定：上方有方块（天花板）
        if (level.canSeeSky(pos.above())) return false
        // R196 亮度检查：blv > rand.nextInt(4) → 拒绝（只有黑暗处能生成）
        val brightness = level.getLightEngine().getRawBrightness(pos, 0)
        if (brightness > random.nextInt(4)) return false
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 仅下界 / 地下世界维度生成规则：火元素、地狱苦力怕的维度限制。
     * （R196 火元素从下界岩浆生成、地狱苦力怕在下界/地下世界生成。）
     */
    private fun checkNetherOrUnderworldSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        val dim = level.level.dimension()
        val isNether = dim == net.minecraft.world.level.Level.NETHER
        val isUnderworld = dim == ICPMPortalHandler.UNDERWORLD_KEY
        if (!isNether && !isUnderworld) return false
        return checkICPMVariantSpawnRules(type, level, reason, pos, random)
    }

    /**
     * 灰银鱼生成规则（R196 在石头山地生成）：脚下方块必须为石头（含深板岩/圆石），
     * 并叠加原版怪物黑暗判定。
     */
    private fun checkHoarySilverfishSpawnRules(
        type: EntityType<out Mob>,
        level: ServerLevelAccessor,
        reason: EntitySpawnReason,
        pos: BlockPos,
        random: RandomSource
    ): Boolean {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false
        if (EntitySpawnReason.isSpawner(reason)) {
            return Mob.checkMobSpawnRules(type, level, reason, pos, random)
        }
        val below = level.getBlockState(pos.below()).block
        if (below != Blocks.STONE && below != Blocks.DEEPSLATE && below != Blocks.COBBLESTONE) {
            return false
        }
        if (level.getMaxLocalRawBrightness(pos) > random.nextInt(16)) return false
        return Mob.checkMobSpawnRules(type, level, reason, pos, random)
    }

    private fun <T : net.minecraft.world.entity.Entity> register(name: String, builder: EntityType.Builder<T>): EntityType<T> {
        val key = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(ICPM.MOD_ID, name)
        )
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key))
    }
}
