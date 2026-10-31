

## v26.1-1.0.3-hotfix1 — Minecraft 26.1.2

**中文**

**模块**：超级堆叠

**摘要**：均分和平铺改由本模组自己算，联机时不用再一格一格地模拟点击。

### 修复

- 均分 / 平铺：合成格里出现大堆时，客户端直接把结果算好写回去，不再逐个模拟点击，联机的手感和单机一样。
- 平铺：只有按平铺时才会把空格算成目标格；均分只摊开本来就放着同类物品的那几格。
- 余数：一格一格地分，撞到单格上限就留在第一格 —— 宁可一格冒尖，也不会少东西。
- 精妙存储 / 背包的合成升级用的是同一套算法，那边点均分同样不再发磨。
- 服务器没装合成辅助时，改由本模组在服务端把结果算实，不会再被下一次同步冲回原样。

**English**

**Modules**: UltraStacks

**Summary**: Balance and spread are computed by the mod itself, so online play no longer replays clicks one slot at a time.

### Fixed

- Balance / spread: with big stacks the client writes the finished layout straight back instead of replaying clicks, so online feels like single-player.
- Spread only pulls the empty slots in when you press spread; balancing just spreads the stacks that are already there.
- The remainder is handed out slot by slot and stays in the first slot when nothing fits - a slot may overflow before an item goes missing.
- The crafting upgrade in Sophisticated Storage runs the same routine, so its button is just as smooth.
- On servers without CraftingTweaks our own packet computes the result server-side, so the next sync cannot undo it.

## v26.1-1.0.3-hotfix — Minecraft 26.1.2

**中文**

**模块**：超级堆叠

**摘要**：上一版留下的一个毛病，修好了。

### 修复

- 均分：服务器没装合成辅助时，按钮照样管用，结果不会被下一次同步冲掉。

**English**

**Modules**: UltraStacks

**Summary**: One leftover from the last release.

### Fixed

- Balance: on servers without CraftingTweaks the button now sticks, instead of being overwritten by the next sync.

## v26.1-1.0.3 — Minecraft 26.1.2 / NeoForge 26.1.2

**中文**

**模块**：超级堆叠

**摘要**：Shift 批量合成多了个保险，合成辅助的均分不再卡，合成与整理里几个老毛病一起修了。

### 新增

- 一次 Shift 合成默认最多 64 个产物（可配置）；按住解锁键，默认鼠标侧键 4，可以整批合完。只作用于原版工作台和背包 2×2 合成格。
- 对应的配置项是 `bulkCraftLimit` 和 `bulkCraftUnlockKey`，把后者关掉就回到「一次合完」。

### 修复

- 合成格里有超过 64 的堆叠时，客户端不再跟着逐次跑一遍，挂在合成事件上的模组每轮扫一遍配方的开销也随之消失。
- 合成结束后产物槽会被清空，不再残留一个拿不走的产物图标。
- 剪刀、模具这类用完原样还回来的材料，不再把整批合成卡成 1 次，也不会被复制。
- 有耐久的物品不再成叠：上限计算和整理都按原版 1 处理。
- 合成辅助的「均分」在大堆下由本模组一次算完，点一下立刻分好，不再一个槽一个槽地试。

**English**

**Modules**: UltraStacks

**Summary**: bulk crafting got a safety limit, the balance button no longer stalls, and a few old crafting and sorting bugs are fixed.

### Added

- A shift-craft now produces at most 64 items by default (configurable). Hold the unlock key (mouse button 4 by default) to finish the whole batch. Vanilla crafting table and the 2x2 grid only.
- The options are `bulkCraftLimit` and `bulkCraftUnlockKey`; turn the latter off to always craft everything at once.

### Fixed

- With stacks larger than 64 in the grid, the client no longer replays the one-craft-per-round loop, so the recipe scan that other mods run on every crafting event is gone.
- The result slot is cleared after a bulk craft instead of keeping an item you cannot take.
- Self-returning ingredients such as shears and moulds no longer cap the batch at one craft, and they are not duplicated.
- Damageable items no longer stack: both the limit calculation and sorting treat them as unstackable.
- CraftingTweaks' balance button now computes the split in one pass for big stacks, so a single click finishes it instead of probing slot after slot.

## v26.1-1.0.2-hotfix — Minecraft 26.1.2 / NeoForge 26.1.2

**中文**

**模块**：超级堆叠

**摘要**：修掉 1.0.2 带出来的三处问题——工作台右键不能一次放一个、批量合成两边各算一次、AE2 终端一次只搬一小撮。

### 修复
- 工作台右键恢复一次放一个；合并量的放大只在左键且槽里已有同类物品时生效。
- 批量合成只在服务端执行，客户端那份菜单副本不再重复算一遍。
- AE2 物品键报的堆叠上限改用物品自身的默认值，终端存取不再被手里那一摞的数量卡住。

**English**

**Modules**: UltraStacks

**Summary**: fixes three problems 1.0.2 introduced — right-click in the workbench no longer places a single item, bulk crafting ran on both sides, and AE2 terminals moved only a fraction of a stack.

### Fixed
- Right-click in the workbench places one item again; the merge amount is only widened for a left-click on a slot that already holds the same item.
- Bulk crafting now runs on the server only, so the client's copy of the menu no longer counts the same batch twice.
- AE2 item keys report the item's own default limit again, so terminals stop capping transfers at the size of the stack they were built from.

## v26.1-1.0.2 — Minecraft 26.1.2 / NeoForge 26.1.2

**中文**

**模块**：超级堆叠

**摘要**：把一次点击的合并量放开，让相同两堆能判同，抽屉取出也压到一组。

### 修复
- 一次点击按槽内剩余空间一次并满。
- 内容完全相同的两堆不再因为默认组件表对象不同被判成两种物品。
- 抽屉单次取出压到一组，并剥掉旧存档留下的堆叠上限标记。

**English**

**Modules**: UltraStacks

**Summary**: lets a single click merge a whole stack, treats identical stacks as equal, and caps drawer extraction at one stack.

### Fixed
- A single click merges as much as the slot can take.
- Two stacks with identical contents are no longer treated as different items because their default component maps are different objects.
- A single drawer extraction is capped at one stack and drops the stack-size marker left by older saves.

## v26.1-1.0.1-hotfix2 — Minecraft 26.1.2 / NeoForge 26.1.2

**中文**

**模块**：超级堆叠

**摘要**：修复上一版热修复在打开背包时仍会崩溃的问题。

### 修复
- 回调参数类型与 FastWorkbench 的目标方法不一致，打开背包仍会崩溃；现在按目标签名声明。

**English**

**Modules**: UltraStacks

**Summary**: fixes the remaining crash with FastWorkbench when opening the inventory.

### Fixed
- The callback signature did not match FastWorkbench's target method; it now matches exactly.

## v26.1-1.0.1-hotfix1 — Minecraft 26.1.2 / NeoForge 26.1.2

**中文**

**模块**：超级堆叠

**摘要**：修复与 FastWorkbench 一起使用时打开背包崩溃的问题。

### 修复
- 上一版 hotfix 在装了 FastWorkbench 的客户端打开背包会直接崩溃，现已修好。

**English**

**Modules**: UltraStacks

**Summary**: fixes a crash with FastWorkbench when opening the inventory.

### Fixed
- The previous hotfix crashed clients with FastWorkbench installed as soon as the inventory was opened.

## v26.1-1.0.1-hotfix — Minecraft 26.1.2 / NeoForge 26.1.2

**模块**：超级堆叠

**摘要**：热修复抽屉的取出与配方问题。

### 修复
- 抽屉里存着超大数量时，Shift 点击一次会把整个抽屉塞进背包；现在一次只取一组。
- 从抽屉取出的物品带着「上限 64」的旧组件，导致 JEI 与配方匹配失败；取用时会把这种组件清掉。
- 装了 FastWorkbench 时，Shift 合成一个大堆叠要一轮一轮地合，现在一次合完整批。


# Changelog / 更新日志

## 26.1-1.0.1 — Minecraft 26.1.2 / NeoForge 26.1.2

**中文**

**模块**：超级堆叠

**摘要**：加入游戏内配置界面，修掉抽屉与背包整理的数量问题。

### 新增
- 游戏内配置界面：NeoForge 自带的设置页。

### 修复
- 功能性存储的抽屉显示能到 2,097,152，实际只吃到 2,048，取出来的整堆既不能堆叠也匹配不上配方（旧升级物品里固化了 64 的上限）。
- 精妙背包与精妙存储点「整理」后，整堆会变成 64 个，多出来的物品直接消失。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: adds an in-game config screen and fixes drawer and sorting counts.

### Added
- In-game config screen: NeoForge ships its own settings page.

### Fixed
- Functional Storage drawers reported room for 2,097,152 but only accepted 2,048, and the stacks pulled out could no longer stack or match recipes (old upgrade items carried a fixed 64 limit).
- Sorting a Sophisticated Backpack or Storage turned a full stack into 64 and dropped the rest.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 26.1-1.0.0 — Minecraft 26.1.2 / NeoForge 26.1.2

**中文**

**模块**：超级堆叠

**摘要**：兼容更多模组，修掉铁砧、附魔台与酿造台的问题，并加上游戏内配置界面。

### 新增
**兼容**：
- 精妙核心
- 应用能源 2
- 精致存储
- 更多熔炉
- 神秘农业（含扩展与自动化）

**兼容补丁**：按模组是否安装自动启用，没装的不加载。

- 游戏内配置界面：模组列表里直接改配置，用 NeoForge 自带的界面，不用另装模组。
- 合成与显示：Shift 一次合完整批，数量缩写成 k / m / b，提示里能看到精确值。
- 批量结算：铁砧、附魔台、酿造台一次处理整批，按实际用量扣材料与经验，剩下的留在槽里。
- 药水与附魔书可堆叠：默认是堆叠上限的 1/16，也能改成固定值或保持原版。
- 各种桶共用上限并跟随空桶（默认 16,384）。
- 创造模式：铁砧不扣经验，附魔台不扣材料。

### 修复
- 酿造台会把整摞药水替换成 1 瓶。
- 铁砧的附魔与修复只出 1 个产物，取产物还会吞掉整堆材料。
- 附魔台对整堆只扣一次经验。
- 四位数在紧凑模式下超出格子。
- 精致存储终端一次只搬 64 或 99 个。
- 应用能源 2 的接口每槽容量写死 99。
- 部分模组的机器与终端槽位写死 64，配置调大后仍只收 64 个。

### 已知限制
- 数量超过 1073741823 的堆叠读档后会变回 1 个，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: compatibility with more mods, fixes for the anvil, enchanting table and brewing stand, and an in-game config screen.

### Added
**Compatibility**:
- Sophisticated Core
- Applied Energistics 2
- Refined Storage
- Iron Furnaces
- Mystical Agriculture (with addons)

**Compat patches**: enabled only when the matching mod is installed.

- In-game config screen: change settings from the mod list, using NeoForge's built-in screen with no extra mod.
- Crafting and display: Shift-craft a whole batch, counts abbreviated as k / m / b with the exact value in the tooltip.
- Batch settling: the anvil, enchanting table and brewing stand handle a whole batch, charging materials and experience by actual use and keeping the leftovers.
- Potions and enchanted books stack: one sixteenth of the stack limit by default, a fixed value, or vanilla.
- Buckets share one limit that follows the empty bucket (16,384 by default).
- Creative mode: no experience from the anvil, no lapis from the enchanting table.

### Fixed
- The brewing stand replaced a whole potion stack with a single bottle.
- The anvil produced one item for enchanting and repair, and taking the result consumed the whole input stack.
- The enchanting table charged experience once for a whole stack.
- Four-digit counts overflowed the slot in compact mode.
- Refined Storage grids moved only 64 or 99 items at a time.
- Applied Energistics 2 interfaces were capped at 99 per slot.
- Machine and terminal slots in several mods were hardcoded to 64 and still accepted only 64 after raising the limit.

### Known limitations
- Stacks above 1073741823 load back as a single item; nothing crashes.
