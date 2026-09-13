# 声明：ICPM（I can't play MITE）是 MITE 的二创移植模组，已修改侵权包名等，欢迎提出意见，我会积极改正。

> 关于美术资源：本项目贴图在 MITE 资源包风格基础上已做**调色板级再处理**（透明度与像素形状不变、色值全部偏离原作，不再存在与原作逐像素相同的文件）；如原作者或权利方仍有异议，请联系我，我会立即替换或移除。

> 本文件为 **NeoForge 端**更新日志（对应产物 `[内测]ICPM-1.1.1-NeoForge.jar`）。Fabric 端日志见项目根目录 `ICPM更新日志.md`。
> 两端共享绝大部分源码（双树结构），本日志只记录 NeoForge 端的差异与落地情况。

## 1.1.2（2026-09-13）· 与 Fabric 端同步：怪物特性收口 + 近战公式与工具攻击全量校准

> 本端与 Fabric 端共享绝大部分源码。本轮 NeoForge 特有的落地见下，其余改动（工具基准攻击校准、近战乘区补全、弱击、小刀 ÷4、变种数值、挖掘门控、熔炉比率、僵尸掉落/持械、骨领主召唤链、熄灯等）与根目录 `ICPM更新日志.md` 的 1.1.2 栏目完全一致，不再重复。

- **村民声望系统（NeoForge 侧注册）**——新增 `ICPMVillagerReputation.kt` 与 3 个 mixin（`ICPMVillagerReputationMixin` / `ICPMVillagerTradeMixin` / `ICPMIronGolemReputationMixin`），并补入本端 `icpm.mixins.json`。
- **凝胶立方族自然生成（NeoForge 侧注册）**——Jelly / Blob / Ooze / Pudding 四个实体在 `RegisterSpawnPlacementsEvent` 中注册刷怪规则，并在 `data/icpm/neoforge/biome_modifier/spawn_icpm_monsters.json` 追加四条刷怪条目（权重 8 / 6 / 6 / 4）。
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
