# 声明：ICPM（I can't play MITE）是 MITE 的二创移植模组，已修改侵权包名等，欢迎提出意见，我会积极改正。

> 关于美术资源：本项目贴图在 MITE 资源包风格基础上已做**调色板级再处理**（透明度与像素形状不变、色值全部偏离原作，不再存在与原作逐像素相同的文件）；如原作者或权利方仍有异议，请联系我，我会立即替换或移除。

> 本文件为 **NeoForge 端**更新日志（对应产物 `ICPM-1.1.6-NeoForge.jar`）。Fabric 端日志见项目根目录 `ICPM更新日志.md`。
> 命名说明：NeoForge 产物自 1.1.3 起统一为 **`ICPM-<ver>-NeoForge.jar`**（此前一度带 `[内测]` 前缀，系早期内部测试遗留，已去除）。
> 两端共享绝大部分源码（双树结构），本日志只记录 NeoForge 端的差异与落地情况。

## 1.1.6（2026-09-26）· 与 Fabric 端同步：砖投掷（新抛射物）+ R196 移植收口 + Infx 讹传归档

> 与根目录 `ICPM更新日志.md` 的 1.1.6 栏目内容一致（双树同源），此处只列 NeoForge 端需注意的点：

- **砖投掷双树同步**：新增 `BrickEntity`（继承 `ThrowableItemProjectile`）+ `BRICK` 实体注册 + `ThrownItemRenderer` 渲染接线 + `BrickThrowMixin`（`@Mixin(Item)` `use` 注入），与 Fabric 端同源码镜像。
- **NeoForge 渲染注册**走 `evt.registerEntityRenderer(...)`（区别于 Fabric 的 `EntityRendererRegistry.register`），其余逻辑一致。
- 燃料表、食物中毒纠偏、打火石耐久16/点燃、中毒间隔、玻璃碎片、动物掉落：双树同源，NeoForge 端同样落地。
- **横扫归档**：R196 无此机制，双树均不实现。

### 验证（NeoForge 侧）

`clean build` BUILD SUCCESSFUL；`neoforge/scripts/audit_mixins.py` 全绿；jar 全 entry 校验 + MD5 通过；三处部署 `ALL_OK=True`。

版本号提升至 `1.1.6`（产物名 `ICPM-1.1.6-NeoForge.jar`）。

## 1.1.5（2026-09-25）· 与 Fabric 端同步：禁用非生存 + 1.1.4 收口补完

> 与根目录 `ICPM更新日志.md` 的 1.1.5 栏目内容一致（双树同源），此处只列 NeoForge 端需注意的点：

- **禁用非生存双树同步**：新增 `ICPMDevMode` + `ICPMGameModeNormalizeMixin` + `ICPMPlayerJoinGateMixin`，改写 `ICPMCreativeModeGateMixin`；客户端新增 `ICPMWorldCreationGateMixin` + `ICPMLanCheatGateMixin` + `ICPMLanScreenGateMixin`（client mixin 注册 +3、common +2）。两树拦截的均为原版类（`ServerPlayerGameMode` / `PlayerList` / `WorldCreationUiState` / `ShareToLanScreen` / `IntegratedServer`），行为一致。
- **dev 模式后门只剩 JVM 参数**：`-Dicpm.devMode=true`。`config/icpm.json` 的 `enableCreativeMode` 已彻底移除（含 malilib 界面选项与译名键），旧配置里的遗留键读到即清除 —— 否则玩家改一下配置文件就能开创造，去作弊形同虚设。
- **`/icpmconfig creative on|off` 删除**，改为只读 `/icpmconfig devmode status`（两树同）。
- **下界合金 IMD 补完**：`assets/icpm/items/` 各 +23 个 IMD、多状态模型各 +4 个（弓 `_pulling_0/1/2`、钓鱼竿 `_cast`）；**创造栏补登**（`ICPMItemGroup`）；**lang 各 +6 键**（铜 / 金 匕首·短斧·镰刀 + 蓝莓丛）。
- **创造栏「金属砧」只列完好阶段**：裂痕 / 损坏砧没有对应物品（`ICPM.java` 注册方块时跳过 BlockItem），其 `asItem()` 返回 `Items.AIR`，而 1.21.11 的 `ItemStack.getCount()` 对 AIR 栈返回 0 ⇒ `CreativeModeTab` 会抛 `Stack size must be exactly 1`，使整个「ICPM 物品」栏构建失败（创造栏与 JEI 全空）。已在循环里过滤 `Items.AIR`。
- **跨树 API 坑**：创造栏补录金属砧时不得用 `BuiltInRegistries.ITEM.get(id)`（NeoForge 返回 `Optional<Reference<Item>>`，编译期直接报类型不兼容），统一走既有 loader 无关工具 `ICPMBlocks.getAnvilVariant(metalType, stage)`。
- **IMD 是 1.21.4+ 的通用改动**（非 Fabric 特例）：NeoForge 端漏 IMD 同样表现为物品栏无物品状贴图，必须双树同步。

- **R196 燃料表**（同日补充）：新增 `ICPMFuelValues`（逐条标注 R196 文件:行），接入 `FurnaceFuelMixin`（原版/高炉/烟熏炉 `getBurnDuration`）与 `ICPMFurnaceBlockEntity.getBurnDuration / getHeatLevel`。显眼三处：原木 300→**1600**、岩浆桶 20000→**3200**、火把在 1.21 不能当燃料→R196 **800**。两树同源。
- **三处食物中毒数值纠偏**（同日补充，`ICPMFoodProperties.VANILLA_CONSUMABLES`）：判决源 R196 `ItemFood.onEaten`（`duration * 20`）—— 生鸡肉 150→**400**、腐肉「饥饿600+中毒300」→**仅中毒 400**、蜘蛛眼 300→**100**；毒马铃薯本就正确。两树同源。
- **1.21 陷阱记录**：挂墙火把（`WALL_TORCH` / `REDSTONE_WALL_TORCH` / `SOUL_WALL_TORCH`）**只有方块、没有 `Items` 常量**（与普通火把共用一个物品），物品级判定只能写 `Items.TORCH / REDSTONE_TORCH / SOUL_TORCH`。

### 验证（NeoForge 侧）

`build` BUILD SUCCESSFUL（41s，增量）；`neoforge/scripts/audit_mixins.py` 全绿（156 mixin / 246 注入点）；发版预检 0 问题；双树自检 FAILS = 0；jar 全 entry 校验通过（`ICPMDevMode` 等新类与新增 IMD / 模型 / 贴图全部命中）；**服务端冒烟** `Done (0.478s)!`。

版本号提升至 `1.1.5`（产物名 `ICPM-1.1.5-NeoForge.jar`）。

## 1.1.4（2026-09-19）· 与 Fabric 端同步：下界合金体系 + R196 移植收口 + 崩溃修复

> 与根目录 `ICPM更新日志.md` 的 1.1.4 栏目内容一致（双树同源），此处只列 NeoForge 端需注意的点：

- **下界合金全套双树同步**：16 件物品（粒 / 链条 / 币 / 箭 / 剪刀 / 钓鱼竿 / 弓 / 桶组 / 锁链甲 ×4）与 7 件方块（砧 ×3 / 工作台 / 箱 / 门 / 符文石）在 NeoForge 端同样注册到 `ICPMItems` / `ICPMBlocks` / `ICPMBlockEntities`，资源（blockstate / 模型 / 贴图 / 译名 / 配方 / 标签）双树一致。
- **砧等级序重排**（秘银 5 / 艾德曼 6 / 下界合金 7）与 Fabric 端同源（`BlockMetalAnvil.MetalType` + `MetalAnvilMenu`）。
- **符文门第三种**（下界合金 80000）与**工作台等级门槛**修正（`ICPMWorkbenchMenu`）双树同源。
- **`@WrapOperation` 迁移**：`ICPMInfernalCreeperSpawnMixin` 由 `@Redirect` 改为 MixinExtras `@WrapOperation`。Fabric 端改动的直接动因是与 Carpet 的注入点冲突（NeoForge 端无此冲突），但双树保持同源；MixinExtras 由 NeoForge 自带（0.5.3）。
- **锈铁材质附魔值 0 → 1**：`FlintTier.kt` / `ICPMItems.kt` 双树各 3 处；NeoForge 端同样会在注册期崩溃，必须同步。
- **`dead_crop` blockstate 键修正**（`age=0` ~ `age=7`）双树同步。

### 验证（NeoForge 侧）

`clean build` BUILD SUCCESSFUL；`neoforge/scripts/audit_mixins.py` 全绿；发版预检 0 问题；jar 全 entry 校验 2861 条（`testzip=None`）；**服务端冒烟** `Done (0.382s)!`。

版本号提升至 `1.1.4`（产物名 `ICPM-1.1.4-NeoForge.jar`）。

## 1.1.3（2026-09-13）· 与 Fabric 端同步：作物随机刻崩溃热修（甜菜根）

> 与根目录 `ICPM更新日志.md` 的 1.1.3 栏目内容完全一致（`CropBlockMixin` 双树同源），不再重复细节。

- **崩溃**：血月夜扫到甜菜根随机刻 → `IllegalArgumentException: Cannot get property age(0-7) as it does not exist in Block{minecraft:beetroots}` → 崩服。
- **根因**：`CropBlockMixin` 用类常量 `CropBlock.AGE`（`AGE_7`）读写作物状态，而 `BeetrootBlock`(AGE_3) / `TorchflowerCropBlock`(AGE_2) / `PitcherCropBlock`(AGE_4+HALF) 各有自己的 age 属性。
- **修复**：新增 `cropAge(BlockState)`（按属性名 `"age"` 遍历取值）与 `isMature(BlockState)`（走 `CropBlock.isMaxAge()`），替换全部 7 处硬编码常量；并加固枯死作物跳过随机刻、肥力吸收前先判方块类型。
- **版本号同步提升至 1.1.3**（`neoforge/build.gradle.kts` + `neoforge.mods.toml`），产物 `ICPM-Neoforge-1.1.3.jar`。

### 附：人形怪物贴图错位（64×32 → 64×64 重映射，与 Fabric 端同源）

> 与根目录 `ICPM更新日志.md` 1.1.3 栏「附：人形怪物贴图错位」完全一致（资源文件双树同步）：`ghoul` / `wight` / `shadow` / `invisible_stalker` / `earth_elemental_clay` / `fire_elemental` / `bone_lord` 共 7 张 64×32 人形贴图升级为 64×64（上半原样保留、左臂左腿镜像写入底部独立区域），根因是 1.21.11 原版 `Zombie/Humanoid/Skeleton` 模型声明 64×64 纹理而 MITE 资源为 64×32，直接喂入导致 UV 错位（头长到脚上）。两端 `src/main/resources` 与 `neoforge/src/main/resources` 已同步。

### 附：配置增强（NeoForge 侧：MaFgLib 图形配置界面）

- **弱击开关 `weakStrike`（双树同源，与 Fabric 端一致）**：默认 `true` = 忠实 R196；`false` = 关闭弱击（空手/未持工具也能正常造成伤害）。经 `ICPMConfig` 落盘到 `config/icpm.json`。
- **`icpm.json` 改为「带中文注释」格式**：读取时自动剥离 `//` 注释再解析，写入时每个选项上方都带一句中文解释（旧无注释文件自动升级，数值保留）。
- **配置 GUI 改用 MaFgLib（本端特有）**：NeoForge 无原版 malilib，改接其非官方移植 **MaFgLib**（同为 `fi.dy.masa.malilib` 包，1.21.11 版 `0.4.6+mc1.21.11`）。落地：`neoforge/libs/mafglib-neoforge-1.21.11-0.4.6.jar`（**仅编译期 `compileOnly`，不打入 jar**）+ 镜像 `ICPMMaLiLibConfig` / `ICPMConfigScreen` / `ICPMConfigAccess`（反射安全入口）+ `neoforge.mods.toml` 追加 `mafglib` 可选前置（`CLIENT` / `AFTER`）。**未装 MaFgLib 时**：`ICPMClient` 按键（默认 H）提示改用 `/icpmconfig` 命令，功能不受影响。
- **验证**：NeoForge BUILD SUCCESSFUL；jar 内新类齐全、含 `weakStrike`/`isWeakStrikeEnabled`，且 **MaFgLib 未被误打包**（`fi/dy/masa/malilib/` 条目 = 0）；部署 `ICPM-1.1.3-NeoForge.jar`（md5 `acc802fc…`，已去除 `[内测]` 前缀）。

### 附：无攻击速度（1.6.4 原版机制）配置项 `noAttackCooldown`（与 Fabric 端同源）

- **机制**：1.6.4 **没有**攻击冷却。判决源 `EntityPlayer.java` / `EntityLivingBase.java` 整文件检索 `attackStrength` / `attackCooldown` / `attackTime` / `getAttackStrengthScale` —— **0 匹配**（只有手臂挥动动画 `swingProgress`），每次挥击都按满伤害结算；现代版的攻击冷却与"充能伤害缩放"是 **1.9 才引入**的。
- **开关**：`noAttackCooldown`，**默认 `true` = 1.6.4 忠实手感**；`false` = 保留现代版攻击冷却（连点衰减）。落地为新 mixin **`ICPMNoAttackCooldownMixin`**（双树同源；在 `Player.getAttackStrengthScale(float)` HEAD 恒返回 `1.0F` 满充能），并已注册进本端 `icpm.mixins.json`。修改途径：`config/icpm.json`（带中文注释）／MaFgLib「上天眷顾」页／命令 `/icpmconfig attackcooldown on|off|status`。
- **为何不改 `ATTACK_SPEED` 属性**：把攻速调到极大也拿不到 1.0（`attackStrengthTicker` 刚被 `attack()` 重置时 `(0+0.5)/delay` 仍 < 1，且 `delay → 0` 除零得 NaN）；返回 1.0 对任何形式的缩放公式都恰为「满充能伤害」。
- **验证**：NeoForge BUILD SUCCESSFUL；mixin 注入点审计 **146 个 mixin / 232 个注入点**全绿；发版预检 **0 问题**；`verify_jar` 通过（2654 条，`testzip=None`）；服务端冒烟 **`Done (0.462s)!`**；jar 内新 mixin 类与 `noAttackCooldown` / `isNoAttackCooldownEnabled` 均在。部署 `ICPM-1.1.3-NeoForge.jar`（md5 `acc802fc…`）。
- **顺带修复（发版闸门）**：`neoforge/scripts/audit_mixins.py`（GBK 控制台下打印 `✓` 抛 `UnicodeEncodeError`）与根目录 `scripts/preflight_release.py`（清理临时文件被本机 safe-delete 钩子拦截）此前**必然在最后一步异常退出**，导致「明明通过却退码 1」；已分别改为**统一 stdout 转 UTF-8**、**不再在脚本内删除临时文件**，现两者均正常退码 0。

### 附：锈铁（rusted_iron）全套 23 件注册（与 Fabric 端同源）

- **同时更正**：此前记载的「MITE RP 1.6.41 无锈铁贴图」是**误判** —— RP 内锈铁贴图一件不缺（tools 13 / armor 8 / armor layer 4 / arrows / chains）。误判源于只扫了 `items/` 一层未进 `tools/` 子目录，以及 R196 用材质化注册（`Item.swordRustedIron = new ItemSword(776, Material.rusted_iron)`，无 `*RustedIron*` 类可搜）。
- **R196 真值**：耐久 4.0 / 附魔 0 / 品质 poor（`EnumEquipmentMaterial:13`）；伤害加值 2.0（`Material:380`）；MinHarvestLevel 2、setMetal（`Material:646`）；挖掘效率 1.25 → 5.0（`ToolMaterialHarvestEfficiency:11`）；护甲板甲 6 / 锁甲 4（`ItemArmor:75,92`）；修复与铁共用（`Item:1015`）；箭伤害 = 材质伤害 = 2（`ItemArrow:85`）。
- **落地**：新增 `RUSTED_IRON_TIER` + 两个护甲材质 + `RUSTED_IRON_MAT_DUR`；注册 23 件（13 工具/武器 + 8 护甲 + 箭 + 链）；13 条工具映射；物品定义/模型各 23 个 + 中英语言各 23 条；23 张贴图从 RP 导入并**去相似化**。
- **骨领主**恢复 R196 原样：武器 swordRustedIron 权重 2（日 ≥10 加 battleAxe、≥20 加 warHammer），护甲锈铁板甲全套（此前无护甲 + 远古金属替代）。
- **验证**：NeoForge BUILD SUCCESSFUL（6m38s）；jar 内容 贴图 23/23 · 护甲层 4/4 · 物品定义 23/23 · 模型 23/23 · 语言 23/23；mixin 审计 / 发版预检 / `verify_jar` 均退码 0。部署 `ICPM-1.1.3-NeoForge.jar`（md5 `ba437b39…`）。
- **仍待接线**：骷髅变种武器与箭（R196 用 `daggerRustedIron` / `arrowRustedIron`）。

### 附：R196 移植路线逐项推进（与 Fabric 端同步）

本轮闭环 **13 项**，NeoForge 端**同步实现**（逐项出处与判决源见根目录 `ICPM更新日志.md` 同名小节）：

- **地狱犬数值**：HP 20 / 攻击 4 / 移速 0.4；点燃改为 40% 概率、1–8 秒（`EntityHellhound.java:51-56,83-93`）。
- **附魔权重**：`ICPMEnchantDifficulty.buildList` 改为**按权重加权抽取**（此前是均匀随机 ⇒ 权重无消费者）；17 个 ICPM 附魔 `weight` 校准；原版 21 个附魔以 `data/minecraft/enchantment/` 覆盖为 R196 值。
- **`ShearsInteractMixin` 补注册**：此前被一段已被证伪的注释屏蔽，现取消并注册 —— 剪取 50 耐久 + 右键去抖在 NeoForge 端恢复生效。
- **近距刷怪放宽** `ICPMSpawnDistanceMixin`：主世界 / 地下世界全黑处放宽到 8 格（`SpawnerAnimals.java:192-206`）。
- **地下世界 32 天雾色循环** `ICPMUnderworldFogMixin`（客户端）：「第 16 天最黑、第 0/32 天最亮」。
- **疫病传播**改 R196：3×3×3 + 50% 门控、**不限成熟度**（`BlockCrops.java:99-108`）。
- **耕地肥力**改 R196 单比特：生长速率 ×1.5、1/256 概率消耗、**收获不扣**；粪便宜只对未施肥耕地生效；肥沃附魔改为 `等级/5` 概率（锄地 + 收获两处）。
- **地狱苦力怕 50% 替换 + 肿胀距离**：`ICPMInfernalCreeperSpawnMixin`（含主世界夜晚露天 75% 放弃）+ `ICPMSwellGoalMixin`（普通 16 / 地狱 36 ÷ 健康比）。
- **刷怪深度门控** `ICPMSpawnDepthGateMixin`：食尸鬼 y≤56、尸妖 y≤48（血月+冰冻）、亡魂 y≤44、追猎者 y≤40、暗影 y≤32（血月+沙漠）、吸血蝙蝠 y≤48（血月）、黑寡妇 50%、普通蜘蛛露天 1/4。
- **锈铁接线**：僵尸随机持械换回**真锈铁**；复仇僵尸补上装备（锈铁武器 + 板甲全套，此前完全无装备）。
- **溯源更正**：B2「箱子 loot 时间锁 + 创世之书」**不成立**（R196 内无 `genesis` / `Debris`，地牢战利品为静态两套表）；`clumsiness` 诅咒**确认早已实现**。

## 1.1.2（2026-09-13）· 与 Fabric 端同步：怪物特性收口 + 近战公式与工具攻击全量校准

> 本端与 Fabric 端共享绝大部分源码。本轮 NeoForge 特有的落地见下，其余改动（工具基准攻击校准、近战乘区补全、弱击、小刀 ÷4、变种数值、挖掘门控、熔炉比率、僵尸掉落/持械、骨领主召唤链、熄灯等）与根目录 `ICPM更新日志.md` 的 1.1.2 栏目完全一致，不再重复。

- **村民声望系统（NeoForge 侧注册）**——新增 `ICPMVillagerReputation.kt` 与 3 个 mixin（`ICPMVillagerReputationMixin` / `ICPMVillagerTradeMixin` / `ICPMIronGolemReputationMixin`），并补入本端 `icpm.mixins.json`。
- **凝胶立方族自然生成（NeoForge 侧注册）**——Jelly / Blob / Ooze / Pudding 四个实体在 `RegisterSpawnPlacementsEvent` 中注册刷怪规则，并在 `data/icpm/neoforge/biome_modifier/spawn_icpm_monsters.json` 追加四条刷怪条目（权重 8 / 6 / 6 / 4）。
- **客户端渲染修复（与 Fabric 端同源）**——矿工僵尸等自定义人形怪物的手持物/护甲不显示：1.21.11 `MobRenderer` 不再自带手持物与护甲层，现统一改经新基类 `ICPMHumanoidRenderer`（`HumanoidMobRenderer` + `HumanoidArmorLayer`）派生。
- **版本号同步提升至 1.1.2**（`neoforge/build.gradle.kts` + `neoforge.mods.toml`），产物 `ICPM-Neoforge-1.1.2.jar`。

## 1.1.1（2026-09-09 ~ 09-11）· NeoForge 1.21.11 移植落地 + R196 逐值审计纠偏 + 火花/枯死作物忠实移植 + 贴图去相似化

NeoForge 端自 1.1.1 起独立分发。本版本包含三部分：**平台移植**（NeoForge 21.11 时序模型适配）、**R196 忠实度纠偏**（与 Fabric 端同步）、**NeoForge 特有坑位修复**。

### NeoForge 平台移植（本端特有）

- **注册时序模型改造（根治 `Registry is already frozen`）**——NeoForge 在 mod 构造期注册表已冻结，原 Fabric 式"类加载即直写注册表"必然崩溃。现全部注册改走 mod bus `RegisterEvent` 按注册表分发，顺序为 `MOB_EFFECT → BLOCK → ENTITY_TYPE → ITEM → BLOCK_ENTITY_TYPE → MENU → RECIPE_SERIALIZER → CREATIVE_MODE_TAB`（依赖方向全部满足：生成蛋在 ITEM 窗口时实体已注册、方块物品在 ITEM 窗口时方块已建好）。
  - `ICPM.java` 的静态注册字段改非 final，新增 `registerDataComponents()/registerMenus()/registerMobEffects()`；`registerAllBlocks()` 拆为 `registerBlocks()`（BLOCK 窗口）+ `registerBlockItems()`（ITEM 窗口）。
  - `ICPMBlueberryBush` 拆为 `registerBlock()` / `registerItem()` 两段。
  - Kotlin `object`（ICPMItems / ICPMEntities / ICPMBlockEntities 等）的 `val` 字段在对应注册窗口首次访问 `INSTANCE` 时注册。
- **Kotlin 运行时前置改用 Kotlin for Forge 6.3.0**——NeoForge 不自带 kotlin-stdlib，缺失会 `NoClassDefFoundError: kotlin/enums/EnumEntriesKt`（Kotlin enum 的 `entries` 依赖它）。现 `build.gradle.kts` 引入 `thedarkcolour:kotlinforforge-neoforge:6.3.0`，`neoforge.mods.toml` 声明 required 依赖 `kotlinforforge`（`[6.1.0,)`、`ordering="AFTER"`），前置缺失时给出明确报错而非崩溃。安装时 mods 目录**只放一个版本的 `kotlinforforge-*-all.jar`**（6.3.0 内嵌 kotlin-stdlib 2.4.0，与本项目 Kotlin 编译器 2.4.10 兼容）。
- **HUD 改用原生 GUI 层**——1.21.11 的 `Gui.render` 已重构为分层系统，`renderHotbarAndDecorations` 被删除（原 mixin 注入点消失，报 `Scanned 0 target(s)`）。营养 HUD 现通过 `RegisterGuiLayersEvent` 注册在 `VanillaGuiLayers.EXPERIENCE_LEVEL` 之上。
- **mixin 包嵌套类型外移**——mixin 包内的 enum/record 被普通代码引用会触发 `IllegalClassLoadError: ... is in a defined mixin package ... cannot be referenced directly`。`ICPMToolRulesMixin` 的嵌套类型已外移为普通类 `ICPMToolRulesData`，mixin 内改用访问器方法。
- **其他 1.21.11 / NeoForge API 适配**：`ClientPacketDistributor.sendToServer` / `PacketDistributor.sendToPlayer`；`Level.getGameRules()` 仅在 `ServerLevel`（规则常量 `GameRules.NATURAL_HEALTH_REGENERATION`、访问器 `.get(rule)`）；`LivingDamageEvent.Pre` 不可 cancel → `setNewDamage(0)`；`SpawnPlacements` 改走 `RegisterSpawnPlacementsEvent(Operation.REPLACE)`；实体属性走 `EntityAttributeCreationEvent`；方块实体走 `new BlockEntityType(supplier, *blocks)`。
- **群系挂载 JSON 修正**——`data/icpm/neoforge/biome_modifier/*.json` 的 `biomes` 列表元素不能写 `"#tag"`（列表走 `Identifier` 编解码器、不接受 `#` 前缀，报 `Not a JSON object`）；已改为 NeoForge OR 语法 `{"type":"neoforge:or","values":[...]}`。
- **新增 NeoForge 专用 mixin 注入点审计脚本** `scripts/audit_mixins.py`——直接解析 mojmap 客户端 jar + NeoForge jar 的字节码，核对**已注册** mixin 的每个注入点（方法选择器含父类链、`@At(INVOKE/FIELD)` 调用是否真实存在于该方法字节码内、`@Accessor/@Invoker` 成员）。发版前与运行时冒烟一起跑。

### R196 忠实度纠偏（与 Fabric 端同步）

- **火焰与燧石**：推翻自创的"点燃计数 / 满 8 次烧毁"机制（R196 源码全局无 `ignition`/`combust` 任何痕迹，且会拦掉营火/蜡烛/TNT 与普通放火）；新增 `icpm:spark`（`BlockSpark extends BlockFire`）忠实还原"燧石打火=放火花、2 tick 后邻格可燃则变火否则消失、烧不起来也照扣 1 点耐久"。
- **饱食度**：按 R196 `FoodStats.java` 重构——饥饿乘数完整实现 `1 + 潮湿 + (营养不良 ? 0.5 : 0)`（淋雨/浸水/寒冷 ×2/炎热归零）；移除床上 ×20（R196 死代码，仅对作者白名单账号生效）；回血补 `naturalRegeneration` 规则与回血附魔系数。
- **工具耐久与伤害**：6 类工具衰减率对齐 R196（剑 2.0/0.5、锄 2.0/2.0、匕首继承剑、小刀、棍棒继承短棒 0.25、鸭嘴锄 0.4）；皮革耐久系数 `0.4/15` → `1.0/10`；72 处工具注册伤害参数统一为 R196 口径；补镰刀割草与附魔特例、斧砂岩 1.875 特例；区域张力补 `difficulty<2 → ×difficulty/2`。
- **作物枯死**：新增 `icpm:dead_crop` 枯死作物方块（保留生长进度、无随机刻、骨粉无效、破坏零掉落）；旱死（耕地无水时每随机刻 5%，未成熟转枯死 / 已成熟按掉落表掉落后枯死）与疫病致死（染病每随机刻 1/64 转枯死）按 R196 `BlockCropsDead` 补全。
- **清理**：删除确认零引用的死代码（旧回血管理器、与源码冲突的旧材质表、未注册的伪 mixin、死字段）；修复金属修复材料标签（`tags/items/` → `tags/item/`，并恢复"锭 + 粒"两档）。
- **贴图去相似化**：全量比对出 496 张与 MITE 资源包逐像素相同的贴图，已做保留风格的调色板级微调——**现在 0 张与原作逐像素相同**，观感与原有风格一致。

### NeoForge 特有坑位（跨树不可直接复制）

- **`CropBlockMixin` 的 `getGrowthSpeed` 注入签名两端不同**：NeoForge 给该方法打了 patch，**首参是 `BlockState`**；而 Fabric/vanilla 首参是 `Block`。直接把 Fabric 版 mixin 覆盖过来会在客户端启动时抛 `InvalidInjectionException` 导致无法进入游戏——本端需保持 `BlockState` 签名。
- 跨树共享代码**只能用两端都存在的 vanilla API**：例如 `FireBlock.canCatchFire` / `BlockState.isFlammable` 为 NeoForge 独有，Fabric 映射下不存在 → 火花方块改用 protected 继承方法 `this.canBurn(state)`。
- `FireBlock` 子类的 `codec()` 泛型不协变：父类为 `MapCodec<FireBlock>`，子类须同样声明 `MapCodec<FireBlock>`。

### 关于"技能/专精"系统

- 经源码考据确认：R196 的技能（专精）系统属**作者未完成的废稿**（无成长途径、仅调试命令可授予，开启后无法正常游玩）。故**明确不实现**，特此说明以免误解为遗漏。

### 验证

- 服务端冒烟 `./gradlew runServer` → `Done (0.4s)!`，Kotlin for Forge 正常加载，无注册表冻结、无 mixin 注入错误。
- mixin 注入点审计：**142 个 mixin / 227 个注入点全绿**。
- 产物：`[内测]ICPM-1.1.1-NeoForge.jar`（NeoForge 21.11.45）。

> 说明：以上全部改动均已双端构建通过、服务端冒烟与 mixin 审计全绿后部署。因未提升版本号，产物仍为 `1.1.1`，不另设版本栏目。

---

> **版本规则**：本日志只有 `neoforge/build.gradle.kts` 的 `version` 提升时才新开 `##` 版本栏目；未发版的改动一律并入当前版本栏目（栏目内按时间倒序排列）。
