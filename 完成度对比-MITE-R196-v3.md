# ICPM（I can't play MITE）对 MITE R196 完成度对比（v3 · 严格对照版）

- **对标对象**：Avernite 的《Minecraft Is Too Easy（MITE / MC 实在是太简单了）》1.6.4 **R196**（非桶子哥《MITE-打破一切》整合包）。
- **判决源（铁律）**：R196 反编译源码 `E:\MITE R196空壳\.minecraft\versions\1.6.4-MITE\decompile\src_deobf\net\minecraft\src\`（已反混淆名）。**凡语义判断一律以该目录源码为准**，禁止凭百科/记忆猜测。
- **盘点方法**：① v2 全量 2265 文件关键词审计结果继承；② 2026-09-02~03 连续两轮 mixin"有名无实"专项审计（注入点命中反编译核验 + HEAD-cancel 短路排查 + 全代码消费核对）；③ 本轮对照 src_deobf 新增判决项全部反编译复核（行号标注于文）。
- **盘点日期**：2026-09-03（v3）。

---

## 总体结论

**综合完成度 ≈ 93%**（v1≈80% → v2≈88% → v3≈93%）。

v3 相对 v2 的增量全部来自 **R196 源码逐条判决 + 落地**：女巫诅咒全套、胰岛素/糖尿病、玩家攻击距离(R196 实值)、鱿鱼麻痹、菌丝蘑菇、砧去附魔、去咒药水(含配方)、R196 水源·桶机制、火焰烧肉按 R196 重构、mixin 审计修掉 7 处"有名无实"。同时依据源码**裁决删除 6 项百科讹传**（见第四节），并把 v2 误当缺口的数项闭环。真正剩余缺口收敛到个位数体验层小项。

---

## 一、v3 新增闭环（2026-09-02 晚 ~ 09-03，全部有 R196 源码行级依据）

| R196 源码依据 | R196 语义（反编译行号） | ICPM 落地（证据文件） |
|---|---|---|
| **女巫诅咒 16 类全套** | `EntityWitch.cursePlayer`(~319)/`EntityAITarget`(116)：女巫锁玩家 1/4 → `WorldServer.addCurse(player,witch,随机诅咒,6000)`；`Curse` 条件系统 + `checkCurses` realize + 杀女巫解除 + 去咒药水；禁甲 realize 瞬脱全身甲 | `curse/ICPMCurse`(16 变体)、`ICPMCurseManager`(效果变体架构：单一 `witch_curse` MobEffect，amplifier=诅咒 id)、`WitchCurseMixin`(即时施咒+死亡解除+施咒前状态检测)、`CurseChest/Armor/Passive/Drink/Entity/Sleep/Fear/Decay` 8 mixin 接真实生效点、`CurseCureItem`+配方(R196 `RecipesMITE:77` 玻璃瓶+地狱疣+煤) |
| **女巫召狼** | `EntityWitch.onLivingUpdate`(131-215)：被玩家打伤→60t 后在目标旁 8–16 格刷 1–3 只狼，需寻路可达，保活+仇恨目标，**一生一次** | `curse/WitchSummonManager`+`WitchSummonHookMixin`(hurtServer guard Witch→倒计时→`setLastHurtByMob+setTarget`) |
| **砧去附魔** | `ContainerRepair`(140-250 `is_disenchanting`)：装备+去咒药水 → 结果=清附魔副本，消耗药水 | `MetalAnvilMenu` 增分支（材料槽放行药水、预览清 `ENCHANTMENTS`、取件耗 1 瓶+原件、不耗金属/砧耐久） |
| **玩家攻击/交互距离** | `EntityPlayer.getReach`(2080-2145)：近战 1.5f、交互 2.5f、方块 2.75f + 武器 bonus + 高度差 ±1 clamp；**源码无潜行加成** | `PlayerReachMixin`：裁剪 `blockInteractionRange()/entityInteractionRange()`（客户端射线/服务端判定同源，无假限制）；LocalPlayer 无覆写→基类注入三端一致 |
| **鱿鱼麻痹** | `EntitySquid.onCollideWithPlayer_`(268-286)：贴近 <1.0 格且非船 → `Potion.moveSlowdown 200t, amp2`(缓慢 III) | `SquidParalysisMixin`(aiStep TAIL，覆盖发光鱿鱼) |
| **菌丝蘑菇生长** | `BlockMycelium.updateTick`(66-100)：菌丝上随机长蘑菇，邻近 ≤2 朵 | `MyceliumMushroomGrowthMixin`(@Mixin `SpreadingSnowyDirtBlock`：HEAD 拦暗处退化→低光 1/1024、9×5×9 ≤2→棕蘑菇) |
| **胰岛素抵抗/糖尿病** | `EnumInsulinResistanceLevel`(阈值 **48000/96000/144000**，非百分比)+`EntityPlayerMP.add/decrementInsulinResistance`(440-575：进食糖累加 IR=糖×4.8、每 tick −1、归零痊愈/跌破轻度留轻度/直上直降)+副作用(抵抗期吃糖 Nausea amp=级、severe+Poison、moderate+ 饱食折扣) | `ICPMInsulinResistance`(IR 0..192000+等级平移镜像+NBT)、`InsulinFoods`(糖表)、挂 `FoodNutritionMixin` 进食漏斗+END_SERVER_TICK 代谢 |
| **水源·桶机制** | `ItemBucket`(全文 432 行)：**接取不耗源头**(生存不删液块，创造才删)、接岩浆熔化概率(艾德曼0/金0.2/其余`0.01×秘银64÷耐久`：铜银16%/铁8%/远古4%/秘银1%)、熔化整桶销毁(harmed-by-lava)、**放置默认放流动**(moving+schedule moving→still 密闭成源/开放扩散)、**Ctrl+100xp 放源头**、同液体取消(防倒海造源)、shift tooltip 熔化概率 | `ICPMBucketRules`+`BucketItemR196Mixin`(原版三件套)+`ICPMBucketItem.use` 委托+`BucketSourcePacket`(C2S)+`BucketCtrlClientMixin`+`BucketTooltipMixin` |
| **火焰烧肉 R196 重构** | `EntityItem`(400-460)：受火 `cooking_progress += fire_damage×3` 满 100 → 熟食+经验+sizzle；烤熟给 3×3 火排程 10t 后按**堆料概率灭火**(<2 不灭/≥2 `0.01×2^n`/>15 强灭)；lava=烤熟非销毁 | `BurningCookingHandler` 重写(持续累计+延后概率灭火，删除"点火 4 次窗口/夹生保火/立即清火/熔炉类热源"自创逻辑)、`ItemEntityMixin` 精简、burning_blocks tag 收窄 fire/soul_fire/lava |

### mixin"有名无实"审计修复（v2→v3 之间，7 处，全部反编译实证）

| 修复 | 根因实证 | 修复方式 |
|---|---|---|
| 再生附魔 / 吸血附魔 / 升级回血 实际不回血 | `DisableVanillaHealingMixin` 对无"生命恢复"效果玩家**无条件 cancel heal()**，误杀 ICPM 自研回血 | 新增 `ICPMHealProgressManager.healAuthorized` 授权入口，三处改走授权 |
| 穿刺附魔永不触发 | `ICPMArmorValueMixin` 在 `getDamageAfterArmorAbsorb` **HEAD-cancel** 短路全方法 → 同方法 RETURN 注入(穿刺)不可达；且旧公式方向写反(注释 20% 实际穿 80%) | 穿刺并入护甲结算(护甲×1−min(1,级×0.2)，不穿附魔保护) |
| 剪羊毛/剪蘑菇牛无效(六把 ICPM 剪刀剪不了羊) | 1.21.11 剪毛在 `Sheep/MushroomCow.mobInteract`（`stack.is(Items.SHEARS)`+hurtAndBreak(1)），**不走** `Item.interactLivingEntity`；旧 mixin 挂错方法+硬比对原版 SHEARS | `ShearsInteractMixin` 重写为 @Mixin({Sheep,MushroomCow})：SHEARS 判定放宽任意 ShearsItem、HEAD 冷却、RETURN 补 49 耐久(合计 50) |
| 僵尸稀有掉落率偏差 5 倍 | `nextInt(base)>=5` = 5/base，注释/R196 为 1/base | 改 `!=0` |
| BowDurabilityMixin 空壳 | 未注册+空方法体；原版 1.21.11 由 `ProjectileWeaponItem.shoot` 统一扣弓耐久 | 文件删除 |
| 桶放"流动水"是薄水膜(审计再修复) | `FlowingFluid.getFlowing(amount,falling)` 把 amount **直接写 LEVEL**；传 7=极薄瞬流干 | LEVEL 7→1(最厚流动态) |
| 密闭结晶可能竖井叠双源 | 沉降检查未排除"下方同种液体" | 下方同液体→不结晶守卫 |

---

## 二、继承 v2 已完整移植 ✅（证据同 v2，经两轮审计复核无空壳）

- **玩家数值/三维营养/等级**：开局 6 血 6 饱食、每 5 级+2 半心、±级经验、64s 回血睡床 8×、**蛋白/必需脂肪/植物营养**（`PlayerNutritionManager` 60+ 食物精确值、营养不良惩罚闭环）。
- **金属/工具/耐久/砧**：7 材料等级链、品类全、硬度扣耐久、制造耗经验选品级、金属砧修理、护甲损坏衰减、7 箭+鱼钩 1 伤、等级镐门控。
- **维度链**：地下世界→地幔→地狱、新矿脉、村庄≥60 天+铁镐。
- **怪物/张力**：区域张力体系（居住时间×难度×月相驱动全怪物装备/首领）、32+ 实体（惧狼/地狱犬/食尸鬼/尸妖/影子潜伏者/地狱苦力怕/四史莱姆/四蜘蛛/元素/巨型·矿工僵尸等）。
- **牲畜 AI**（找水避雨避火、恐慌传递、拥挤生病、粪便、真喂食繁殖）、**耕种**（洋葱蓝莓橘香蕉、疫病/骨粉治病、肥力）、**月相**（血月狂暴/不能睡、蓝月、丰收月）**+四季增强**、**白天小睡**。
- **合成耗时/附魔 17 项**（17 个 enchantment JSON + 消费 mixin 全核验；speed/fishing_fortune 为数据驱动效果非空壳）、**食物 20+ 大类**（R196 精确营养值）。
- 成就引导、保险箱、重力泥土、沙砾掉粒、史莱姆投掷、Z 缩放键、唱片（地牢/地下城）、可可豆限制、创造模式兼容。
- **熔炉 5 级热量 + 遮挡熄灭**：`ICPMFurnaceBlockEntity`（isSmothered/isFlooded + 粘土1/沙石·原石2/硬化3/黑曜石/地狱岩 分级 maxHeat）经 09-02 审计确认**已闭环**（v2 误标"未确认"）。

---

## 三、经 R196 源码裁决：非缺口（v2 部分项 → 实锤）

| v2 状态 | 项 | R196 源码裁决 |
|---|---|---|
| 🔶 部分移植 | **胰岛素/糖尿病** | ✅ 已实现（见上），v3 升为完整 |
| 🔶 部分移植 | **熔炉热量分级+遮挡熄灭** | ✅ 确认已闭环（`TileEntityFurnace`/`getMaxHeatLevel` 对照一致），无需改动 |
| ❌ 缺失#1 | **攻击距离削减** | ✅ PlayerReachMixin 已实现（R196 无潜行加成，已如实记录） |
| ❌ 缺失#5 | **鱿鱼麻痹** | ✅ 已实现 |
| ❌ 缺失#9 | **菌丝蘑菇生长** | ✅ 已实现 |
| ❌ 缺失#10 | **女巫强化** | ✅ 全套已实现（小屋刷女巫为 1.21.11 原版结构自带，非缺口） |
| ❌ 缺失#2 | **掉落物 1 天消失** | ⛔ **讹传**：`EntityItem` 是 `age<6000`（5 分钟），与 1.21.11 一致，无缺口 |
| 🔶 部分移植 | **熔炉冶炼/烹饪经验（mc1.8）** | ⛔ **讹传**：R196 `TileEntityFurnace` 零引用 |
| ❌ 缺失#3 | **TNT 不毁矿** | 未裁决：R196 爆炸类源码未逐条核（暂留待查，非 v3 闭环项） |
| ❌ 缺失#7 | **村民移除/铁匠铺秘银镐锁** | ⛔ **讹传**：R196 保留村民交易 AI（EntityVillager 全套）；铁匠铺锁无源码证据 |
| ❌ 缺失#8 | **火把/同种密植加速作物** | ⛔ **讹传**：R196 同种相邻实为 **×0.5 惩罚**（非加速）且无火把加成；1.21 原版已含密植惩罚 |
| 🔶 部分移植 | **蓝月资源重生门控** | ⛔ **讹传**：R196 蓝月仅影响亮度/刷怪/繁殖，无矿石重生 |
| ❌ 缺失#4 | **F3 精简** | 未移植（客户端项；R196 语义未逐行核，待查） |
| ❌ 缺失#11 | **创世之书（九本）** | 未移植（R196 `ItemGenesisBook?` 未逐行核，待查） |
| ❌ 缺失#12 | **创造模式移除** | 架构取舍：Fabric 无法移除游戏模式，已做兼容 |

---

## 四、确认仍缺/待裁决（诚实清单，仅 4 项体验层 + 3 项未裁决）

1. **F3 精简为只显示 FPS**（客户端项，低优先）
2. **创世之书（九本）**（未裁决：需先核 R196 是否有 ItemGenesisBook 再定）
3. **TNT/恶魂火球不毁矿**（未裁决：R196 爆炸相关类待逐条核）
4. **饱食度耗尽"全惩罚"联动**（禁合成/减速等细项未逐一核 R196 FoodStats 全部惩罚分支）
5. 奶/石桶：R196 中 stone 桶不可倒出（`ItemBucket.contains(Material.stone)→false`）；ICPM 石桶语义已独立（`ICPMStoneBucketItem`）——待实测对齐
6. 创造模式移除：架构不可行（已兼容）

> 其余 v2 所列"缺口"不是已实现，就是源码裁决的讹传——v3 后**不再有 v2 那种"名义缺口"**。

---

## 五、完成度评分（v1 → v2 → v3）

| 模块 | v1 | v2 | v3 | v3 依据 |
|---|---|---|---|---|
| 玩家数值/营养/等级 | 85% | 92% | **96%** | +胰岛素/糖尿病（IR 48000/96000/144000 判决移植）；剩"耗尽全惩罚"细项 |
| 金属/工具/耐久/砧 | 90% | 95% | **97%** | +砧去附魔(R196 is_disenchanting)+去咒药水配方 |
| 熔炉/冶炼 | 60% | 60% | **90%** | 5 级热量+遮挡熄灭确认闭环；冶炼经验=讹传剔除 |
| 维度链/地下世界 | 90% | 90% | 90% | 无变化 |
| 怪物/张力 | 90% | 95% | **98%** | +女巫诅咒 16 类+召狼+小屋(原版自带)；击晕/吸血/缴械等 mixin 审计修复后真实生效 |
| 牲畜 AI | 90% | 90% | 90% | 无变化 |
| 耕种/疫病/肥力 | 80% | 80% | **90%** | +菌丝蘑菇；火把/密植=讹传剔除 |
| 天气/月相/季节 | 90% | 90% | 90% | 无变化 |
| 合成耗时/附魔 | 75% | 95% | **96%** | 两轮审计修 7 处空壳（再生/吸血/穿刺/剪毛/桶流动层等） |
| 食物系统 | 60% | 95% | **96%** | +糖类真实胰岛素反应（此前"糖=0 营养"简化升级） |
| 桶/水源/燃烧 | — | — | **95%** | 新增模块：R196 桶机制 + 火焰烧肉重构 |
| 成就/杂项 | 60% | 70% | **80%** | 掉落物 1 天/熔炉经验/村民移除/蓝月重生=讹传剔除；F3/创世之书/TNT 未裁决 |
| **综合** | **≈80%** | **≈88%** | **≈93%** | 核心全量闭环 + 判决制剔除 6 项讹传 |

---

## 六、规模速览（v3 实测）

- 主 mixin **109** 个 / 客户端 mixin **15** 个（含 8 诅咒效果 mixin + 桶 2 客户端 mixin）
- 自定义效果：`witch_curse`（16 变体）+ 营养相关 MALNUTRITION
- 附魔 JSON **17** 个（全部有消费端）；ICPM 配方 JSON **328**+（另含 minecraft 覆盖）；zh 语言键 **581**
- 实体注册表 85 条（含 R196 怪物 + ICPM 扩充）；桶物品变体 31 个（铁/铜/银/金/远古/秘银/艾德曼 × 空/水/岩浆/奶/石）
- 诅咒引擎 4 文件 / 胰岛素 2 文件 / 桶机制 6 文件（规则+包+3 mixin）

> 本版结论仅依据：R196 src_deobf 反编译源码（行级引用见上）+ 本地全量扫描 + 两轮 mixin 审计的反编译注入核验。凡标"未裁决/待查"均未武断下结论。若你发现遗漏的实现，直接给文件路径，我立即去核并入 v4。
