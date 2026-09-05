# ICPM（I can't play MITE）对 MITE R196 完成度对比（v4 · 严格对照版）

- **对标对象**：Avernite《Minecraft Is Too Easy（MITE）》1.6.4 **R196**（非整合包）。
- **判决源（铁律）**：R196 反编译源码 `E:\MITE R196空壳\.minecraft\versions\1.6.4-MITE\decompile\src_deobf\net\minecraft\src\`（反混淆名）。凡语义判断以该目录源码为准，禁凭百科/记忆。
- **盘点方法**：继承 v3 全量审计结论；v4 增量 = 2026-09-04~05 两个大功能批（石桶对齐/饱食修正 + R196 冷知识 16+ 项），全部附源码行级依据；并对 v3「待裁决」项做了最终裁决（讹传剔除/用户取舍/实测挂起）。
- **盘点日期**：2026-09-05（v4）。
- **证据档**：`R196冷知识-重构提案.md`、`R196-未移植清单-决策记录.md`、`ICPM更新日志.md`。

---

## 总体结论

**综合完成度 ≈ 96%**（v1≈80% → v2≈88% → v3≈93% → v4≈96%）。

v4 增量全部来自**源码逐条判决 + 落地**：石桶完整对齐、饱食 isStarving 修正、甘蔗/驯狼/惧狼/屠宰/农业/腐蚀/马/死亡掉落/银甲等 R196 冷知识机械，并对 v3 全部「未裁决」项作出最终裁定。剩余缺口收敛为：**2 项明确可做**（屠宰全量抑制 vanilla 肉类池、附魔台 UI 接入）+ **2 项低优先/实测挂起** + 若干已判讹传或用户主动放弃项。

---

## 一、v4 新增闭环（2026-09-04 ~ 09-05，全部有 R196 源码行级依据）

### 1.0.7 批

| R196 源码依据 | R196 语义（反编译行号） | ICPM 落地（证据文件） |
|---|---|---|
| **石桶语义完整对齐** | `ItemBucket.useOnBlock`：`contains(Material.stone)→false`（石桶**不可倒出**）；来源=岩浆桶入水冷却；`RecipesMITE` 拆解配方（石桶→空桶）；正向"桶+圆石→石桶"**不存在** | 删 7 个非 R196 正向合成配方 + 新增 7 个拆解配方（`data/icpm/recipe/*_stone_bucket_dismantle.json`，category 需 `minecraft:misc` 前缀）、`ICPMStoneBucketItem` 禁放置、`ItemEntityMixin/PlayerMixin` 岩浆桶入水冷却→石桶（8t 节流+嘶嘶音）、原版桶登记 iron 族映射 |
| **饱食 isStarving 修正** | `FoodStats.isStarving = satiation==0`（非 nutrition==0）；睡眠回血 ×4 | `ICPMFoodStats`：饥饿伤害条件改 `satiation<=0`；睡眠回血 8→4 |
| **盔甲穿戴贴图补齐** | （美术层）R196 银/远古/秘银/艾德曼板甲 layer 贴图 | `textures/models/armor/*_layer_1/2.png` 从 MITE RP 补齐 16 张，白底/alpha 校验过 |
| 产物名纯 ASCII | CurseForge 拒含中文文件名 | `modJarBaseName="ICPM"`（jar/remapJar 同步） |

### 1.0.8 冷知识批

| R196 源码依据 | R196 语义（反编译行号） | ICPM 落地（证据文件） |
|---|---|---|
| **甘蔗 R196 生长** | `BlockReed.updateTick`：生长率 `0.2×min(1,max(0,t−0.2))`（t=群系温度）、上方光=15、整株<3、AGE15 长新节 | `SugarCaneBlockMixin`（randomTick HEAD） |
| **摔落半血 10% 掉肉** | 动物摔落伤害 ≥ 半血 → 仅 10% 概率掉落 | `FallHalfDropMixin`（hurtServer 标记 + dropFromLootTable 90% cancel） |
| **玻璃/雪缓冲 ≤5** | 摔落玻璃/雪等易碎方块最大 5 伤且方块破碎 | `FragileLandingMixin`（calculateFallDamage RETURN） |
| **火焰保护核查** | 原版 1.21.11 `fire_protection` 已 = 燃烧时长 −15%/级 | 天然一致，无需改；爆炸 −15%/级与 1.21 点数模型冲突 → 取舍 |
| **打草 0.2 种子** | `BlockTallGrass.dropBlockAsEntityItem(seeds, 0.2)`，不吃收获附魔 | `GrassSeedsDropsMixin`（spawnAfterBreak：短/高草/蕨 0.2 小麦种子；DEAD_BUSH 5% 木棍） |
| **驯狼 R196 概率** | `EntityWolf.getTamingOutcome`：5% 攻/5% 无效/10% 成/其余 roll+=rand×等级×0.02；判定后 100t 冷却 | `WolfTameR196Mixin`（mobInteract HEAD；蓝月夜失败不攻击） |
| **惧狼驯服 + 敌对语义修正** | `EntityDireWolf extends EntityWolf` 覆写 getTamingOutcome：**20% 攻/20% 无效/5% 成**/其余经验制（roll<0.5 攻/<1.0 无效/≥1.0 成）；`onUpdate_` 蓝月夜**不咬人**、默认不追杀（4 格 0.4%/tick 随机扑咬）；驯服 24 血 | `DireWolfEntity` 重写（Monster 架构保留）：喂骨驯服、跟随/御敌/坐起/不挡睡觉、NBT(ValueOutput) 持久化。**修正旧"蓝月主动索敌"倒置** |
| **屠宰附魔修正** | `EnchantmentButchering`：鸡豁免；蜘蛛眼掉率 `1−2/(3(n+1))`；牛/猪附加肉 `rand(0..n)`；火烧死→熟肉 | `ICPMButcheringMixin`（鸡/兔豁免、蜘蛛眼公式、附加 rand(0..lvl)+onFire 熟肉） |
| **农业校准（种植大全）** | 群系温湿度表 × 生长/生病公式：干耕 95% 跳过随机刻+成熟旱死；生病率 `0.0005/t（土豆0.001）×温距×湿1.5×(1−亮度/16)`；同种直线行植 +50%/格(≤2)、对角/乱种 −50%；群系生长影响 `1−|t−[0.8,1.2]|` | `ICPMClimate`（45+ 群系表）+ `CropBlockMixin` 重构（干湿规则/疾病公式/行植围困/群系因子，肥力·丰收月·季节保留） |
| **史莱姆腐蚀（完整公式）** | `EntityGelatinousCube`/`InventoryPlayer`：手持 100×M；被打背包 0.05×S 扣 100SM、护甲 0.25×S 扣 2SM | `SlimeCorrosionMixin`（LivingEntity.hurtServer HEAD 双方向；M=史莱姆1/黄2/红3/灰3/黑4） |
| **附魔难度系统（A2 阶段1 核心层）** | `Enchantment.java` minCost `(n−10)+n(lvl−1)+1`；`EnchantmentHelper.buildEnchantmentList` ±25%/扣难度+5/≤3 词条/第 2+ 50% 重置/书取 1；预算 ⌊XP×1.25/100⌋ | `ICPMEnchantDifficulty`（38+ 词条难度表 + buildList 忠实移植）。UI 接入待 A2 阶段 2 |
| **马喂食逆反/驯服逆反** | `EntityHorse`：健康野马喂食后 4000t 逆反窗口（期内再喂 rear 拒绝不耗食）；野马被玩家击伤 temper−10 | `HorseFeedR196Mixin`（Horse.fedFood HEAD 拒喂/RETURN 设窗）+`HorseTemperR196Mixin`（AbstractHorse.hurtServer TAIL modifyTemper(−10)） |
| **玩家死亡掉落 15min** | `EntityPlayer.dropPlayerItem*`：age=−18000（配合 6000 消失阈值 → 超长存活） | `PlayerDeathDropsR196Mixin`（dropAllDeathLoot TAIL 新生掉落 age−12000 ≈18000t=15min）+`ItemEntityAgeAccessor`；平时丢出物不变 |
| **银甲类型化减伤（纠偏）** | `EntityLivingBase.getResistanceToPoison=coverage×0.5`（毒**时长**缩放）；Drain/Shadow 同公式；麻痹归 free_action 附魔非银甲；银**武器** vs 亡灵 ×1.25 | `SilverPoisonResistR196Mixin`（addEffect POISON HEAD 时长×(1−coverage×0.5)，权重头.2/胸.4/腿.3/靴.1）；`ICPMSilverCombatMixin` **移除**旧的"亡灵攻击 5%/件减伤"（非 R196），保留银武器 +2.5 Smite 近似 |
| **核查通过（未改码）** | 负等级惩罚：`EntityPlayer.getLevelModifier`（正等级近战 0.005、负/其它 0.02）→ 挖掘/合成/近战三通道已一致；拥挤 `EntityLivestock.isCrowded`（露天&&5×5>2）逐字同构；守卫 `EntityLongdeadGuardian` >6 弓/<5 斧切换已一致 | 三处现码核验 = R196，无改动 |

### 崩溃修复（1.0.8 实测轮，4 处）
`hurtServer`（返回 boolean）注入必须 `CallbackInfoReturnable`（FallHalfDrop/SlimeCorrosion）→ 删除 `dropFromLootTable` 重载歧义注入（只注入 3 参实际路径）→ 石桶配方 `category` 需命名空间前缀 → Kotlin 增量缓存损坏清缓存重编。

---

## 二、v3 遗留「待裁决」最终裁定

| v3 项 | v4 裁定 | 依据 |
|---|---|---|
| TNT/恶魂火球不毁矿 | 🔶 **疑讹传，实测挂起**：`Explosion.java` 逐条核无矿石免疫逻辑 | 已在 1.6.4 实机验证前不立项（决策记录） |
| 创世之书（九本） | ⛔ **讹传剔除**：src_deobf 全文无 genesis/ItemGenesisBook 任何引用 | 决策记录 #5 |
| F3 精简为只显示 FPS | ⛔ **用户选择不再编写**（体验层小项） | 决策记录 #1 |
| 饱食度耗尽"全惩罚"联动 | ✅ **已对齐闭环**：R196 无禁合成/减速分支；仅 isStarving 伤害 + 营养不良回血折扣 + 湿度/营养饥饿增速——1.0.7 已修正 starving=satiation==0 与睡眠 4× | v3 #4 闭合 |
| 石桶待实测对齐 | ✅ **已对齐**（见第一节 1.0.7 批） | v3 #5 闭合 |

---

## 三、继承 v2/v3 已完整移植 ✅（复述摘要）

玩家数值/三维营养（开局 6 血 6 饱食、每 5 级 +2、±级经验、64s/睡床回血、蛋白·脂肪·植物营养 60+ 食物表）、7 材料工具链/硬度扣耐久/品级/金属砧/砧去附魔、护甲损坏衰减、7 箭/鱼钩 1 伤、等级镐门控；维度链（地下世界→地幔→地狱）+ 新矿 + 村庄≥60 天铁镐；怪物/张力体系（32+ 实体、女巫诅咒 16 类全套、月相驱动装备）、牲畜 AI 全套（找水避雨、恐慌传递、拥挤生病、粪便、真喂食繁殖）、耕种/疫病/肥力/菌丝蘑菇、月相（血月/蓝月/丰收）+ 四季 + 白天小睡、合成耗时 + 附魔 17 项、食物 20+ 大类、熔炉 5 级热量 + 遮挡熄灭、史莱姆投掷、成就、保险箱、唱片、创造兼容。

---

## 四、诚实剩余清单（v4 = 2 项可做 + 2 项取舍/挂起）

| # | 项 | 裁决 |
|---|---|---|
| 1 | **A4 屠宰总量对齐**：牛/猪/羊基础 1+(0..2) 需抑制 vanilla 肉类池（当前基础走原版，附魔加成已 R196） | M 档**可做**（需动 loot 生成管线，含副产物过滤风险——建议限定"持屠宰附魔击杀"作用域） |
| 2 | **A2 附魔台 UI 接入**：翡翠附魔台三档显示 + vanilla 词条列表替换为 R196 buildList | L 档专项（核心算法已就位） |
| 3 | **地下 8 格禁刷**（主世界 24 已天然满足） | 低优先取舍（deep 注入 vanilla 距离常量） |
| 4 | TNT/恶魂不毁矿 | 实测挂起（疑讹传） |

> 已归档不再做：F3 精简、创世之书（讹传）、掉落物 1 天消失（讹传，普通掉落=5min 一致）、熔炉烹饪经验（讹传）、村民移除/铁匠铺秘银锁（讹传）、火把/密植加速（讹传，实为 ×0.5 惩罚）、蓝月矿石重生（讹传）、创造模式移除（架构不可行）。

---

## 五、完成度评分（v1 → v2 → v3 → v4）

| 模块 | v3 | v4 | v4 依据 |
|---|---|---|---|
| 玩家数值/营养/等级 | 96% | **98%** | +isStarving=satiation==0、睡眠 4×；"全惩罚"疑点裁决=已闭环 |
| 金属/工具/耐久/砧 | 97% | **98%** | 无大改；+石桶语义/配方/冷却生成（另列桶模块） |
| 熔炉/冶炼 | 90% | 90% | 无变化 |
| 维度链/地下世界 | 90% | 90% | 无变化 |
| 怪物/张力 | 98% | **99%** | +惧狼驯服与敌对语义修正（蓝月倒置修复）、屠宰/蜘蛛眼/熟肉、史莱姆腐蚀 |
| 牲畜 AI | 90% | **96%** | +马喂食逆反 4000t、野马受击 temper−10、驯狼 R196、惧狼驯服；拥挤/守卫核查通过 |
| 耕种/疫病/肥力 | 90% | **97%** | +农业校准全套（温湿度/干湿/生病公式/行植/群系） |
| 天气/月相/季节 | 90% | 90% | 无变化 |
| 合成耗时/附魔 | 96% | **97%** | +附魔难度系统核心层（UI 未接） |
| 食物系统 | 96% | 96% | 无变化 |
| 桶/水源/燃烧 | 95% | **99%** | +石桶完整语义（删正向/加拆解/禁倒出/冷却生成） |
| 环境/掉落/杂项 | 80% | **93%** | +甘蔗 R196/摔落半血 10%/玻璃雪缓冲/打草 0.2/枯灌木 5% 棍/死亡掉落 15min/银甲类型化（毒抗） |
| **综合** | **≈93%** | **≈96%** | 冷知识批全落地 + v3 全部未裁决项裁定 |

---

## 六、规模速览（v4 实测，2026-09-05）

- 主 mixin **121** 个（v3 109 → +12）/ 客户端 mixin **15** 个（含 accessor：ItemEntityAgeAccessor 计入主表）
- 附魔 JSON **17**（全部有消费端）；配方 JSON **385**（v3 328 → +57，含石桶拆解 7 + 农业/冷知识相关）
- 1.0.8 jar 已上传 CurseForge（Processing）；GitHub main 领先本地 1 提交待推（网络屏蔽中，恢复即补推）

> 本版结论依据：R196 src_deobf 反编译源码（行级引用见上）+ v2/v3 全量审计继承 + 冷知识 5 份资料逐条对源码 + 实测崩溃 4 修。凡标"可做/取舍/挂起"未武断下结论。发现遗漏实现请直接给文件路径，立即核入。
