# ICPM 物品「生存可获得性」审计报告（2026-09-05）

> 方法：全部 329 个已注册物品模型 × 全仓库（data 配方/战利品/标签 + 源码掉落/机制消费）交叉引用；
> 判定「可获得」= 出现在任意 recipe 结果 / loot 表 / 方块挖掘产物 / 代码掉落或容器转换机制。
> 「零消费」= 除注册文件、创造物品栏、JEI 展示外**不存在任何获取途径**。

## 一、真实无来源（注册即死，生存拿不到）

### ① 矿石碎块 ×8 —— 应为"采矿掉落"却从未接线
`copper_ore_chunk / silver_ore_chunk / gold_ore_chunk / iron_ore_chunk / tin_ore_chunk / lead_ore_chunk / mithril_ore_chunk / adamantium_ore_chunk`
> R196：矿脉开采掉落对应碎块（chunk），碎块烧炼成锭。ICPM 采矿仍走 vanilla 原矿掉落，chunk 无任何产出点。

### ② 金属锭 ×4 —— 无熔炼配方
`tin_ingot / lead_ingot / bronze_ingot / steel_ingot`
> 应为 chunk（或合金冶炼）的熔炼产物，项目无对应 smelting 配方。

### ③ 宝石 ×6 —— 无矿无掉落
`ruby / sapphire / topaz / amethyst / opal / peridot`

### ④ 合金体系 ×10 —— 整套未落地
`alloy_axe / alloy_pickaxe / alloy_shovel / alloy_sword / alloy_hoe / alloy_helmet / alloy_chestplate / alloy_leggings / alloy_boots / alloy_upgrade_template`
> 疑似预留的下一材料层，无矿石/配方/合成台逻辑。

### ⑤ 马铠 ×5 —— 无合成配方
`copper_horse_armor / silver_horse_armor / mithril_horse_armor / ancient_metal_horse_armor / adamantium_horse_armor`
> R196 马铠可工作台打造（金属块 6 块造型），ICPM 只注册未加配方。

### ⑥ 杂项
`slime_sphere`（应挂史莱姆/果冻掉落，未接线）、`bow`（icpm:bow 疑似旧版遗留占位，与 vanilla bow 重复）

---

## 二、非缺口（脚本误报，机制/设计上可得）

| 类 | 说明 |
|---|---|
| `*_spawn_egg` ×16 | 刷怪蛋=创造模式专用（设计如此） |
| 各族 `*_water_bucket / *_lava_bucket / *_stone_bucket` | 空桶接液 / 岩浆桶遇水冷却生成（R196 桶机制，非配方产物） |
| `core / mantle` | 地下世界结构性方块（世界生成放置，破坏不掉落，符合 R196） |
| 9 系普通食物/材料 | 已有配方/箱子/作物来源（本次审计判定可获得） |

---

## 三、建议修复优先级

| 档 | 项 | 工作量 |
|---|---|---|
| **S（推荐先做）** | 马铠 ×5 加工作台配方（对齐 R196 金属块造型） | 5 个 json，10 分钟 |
| **S** | 矿石碎块 ×8 → 矿石方块挖掘掉落 chunk（替换原矿掉落或并出）；chunk→熔炼锭配方（tin/lead/bronze/steel 等 4 锭） | loot 表 + smelting json |
| **M** | 宝石 ×6：加宝石矿块+世界生成+掉落 | 方块+贴图+特征，较大 |
| **M/L** | 合金体系：需先定 R196/原创合成链（模板+基材），整条未设计 | 待产品决策 |
| **S** | slime_sphere 挂史莱姆掉落；确认 icpm:bow 是否移除 | 小 |

> 注：以上 33 项目前只在**创造模式物品栏**可见（物品栏遍历所有注册项）；生存模式完全不可得。
> 若某类本意就是"后续版本再开"（如合金/宝石），可在本文档标注 pending 即可，无需立即做。
