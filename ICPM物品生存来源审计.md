# ICPM 物品「生存可获得性」审计报告（2026-09-05 · 终版）

> 方法：329 个已注册物品模型 × data(recipe/loot/tag) + 源码消费 全量交叉。
> **最终裁决（用户）**：本项目还原**纯净 R196**。仅**马铠系**为真 R196 内容需补来源；
> 其余（矿石碎块/钢锡铅青铜锭/合金体系/宝石/slime_sphere/icpm:bow 等）均为**衍生 mod（ITF 等）贴图与功能，不计入、不修**。

## 一、真 R196 缺口（需修）

### 马铠 ×5 —— 注册存在但无任何获取途径
`copper_horse_armor / silver_horse_armor / mithril_horse_armor / ancient_metal_horse_armor / adamantium_horse_armor`

R196 事实：
- `ItemHorseArmor` 材质 = copper / silver / gold / iron+ancient_metal / mithril / adamantium（有效材质表，ICPM 5 种金属各一）
- **R196/1.6.4 无合成配方**（RecipesMITE 无 horse/barding 条目）→ 按 1.6.4 时代语义 = **宝箱战利品**

修复：加入主世界地牢（刷怪笼）箱子 loot（金属阶 稀有度递降），见 `data/minecraft/loot_table/chests/simple_dungeon.json` 新池。

## 二、已归档（衍生 mod 内容，不修）

| 组 | 项 |
|---|---|
| 矿石碎块 ×8 | copper/silver/gold/iron/tin/lead/mithril/adamantium_ore_chunk（ITF 采矿机制） |
| 金属锭 ×4 | tin/lead/bronze/steel_ingot（ITF 金属层） |
| 合金系 ×10 | alloy 工具×5 + 盔甲×4 + alloy_upgrade_template |
| 宝石 ×6 | ruby/sapphire/topaz/amethyst/opal/peridot |
| 杂项 | slime_sphere（R196 用原版 slime ball，此物无主）、icpm:bow（旧占位） |

## 三、非缺口（误报澄清）

`*_spawn_egg`(创造) · 各族水/岩浆/石桶(R196 桶机制) · core/mantle(结构方块，R196 语义破坏不掉落)

## 四、修复后本报告收敛为：**马铠 ×5（1 个 loot 池）**
