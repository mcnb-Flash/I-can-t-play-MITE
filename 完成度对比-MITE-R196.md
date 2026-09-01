# ICPM（I can't play MITE）对 MITE R196 完成度对比（v2 修正版）

- **对标对象**：Simulectics 的 **Minecraft Is Too Easy（MITE / MC 实在是太简单了）1.6.4 R196**（注意：不是 mcmod 上桶子哥的《MITE-打破一切》整合包）
- **盘点方法**：本地 ICPM 源码/数据包**全量关键词扫描**（2265 个文件，22 组特性关键词 × 逐文件比对）+ 关键文件精读（ICPMTension / ICPMFoodProperties / PlayerNutritionManager / ICPMCombatEnchantMixin / ICPMKeyBindings / CreeperExplosionMixin / ItemEntityMixin / CropBlockMixin 等） × MC 百科资料站（minecraftxz《MC实在是太简单了》特性总览 + 1Minecraft MITE 特性列表）逐项交叉核对
- **盘点日期**：2026-09-02（v2：修正首版误判——首版仅按目录+零星 grep 判定，漏检大量实现；本版全部以源码检索证据为准）

---

## 总体结论

**综合完成度 ≈ 88%，核心系统近乎全量移植，缺失集中在"体验层小项"。**

v1 报告有 10+ 项误判为"缺口"，实际源码里都有（附魔 5 项、食物 20+ 种、区域张力体系、Z 缩放键、唱片掉落、可可豆限制等）。修正后：**R196 的数值体系、金属/工具/砧、维度链、怪物阵容、牲畜 AI、耕种/疫病/肥力、月相季节、合成耗时、附魔体系均已完整实现**；真正缺失的只剩 12 项左右且多为低成本小项。

---

## 一、已完整移植 ✅

### 玩家数值 / 营养 / 等级
| R196 特性 | ICPM 依据（源码实据） |
|---|---|
| 开局 3 心 3 鸡腿（6 血 6 饱食） | `PlayerStatsManager` MIN_MAX_HEALTH=6 / MIN_MAX_FOOD=6 |
| 每 5 级 +1 心上限，至 20 | `PlayerStatsManager.updatePlayerStats`（每 5 级 +2 半心） |
| 200 级上限 + 带符号经验（可负级） | `ICPMExperience` MAX_LEVEL=200 / MIN_LEVEL=-40 / MIN_EXPERIENCE=-800；`PlayerMixin.giveExperiencePoints` @Overwrite |
| 每级 +2% 合成 / +2% 破坏 / +0.5% 近战 | `ICPMExperience.getLevelModifier` → `ICPMWorkbenchMenu` / `ICPMToolRulesMixin` / `ICPMCombatEnchantMixin` |
| 生命回复 64s/点、睡床 8 倍 | `ICPMHealProgressManager` + `PlayerMixin` |
| **蛋白质 / 必需脂肪 / 植物营养素 三维营养**（R196 ux/jv 精确移植，恢复量=营养级×8000，上限 160000） | `PlayerNutritionManager`（食物表 60+ 项精确数值；每 tick 消耗；营养不良判定 protein==0 OR phytonutrients==0 → 回血 25% + 饥饿 +50%） |
| 饱食度双槽（satiation/nutrition）+ 饥饿累积 | `ICPMFoodStats`（睡眠 ×20、湿/营养不良乘数、饥饿伤害、自然回血） |

### 金属 / 工具 / 耐久 / 砧
| R196 特性 | ICPM 依据 |
|---|---|
| 铜/银/金/铁/远古/秘银/艾德曼 7 材料等级链 | `ICPMMaterials` + `ICPMItems`（287 注册） |
| 移除木/石/钻石工具，木剑→木棒 | `remove_vanilla_recipes` + 配方覆盖 |
| 棍棒/小刀/短剑/战锤/战斧/鹤嘴锄/镰刀/短斧 | `ICPMItems` |
| 按方块硬度消耗耐久 | `ICPMDurability` / `ICPMToolDurabilityMixin` |
| 制造消耗经验选品级（wretched→legendary） | `EnumQuality` + `CraftPreviewComponent` + `ICPMWorkbenchMenu` |
| 金属砧修理（同/高级砧、金属粒、无经验、砧耐久） | `MetalAnvilMenu` / `TileEntityMetalAnvil` / `ICPMMetalAnvilItem` |
| 护甲损坏后护甲值下降 | `ICPMArmorValueMixin` |
| 7 种箭 + 鱼钩 1 点伤害 | `ICPMItems` 箭矢 + `ICPMArrowEntity` |
| 金属等级限制（高级方块需高级镐、艾德曼不可破） | `ICPMBlockHardness` / `ICPMToolRulesMixin` |

### 维度 / 世界
| R196 特性 | ICPM 依据 |
|---|---|
| 地下世界维度（基岩建门进入） | `icpm:underworld` + `UnderworldPortalBlock` + `ICPMPortalShape` |
| 地狱需经地下世界地幔 | `HellPortalBlock` + `ICPMPortalHandler` |
| 地下世界地幔/基岩豁口 | `ICPMUnderworldBedrock` |
| 新矿石（地下大型矿脉） | `ICPMOreGenerator` |
| 村庄 ≥60 天 + 铁镐条件 | `ICPMVillageGenerationMixin`（MIN_DAY_FOR_VILLAGE=60） |

### 怪物 / 张力
| R196 特性 | ICPM 依据 |
|---|---|
| **区域张力（Tension）难度体系：玩得越久、区块居住越久 → 怪物穿甲/附魔/首领/土元素挖矿冷却/蜘蛛药水概率越高**（R196"越玩越难"核心） | `ICPMTension`（居住时间/3600000×难度×月相，驱动全怪物装备概率） |
| 32+ 实体：惧狼/地狱犬/食尸鬼/尸妖/影子潜伏者/地狱苦力怕/四史莱姆/四蜘蛛/火·土元素/巨型·矿工僵尸/吸血蝙蝠/骨王/湮灭骷髅/夜翼/暗影/银甲虫等 | `ICPMEntities`（32 个注册名）+ `ICPMNewMonsters` + 各 Entity.kt |
| 僵尸破方块/烧树 | `ZombieDigGoal` / `ZombieBurnTreeGoal` |
| 骷髅近战/远程分支 + 狂暴 | `ICPMSkeletonVariants` / `SkeletonFrenzyState` |
| 怪物带装备武器 | `MobRandomIcpmArmorMixin`（张力驱动） |
| 苦力怕爆炸重算 | `CreeperExplosionMixin` |

### 牲畜 / 耕种 / 天气 / 月相
| R196 特性 | ICPM 依据 |
|---|---|
| 动物需水/食物、主动找水、避雨避火 | `ICPMSeekWaterIfThirsty` / `ICPMSeekShelterFromRain` / `ICPMGetOutOfWater` / `ICPMSeekFoodIfHungry` |
| 恐慌传递、拥挤生病、粪便 | `ICPMFleeWhenSpooked` / `ICPMSeekOpenSpaceIfCrowded` / `LivestockState` / `ICPMCompostHelper` |
| 繁殖需真喂食 | `ICPMLivestockBehaviorMixin` / `ICPMLivestockGrowthMixin` / `ICPMLivestockTickMixin` |
| 洋葱/蓝莓 + 香蕉/橘子等水果作物 | `icpm:onion` / `icpm:blueberry` / `icpm:orange` / `icpm:banana` |
| 作物需自然光、生长慢、季节加成 | `CropBlockMixin` + `ICPMSeason`（四季加成） |
| 疫病传播、骨粉治病 | `ICPMPlantDisease` + `BoneMealMixin` |
| 耕地肥力 | `ICPMFarmlandFertility` |
| 小麦减产 | 配方/战利品覆盖 |
| 血月/蓝月/丰收之月 + 广播 | `ICPMMoonPhase` + `ICPMWeatherMixin` + `BloodMoonSkyMixin` |
| 血月：怪物狂暴、作物染病、不能睡 | `ICPMMoonFrenzyMixin` / `ICPMMoonNoSleepMixin` / 病害联动 |
| 雨天/水中饥饿加速、晨昏/雨天钓鱼 + 虫饵 | `ICPMFoodStats` 环境乘数 + `ICPMMoonFishingMixin` |
| 白天小睡、睡觉限制 | `ICPMDaySleepMixin` / `ICPMBedBlockMixin` |

### 合成 / 附魔 / 食物 / 杂项
| R196 特性 | ICPM 依据 |
|---|---|
| 合成需时间、受控制不能合成 | `CraftingTimeMixin` / `CraftingTimeHelper` / `ICPMCraftingDelayMixin` / `ICPMCraftCooldowns` |
| **附魔：击晕(战锤)、吸血(剑)、缴械(近战)、穿刺(近战)、砍伐(斧)、肥沃(锄)、饵钓(鱼竿)、精准/迅捷/中毒/回收(远程)、再生(胸甲)、耐力、自由移动(鞋)、速度(鞋)** | `ICPMCombatEnchantMixin`（piercing/stun/vampiric/disarming）+ `ICPMRegenerationMixin`（regeneration）+ `ICPMArmorDurabilityMixin`（endurance）+ `ICPMArmorEnchantMixin`（freeAction）+ `ICPMAxeFellingMixin` + `ICPMFertilityMixin` + `ICPMMoonFishingMixin` + `ICPMArrowEnchantMixin` + `ICPMEnchantEffects` |
| **食物大类：面粉/面团/巧克力/奶酪/冰淇淋/冰沙/土豆泥/牛肉炖菜/鸡汤/蔬菜汤/奶油蔬菜汤/奶油蘑菇汤/南瓜汤/沙拉/粥/麦片/牛奶碗/蚯蚓（生熟）** | `ICPMFoodProperties` + `ICPMItems` + `PlayerNutritionManager.FOOD_NUTRITION`（R196 精确数值） |
| 成就引导 | `ICPMAchievementTriggers` + advancements |
| 保险箱（仅本人开、需高级镐） | `ICPMStrongboxBlockEntity` |
| 金属桶（岩浆损坏、浸水凝固/牛奶流失） | `ICPMBucketItem` / `ICPMBuckets` |
| 沙砾掉金属粒（加权单池） | `data/minecraft/loot_table/blocks/gravel.json` |
| 重力方块（泥土塌方） | `ICPMDirtGravityMixin` |
| 史莱姆球/砖投掷 | `GelatinousSphereEntity` |
| **Z 缩放键（默认 C，R196 为 Z）** | `ICPMKeyBindings.ZOOM` + `ZoomHandler` + `GameRendererMixin` |
| **唱片**（地下城/简单地牢战利品含 music_disc） | `underworld_dungeon.json` / `simple_dungeon.json` |
| **可可豆限制** | `ICPMToolRulesMixin`（Cocoa） |

---

## 二、部分移植 🔶

| R196 特性 | 现状 | 缺口 |
|---|---|---|
| 熔炉热量分级体系 | 自定义熔炉实体/燃料逻辑（`ICPMFurnaceBlockEntity` / `FurnaceFuelMixin`） | 粘土/沙石/硬化粘土/黑曜石/地狱岩 5 级完整分级、秘银需岩浆/艾德曼需烈焰棒的分级冶炼、**被不透明方块遮挡熄灭**均未确认闭环 |
| 饱食度耗尽惩罚联动 | 饥饿伤害/营养不良已实现 | "耗尽时禁合成/禁放置/减速/空手无伤"的全惩罚未确认闭环 |
| 装备材质特效（银抗腐蚀/远古抗高温/亡灵杀手） | 银/远古金属护甲存在（`ICPMSilverArmor`） | 抗腐蚀/高温防护特效未检索到实现 |
| 蓝月资源再生门控 | 蓝月效果全有（动物繁衍/作物/钓鱼加速/怪物抑制） | 严格"资源耗尽至蓝月才重生"的游牧节奏未确认 |
| 胰岛素反应/糖尿病 | 糖类食物（sugar/chocolate/honey）营养值标记为 0（简化处理） | 胰岛素负反馈/糖尿病机制未实现，以"糖类无营养"替代 |

---

## 三、确认缺失 ❌（源码检索无证据）

1. **攻击距离削减（空手 1.5 格）与潜行延长**（"reach"命中为 forEach 误报）
2. **掉落物 1 天消失**（`ItemEntityMixin` 仅做"掉落物火上烤熟"）
3. **TNT 不毁矿**（`CreeperExplosionMixin` 注释明确"其余爆炸（TNT、恶魂火球）不受影响"）
4. **F3 精简为只显示 FPS**
5. **鱿鱼靠近麻痹**
6. **熔炉冶炼/烹饪产出经验（mc1.8 移植）+ 漏斗溢出经验球**
7. **村民移除 / 铁匠铺箱子需秘银镐**
8. **火把/人造光源加速作物、相邻同种作物加速**（`CropBlockMixin` 只有季节加成）
9. **菌丝蘑菇生长机制**（`ICPMDirtGravityMixin` 仅注释"菌丝不参与物理效果"）
10. **女巫强化**（沼泽小屋/诅咒/召狼——"witch"命中均为 switch 误报）
11. **创世之书**（唱片有，九本创世之书无）
12. **创造模式移除**（Fabric 架构取舍：1.6.4 覆盖核心类可移除，Fabric 无法；已做创造模式兼容性处理）

---

## 四、ICPM 额外内容（R196 没有或增强）

- 可燃物烤制烹饪系统（点火状态机、短燃/正常火/烧毁、夹生、多份同烤）——`CombustionHandler` / `BurningCookingHandler`
- 四季（春夏秋冬）广播与作物生长联动——`ICPMSeason`
- 弓拉弦动画（1.21.4+ 物品模型）、ICPM 专属弓
- 经验负等级死亡/重生下限体系（AFTER_RESPAWN、keepInventory 豁免）
- 附魔台全量重构（`ICPMEnchantMenuMixin`）
- 额外怪物扩充（骨王/古骨王/湮灭骷髅/夜翼/暗影/复活者/银甲虫/黏土傀儡等 10+）
- 地下世界基岩可透光豁口、世界底部地幔封底
- 贴图全量回滚 MITE 1.6.41 资源包画风

---

## 五、完成度评分（v2 修正后）

| 模块 | v1 | v2 | 说明 |
|---|---|---|---|
| 玩家数值/营养/等级 | 85% | 92% | 三维营养+脂肪+营养不良全有；仅胰岛素缺失（有糖类简化替代） |
| 金属/工具/耐久/砧 | 90% | 95% | 完整 |
| 熔炉/冶炼 | 60% | 60% | 分级/遮挡/经验未闭环 |
| 维度链/地下世界 | 90% | 90% | 完整 |
| 怪物/张力 | 90% | 95% | 张力体系+32 实体；女巫强化缺失 |
| 牲畜 AI | 90% | 90% | 完整 |
| 耕种/疫病/肥力 | 80% | 80% | 核心全；光源加速/菌丝/可可豆(已实现) |
| 天气/月相/季节 | 90% | 90% | 完整 + 四季增强 |
| 合成耗时/附魔 | 75% | 95% | 附魔 15+ 项全有（v1 漏检 5 项） |
| 食物系统 | 60% | 95% | 20+ 新食物 + R196 营养值（v1 漏检整类） |
| 成就/杂项 | 60% | 70% | 成就/唱片/缩放有；F3/掉落物/创世之书缺 |
| **综合** | **≈80%** | **≈88%** | 核心玩法全量闭环，缺项为体验层小项 |

---

## 六、后续补全建议（按性价比排序）

1. **熔炉热量分级闭环 + 被遮挡熄灭 + 冶炼经验**（改动集中，机制感强）
2. **饱食度耗尽全惩罚联动**（禁合成/禁放置/减速/空手无伤）
3. **攻击距离削减 + 潜行延长**（体验差异大，需谨慎调参适配 1.21 战斗）
4. **掉落物 1 天消失 + TNT 不毁矿 + F3 精简**（杂项低成本）
5. **女巫强化**（沼泽小屋/诅咒/召狼）
6. **胰岛素/糖尿病**（把糖类从"无营养"升级为真正的胰岛素反应）
7. **光源加速作物 / 菌丝蘑菇 / 创世之书**（完善耕种与收藏）

> 注：本版所有判定均基于 2265 个源文件的全量关键词检索 + 关键文件精读；标注"未确认"的项是检索无证据且无替代实现。若仍有遗漏欢迎指出，可继续深挖。
