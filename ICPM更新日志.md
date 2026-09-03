# 声明：ICPM（I can't play MITE）是 MITE 的二创移植模组，已修改侵权包名等，欢迎提出意见，我会积极改正。

## 1.0.6

**新增：女巫诅咒全套 + 去咒药水（R196 完整移植，效果/变体架构）· 2026-09-03**

㉘ R196 女巫诅咒机制全套移植：

- **架构**：诅咒 = 单一 MobEffect `icpm:witch_curse`（效果/变体架构，amplifier 编码 16 类诅咒类型），玩家至多一中（原版效果槽天然保证），效果本体随玩家 NBT 自动持久化；检测统一 `ICPMCurseManager.isCursed(entity, curse, learn)`。
- **生命周期**：女巫远程攻击 1/4 概率施咒（6000 tick 延迟）→ pending 到期 realize 施加无限时长效果 → 杀施咒女巫 / 喝去咒药水解除；诅咒豁免牛奶与死亡清除（removeAllEffects 保留 witch_curse，牛奶不再成为解咒捷径）。
- **16 类诅咒全部接入真实生效点**：禁箱（ChestBlock.useWithoutItem）、禁甲（ArmorSlot.mayPlace + realize 瞬间自动脱甲）、厌食动/植物与禁饮（Player.canEat，食物分类自动取 FOOD_NUTRITION 蛋白/植物营养表）、禁饮药水（Item.use 拦普通药水，投掷药水放行）、装腐（hurtAndBreak 中央 4 参 ×2）、笨拙（合成等效等级−20 + 经验花费×2）、禁跑（setSprinting）、屏息（air 钳制 90）、缠绕（脚下藤蔓/植物减速）、禁眠（startSleepInBed 失败）、末影敌视（附近末影人周期锁定）、恐惧系（亡灵/蜘蛛/狼/苦力怕被命中 3/4 咬住不放）。
- **召狼**：女巫被玩家打伤后 3 秒在目标旁刷 1-3 只敌意狼（一生一次；setLastHurtByMob+setTarget 使其真实追击）。
- **去咒药水**：新物品 `icpm:bottle_of_disenchanting`（R196 ItemBottleOfDisenchanting），饮用即解咒且豁免禁饮。
- 68 个诅咒语言键（名称/描述/生效/解除，中英）。

㉙ 诅咒机制三项调整（R196 配方 + 即时诅咒 + 施咒前检测）：

- **去咒药水合成配方**（R196 RecipesMITE.java:77：水瓶 + 地狱疣 + 煤 无序合成）：新增 `data/icpm/recipe/bottle_of_disenchanting.json`（玻璃瓶 + 地狱疣 + `#minecraft:coals`；1.21 无独立"水瓶"物品故用玻璃瓶映射，配方语义等价）。
- **立即诅咒**：施咒不再等 6000 tick——`CURSE_DELAY_TICKS` 改为 0，`curse()` 在 delay≤0 时同步 realize 并施加效果（含禁甲瞬间脱甲与提示），onServerTick 仅作 pending 兜底。
- **施咒前状态检测**：女巫每次攻击前先查玩家——已有诅咒（生效/待生效）则直接放弃本次掷骰，不再尝试叠加。

**修复：剪羊毛机制空转 + 弓耐久空壳 + 僵尸稀有掉落率偏差（mixin 审计第二轮）· 2026-09-02**

㉗ 专项审计第二轮（全量精读剩余行为/方块/耐久/客户端 mixin + 字节码实证）确认并修复：

- **剪羊毛/剪蘑菇牛 50 耐久 + 右键去抖完全空转（字节码实证）**：
  - 1.21.11 剪羊毛/剪蘑菇牛发生在 `Sheep.mobInteract` / `MushroomCow.mobInteract`（各含 `stack.is(Items.SHEARS)` + `shear()` + `hurtAndBreak(1)`），**不走** `Item.interactLivingEntity`（`ShearsItem` 不覆写该方法）→ 旧 `ShearsInteractMixin` 挂在 `Item.interactLivingEntity` 上完全无效：冷却不触发、补 49 耐久不生效；且原版判定硬比对 `Items.SHEARS`，ICPM 六把自定义剪刀 ≠ 原版物品 → **剪不了羊/蘑菇牛**。
  - 修复：重写 `ShearsInteractMixin` 为 `@Mixin({Sheep, MushroomCow})`：`@Redirect ItemStack.is(Item)` 把 `Items.SHEARS` 比对放宽为「任意 ShearsItem 子类」（蘑菇牛的 BOWL/煲汤比对互不干扰）；`mobInteract` HEAD 去抖冷却（仅对剪刀类生效，喂食/挤奶不受影响）；RETURN 成功后补扣 49 耐久（原版已扣 1，合计 50，对齐 R196）。
- **`BowDurabilityMixin` 为未注册空壳**：文件存在但不在 mixins.json，方法体为空；1.21.11 原版在 `ProjectileWeaponItem.shoot` 统一扣弓耐久 → 文件已删除。
- **僵尸/村民僵尸稀有掉落率与注释/R196 差 5 倍**：`nextInt(base)>=5` 实际 5/base（1/40、1/10），注释写明 1/50、1/200 → 修正为 `nextInt(base)!=0`（1/base）。
- 构建 + 双端部署（备份 `.bak.20260902_220416`，testzip OK）。

**修复：多个"有名无实"附魔/回血机制（mixin 名不副实专项审计第一轮）· 2026-09-02**

㉖ 专项审计：106 个 mixin × 145 个注入点全量盘点 + 精读 + 全代码引用核对，确认并修复两类根因导致的 4 个空壳机制：

- **根因 A：`DisableVanillaHealingMixin` 一刀切拦截 `heal()`**，对无"生命恢复"药水效果的玩家无条件 cancel，误杀所有 ICPM 自研回血：
  - ❌ 再生附魔（ICPMRegenerationMixin 的 heal 被 cancel）→ 修复：经新增的 `ICPMHealProgressManager.healAuthorized` 授权回血；
  - ❌ 吸血附魔（ICPMCombatEnchantMixin 的 player.heal 被 cancel）→ 修复：同上；
  - ❌ 升级回血（ICPMExperience 升级时 heal 差值被 cancel）→ 修复：同上。
  - 自然回血（ICPMFoodStats 经 begin/endHealing 保护）原本正常，未受影响。
- **根因 B：`ICPMArmorValueMixin` 在 `getDamageAfterArmorAbsorb` HEAD-cancel 接管护甲**，使同一方法的 RETURN 注入（ICPMCombatEnchantMixin 的穿刺）永不触发——原 javadoc"穿刺在 RETURN 其后执行，无冲突"是错误假设（HEAD-cancel 会短路整个方法体，RETURN 注入点不可达）：
  - ❌ 穿刺附魔完全无效 → 修复：把穿刺逻辑迁入 ICPMArmorValueMixin 的护甲结算内（护甲部分 ×(1−min(1,级×0.2))，仅穿透护甲、不穿透附魔保护）；并修正旧公式方向错误（旧式 base+reduced×0.2 实际"保留 20% 减免、穿透 80%"，与"每级穿透 20%"注释相反）。
- 排查后排除（非空壳，证据充分）：5 个 `require=0` 注入目标全部真实存在（Player.canEat / Item.isCorrectToolForDrops / BlockBehaviour.getDestroyProgress / PathNavigation.isStableDestination / startShutdownWatchdog）；挖掘/采集/护甲/食物/盾牌/银器 mixin 逻辑闭环；`speed`、`fishing_fortune` 附魔为**数据驱动效果**（attributes / fishing_luck_bonus），无需 Java 即生效；附魔数据无解析错误。
- 遗留待办：ICPMEdibleUseMixin 注释与实现不符（注释称改用 "use"，实际仍硬编码 `method_7836 remap=false`，功能正常但脆弱，建议后续改用官方名）；`HealthRegenManager.kt` 与 `ICPMHealProgressManager.addTickProgress` 为孤儿死代码（无调用）。
- 已编译验证（compileJava BUILD SUCCESSFUL）。

**修复：保存世界卡死（保存界面永不消失）+ 玩家数据错乱（UUID 不承接旧存档）· 2026-09-01**

㉒ 玩家数据错乱根因：1.21.11 本地玩家 UUID 由【启动器】计算，不再经过 `UUIDUtil.createOfflinePlayerUUID`，导致旧的 `FixedPlayerUuidMixin`（覆盖该函数）对本地玩家**完全失效**：

- 现象：每次进旧档都会用启动器给的随机 UUID 新建 `playerdata/<新uuid>.dat`，老 MITE 存档的 `playerdata/00000000-0000-3004-998f-501a96e2ae48.dat` 被孤立 → 背包/等级/进度全部"重置"，即用户反馈的"玩家数据错乱"。日志佐证（latest.log）：
  `Local player id 26977e4c-a09d-4fe2-ae82-bdb5168dc209 was not found in the known players list [00000000-0000-3004-998f-501a96e2ae48, ...]! FTB Teams will not be able to function correctly!`
- 修复：新增客户端 mixin `FixedLocalPlayerUuidMixin`，注入 `Minecraft.getUser()` 的 RETURN，把离线模式（accessToken 为空/"0"）下返回的 `User` 替换为携带固定 MITE UUID 的新 `User`（保留名称/令牌/xuid/clientId）。`getUser()` 是本地玩家登录握手（`ClientHandshakePacketListenerImpl`）与 FTB Teams 读取本地 UUID 的唯一来源，改这一处即可让登录包 UUID、服务端 ServerPlayer 的 GameProfile/UUID、playerdata 文件名、客户端 LocalPlayer 全部回到固定 UUID，旧档进度无缝承接。**在线模式不生效**，不影响正版/第三方服务器。

㉓ 保存世界卡死（保存界面永不消失）——已完成根因定位 + 埋点，待一次复现实锤卡点：

- 反编译 1.21.11 关闭链路确认：
  1. 渲染线程在 `Minecraft.disconnect(Screen,ZZ)` 内 `while (!integratedServer.isShutdown()) runTick(false)` 循环绘制"保存世界中"界面；`isShutdown() == !serverThread.isAlive()`，即**等服务端线程死**。
  2. 服务端玩家断线走 `ServerGamePacketListenerImpl.onDisconnect` → 先打 `xxx lost connection` → `removePlayerFromWorld()` → `PlayerList.remove(player)`（保存玩家/移除实体/广播）。
  3. `removePlayerFromWorld` 返回后 `ServerCommonPacketListenerImpl.onDisconnect` 才会执行 `Stopping singleplayer server as player logged out` → `server.halt(false)` 让服务端线程退出。
  4. **本次卡死日志中【没有】** `Stopping singleplayer server` 且服务端线程在 `mcnb退出游戏` 后静默 ⇒ **`PlayerList.remove` 一直没返回** ⇒ 服务端线程活着 ⇒ `isShutdown()` 永假 ⇒ 保存界面永转（与用户"卡死在保存世界页面"完全吻合）。
- 已排查排除（非根因）：`PlayerMixin` 存档注入（`writeExperience`/`writeIcpmPlayerData`）、`PortalPositionStorage`/`PlayerNutritionManager`/`ICPMFoodStats`/`PlayerStatsManager`、全部自定义 DataComponent（`QualityComponent`/`NutritionComponent`/`CraftPreviewComponent`/`COIN_XP`/`RUNESTONE_VARIANT`/`SHIELD_ATTACHED`，均为 int/bool/简单 record codec，序列化安全）、`ICPM.java` 生命周期回调、`FixedPlayerUuidMixin`、弓/箭/鱼竿改动（构造器参数非组件）。
- **第二轮复现（2026-09-01 23:20，含 PlayerListShutdownTracer）进一步锁定**：
  - `PlayerList.remove` 本轮**正常返回**（`[SHUTDOWN] PlayerList.remove ENTER → EXIT` 同一秒），但之后**仍无** `Stopping singleplayer server`、也无 `MinecraftServer.halt ENTER` ⇒ 卡点收窄到 **remove EXIT 之后、halt 之前**的断线链尾部（`textFilter.leave()` / `super.onDisconnect` 的 isSingleplayerOwner 名称比对 + LOGGER + halt），该段全部是平凡原版代码，且 ICPM 无任何 mixin 挂在这几个类上 ⇒ **高度怀疑第三方 mod（TPA/FTB 等）的断线钩子或该段抛出的被吞异常**。
  - `isSingleplayerOwner(NameAndId)`（反编译确认）只比对**玩家名**（equalsIgnoreCase），UUID 不对称（客户端 26977e4c vs 服务端 00000000）不影响该判定。
  - `MinecraftServer.halt(false)` 仅 `running=false`（join 仅在 waitForShutdown=true 时）——若 halt 被调用服务端线程会很快退出；日志无 halt ⇒ 确实没走到。
  - 服务端玩家 UUID 已确认固定为 `00000000-0000-3004-998f-501a96e2ae48`（`PlayerList.remove ENTER player=00000000-...`）——`FixedPlayerUuidMixin` 经服务端离线登录的 `createOfflinePlayerUUID` 重新派生生效，旧档 playerdata 可正常承接。
- 新增埋点（下轮复现即可实锤卡点，dump 全线程栈含 "Server thread" 精确卡点）：
  - `PlayerListShutdownTracer`（服务端）：`PlayerList.remove` HEAD/TAIL 打点 + **4 秒看门狗**。
  - `DisconnectChainTracer`（服务端，新增）：`ServerGamePacketListenerImpl.onDisconnect` HEAD/TAIL 打点 + **5 秒看门狗**，覆盖 remove 之后的整段断线链。
  - `ServerShutdownTracer` 增补 `MinecraftServer.halt` HEAD/TAIL 打点。
  - `ClientShutdownTracer` 增补 `disconnect(Screen,ZZ)` **8 秒渲染线程看门狗**（保存界面不消失即 dump 全线程栈，同时可见服务端线程状态）。
  - 复现方法：进档 → 「保存并退出」→ 停在保存界面后**等 15 秒**再强制关窗，把 `E:/.minecraft/versions/1.21.11MITE测试/logs/latest.log`（或 RawOutput.txt）发我即可（8s 内自动出全栈 dump）。

㉔ 顺带修复潜在卡死：`ICPMExperience.getExperienceLevel` 的无限循环

- `while (getExperienceRequired(level + 1) <= experience) level++`：`getExperienceRequired(level > 200)` 恒返回 `Int.MAX_VALUE`，若 `totalExperience` 达到 `Int.MAX_VALUE`（如 `/xp set 2147483647`），循环将永不退出 → 服务端死循环卡死。已加 `level < MAX_LEVEL` 上限钳制。

㉕ 保存世界卡死——**根因实锤 + 根因级修复**（2026-09-01 第三轮，新档同样复现）

- 第三轮复现（全新存档）拿到全线程栈，机制 100% 闭合：
  - `[SHUTDOWN]` 断线链完整走完：`onDisconnect ENTER→EXIT`、`PlayerList.remove ENTER→EXIT`（玩家名 mcnb、UUID 已固定为 00000000-...）；
  - 但**没有** `Stopping singleplayer server as player logged out`、**没有** `MinecraftServer.halt ENTER`；
  - **线程栈铁证**：`Server thread state=TIMED_WAITING`，parked 在 `waitUntilNextTick` —— 服务端线程健康活着、`running` 仍为 true ⇒ **`halt` 从未被调用 ⇒ `IntegratedServer.isSingleplayerOwner` 返回了 false**；
  - 于是渲染线程 `while(!isShutdown())` 永真 → 保存界面永转。新档/旧档都复现 ⇒ 与存档数据无关。
- 根因（反编译 `Minecraft.getGameProfile()` + `IntegratedServer.<init>`）：
  - `IntegratedServer.<init>` 用 `setSingleplayerProfile(this.getGameProfile())` 设置房主 profile；
  - `getGameProfile()` **优先返回启动器异步 profile 查询（profileFuture）的 ProfileResult.profile()**，其 name 未必等于游戏内玩家名；离线回退分支才用 `this.user`（字段，非 getUser()）。
  - 本启动器 UUID 为随机 v4（非名字派生），账号名与游戏内名 "mcnb" 不一致 ⇒ `singleplayerProfile.name() ≠ "mcnb"` ⇒ 原版按名字比对的 `isSingleplayerOwner` 恒 false ⇒ 房主断线时集成服务器不会自行停机。
- 修复：新增客户端 `IntegratedServerOwnerFixMixin`，`@Inject isSingleplayerOwner HEAD`：singleplayerProfile 名字与断线玩家一致时放行原版；否则回退为「与客户端本地用户名 `Minecraft.getUser().getName()` 比对」——本地玩家就是房主（原版本意），LAN 他人名字不同仍返回 false。附带 `[SHUTDOWN] isSingleplayerOwner FALLBACK` 打点输出两边实际值。
- 双端构建部署，备份 `.bak.20260901_23xxxx`，testzip OK。

**修复：ICPM 弓射不出 ICPM 箭 + ICPM 鱼竿无法使用 · 2026-08-30**

① 弓射不出 ICPM 箭（箭矢实体根本构造不出来）

- **现象**：手持任意弓（含 `icpm:bow` / `ancient_metal_bow` / `mithril_bow`），背包里带着 ICPM 箭（`#minecraft:arrows` 标签里已有 9 种 ICPM 箭），拉满弦松手后**什么都没射出来**，也听不到箭矢破空声。
- **根因**：`ICPMArrowEntity.getDefaultPickupItem()` 里调用了 `getPickupItem()`。而 1.21.11 原版 `AbstractArrow(EntityType, Level)` 构造器的最后一步就是
  `this.pickupItemStack = this.getDefaultPickupItem();`
  此刻 `pickupItemStack` **仍为 null**（没有任何行内初始化），而 `getPickupItem()` 的实现是 `this.pickupItemStack.copy()` → **NullPointerException**。
  结果：`ArrowItem.createArrow` 一抛异常，`ProjectileWeaponItem.shoot` 整条链路中断，连 `addFreshEntity` 都到不了 —— 表现就是「弓拉满弦却射不出箭」，且日志不一定显眼。
  作为对照，原版 `Arrow.getDefaultPickupItem()` 是 `new ItemStack(Items.ARROW)`，从不回溯自身状态。
- **修复**（`src/main/kotlin/name/icpm/entity/projectile/ICPMArrowEntity.kt`）：
  - `getDefaultPickupItem()` 改为直接 `return ItemStack(ICPMItems.FLINT_ARROW)`，**禁止**在构造期读取自身任何字段（已写进文件头注释作铁律）。
  - `onHitBlock()` 的回收判定用 `pickupStackSafe()`（`runCatching` 兜底），避免任何异常把整支箭搞崩。
- 顺带加固（`ICPMArrowItem.kt`）：`createArrow` 的武器栈为空时兜底为普通弓，避免原版在服务端抛 `IllegalArgumentException: Invalid weapon firing an arrow`（发射器/命令等非弓路径）。

② ICPM 鱼竿无法使用（右键永远只能收线）

- **现象**：右键 ICPM 鱼竿没有任何反应，抛不出浮漂。
- **根因**：原版 `FishingRodItem.use()` 的行为被 `Player.fishing` 是否为 null **二分**——非 null 只走「收线」分支（`retrieve` + 扣耐久），**永远不会再抛竿**。一旦玩家身上残留一个失效钩子引用（钩子被丢弃 / 玩家换维度 / 实体生成失败），右键就永久卡在收线分支，且**不会有任何报错**。
- **修复**：新增 `src/main/kotlin/name/icpm/item/ICPMFishingRodItem.kt`，9 种 ICPM 鱼竿改用它注册。它在调用原版逻辑前先清理失效引用（`isRemoved || !isAlive || hook.level() !== player.level()`），其余抛竿/收线/耐久/统计全部沿用原版实现。

③ 顺带修正：ICPM 弓没有拉弦动画

- `assets/icpm/items/{bow,ancient_metal_bow,mithril_bow}.json` 仍是 1.21.4 之前的 `models/item/*.json` + `overrides` 思路，1.21.11 早已不读 `overrides`，导致拉弓时模型纹丝不动，玩家更难判断「到底有没有在拉弦」。
- 改为 1.21.4+ 的物品模型定义：`condition(minecraft:using_item)` → `range_dispatch(minecraft:use_duration, scale 0.05, threshold 0.65/0.9)`，与 `assets/minecraft/items/bow.json` 结构一致。

**画风回滚：恢复 MITE Resource Pack 1.6.41 贴图 · 保留自绘长矛 · 2026-08-30**

- 根因：上一轮程序化生成的"原创"扁平贴图效果极差，玩家反馈"很多物品没有恢复原来的贴图"。
- 方案：
  - 从 git 历史提交 `ddde068`（贴图重构前）整体还原全部 534 个游戏内贴图 + 2 个 mod 图标，这些文件本来就是 MITE 画风。
  - 再用 `E:\MITE Resource Pack 1.6.41` 按「精确子路径优先 → 同类别唯一文件名回退」二次覆盖 312 个能找到唯一对应源的贴图；其余 212 个 ICPM 独有/无唯一 MITE 匹配的文件保留还原后的预重构版本。
  - **长矛贴图保护**：`item/adamantium_spear*.png`、`item/ancient_metal_spear*.png`、`item/mithril_spear*.png`、`item/silver_spear*.png` 共 8 个文件被判定为"前几轮自行绘制、无侵权风险"，强制保留预重构版本，不被 MITE RP 覆盖。
- 结果：534 个游戏贴图 + 2 个 mod 图标全部恢复为 MITE 画风（或预重构版本），`clean build` 验证资源可正常加载；双端部署（`.bak.20260830_095254`）。
- 版权提示：这些贴图源自 MITE Resource Pack 1.6.41，公开分发可能存在授权风险；用户已知情并接受。

**修复：贴图白色背景板（部分物品加载错误）· 2026-08-30**

- **现象**：从 MITE RP 1.6.41 恢复贴图后，部分物品贴图带上了不透明白色背景板（本应有透明通道的物品贴图变成了实白底），导致贴图加载错误。
- **根因**：恢复流程中 6 张工作区贴图被误写成实白底（对比 HEAD 应为透明版）；`blueberry` 被 MITE RP 覆盖成实白底（对比提交 `ddde068` 应为透明版）。属批量恢复/覆盖脚本未做透明通道校验遗留的问题。
- **修复**：用 Pillow 全量审计 532 张 png 的「透明像素占比 / 实白占比」，跨「工作区 / HEAD(`d06edad`) / `ddde068` / MITE RP」四版对比定位异常；对 6 张（`copper_chain` / `gold_battle_axe` / `gold_lava_bucket` / `mithril_leggings` / `salad` / `silver_war_hammer`）执行 `git checkout HEAD --` 恢复为透明版，对 `blueberry` 执行 `git checkout ddde068 --` 恢复为透明版；其余 5 张门贴图（adamantium/ancient_metal/gold/mithril/silver_door）本就不透明、属正常，确认无须修。
- 构建 BUILD SUCCESSFUL（7m6s）；双端部署（先 `msvcrt` 探测 JVM 锁：服务端/客户端均 UNLOCKED，备份 `.bak.20260830_142000` 后覆盖，`zipfile.testzip` 校验 + 校验 jar 内 `blueberry`/`copper_chain`/`salad`/`silver_war_hammer` 透明通道均 OK）；弓拉弦动画 json（bow/ancient_metal_bow/mithril_bow）随同一 jar 入包。

~~**画风整体重构（程序化原创 · 移除 MITE 素材 · 2026-08-29）**~~（已回滚，见上）

①按 R196 源码修正金属砧耐久机制（把"GUI 画假条 + 软储存"改为"有耐久值的方块 + 完整耐久机制"）：

- **砧物品拥有真实耐久**：新增 `ICPMMetalAnvilItem`（`BlockItem` 子类），用 `Item.Properties.durability(maxDurability)` 让砧物品本身承载最大耐久（1.21.11 的正确 API 是 `durability(int)`，非旧版 `maxDamage(int)`）。物品栏悬停显示「砧耐久: 剩余/最大」，按剩余比例着色（充足红 / 中等橙 / 将损绿）。
- **掉落与放置双向同步**：`BlockMetalAnvil.spawnAfterBreak` 掉落物同时写入 `ItemStack.damage`（真实计入物品数据）+ `BLOCK_ENTITY_DATA` 组件（放置时由 Fabric 自动注入方块实体还原 `Damage`）；完全损坏（`damage ≥ maxDurability`）不掉落，与 R196「stage==3 不掉落」一致。
- **耐久权威值持久化**：砧的磨损值仍存于方块实体 `damage` 字段并写入 NBT（key "Damage"，`TileEntityMetalAnvil.saveAdditional/loadAdditional`），跨重启、掉落拾取、变体切换全程保留——彻底告别 1.0.4 时代的 GUI 假条与软储存。
- **注册**：`ICPM.java registerAllBlocks` 中 21 个砧变体（7 金属 ×完好/chipped/damaged）统一注册为 `ICPMMetalAnvilItem`。
- 注：阶段阈值沿用 1.0.4 已对齐的 R196 规则（损伤比例 `<0.5` 完好、`[0.5,0.8)` chipped、`[0.8,1.0)` damaged、`≥1.0` 销毁），最大耐久 `1600×31×材质系数`。

②修正 1.21.11 兼容性编译细节：

- `appendHoverText` 改用新签名 `(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)`（第 3 参是 `TooltipDisplay`、第 4 参是 `Consumer<Component>`，非旧版 `List<Component>`），避免覆盖超类型方法报错。

③修复金属砧「Shift+左键（快速移动）取走结果槽」刷物品：

- **现象**：在金属砧菜单用 Shift+左键取下结果槽物品时，一次性把整堆输入材料/成品复制带走（“一次性复制一堆”）。
- **根因**：反编译 `AbstractContainerMenu.doClick` 确认 —— QUICK_MOVE（快速移动）分支只调用 `quickMoveStack`、不触发 `Slot.onTake`，且该分支在返回「非空且不等于光标物品」时会循环重试 `quickMoveStack`。原结果槽分支成功后返回 `originalStack`（非空），导致 doClick 反复调用，一次 shift 点击连续取走所有可用输入/材料。
- **修复**（`src/main/kotlin/name/icpm/inventory/MetalAnvilMenu.kt`）：
  - 三处结果生成路径（`updateRepairResult` / `handlePureRename` / `applyBookToEquipment`）均改为 `setCount(1)`，保留「单件」结果数量（保留整堆数量会导致取出整堆）。
  - `quickMoveStack` 结果槽（slot 2）分支成功转移后返回 `ItemStack.EMPTY`，终止 doClick 的循环重试；保留手动调用 `onResultTaken(player)`（QUICK_MOVE 不触发 onTake，需显式消耗）。
  - `onResultTaken` 修正：仅当实际用于修复（`stackSizeToBeUsedInRepair > 0`）才消耗金属粒，纯命名不再误扣材料。

④修复打开创造标签页即崩溃（Stack size must be exactly 1）：

- **现象**：打开创造标签页瞬间崩溃，`IllegalArgumentException: Stack size must be exactly 1`，堆栈来自 `ICPMBlockGroup.lambda$register$2`。
- **根因**：`BLOCK_NAMES` 含 `chipped_*` / `damaged_*` 旧砧变体（仅注册 Block、未注册 BlockItem，`block.asItem() == Items.AIR`）；`new ItemStack(AIR)` 得到 count=0 空栈，触发 `CreativeModeTab$Output.accept` 的「Stack size must be exactly 1」强制校验崩溃。
- **修复**（`src/main/java/name/icpm/block/ICPMBlockGroup.java`）：遍历 `BLOCK_NAMES` 注册创造标签页物品时增加守卫，跳过 `asItem() == Items.AIR` 的方块（旧砧变体不进创造栏 / JEI）。

⑤新增睡眠机制（白天可睡、睡到次日 5 点不强制弹起、睡时回血加快）：

- **白天入睡不跳时间**：`ICPMDaySleepMixin` 仅当 `13000 ≤ getDayTime() % 24000 < 23000`（夜晚）才允许跳时间，白天睡觉不再推进时间。
- **睡到次日 5:00 不强制弹起**：取消 `ServerLevel.tick` 中的 `wakeUpAllPlayers()`，玩家可一直躺到自然醒；`setDayTime(time - 1000)` 使醒来时间为次日 5:00（配合白天入睡门槛，避免在夜晚反复跳时间）。
- **睡眠回血加速**：睡觉时回血系数由 4 倍提升为 8 倍（`src/main/kotlin/name/icpm/common/ICPMFoodStats.kt` 中 `if (player.isSleeping) 8f else 1f`）。

⑥新增：创建新存档时自动开启 `lava_source_conversion`：

- **诉求**：1.21 默认 `lava_source_conversion` 为 false（岩浆不再像旧版一样自然形成 source 方块）；希望在新建世界时默认开启（`/gamerule lava_source_conversion true`）。
- **实现**（`src/main/java/name/icpm/ICPM.java`）：在 `ServerWorldEvents.LOAD` 中，对每个加载的 `ServerLevel` 检查该 gamerule，仅当当前为 false 时通过命令 `gamerule lava_source_conversion true` 开启（绕开 1.21 重构后不可直接引用的 gamerule value 类型名）。新存档创建即生效；已开启的世界不重复设置，也不覆盖玩家之后的手动更改。

⑦修复战利品表 `infernal_creeper.json` 解析报错（Couldn't parse data file ... Unknown registry key ... entity_sub_predicate_type: minecraft:creeper）：

- **现象**：进存档时日志报错 `Couldn't parse data file 'icpm:entities/infernal_creeper' from 'icpm:loot_table/entities/infernal_creeper.json'`，该战利品表不生效（地狱苦力怕不掉碎片）。
- **根因**：文件用旧版 `entity_properties` 条件写法 `"predicate": { "type_specific": { "type": "minecraft:creeper", "charged": false } }`。1.21.11 的 `entity_sub_predicate_type` 注册表只有 `lightning / fishing_hook / player / slime / raider / sheep`，**没有 `creeper`**，按注册键查找 `minecraft:creeper` 失败。注：`blob / pudding / jelly / ooze` 四个 slime 战利品表用的是已注册的 `minecraft:slime`、且写法与原版 `slime.json` 完全一致，无需改动（日志也未报它们错）。
- **修复**（`src/main/resources/data/icpm/loot_table/entities/infernal_creeper.json`）：移除该 `entity_properties` 条件（`infernal_creeper` 为自定义实体、本无"带电"概念，条件恒为真，移除后掉落行为不变），恢复掉落 `infernal_creeper_frag`。

⑧燧石工作台支持 11 种原木合成（多原木衍生外观，红树木用玩家指定贴图）：

- **诉求**：燧石工作台可用各种原木合成，产出对应材质的版本；红树木版本的侧边使用玩家提供的 `mangrove_log.png`。
- **设计（关键）**：按"衍生类型"而非独立方块实现——**只有 1 个方块 `icpm:flint_workbench`、一套合成等级 tier=0**，用 `wood` 状态属性（0..10，整数）区分外观，与砧的 `STAGE` 同类的多态方块。好处：不新增方块 ID、旧存档不受影响、合成等级逻辑完全复用。
- **变体枚举**：`BlockICPMFlintWorkbench.WoodType` = 橡木/云杉木/白桦木/丛林木/金合欢木/深色橡木/红树木/樱木/竹/绯红木/诡异木（序数即状态值，顺序不可随意调整）。
- **变体承载**：用**原版 `minecraft:block_state` 组件**（键 `wood`），全链路闭环、零自定义数据组件：
  - 合成：`data/icpm/recipe/flint_workbench[_<wood>].json` 共 11 个，沿用原 R196 形态 `"FS"/"s#"`（燧石 + `#icpm:cords` + 木棍 + 对应原木），结果 `result.components` 写入 `{"minecraft:block_state": {"wood": "6"}}`（**扁平 map，不是 `{"properties":{...}}`**）。
  - 放置：`FlintWorkbenchItem#getPlacementState` 取手中物品的该组件 `apply` 到方块状态。
  - 破坏：战利品表加 `minecraft:copy_state`（`properties: ["wood"]`）→ 掉落物自动带回原木类型，不会"挖了变回橡木"。
  - 显示：新增 `assets/icpm/items/flint_workbench.json`，用 `minecraft:select` + `property: minecraft:block_state` + `block_state_property: wood` 选模型；物品名由 `Item.getName` 动态拼（如"红树木燧石工作台"），无需 11 条语言条目。
- **贴图**：侧边/粒子用对应原木（下界用 `crimson_stem`/`warped_stem`，竹用 `bamboo_block`），底面用对应木板，台面统一 `icpm:block/crafting_table/flint/top`；其余 10 种直接引用**原版 `minecraft:block/*` 路径**，因此 MITE 材质包会自动生效；**红树木**使用玩家提供的 `assets/icpm/textures/block/workbench/mangrove_log.png`（1.6.4 无红树木，故单独提供）。
- 新增/修改：`BlockICPMFlintWorkbench.kt`（新）、`FlintWorkbenchItem.java`（新）、`ICPMBlocks.kt`、`ICPM.java`（物品注册分支）、`ICPMItemGroup.java`（创造栏 11 个变体）、11 个方块模型、blockstates、11 个配方、战利品表、物品模型定义。
- clean build BUILD SUCCESSFUL（2m50s），jar `testzip: None` + 关键 entry 全量可读；无 java 进程下双端部署（旧 jar 已 `.bak.20260828_174723` 备份），mods 内仅余主 jar。

⑨重构地下世界维度（深板岩层 + 维度下移 + 远古城市 + 负层深板岩矿石）：

- **维度下移（y 下限 -60）**：`dimension_type/underworld.json` 与 `noise_settings/underworld.json` 的 `min_y` 由 0 改为 **-64**、`height`/`logical_height` 由 128 改为 **192**（注：Minecraft 强制 `min_y` 与 `height` 必须均为 16 的倍数，-60/188 非法，取最接近且合法的 -64/192；维度范围变为 **y -64 ~ 127**，共 192 层，顶部 127 不变；基岩地板仍落在 -60~-55，玩家视角下的"地板"即 -60）。
- **深板岩层（符合高版本特性）**：在 `noise_settings` 的 `surface_rule` 序列中，于最终石头块之前插入一条 `vertical_gradient`（`random_name: icpm:deepslate`，`true_at_and_below:{absolute:0}` / `false_at_and_above:{absolute:8}`）条件块 → `minecraft:deepslate{axis:"y"}`；效果为 **y≤0 全为深板岩、0~8 过渡、>8 为石头**，与原版主世界深板岩带一致，原版矿石在此以深板岩变种生成。
- **地幔 + 基岩整体下移至 y -60 ~ -55**：`ICPMUnderworldBedrock.java` 逐列代码生成的地板基准由 0 改为 `FLOOR_BASE=-60`、封顶 `FLOOR_TOP=-55`；地幔占 `y=-60 .. -60+numBedrock-1`（每列 1~3 层），基岩仅在地幔之上、至多 3 层（封顶 -55），`bedrock_noise≤0` 处留豁口（盆地/地幔裸露）。顶部基岩天花板仍在 `below_top 0~4`（即 y123~127，世界顶）。
- **随机刷怪笼（古尸 / 古尸守卫）**：怪物房 `placed_feature/underworld_monster_room.json` 的 `height_range` 下限由 52 放宽到 **-30**（上限仍 80），使刷怪笼可出现在负层；笼内实体已在 `ICPMMonsterRoomFeatureMixin` 实现（`random.nextInt(6)==0` → 古尸守卫 `LONGDEAD_GUARDIAN`，否则古尸 `LONGDEAD`），无需改动。
- **极小概率生成"远古城市"**：复用原版 jigsaw 模板与拼图池，新增 `worldgen/structure/ancient_city.json`（`biomes` 指向自建标签 `#icpm:is_underworld`，`start_height:{absolute:-27}`，`terrain_adaptation:beard_box`，`step:underground_decoration`）、`worldgen/structure_set/ancient_cities.json`（`random_spread` 间距 `spacing:52`、`separation:8`、独立 `salt:19191823` —— 间距远大于原版 24，故极稀有）、`tags/worldgen/biome/is_underworld.json`（值 `["icpm:underworld"]`）。深暗之域建筑（幽匿系列、蜡烛、强化深板岩等）将随结构在地下世界生成。
- **负层可发现深板岩矿石**：原版煤等矿石随深板岩层自动以深板岩变种出现（`biomes` 的 `UNDERGROUND_ORES` 已显式加入 `minecraft:ore_coal_upper` / `ore_coal_lower` —— 注意 `ore_coal` 是 configured_feature，不能写进须填 placed_feature 的 features 列表，否则报 `Unbound values`，必须用 placed_feature `ore_coal_upper`）；ICPM 深板岩矿 `ore_silver_underworld` / `ore_mithril_underworld` / `ore_adamantium_underworld` 三个 `placed_feature` 的 `height_range` 下限由 0 下探到 **-60**，可在负层开采到 icpm 深板岩矿（银/秘银/精金）。
- clean build BUILD SUCCESSFUL（58s），jar `testzip: None` + 关键 entry（`structure/ancient_city.json`、`structure_set/ancient_cities.json`、`tags/.../is_underworld.json` 等）全量可读；无 java 进程下双端部署（旧 jar 已 `.bak.20260829_111728` 备份），mods 内仅余主 jar。

⑩修复 ⑨ 部署后"无法进入存档"的数据包崩溃（非玩家数据损坏）：

- **现象**：客户端点击进入存档即报错退出，日志显示 `Failed to load level data or datapacks, can't proceed with server load`，玩家误判为"玩家数据损坏"。
- **根因（两个独立的数据包校验错误）**：
  1. `dimension_type/underworld.json` 与 `noise_settings/underworld.json` 的 `height=188`、以及 `min_y=-60` 均不满足 Minecraft 硬约束 **`min_y` 与 `height` 必须均为 16 的倍数**（已核实 jar 内 `esh.class`/`euy.class` 同时校验 `min_y has to be a multiple of 16` 与 `height has to be a multiple of 16`）。原值 -60/188 非法，导致维度与噪声设置解析失败。
  2. `biome/underworld.json` 的 features 列表误把 `minecraft:ore_coal`（**configured_feature**）写进了须填 **placed_feature** 的槽位，报 `Unbound values in registry placed_feature: [minecraft:ore_coal]`。
- **修复**：`min_y` 改为 **-64**、`height`/`logical_height` 改为 **192**（最接近 -60/188 且合法的 16 倍数组合，顶部仍 127，基岩地板仍 -60~-55）；`ore_coal` → **`ore_coal_upper`**（placed_feature，`ore_coal_lower` 本就正确）。
- clean build BUILD SUCCESSFUL（6m57s），jar `testzip: None` 并内嵌校验 `min_y=-64/height=192`、`ore_coal` 已移除、`ore_coal_upper` 已就位；无 java 进程下双端部署（旧 jar 已 `.bak.20260829_120507` 备份），mods 内仅余主 jar。重进游戏/启动服务端即可正常进入地下世界。

⑪修复 ⑨/⑩ 后实测暴露的地下世界三大问题（实心无洞 / 刷怪笼爆量 / 远古城市不生成 / 基岩未随 min_y 下移）：

- **现象**：地下世界 `y≤0` 的深板岩区完全没有类似主世界的矿洞空腔，最深处只剩满地刷怪笼；远古城市（深暗之域）始终无法验证；基岩地板固定在 -60~-55，世界真正底部 -64~-61 露出普通石头。
- **根因（逐一定位）**：
  1. **群系 carvers 用错**：`biome/underworld.json` 的 `carvers` 原填 `minecraft:nether_cave`，其 Y 范围为 `absolute:0 ~ below_top:1`（即 y 0~126），而本维度实心区为 **y -64~0、地表在 0**，于是 nether_cave 全在空气里 carving，实心地下一个洞都没有——世界是实心的，唯一空腔就是刷怪笼挖的小房间 → 刷怪笼显得"离谱地多"。
  2. **维度未登记 structures**：`dimension/underworld.json` 的 `generator` 缺 `structures` 字段，jigsaw 结构（远古城市）在维度内根本不调度生成。
  3. **基岩未随 min_y 下移**：`ICPMUnderworldBedrock` 的 `FLOOR_BASE=-60` 在 ⑩ 把 `min_y` 改 -64 时没同步下移，导致世界底 -64~-61 露出普通石头。
  4. **刷怪笼过密**：`underworld_monster_room` 原 `count=2` 且 `height_range` -30~80 过宽，整个世界实心时每次尝试都成功 → 爆量。
- **修复**：
  1. `carvers` 改为 `["minecraft:cave", "minecraft:cave_extra_underground"]`——二者 Y 用 `above_bottom` 相对维度底，自动适配 `min_y=-64` 落到 **y -56~0** 的深板岩区，地下世界从此有自然矿洞（与 -10 附近深板岩带衔接）。
  2. `dimension/underworld.json` 新增 `generator.structures: {"icpm:ancient_city": "icpm:ancient_cities"}`，远古城市结构正式在地下世界调度生成（复用原版 jigsaw 模板，`start_height:{absolute:-27}`、`beard_box` 自适应挖空）。
  3. `ICPMUnderworldBedrock` 的 `FLOOR_BASE` **-60→-64**（世界真正底部）、`FLOOR_TOP` **-55→-59**，地幔+基岩地板整体占据 **y -64~-59**，随 `min_y` 一并下移到世界底。
  4. `underworld_monster_room` 降为 `count=1`、`height_range` **-55~-10**（限制在深板岩实心区、避开基岩地板与地表空气），刷怪笼密度大幅下降。
- clean build BUILD SUCCESSFUL（2m56s），jar 内嵌校验 `structures`/`carvers`/`monster_room` 三项均就位；无 java 进程下双端部署（旧 jar 已 `.bak.20260829_122340` 备份），mods 内仅余主 jar。**验证提示**：远古城市为"极小概率"（结构集随机散布 spacing 52，比原版 24 更稀有），可用指令 `/locate structure icpm:ancient_city` 直接定位验证其确实生成；有矿洞后可正常下探探索。

⑫修复 ⑪ 后实测暴露的"水灾 / 传送门判定"问题：

- **现象**：深板岩区域"层数以下便全是水"，只有含水的小型"水道"而非普通矿洞；地下世界尝试造地狱传送门时，「仅达成 isOnMantle 或 y<-55 其一」直接 return null（什么都不建），导致 -10 层放地幔也能造出地狱门或造不出；基岩豁口因旧世界 min_y=-60 旧基岩与现行 -64 新基岩堆叠而看起来消失。
- **根因（逐一定位）**：
  1. **水灾真凶是泉水特征**：`biome/underworld.json` 的 features 填了 `minecraft:spring_water`/`minecraft:spring_lava`，二者作为 placed_feature 在**每个区块各尝试 25/20 次、覆盖全高度（above_bottom:0 → absolute:192）**，在洞穴密集的地下世界把负层灌满。`aquifers_enabled` 字段名正确且已是 false，并非 aquifier 所致。
  2. **传送门判定逻辑错位**：`FlintAndSteelMixin` 地下世界分支把"能建传送门"与 `y>-55` 绑死，`y≤-55 且未触地幔` 直接 return null；不符合"地下世界传送门（返回主世界）任意 Y 可建、仅同时满足地幔+ y<-55 才建地狱门"的需求。
  3. **基岩豁口**：`ICPMUnderworldBedrock` 的"bedrock_noise≤0 留豁口"算法本身正确；旧世界（min_y=-60 时）已生成的基岩在 -60~-55，与现行 -64~-59 新基岩堆叠 → 旧世界看起来无豁口（新世界正常）。
- **修复**：
  1. `biome/underworld.json` 移除 `spring_lava`/`spring_water`，新增自定义 placed_feature `icpm:spring_water_sparse`（`count=2`、限定深板岩区 `above_bottom:8`~`absolute:0`，即 y -56~0），实现"随机水源而非整个被水填满"。
  2. `FlintAndSteelMixin` 地下世界分支改为：仅当 `icpm$isOnMantle && y<-55` 建地狱传送门（红）；其余情形（含仅达成其一）一律建地下世界传送门（紫，返回主世界，任意 Y 可建）。地狱维度判定逻辑不变。
  3. 基岩维持 `FLOOR_BASE=-64`/`FLOOR_TOP=-59`；如需验证豁口，建议新建地下世界存档（旧世界已生成的基岩会堆叠）。
- clean build BUILD SUCCESSFUL（2m56s）；**部署待执行**：检测到 java 进程（PID 17968，~1.9GB，疑似游戏/服务端）仍在运行，覆盖 mods 会被锁定/损坏，待用户关闭后再双端部署（沿用 `.bak` 备份机制）。

⑬重构"可燃物烧肉"机制（R196 忠实移植）：

- **背景**：0.1.0 遗留的 `BurningCookingHandler` + `ItemEntityMixin` 是"每秒检测燃烧方块、瞬间烤熟"，缺少进度、经验、灭火，与 R196 不符。
- **R196 逻辑**：`EntityItem` 受火焰伤害时累计 `cooking_progress += damage.getAmount() * 3`，满 100 时由 `getItemProducedWhenDestroyed` 转成熟食，并给四周火方块排程熄灭、掉落经验球。
- **移植实现**：
  - `ItemEntityMixin` 新增 `icpmCookingProgress` 字段；在 `tick` 的 TAIL 检测实体是否位于燃烧方块（pos / pos.below() 命中 `icpm:burning_blocks`），是则每 tick `+3`（等效火焰伤害量 1.0 ×3），累计满 100 调用 `BurningCookingHandler.completeCook`。
  - `BurningCookingHandler`：`RAW_TO_COOKED` 覆盖 R196 的 `setCookingResult` 集合（猪/牛/鸡/兔/羊/鳕/鲑/土豆/海带）；烤熟播放滋滋声（`SoundEvents.FIRE_EXTINGUISH`）、熄灭四周 3×3 火方块、按 `RAW_TO_XP`（猪3/牛4/鸡3/羊2/鳕3/鲑4/土豆0/海带0/兔0）掉落 `ExperienceOrb`。
  - 现代 MC 物品实体不被 `FireBlock` 直接伤害，故以"位于燃烧方块"等效 R196 的"受到火焰伤害"语义。
- clean build BUILD SUCCESSFUL（1m1s）；双端部署：客户端 `E:/.minecraft/versions/1.21.11MITE测试/mods/` + 服务端 `E:/1.21.11fa-MITE/1.21.11/mods/`，各自先 `.bak.20260829_160328` 备份后覆盖；Python `zipfile.testzip`（None=OK）校验通过，`ItemEntityMixin.class`/`BurningCookingHandler.class` 均入包。

⑭修复"火焰/岩浆烧肉"物品立刻消失、无法累计（本次）：

- **根因**：现代 MC 的物品实体一旦接触火/熔岩（1.21 熔岩是流体 `LavaFluid`，不是 `LavaBlock`）会被直接销毁，根本轮不到逐 tick 累计；R196 的 `EntityItem` 有独立 health 系统，受火焰伤害只会累计 `cooking_progress` 而不会消失。
- **修复**：`ItemEntityMixin` 新增 `fireImmune()` 注入——当物品是"可烹饪生食"且处于燃烧方块/熔岩/火（`icpm:burning_blocks`，熔岩本身在 tag 内）时 `setReturnValue(true)` 并 `cancel`，等价于 R196"受伤但不死、累计进度"语义，使生食能停在火/岩浆上被逐 tick 烤熟（约 1.7s：每 tick +3，满 100）。
- **递归崩溃修复（同轮）**：`fireImmune` 注入内最初写了 `self.isOnFire()`，而 `isOnFire()` 会回环到 `fireImmune` 包装器（`class_1542.method_5753` ↔ `class_1297.method_5809` ↔ `icpm$fireImmune`），导致 StackOverflow 崩服。现已移除 `isOnFire()/isInLava()` 调用，仅用 `isOnHeat(level,pos)`（熔岩作为流体方块在 tag 内，已覆盖浸入场景），并补 `cir.cancel()` 跳过原方法体。
- clean build BUILD SUCCESSFUL（59s）；双端重新部署（备份 `.bak.20260829_164719` 后覆盖），`zipfile.testzip` 校验 None，`ItemEntityMixin.class` 已入包。

⑮修复"夹生/熟食消失"并还原 R196"点 4 次才熟"节奏（本次）：

- **熟食消失根因**：上一版的 `fireImmune` 仅在"生食"时返回 true——烤熟后 `getCooked` 返回 null，`fireImmune` 变回 false，若成品仍泡在熔岩/火里会被立即销毁（"夹生/熟食又被烧没了"）。
- **修复**：`fireImmune` 改为"生食 或 已标记 `icpmCooking` 的物品"在热源上即免疫；`icpmCooking` 在首次受热烹饪时置 true 并烤熟后仍保持，使成品不被销毁。
- **节奏还原**：R196 中要点 4 次火才熟。现改为"离散燃烧窗口"——每次连续受热只贡献 `COOK_UNIT=25` 进度（`COOK_THRESHOLD=100`，恰好 4 次），窗口结束后必须离开热源再放回才能继续累计（`icpmBurnConsumed` 标志），对应 R196 的反复点燃。`COOK_UNIT`/`COOK_THRESHOLD` 可调。
- 新增 `BurningCookingHandler.isRawFood`；移除旧 `COOK_STEP`。
- 部署前先用 PowerShell 以 `FileShare.None` 探测两个目标 jar 是否被 JVM 锁定（UNLOCKED 才覆盖）；本次两端均 UNLOCKED，备份 `.bak.20260829_171035` 后覆盖，`testzip` 校验 None。

⑯重构 R196 燃烧限制（非可燃不燃 / 短燃 3 tick / 第 5 次正常 / 第 8 次烧毁 / 多食物同烤）：

- **可控点火状态机**（`src/main/java/name/icpm/common/CombustionHandler.java`，新建）：按用户的 R196 规则实现逐方块点火计数（维度+坐标 → 次数，内存 `ConcurrentHashMap`，重启清零）：
  - 非可燃方块打火石点火 → `InteractionResult.FAIL`（无法点燃）；
  - 可燃且非植物：前 4 次（`count < NORMAL_FROM=5`）仅短燃 `SHORT_TICKS=3`（3 tick 后自动熄灭、不毁方块）；
  - 植物（见 `data/icpm/tags/block/plant.json`）不参与短燃，按正常火处理；
  - 第 5~7 次（`NORMAL_FROM=5` 且未达 `BURN_UP_AT=8`）→ 正常火时长、不烧毁；
  - 第 8 次及以后（`BURN_UP_AT=8`）→ 火燃烧 `DESTROY_TICKS=15`（≈正常火时长）后熄灭并烧毁该可燃方块。
- **点火入口**（`FlintAndSteelMixin.icpm$handleCombustion`）：非可燃直接 FAIL；可燃则 `registerIgnition` 累计次数并放置火；按 count 决定短燃/正常/烧毁登记；非植物触发 `markShort`/`markDestroy`，植物与原版火一致。
- **受控火熄灭**（`FireBlockMixin` 注入 `FireBlock.tick` HEAD）：命中受控火时取消原版 tick（阻止蔓延），按剩余 tick 计数到 0 才熄灭；SHORT 仅灭火，DESTROY 灭火并烧毁相邻可燃方块。
- **夹生修复**（`BurningCookingHandler`）：`completeCook` 仅在附近无剩余生食（`hasRawNearby`，3×3×3 扫描 ItemEntity）时才熄灭周围火，使多份食物都能烤熟；烹饪节奏沿用离散窗口（`COOK_UNIT= 25`/`COOK_THRESHOLD=100`，约 4 次点燃）。
- 新增标签：`data/icpm/tags/block/combustible.json`（可燃方块：各类木板/原木/栅栏/楼梯/压力板/按钮/活板门/门/书架/TNT/高草/蕨/草/枯灌木/灌木/羊毛/藤蔓/煤块/干草/蛛网/作物/甘蔗/制图台/工作台/讲台/堆肥桶/梯子）；`data/icpm/tags/block/plant.json`（树叶/草/高草/蕨/枯灌木/灌木）用于排除短燃。
- `icpm.mixins.json` 已注册 `FireBlockMixin`。点火计数在服务器重启后清零（内存实现，非 WorldSavedData），如需持久化后续可接入存档。
- clean build BUILD SUCCESSFUL（51s）；部署前以 `msvcrt` 独占锁探测两个目标 jar 均 UNLOCKED，先备份 `.bak.20260829_175738` 再覆盖，Python `zipfile.testzip`（None）校验通过，关键 entry 全部入包。

⑰修复"可燃方块烧肉"（生食站可燃方块上头顶有火却烤不熟） + 远古城市无空腔：

- **可燃烧肉补充修复**：`BurningCookingHandler.isOnHeat` 此前只检测「自身 / 下方」是否为 `icpm:burning_blocks`，而打火石点燃可燃方块时火位于 `firePos=被点方块.relative(点击面)`（通常在该方块**上方一格**）；生食丢在可燃方块上、blockPosition 落在该方块本身，其「自身」是可燃方块、「下方」是地面，`isOnHeat` 漏掉「上方」→ 火焰在头顶却不触发累计，永远烤不熟。修复：改为 6 向（自身 + 上下 + 四方）任一命中热源即算 on heat，站着火可燃方块上的生食也能被烤。
- **远古城市无空腔修复**：underworld `noise_settings` 在 `y<-8` 区域 `final_density` 恒为 1.6（完全实心，无可 cavity），而 `ancient_city` 原 `start_height:{absolute:-27}` 落在此实心区，`beard_box` 无法挖出探索空间。先后试过 `y=8~24`（仍偏底层仍偏实心），最终按用户要求改为 `uniform 20~40`，落入 noise 洞穴层（`final_density` 在 y≥24 由噪声驱动，洞穴充足）。已生成的旧城区不会回溯，需去新区块或开新世界验证。
- clean build BUILD SUCCESSFUL（6m42s）；双端部署（先 `msvcrt` 探测 JVM 锁：服务端/客户端均 UNLOCKED，备份 `.bak.20260829_181951` / `.bak.20260829_181924` 后覆盖，`zipfile` 校验关键 entry `BurningCookingHandler.class`/`ancient_city.json` 均入包）。

## 1.0.5

①新增血月巨型僵尸（R 196 `EntityGiantZombie` 忠实移植，地表血月替换体）：

- 新增 `GiantZombieEntity`（继承原版 `Zombie`，包名 `monster.zombie.Zombie`），数值对齐 R196：HP 100 / 移速 0.5 / 攻击 50 / 碰撞箱 3.6×11.7（×6 放大）；`finalizeSpawn` 设 `setBaby(false)` + `xpReward=50`。
- 注入点 `ZombieMiteSpawnMixin.finalizeSpawn` TAIL：血月（`ICPMMoonPhase.isBloodMoonNight`）+ 地表（`canSeeSky`）+ `reason==NATURAL` + 1/200 概率 → `discard()` 原僵尸并 `addFreshEntity` 新巨型僵尸；带 `instanceof GiantZombieEntity` 守卫防递归替换。
- 渲染 `GiantZombieRenderer`：复用 `ZombieModel`，通过 `MobRenderer.scale(state, poseStack)` 钩子放大 ×6（`render` 为 final 不可覆写）；注册于 `ICPMClient`。
- 战利品 `data/icpm/loot_table/entities/giant_zombie.json`（腐肉 3~6＋抢夺、铁锭 50%、金锭 25%、钻石 15%、绿宝石 10%×2）。

②补全 A 项四种怪物（对齐 R196）：

- **火元素 `FireElementalEntity`**：`Monster` 子类，followRange 40 / 移速 0.25 / 攻击 5 / 每 40 tick 对水伤 1 / 近战点燃 6 秒 / 经验 ×3 / 免疫火与岩浆 / 仅水伤害 + 雪球可伤。注册（下界 + 地下世界，权重 10）。
- **地狱苦力怕 `InfernalCreeperEntity`**：`Creeper` 子类，爆炸半径 ×2 / 天然防御 +2 / 免疫火与岩浆 / 掉地狱碎片 / 经验 ×3。注册（下界 + 地下世界，权重 10）+ 战利品表 `infernal_creeper.json`（掉 `infernal_creeper_frag` 0~3＋抢夺）。
- **恐狼 `DireWolfEntity`**：`Monster` 子类（规避 Wolf 父类 private/final），血 16 / 驯服 24、攻击 5、经验 ×2、蓝月夜主动索敌；自然生成野生敌对。注册（全维度黑暗，权重 8）+ 渲染器复用 `WolfModel` 强制 angry。
- **灰银鱼 `HoarySilverfishEntity`**：继承 `Silverfish`（R196 源码空，行为同原版）；注册（全维度脚下石头，权重 6）+ 渲染器继承 `SilverfishEntityRenderer` 覆写 `hoary.png` 纹理。
- 4 渲染器（`FireElemental`/`InfernalCreeper`/`DireWolf`/`HoarySilverfish`）加至 `ICPMNewMonsterRenderers.kt` 并注册于 `ICPMClient`；4 刷怪蛋入 `ICPMMonsterSpawnEggs.kt` + 资源 json + 占位 egg png + lang（zh_cn / en_us）。编译修正：Creeper/Silverfish 属性用 `Monster.createMonsterAttributes()`（1.21.11 无 `createCreeperAttributes`/`createSilverfishAttributes`）；渲染器模型真实包 `CreeperModel`/`SilverfishModel`（非泛型）；`spawnAtLocation` 用 serverLevel + ItemStack + Float 重载。

③修复土元素生成（前轮遗留 bug）：

- 原 `BiomeModifications.addSpawn` 误传第 7 个 spawn 谓词参数（编译错），且 `foundInOverworld/Nether/End` 与 `BiomeSelectors.all()` 叠加导致权重重复计数。
- 改为单条 `BiomeSelectors.all()` 权重 40 + 现有 `checkEarthElementalSpawnRules` 谓词（已允许 STONE/DEEPSLATE/OBSIDIAN/NETHERRACK/END_STONE 地板，覆盖地下世界）。删除冗余 `checkEarthElementalUnderworldSpawnRules`。

④贴图：从 r196 资源包 `E:/MITE Resource Pack 1.6.41` 复制 `fire_elemental.png` / `infernal_creeper.png` / `dire_wolf` 全套 / `silverfish/hoary.png` 覆盖项目（用户指定优先 1.6.41）。

## 1.0.4

①新增"装盾格挡"全新机制（R196 `Damage.applyTargetDefenseModifiers` 忠实移植）：

- 在对应等级 ICPM 工作台将任意 ICPM 工具/武器与 `minecraft:shield` 合成，结果 = 该工具并获得 `SHIELD_ATTACHED` 数据组件（可右键格挡，格挡效果与 R196 相同）；合成时盾牌仅消耗 25% 耐久（按 `max_damage × 0.25` 取整），以独立产物发还，可继续用或再参与合成。工作台合成产物自动落在对应等级，无需额外配置。
- 新增 `ShieldAttachRecipe`（`icpm:shield_attach` 自定义配方，自动并入 `RecipeType.CRAFTING`，复用既有 `performTake`/`onTake` 取物钩子发还受损盾牌）；`ICPMRecipes` 注册其 `Serializer`（`MapCodec` + `StreamCodec`）；`data/icpm/recipe/shield_attach.json` 配方文件。配方门控：恰好 1 个 ICPM 工具（未装盾）+ 1 个 `minecraft:shield`，其余格必须为空。
- 格挡行为（两个 Mixin，已登记 `icpm.mixins.json`）：
  - `ShieldBlockItemMixin`（`@Mixin(Item.class)`，HEAD 注入 `use`/`getUseDuration`/`getUseAnimation`）：装盾工具右键 `use` → `startUsingItem` 并返回 `CONSUME`（按住右键进入格挡态）；`getUseAnimation` → `BLOCK`、`getUseDuration` → 72000，使工具进入"使用物品"态且 `isBlocking()` 可能为真（抬手姿态）。
  - `ShieldBlockHurtMixin`（`@Mixin(LivingEntity.class)`）：`modifyAppliedDamage`（`@ModifyArgs` HEAD）在伤害路径末端将最终伤害减半（下限 1），并对装盾工具扣耐久 `int(减半后伤害 × 攻击衰减率)`（`hurtAndBreak` 处理耐久三，忠实 R196 `tryDamageItem`）；`isBlocking`（`@Inject` HEAD）重写为"正在使用装盾工具且动画为 BLOCK"时返回 true（姿态 + 一致性）。严格遵循选择"仅伤害减半(下限1) + 工具扣耐久；不挡箭、不免疫击退"——跳过 `DamageTypeTags.IS_PROJECTILE` 与 `BYPASSES_ARMOR` 来源；原版 `hurtCurrentlyUsedShield` 仅对 `ShieldItem` 生效，装盾工具非 `ShieldItem` 故不被误触发。减伤与扣耐久仅服务端执行。
- `QualityTooltipMixin` 新增装盾提示：带 `SHIELD_ATTACHED` 的物品显示金色"右键格挡（伤害减半）"。
- 约束：1.21.11 无 R196 的"剑格挡"机制（原版 1.6.4 系），本机制为从零新增；`LivingEntity.isBlocking()` 在 1.21.11 依赖 `ITEM_SHIELD_BLOCK` 物品标签，普通工具不在该标签内，故除改 `getUseAnimation→BLOCK` 外另重写 `isBlocking()` 使装盾工具在格挡态返回 true。

②按 R196 源码重构饱食度消耗机制（忠实移植 `FoodStats` 的 satiation / nutrition / hunger 三件套，替换原版 foodLevel / saturation / exhaustion 消耗模型）：

- **双槽系统**：新增 `ICPMFoodStats` 管理每玩家的 **satiation（饱腹）** 与 **nutrition（营养）**，上限随等级 `getNutritionLimit = clamp(6 + 等级/5×2, 6, 20)`（等级 0 上限 6、等级 35 达 20，与 R196 一致）。原版 `FoodData` 仅作显示层：食物条主格 = nutrition、半格 = satiation。
- **消耗机制（R196 核心）**：`hunger` 每 tick 固定累积 **0.002**（不随行走/奔跑/跳跃等活动变化，原版 exhaustion 全禁）；每累计 **4.0** 消耗 1 单位——优先消耗 satiation，satiation 耗尽 或 "只计营养的饥饿"（hunger_for_nutrition_only ≥ 4.0 且 nutrition>0）时消耗 nutrition；非创造模式额外 0.0005/tick 计入营养饥饿，保证 nutrition 最终也会下降；睡眠时消耗 ×20。
  - 直观感受：等级 0 满 6+6=12 单位 → 约 20 分钟吃空；等级越高上限越高、越耐饿。
- **自然回血（R196 heal_progress）**：回血速率由 nutrition 决定——每 tick 累积 `(0.0004 + nutrition×0.00002)`，营养不良 ×0.25、睡眠 ×4；累积满 1.0 回 1 点血并 +1.0 hunger（nutrition 越高回血越快）。替代旧的固定 1280 tick 回血。
- **饥饿伤害**：nutrition 归零即"饥饿"——starve_progress 每 tick 累积 0.002，每 1.0 按难度扣 1 点血（血>10 恒扣；普通难度需血>1；困难恒扣）。
- **进食数值（R196 数值表）**：按物品原版 ID 套用 R196 `setFoodValue(satiation, nutrition)` 数值——apple 2/1、bread 8/2、生猪肉 4/4、熟猪排 8/8、生牛肉 5/5、牛排 10/10、生鸡肉/鳕鱼/鲑鱼 3/3、熟鸡/熟鳕鱼 6/6、熟鲑鱼 10/10、饼干 3/1、西瓜片 1/1、胡萝卜 1/2、马铃薯 3/1、烤马铃薯 6/2、金胡萝卜 1/2、金苹果 2/1、蘑菇煲 2/4 等 32 项；未收录食物按默认规则（satiation = 原版营养值、nutrition = 原版营养值/2）。
- **实现载体**：`FoodDataMixin` 禁用原版 `tick`（消耗/回血/饥饿）与 `addExhaustion`（活动疲劳）与 `eat(FoodProperties)`（原版食物数值），R196 逻辑由 `PlayerMixin.tick` 每 tick 驱动 `ICPMFoodStats.tick`（仅服务端）；状态随玩家 NBT 存档（`icpm_satiation` / `icpm_nutrition` / `icpm_hunger` 等字段），旧档自动按上限初始化。
- 注：原版食物条/饱和度条仍正常显示（foodLevel=nutrition、saturationLevel=satiation）；"吃完不能立刻再吃"沿用既有 canEat 门控（nutrition 未满或 satiation 未满即可进食）。

## 1.0.3

①新增 MITE 剪刀及忠实机制（R196 ItemShears）：

- 新增 6 把 ICPM 剪刀（铜/金/银/秘银/精金/远古金属），用 `ShearsItem` 注册，耐久按 R196 公式 `miteDurability(materialDurability, components=2)`（铜/金/银=320）；铜、金剪刀直接用原版铜/金锭合成，其余用 `icpm:*_ingot`。原版 `minecraft:shears` 即"铁剪刀"，其 MITE 属性完全由 Mixin 注入，不另立物品。配方、创造标签、中英文语言、铜/金剪刀 16×16 纹理（纯 Python 生成，无 PIL）均已补全。
- `ShearsDurabilityMixin`（`@Mixin(ShearsItem.class)`，已登记 `icpm.mixins.json`）忠实移植三套 R196 机制：
  - **剪取类动作消耗 50 耐久**：原版每次只扣 1，Mixin 额外补 49（合计 50）。覆盖 `useOn` 命中（雕刻南瓜/剪蘑菇牛，SUCCESS 分支）与 `interactLivingEntity`（剪羊毛/剪蘑菇牛实体）。
  - **右键剪取方块（R196 onItemRightClick 的 silk harvest）**：原版未处理的可剪取方块（叶/毛/藤/发光地衣/蛛网/花/树苗/绊线），以物品形式完整收获，播放剪切音效，并扣 R196 破坏衰减耐久。
  - **左键破坏方块耐久（R196 onBlockDestroyed → getToolDecayFromBreakingBlock）**：用剪刀破坏可剪取方块时按方块硬度结算衰减（hardness==0→0；否则 `max(max((int)(h*100),5),1)`），原版 ShearsItem 本不耗耐久。
  - **右键延迟（R196 右键全局去抖 `PlayerControllerMP.setUseButtonDelay` ≈500ms）**：剪取类右键动作间强制间隔 `SHEAR_USE_DELAY_TICKS=10` 刻（≈0.5s），冷却中右键剪取方块（及剪羊/蘑菇牛实体）不生效——点击被忽略，对齐 R196 防连点/误触手感。以玩家 UUID + 服务端游戏刻去抖，仅服务端写入/读取。
- 约束：R196 剪刀"慢速破坏"源于其低采掘效率（base 4.0 × 材质系数），而 1.21.11 采掘速度由不可编译的 `ToolComponent` 决定（本项目映射未暴露该类），故"慢"以经济代价（破坏衰减耐久）形式忠实体现，未降低实际挖掘速度；R196 另要求的 `block.canSilkHarvest` 门控在本版本映射中未暴露，而上述可剪取方块在 1.21.11 均为 silk-harvestable，故以 `isShearEffective` 等价替代。

②修复众多 ICPM 怪物机制/贴图/模型（对齐 R196）：
- **土元素受击方式**（R196 `EntityEarthElemental.isImmuneTo` 忠实移植）：重写 `EarthElementalEntity.hurtServer` 独立判定——非木类变种仅 镐类(PICKAXE) 或 战锤(WAR_HAMMER) 近战可伤，另有 坠落/虚空/铁傀儡近战/爆炸 可伤，其余武器一律免疫；木材质变种（plank）仅 斧类(AXE) 或 战斧(BATTLE_AXE) 可伤；火焰行为保留（木变体受火×2、非木变体免疫火焰并转化为熔岩态）。武器判定用 `ICPMToolProperties.getToolCategory`（主手优先、副手兜底）。
- **土元素/黏土魔像人形模型**：弃用错误的立方体模型，改用 `HumanoidModel`（对应 R196 `ModelInvisibleStalker` 的 64×32 人形布局），复用 `ModelLayers.PLAYER`；实体尺寸 1.0×1.0 → 0.6×1.95；黏土魔像贴图改为人形 clay 贴图。
- **潜伏者 5% 透明**（R196 `RenderInvisibleStalker.getModelOpacity()=0.05`）：由 wight 贴图生成 alpha×0.05 的专属贴图并半透明渲染，实现"近乎隐形"。
- **补全实体名翻译键**：en_us/zh_cn 补齐 ghoul/wight/shadow/invisible_stalker/revenant/clay_golem/ancient_bone_lord/vampire_bat/nightwing（en 另补 miner_zombie）等 10 个实体显示名。

③怪物刷新范围对齐 R196（BiomeGenBase / End / Hell / Underworld）：
- 食尸鬼/尸妖/暗影/潜伏者/亡魂：全群系权重 2 → **10**、1-1；
- 土元素：按维度拆分——主世界 **10,1,1** / 下界 **40,1,1** / 末地 **20,1,4**，且仅 石头/黑曜石/地狱岩/末地石 上方生成；
- 黏土魔像：权重 10 → **50**、1-1，且仅黏土上方生成；
- 远古骨王：仅地下世界维度生成、权重 **5,1,1**（R196 BiomeGenUnderworld 专属）；
- 食尸鬼/亡魂：地下世界维度禁止刷新（R196 从生成列表移除）；
- 吸血蝙蝠/夜翼：改 AMBIENT 类别，权重 **20,8,8** / **4,1,4**（映射 R196 洞窟生物列表）。

④修复金属砧 shift+左键取走结果时材料不消耗（并修复刷物品漏洞）：
- 根因：quickMoveStack 结果槽分支依赖 doClick 的 `onTake` 回调时机，消耗逻辑可能不执行。
- 修复：结果槽分支改为**显式调用 `onResultTaken`**（消耗金属粒 + 输入工具 + 砧损耗，幂等防双扣）；`removed()` 先清空结果槽"修复预览"再归还材料（否则关闭 GUI 会白得修复后工具——刷物品漏洞）。

⑤修复 ICPM 工作台点开 JEI 查看配方后工作台内物品消失：
- 根因一：JEI 打开全屏配方页会关闭容器触发 `removed()`，原实现直接丢弃合成完成品；
- 根因二：鼠标点击坐标与结果槽区域重叠时可能误触发"开始合成/取走"，自动合成会消耗材料。
- 修复：`completeCrafting()` 剥离合成预览组件使成品可辨识；`removed()` 仅归还"合成完成且非预览"的真实成品（进背包/背包满掉落）；结果槽点击改用 `hoveredSlot` 判定，不再用纯坐标。

⑥修复 6 把剪刀配方解析失败（1.21.11 新配方格式）：
- 现象：copper/silver/gold/mithril/ancient_metal/adamantium 剪刀配方加载报 `No key fabric:type`。
- 根因：配方仍是旧格式（key 值 `{"item":...}`、result 用 `"item"`），1.21.11 需要新格式（key 值为字符串、result 用 `"id"`）。
- 修复：6 个 `*_shears.json` 全部改写为新格式。

⑦金属砧重构：新增砧耐久条显示 + 恢复命名功能（移植 R196 GuiRepair）：
- **砧耐久条**：菜单 DataSlot 同步砧 damage/maxDurability（原版铁砧无耐久概念、不显示），GUI 绘制绿/黄/红三色耐久条 + "砧耐久: X/Y" 文字。
- **砧命名**：GUI 新增命名框（EditBox），输入实时同步服务端 `setItemName`（新增 `AnvilRenamePacket` C2S）；纯命名（无材料）生成命名副本且不消耗金属粒；修复 + 命名同时生效；清空命名框可移除自定义名；命名框聚焦时键盘输入优先路由（不误触发容器快捷键）。

## 1.0.2

①移植符文石与符文门机制（R196 BlockRunestone / ItemRunestone / BlockPortal 第 8 位 runegate）：

- 新增 `BlockRunestone`（16 变体，VARIANT 方块属性 0..15，魔法名 Nul/Quas/Por/An/Nox/Flam/Vas/Des/Ort/Tym/Corp/Lor/Mani/Jux/Ylem/Sanct；秘银/艾德曼两金属，黑曜石强度）与 `RunestoneItem`（按 `RUNESTONE_VARIANT` 数据组件放置、显示名追加魔法名）。
- 配方沿用 R196（" n "/"n#n"/" n "，黑曜石居中 + 4 个金属粒十字 → 1 个符文石，默认 Nul）；创造栏展示全部 16 变体。
- 符文门：复用现有 3 种 ICPM 传送门的进入逻辑，进入时实时读取框架 4 角符文石——4 角同为某金属符文石则判定为符文门，4 角变体组合成 seed（BL + BR<<4 + TL<<8 + TR<<12，与 R196 角序一致）；同维度内按 seed 随机传送（mithril 半径 5000 / adamantium 半径 40000，adamantium 远离原点、避开海洋，seed==0→原点），落点不自动生成传送门。
- 修复（符文门恒传送到出生点）：根因为配方只产出 Nul、且 R196 靠专属合成 UI 选变体的机制本 mod 没有，导致 4 角变体恒为 0、seed 恒为 0（R196 规定 seed==0→原点=各维度出生点，表现为"传送回第一次进入的位置"）。新增**手持符文石对空中右键循环变体（0↔15）**，选好变体再放置，4 角即可编码出不同 seed→不同落点；创造栏 16 变体物品同样带对应变体。

②符文石与符文门操作指南（如何使用）：

- **合成符文石**：用任意 ICPM 工作台按 R196 配方（" n "/"n#n"/" n "，黑曜石居中 + 4 个同金属粒十字），产出 1 个符文石（默认 Nul 变体）。秘银粒→秘银符文石，艾德曼粒→艾德曼符文石。
- **选择变体（设密码）**：手持符文石对**天空/空中右键**可循环变体 0↔15，动作栏会提示"符文石变体 → Quas"等魔法名；也可用创造栏里已带变体的 16 个物品直接取用。变体就是符文门的"地址编码"，不同变体组合决定不同落点。
- **搭建符文门**：用任意 ICPM 传送门框架（无论是地下世界门、返回门还是地狱门），把框架的 **4 个黑曜石角**替换成**同金属**符文石（4 角必须同为秘银或同为艾德曼），门即被判定为符文门。4 个角的变体按 右下+左下<<4+右上<<8+左上<<12 组合成 seed。
- **传送**：走进该门即可在**当前维度内**按 seed 传送到对应坐标（秘银半径 5000 / 艾德曼半径 40000，艾德曼远离原点且避开海洋；seed==0 即 4 角全为 Nul 时落点仍是原点）。**同 seed 必然落在相同坐标**，所以记住你 4 角设的变体，就可以往返同一个地点。落点不会自动生成传送门，需自行用同套变体在落点另搭一门返回。

③按 R196 修正村庄与末地要塞（stronghold）生成：

- 村庄：频率由 vanilla spacing=34/separation=8 改为 R196 的 distance=40/min=20（即 spacing=40、最小间距 20，村庄更稀疏）；生物群系限为 plains/desert（R196 villageSpawnBiomes），移除 savanna/snowy/taiga 村庄；种子加盐 10387312（与 R196 一致）。
- 末地要塞（stronghold）：数量由 vanilla 128 个降为 R196 的 3 个（structure_set count=3、spread=3，3 个全落最内环、角度间隔 120°，与 R196 一致）。
- 注：R196 村庄另有 `villageConditions≥16` 前置，在 R196 反编译源码中无自动增长逻辑（仅命令/网络包写入），无法在 1.21.11 确定性结构生成模型下忠实移植；本次聚焦频率/群系/数量参数。（"世界前 60 天不生成"机制已在 ⑤ 实现）

④修复 `/day` 指令显示天数错误（到了第二天仍显示"第 1 天"）：

- 根因：本 mod 的睡眠唤醒在 `ICPMDaySleepMixin` 中改为次日 **5:00（dayTime-1000）** 唤醒，而天数边界本在 6:00（dayTime=24000）。唤醒瞬间 dayTime=23000，`23000/24000+1` 仍得 1，要再等 1000 tick（约 50 秒）跨过 24000 才显示"第 2 天"——表现为"到了第二天却没显示 day2"。
- 修复：天数算式改为 `(getDayTime() + 1000) / 24000 + 1`，使天数边界与 mod 的 5:00 唤醒对齐，醒来即显示正确天数；`getDayTime()` 为累计世界刻（与季节/月相判定同源），自然数天递增。

⑤加入村庄 60 天前置生成机制（R196 忠实移植）：

- R196 `MapGenVillage.canSpawnStructureAtCoords` 中 `if (this.worldObj.getDayOfWorld() < 60) return false;` 及 `MITEConstant.min_day_for_village_generation = 60`，即世界前 60 天禁止生成村庄。
- 天数算法对齐 R196 `World.getDayOfWorld = (getAdjustedTotalWorldTime(tick) + 6000) / 24000 + 1`（其中 `getAdjustedTotalWorldTime = tick + 6000`）：第 1~59 天不生成，第 60 天 0:00（总刻 1410000）起允许。
- 1.21.11 实现：新增 `ICPMVillageGenerationMixin`（`@Mixin(ChunkGenerator.class)` + `@Inject(method="createStructures", at=@At("HEAD"), cancellable=true)`），在 `createStructures` 末尾第 6 参 `ResourceKey`（即结构集注册键 `minecraft:villages`）识别村庄结构集，仅当世界天数 < 60 时 `ci.cancel()` 跳过；其余结构集（要塞、下界堡垒、末地城等）一律放行不受影响。
- 天数读取规避 mixin 铁律（绝不对 mixin 类 `ChunkGenerator` 作类型转换）：通过 `FabricLoader.getInstance().getGameInstance()` 取 `MinecraftServer`，再 `server.getLevel(Level.OVERWORLD).getGameTime()` 计算天数（主世界游戏时间即全局世界时间，跨维度同步）；仅在服务端（`MinecraftServer` 实例）生效，客户端（无 `MinecraftServer`）直接放行不干预。
- 语义：仅拦截"区块首次生成时是否放置村庄结构"，不影响已生成区块；配合 ③ 的 spacing=40/separation=20/salt=10387312，世界前 60 天已加载/将加载的区块都不生成村庄，第 60 天后新加载的区块才会按 R196 频率生成。

⑥修正 `/day` 天数边界为"游戏日 00:01"（午夜 +1 分钟）：

- 旧逻辑（④）：`(getDayTime()+1000)/24000+1`，天数边界对齐 mod 的 5:00 唤醒（早晨），即每过早晨 5:00 天数 +1，与玩家直觉的"午夜跨日"不符。
- 新逻辑：`(getDayTime()+4800)/24000+1`。Minecraft 午夜对应 `timeOfDay=18000`，00:01 = `18000+1200=19200`；该公式的翻转点恰好落在 `timeOfDay=19200`，故**每过午夜 1 分钟天数 +1**，符合"每一天从游戏日 0 点 1 分开始算起"。
- 注：`getDayTime()` 为累计世界刻（与季节/月相判定同源），单调不减；睡眠唤醒仍在 5:00（`ICPMDaySleepMixin`），但因 5:00 已过午夜，睡醒后 `/day` 显示次日，与跨午夜判定一致。若日后要改为"精确午夜 00:00 翻转"，把 +4800 改为 +6000 即可（差 1 分钟）。

⑦修复挖掘速度过慢（仅黑曜石系 / 工作方块 / 金属砧恢复原版速度，其余方块仍慢 17 倍）：
- 现象：覆盖挖掘规则后，黑曜石（硬度 50）钻石镐约 160 秒、空手挖工作台约 64 秒、空手挖金属砧约 153 秒，明显"非常慢"。
- 根因：`ICPMToolRulesMixin.icpm$calculateICPMProgress` 中进度增量除数为 `512.0f`，而原版 `BlockState.getDestroyProgress` 用 **30（有正确工具）/100（无正确工具）**。
- 用户意图（二次澄清）：**只**把"黑曜石系方块（黑曜石 / 哭泣黑曜石）+ 工作方块（各类工作台、各类金属砧）"恢复为原版等价速度；**其余普通方块仍保持 512 除数（比原版慢约 17 倍）**。因此不能一刀切改 30。
- 修复：除数改为条件取值——`icpm$isFastMiningBlock(state) ? 30.0f : 512.0f`。判定含：黑曜石 / 哭泣黑曜石；`BlockICPMWorkbench`（燧石→艾德曼 8 档工作台）+ 原版工作台（`CRAFTING_TABLE`）；`BlockMetalAnvil`（铜/银/铁/金/远古金属/秘银/艾德曼 7 系 ×完好/chipped/damaged 三态）+ 原版铁砧三态（`ANVIL`/`CHIPPED_ANVIL`/`DAMAGED_ANVIL`）。其余方块一律 512。
- 验证速度（等级 0）：黑曜石钻石镐 `8/50/30≈0.0053`→约 9.3 秒（原版等价）；空手挖工作台 `1/2.5/30≈0.0133`→约 3.75 秒；铁镐挖黑曜石 `6/50/30≈0.004`→约 12.5 秒。普通石头（仍 512）钻石镐 `8/1.5/512≈0.0104`→约 96 秒（保持偏慢手感）。
- 注：512 是 mod 的有意偏慢设定（非单纯误写），仅对指定的"黑曜石系 / 工作台 / 金属砧"放开为 30。

⑧重构合成品质展示（对齐 R196 `ItemStack.getTooltip` / `SlotCrafting`，修复"右键合成物看不到状态/经验/提升"）：
- 现象：改造后工作台结果槽预览物品不带品质组件、无 tooltip 钩子、经验消耗仅在聊天栏闪现，玩家无法在物品上看到①当前品质状态②消耗经验多少③实际提升（耐久/属性）。
- 方案：
  - 新增 `CraftPreviewComponent`（record，`xp_cost`/`min_q`/`max_q` 三字段 + CODEC/STREAM_CODEC），仅挂在"工作台结果槽预览物品"上，取走/Shift 取走时 `remove` 剥离，绝不泄漏进背包（避免污染存档与物品 NBT）。
  - 新增 `QualityTooltipMixin`（`@Mixin(Item.class)`，注入 `Item.appendHoverText` 在 TAIL）作为通用 tooltip 钩子（对应 R196 `addInformation`）：
    - 任意带 `QUALITY` 组件的物品 → 灰字显示品质描述符（中文，如"传说"）+ 蓝字"耐久 +X%"（mod>1）或红字"耐久 -X%"（mod<1），直观呈现"实际提升"。
    - 带 `CRAFT_PREVIEW` 组件（仅结果槽预览物品）→ 若 `maxQ>minQ` 加黄字"右键结果槽可切换品质"；`xp>0` 加黄字"合成消耗经验: X"，直观呈现"消耗经验多少"。
  - `ICPMWorkbenchMenu.applyPreviewComponents`：把"钳制进 [min,max] 的当前选中品质"写回预览物品的 `QUALITY` 组件，使结果槽物品**即时**显示当前品质（右键循环 `cycleQuality` 后同步刷新），对应 R196 `SlotCrafting.modifyStackForRightClicks` 即时写入结果槽。
- 冲刺坑（保命）：本 yarn(1.21.11) `Item.appendHoverText` 真实签名第三参是 `net/minecraft/world/item/component/TooltipDisplay`，**不是** `LivingEntity`（早期两次构建 `Cannot remap ... does not exist` 即因错写成 LivingEntity）；描述符为 `(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V`。

⑨修复主世界 MITE 矿物过少 + 蝙蝠维度错误：
- 现象：主世界矿洞基本看不到银/秘银/艾德曼矿；蝙蝠在主世界洞窟也会刷（应在地下世界）。
- 根因（矿物）：`data/.../placed_feature/ore_{silver,mithril,adamantium}.json` 主世界版本 count 仅 `silver=1 / mithril=1 / adamantium=rarity_filter(2)≈0.5 脉/区块`（地下世界版本是 `count=8`），且高度范围下限为 **y=0**，y<0 的深板岩层从不生成 → 深板岩矿石变体在主世界是死内容。两者叠加导致主世界几乎见不到 ICPM 矿。
- 修复（矿物）：主世界 placed_feature 提 count（`silver=8 / mithril=6 / adamantium=4`，去掉 adamantium 的 rarity_filter 改用 count）并将高度范围下限下探到 **y=-16**，使石头层（0~63）与浅深板岩（-16~0）都能生成；configured_feature 矿脉大小保持 6/3/3 不变。地下世界 placed_feature（`*_underworld`，count=8，y0~135）未动，仍为其专属富集层。生成仍走 data-driven（BiomeModifications 把这三个 placed_feature 挂到主世界 UNDERGROUND_ORES 阶段，见 `ICPMOreGenerator.register`）。
- 根因（蝙蝠）：`ICPMEntities.checkICPMBatSpawnRules` 只判"有天花板（洞窟）"，**无维度限制**；而 `BiomeModifications.addSpawn` 用 `BiomeSelectors.all()`（所有维度），导致主世界/下界/末地洞窟都会刷蝙蝠。
- 修复（蝙蝠）：在 `checkICPMBatSpawnRules` 开头加 `if (level.level.dimension() != ICPMPortalHandler.UNDERWORLD_KEY) return false`，蝙蝠（VampireBat / Nightwing）仅在地下世界维度刷新；其余维度（含主世界）一律禁止。注释同步改为"仅地下世界"。

⑩对齐 R196 金属砧耐久机制（替换 1.18.2 遗留的 1/3、2/3 阈值）：
- 现象：金属砧耐久机制此前按 1.18.2 ICPM 实现，阶段阈值用 1/3、2/3，与 MITE R196 源码不符。
- 修复（`BlockMetalAnvil`）：`getDamageStage` 阈值改为 **0.5 / 0.8**（损伤比例 `<0.5` 完好、`[0.5,0.8)` chipped、`[0.8,1.0)` damaged、`>=1.0` 销毁），对齐 R196 `BlockAnvil.getDamageStage`；`getMinimumDamageForStage` 改为整数上取整 `ceil(max/2)` / `ceil(4·max/5)`（等价于 R196 的"递增 damage 直到达阶段"算法）；`initialDamage`（chipped/damaged 变体初始损坏）相应改为 `0.5×max` / `0.8×max`。
- 修复（砧损耗乘数）：原硬编码 `护甲/弓 ×200`、`鱼竿 ×22`，改为 R196 `ContainerRepairINNER2.onPickupFromSlot` 的「已修复点数 × 耐久比」——`工具 ×1`、`护甲 × ratio_tool_to_armor`、`弓 × ratio_tool_to_bow`、`鱼竿 × ratio_tool_to_armor/9`；比值用 `ItemStack` 实测 `getMaxDamage()` 动态计算（铁镐/铁靴、秘银铲/秘银弓），`lazy` 避免与 `ICPMItems` 初始化顺序竞争。原 `MetalAnvilMenu.calculateAnvilDurabilityLoss` 死代码删除，统一调用 `BlockMetalAnvil.calculateAnvilDurabilityLoss`（改 `internal`）。
- 一致性：原版铁砧 mixin `AnvilRepairDurabilityMixin.applyVanillaAnvilDamage` 的 1/3、2/3 阈值同样改为 0.5/0.8（同样声称 MITE 确定性耐久）。
- 注：最大耐久公式 `1600×31×材质系数` 本就与 R196 一致，未改；`TileEntityMetalAnvil.addDamage` 经 `getDamageStage` 自动套用新阈值完成变体切换。

⑪对齐 R196 地下世界基岩生成（保留地幔层）：
- 现象：地下世界基岩边界与 R196 `ChunkProviderUnderworld.replaceBlocksForBiome` 不符——顶部基岩用 `below_top 0~5`（最厚 6 层，比 R196 多 1 层），底部地幔仅 `above_bottom 0~2`（1-3 层，比 R196 基岩地板薄）。
- 修复（`data/icpm/worldgen/noise_settings/underworld.json` 的 `surface_rule`）：顶部基岩屋顶改用 `below_top 0~4`（最厚 5 层），底部地幔层改用 `above_bottom 0~4`（最厚 5 层），精确对齐 R196 的 `nextInt(5)`（顶 y123-127 必含 y127、底 y0-4 必含 y0，中间石头/空洞）。
- 保留地幔层：`icpm:mantle` 为不可破坏方块，作为 ICPM 特色的底界，等价替代 R196 的基岩地板封底防穿（`ICPMBlocks.kt` 注释确认地幔"地下世界最底层，不可破坏"）。
- 验证：解压 vanilla `minecraft-common.jar` 的 `nether.json` 确认 `bedrock_roof` 标准写法即 `not + vertical_gradient`（当前写法正确）；`final_density` 中段密度约 `[-0.64,0.64]` 生成下界式空洞世界，基岩屋顶（y250-255）/地幔底（y0-4）正确覆盖。

⑫修复传送门返回判断（原路返回不再在地表另建传送门）：
- 现象：从地下世界/地狱返回上一维度时，即便原路返回，也会在**地表**新建一个传送门，导致越回越多。
- 根因：返回路径用 `findSafeDestination` 把落点沿列顶往下推到地表（y≈地形高），随后 `findOrCreatePairedPortal` 在**地表落点**周围半径 16 搜索原门；若原门建在地下深处（如 y=20 矿洞基地），地表落点 y≈70、搜索区间 [54,86] 覆盖不到 y=20，于是判定"没门"而新建。
- 修复（`ICPMPortalHandler.java`）：抽出共用 `computePortalPlan(...)`，返回（有记忆坐标）时改为在**记忆坐标（含真实 y）**附近优先 `findExistingPairedPortal(...)` 查找已有配对门——找到则复用（落点设在已有门内、`portalToCreate=null`、复用时不挂 portal ticket），仅当确实找不到时才按维度缩放落点并 `findOrCreatePairedPortal` 新建。`teleport` 与 `createTeleportTransition` 两条路径统一走 `computePortalPlan`，避免分叉。
- 语义：仅当"上一维度传送门正常工作（存在且可达）"时复用，原门被拆毁等情况下仍按旧逻辑新建，符合"正常则不再另外创建"的诉求。

⑬修正地下世界基岩结构（按 R196 规格：顶层基岩 / 中层石头矿 / 底层三层基岩 / 最底地幔）：
- 现象：换了新 jar、去了新区块，地下世界活动层也看不到基岩——底层只生成了整片 `icpm:mantle`（地幔），根本没有基岩地板。
- 根因（前版错误）：上一版把底层写成 `vertical_gradient above_bottom 0~4` 整片**地幔平铺**，漏掉了 R196 的"底层三层基岩"。叠加维度若为 256 高时 `below_top 0~4`=y251~255（够不到）、`above_bottom 0~4`=y0~4（得挖穿世界底），活动层 y20~100 内确实无基岩。
- R196 真相：`ChunkProviderUnderworld.replaceBlocksForBiome` 主层 **128 高**（y0~127），顶 `y≥127-nextInt(5)`=y123~127、底 `y≤0+nextInt(5)`=y0~4 各 1~5 层随机基岩封闭；最底基岩为"山脉"结构、地幔裸露于其盆地中（挖到地幔也不会前功尽弃）。
- 修复（`noise_settings/underworld.json` 的 `surface_rule`，维度已 128 高）：① 顶层基岩天花板 `below_top 0~4`=y123~127（世界顶不可建造/跳跃）；② 中层石头+空洞+矿石（noise 不变）；③ **底层三层基岩地板** `vertical_gradient above_bottom 1~4`（随机豁口、每层持续几格高，等价 vanilla `bedrock_floor`）；④ **最底地幔** `above_bottom 0`=y0（`icpm:mantle` 不可破坏，位于基岩地板盆地之底——挖穿基岩地板即见地幔，盆地处基岩薄、地幔浅露）。地幔 NOT 整片平铺底层：底层必须是基岩地板，地幔仅在 y0。
- 注：基岩/地幔是**世界生成时**决定，已加载旧区块不回溯；需去未探索新区域或开新世界才能看到新结构。

⑭地下世界/地狱床提示"你感觉到不够安全"（R196）：
- 现象：原 `ICPMBedBlockMixin` 只拦截地下世界、且消息 key `message.icpm.not_safe_here` 在语言文件里不存在（会显示原始 key 文本）。
- 修复：扩展为**地下世界 + 地狱**都拦截 `BlockBehaviour.useWithoutItem`（床的睡眠/爆炸逻辑所在地，1.21.11 方块交互入口 `useItemOn` 委托至此）；提示改为字面量中文"你感觉到不够安全"（`displayClientMessage(..., true)` 走 action bar），并返回 `SUCCESS_SERVER` 取消原版逻辑——地下世界上床失败、地狱床**不再爆炸**（对齐 R196 地狱床也不炸）。主世界/末地睡眠不受影响。

⑮地下世界底层基岩豁口概率修正（严格按 R196 源码，移除猜测的平滑梯度）：
- 现象：地幔上方基岩的豁口概率不对——旧版 surface_rule 用 `vertical_gradient above_bottom 1~4` 平滑渐变，每一列都必定在 y1 铺一层基岩、永远不会出现"整列无基岩、地幔直接裸露"的豁口，与 R196 的"按列二值噪声 + 概率豁口"不符。
- 修复（R196 真相，GitHub `TesseractLHY/Underworld` 的 `UnderworldHook.init`）：底层基岩改由 `ICPMUnderworldBedrock` 在 `ServerChunkEvents.CHUNK_LOAD` 逐列代码生成。每列 `num_bedrock_blocks=random.nextInt(3)+1`（地幔层数 1~3）；`bedrock_noise=max(strata1a,strata1b)+各 bump 加权`（bump1a*0.25 / bump1b*0.125 / bump1c*0.125 / bump4*0.09375+0.125）；**仅当 `bedrock_noise>0` 才铺基岩**（紧接地幔之上、≤3 层"三层基岩"），`bedrock_noise<=0` 即豁口（盆地裸地幔）。噪声用 1.21.11 `SimplexNoise`（绝对坐标采样保证跨区块无缝），scale 对齐 R196（strata 0.03125、bump 0.125/0.25/0.5/1.0）。
- surface_rule 中底层 `vertical_gradient` 基岩地板与 `icpm:mantle` 平铺规则**已移除**，仅保留顶层基岩天花板（y123-127）。旧区块不回溯，需去新区域或开新世界查看。

⑯修复传送门返回落点（地下世界入口在主世界 y<=-55 深处，返回却落到地表）：
- 现象：从地下世界返回主世界，原路返回"不再新建门"（⑫）虽生效，但玩家落点仍被扔到**地表**，远离自己位于主世界地下的传送门——返回机制依旧不可用。
- 根因：复用分支调用 `findSafeDestination(targetLevel, x, z)`，该方法**永远从世界顶部往下找**第一个可站立处，完全忽略记忆坐标的真实 Y。而 MITE 进入地下世界的传送门在主世界 y<=-55 的地下，于是复用虽找到门，落点却被顶到地表。
- 修复：`ICPMPortalHandler.computePortalPlan` 复用分支改为**直接落在传送门内部**（沿本列下探到最底部传送门方块，玩家站立其上方的黑曜石地板），不再做"从顶搜下"的安全搜索；另新增 `findSafeDestinationNear`（以记忆 Y 为中心 ±3/-6 有限范围搜索，含天花板维度上限处理），用于"记忆坐标附近无门需新建"的回退分支，同样尊重真实 Y；`createTeleportTransition` 在落点确定后补 `setPortalCooldown()`，避免落点恰在门内被原路立刻弹回。
- 构建 BUILD SUCCESSFUL（25s），`cp -f` 覆盖 mods（20:44，size 1553593）。

⑰修复燧石系工具/工作台配方不支持 icpm 皮革线（`icpm:leather_cord`）：
- 现象：燧石工作台（`flint_workbench`）配方用 `minecraft:leather` 写死、燧石钓鱼竿（`flint_fishing_rod`）用 `minecraft:string` 写死，导致持有 `icpm:leather_cord`（由 1 皮革→4 皮革线）时**无法**用于合成这两样燧石系物品；而其余燧石工具（knife/shovel/hatchet/axe）早已用 `#icpm:cords` 标签（含 `string`+`leather_cord`），前后不一致。
- 根因：配方 JSON 手工维护（两个生成器 `RecipeGenerator`/`ResourceGenerator` 均指向旧目录 `icpm-template-1.21.11`，与本项目无关，长期失同步）；工作台/钓鱼竿漏改。
- 修复：将 `flint_workbench.json` 的 `A` 与 `flint_fishing_rod.json` 的 `|` 均由写死的 `minecraft:leather`/`minecraft:string` 改为 `#icpm:cords` 标签，与燧石工具保持一致——既支持 `icpm:leather_cord`（用户诉求），又保留 `minecraft:string` 向后兼容。金属系钓鱼竿（copper/silver/gold/iron/ancient_metal/mithril/adamantium/obsidian）属更高阶，仍用原版 `string`，未动。
- 构建 BUILD SUCCESSFUL（23s），`cp -f` 覆盖 mods（15:04，size 1553612）。

⑱重写全部 8 个 icpm 工作台方块配方，严格对齐 R196 源码（`E:\MITE R196空壳\...\decompile\src_deobf\net\minecraft\src\RecipesMITE.java` L129-148 + `BlockWorkbench.java` 的 `getToolMaterial`/`getBlockComponent` 映射）：
- 现象（"乱七八糟"）：7 个金属工作台（copper/silver/gold/iron/ancient_metal/mithril/adamantium）全被写成 `III / III / " S "`——9 个锭 + 1 木棍的 3×3，与 R196 **完全不符**；燧石工作台形状也非 R196 形态。
- R196 真实配方（按 tier 分档，每档 `Block.workbench` 一个 metadata sub-block）：
  - **flint 档**（material=flint，组件=`Block.wood` 原木）：`"FS"/"s#"` = `flint` + 绑绳(silk/sinew) + `stick` + 原木。本项目绑绳统一用 `#icpm:cords`（=string+leather_cord），**既忠实 R196 的"绑绳槽"、又满足 ⑰ 的皮革线支持诉求**。
  - **metal 档**（copper/silver/gold/iron/ancient_metal/mithril/adamantium，组件=`Block.planks` 4 种）：`"IL"/"s#"` = 对应 `ingot` + **`Item.leather`（真皮革，非皮革线）** + `stick` + 木板。R196 金属档用的是 `leather` 包裹槽，不是 cords；故金属工作台不混用皮革线，严格按源码。
  - （obsidian 档 metadata 11-14 本 mod 未实现，跳过。）
- 修复：8 个 `*_workbench.json` 全部重写为 2×2：`flint_workbench` 用 `FS/s#`（flint + `#icpm:cords` + stick + oak_log）；其余 7 个金属用 `IL/s#`（各自 ingot + `minecraft:leather` + stick + `#minecraft:planks`，`#minecraft:planks` 等价 R196 的 4 种木板循环）。
- 构建 BUILD SUCCESSFUL（2m7s），`cp -f` 覆盖 mods（15:19，size 1553739）。

⑲修复挖沙砾时"有时什么都不掉、有时同时掉好几样物品"：
- 根因：`data/minecraft/loot_table/blocks/gravel.json` 把 11 种掉落（沙砾本身 + 燧石碎片/燧石/铜银金粒/黑曜石翡翠钻石碎片/秘银精金粒）拆成 **11 个独立 pool（`rolls:1` 各自独立判定）**。无精准采集时沙砾本身仅 75% 概率（25% 连沙砾都不掉），且各 nugget/shard 池与沙砾池独立命中 → 既会"什么都不掉"（沙砾失败且所有低概率池失败），又会"掉好几样"（沙砾+某 nugget 同时命中）。这违背 R196 正统规则。
- R196 真值（`src_deobf/.../BlockGravel.java` `dropBlockAsEntityItem`）：沙砾每次破坏**只掉恰好 1 个物品**——约 75% 掉沙砾本身，否则（约 25%）按权重随机掉燧石碎片(15.625%)/燧石(1.04%)/铜粒(5.56%)/银粒(1.85%)/金粒(0.617%)/黑曜石碎片(0.309%)/翡翠碎片(0.103%)/钻石碎片(0.0343%)/秘银粒(0.0114%)/精金粒(0.00381%) 之一，永不既掉沙砾又掉别的、也永不空手。
- 修复：合并为 **1 个 `rolls:1` 的加权单池**（精准采集单独走"只掉沙砾"池），各条目 `weight` = R196 边缘概率，保证恰好 1 个掉落；并顺手把原表里偏低的黑曜石/翡翠/钻石/秘银碎片概率修正为 R196 真值。
- 构建 BUILD SUCCESSFUL（12s，二次重跑确认资源重打包），`cp -f` 覆盖 mods（15:43，size 1553654）；已解压 jar 校验：2 pool、11 weight 条目均在。

⑳修复"icpm 工作台的合成功能和 UI 都没了"（实为 jar 损坏，非代码 bug）：
- 现象：右键工作台菜单打不开、无法合成；`logs/latest.log` 海量报错 `Failed to load class file for 'name.icpm.block.BlockICPMWorkbench$getMenuProvider$1'!` → `Caused by: java.util.zip.ZipException: ZipFile invalid LOC header (bad signature)`；`ICPMPlantDisease` 等其它类也加载失败，印证**整个 jar 损坏/截断**。
- 根因：上一轮 ⑲ 构建首次 `exit 1`、二次"成功"的异常，产物 jar 未写完整即被 `cp -f` 覆盖部署 → 损坏 jar。游戏开门时加载匿名内部类失败 → 菜单永远打不开 → 表现为"UI 与合成功能一起没了"。
- 修复：`clean build`（重生成全部 class，无增量残留）→ `unzip -t` 校验 **No errors detected** → 删除 mods 内旧损坏 jar → 重新 `cp -f` 部署；部署后再 `unzip -t` 复核部署目标 jar 也完整。
- 教训（写入 MEMORY）：**部署前必须 `unzip -t` 校验 jar 完整性**；构建若出现过 `exit 1`，必须 `clean build` 重来，绝不能拿半写的 jar 部署。损坏 jar 的症状是"某功能+其 UI 一起消失"且日志报 `ZipException invalid LOC header`。

㉑负等级死亡惩罚不对"开启死亡不掉落（keepInventory）"的玩家生效：
- 诉求：负等级惩罚机制（死亡且经验≤0 时每次下调一个负等级档位、压低重生经验下限并弹惩罚提示）不应作用于开了 `gamerule keepInventory true` 的玩家。
- 实现：`ICPMExperience.computeRespawnFloor` / `recordDeath` 新增 `applyPenalty: Boolean = true` 参数；`keepInventory` 开启时传 `false`，该分支直接返回非负哨兵 0 → 后续 `AFTER_RESPAWN` 既不会把经验压到负下限、也不会弹"你死亡损失了 X 点经验（负等级惩罚）"提示。
- 取值坑（1.21.11）：`GameRules` 已重构，`KEEP_INVENTORY` 字段类型为 `GameRule`（原始类型），旧式 `getBoolean(GameRules.Key<...>)` 不存在；改用 `level().getGameRules().get(GameRules.KEEP_INVENTORY)`（泛型擦除返回 `Object`，布尔规则实际为 `Boolean`），以 `Boolean.TRUE.equals(...)` 安全取值。读取点在 `ICPM.java` 的 `ServerLivingEntityEvents.ALLOW_DEATH` 注册内（持有 `ServerPlayer`，可取到 `player.level().getGameRules()`）。
- 构建 `clean build` BUILD SUCCESSFUL（2m3s），`unzip -t` 校验 **No errors detected** 后删除旧 jar、`cp -f` 覆盖 mods（16:36，size 1554014）。

## 1.0.1

①修复负等级采集（挖掘速度）惩罚不生效：将采集等级修正直接乘入真正的挖掘计算中心（BlockBehaviour.getDestroyProgress 的 icpm$calculateICPMProgress），负 40 级时挖掘速度降至约 0.2 倍。

②重构合成品质机制（移植 R196）：品质由等级（最低值）与经验（最高值）共同决定，支持 [min,max] 循环切换，品质高于均值时扣经验；已接入模组工作台。

③修正负等级死亡惩罚阈值：仅死亡时经验 ≤ 0 才每次扣 20 点经验（降一级，最低 -40 级）；经验 > 0 不受惩罚。

④优化 /xp 指令输出：只显示当前经验值（You now have {值} experience points）。

⑤修改成就"我们需要再深入些"触发条件：由进入下界改为进入模组地下世界维度（icpm:underworld）。

⑥修正地下世界刷怪笼刷新概率（过高→过低→修正）：按 R196 改为每区块 16 次尝试、天然空腔门控（中心外环 1~5 空气格 + 实心地板/天花板）；真实高度带 y∈[140,171]（= nextInt(32)+20+underworld_y_offset，R196 的 Y_OFFSET_FOR_UNDERWORLD=120，此前漏算 +120 偏移导致压在贴底无洞区、几乎不刷新）。

## 1.0.0

1.将所有可能侵权MITE的内名称(1.21.11MITE->[我不能玩mite]ICPM)和包名(name.mite78) 重置为name.icpm.ICPM(I can't play MITE缩写)

2.修复了负等级测试接口？
