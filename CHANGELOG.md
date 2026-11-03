# Changelog / 更新日志

## v20.1-1.0.4-beta — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：铁砧支持 Shift 批量合成，并在格子上直接显示批量数量。

### 新增

- 铁砧改成按件结算：一次只从左右两格各取 1 个，剩下的材料留在槽里。
- 铁砧支持 Shift 批量：一次最多 64 件，按住解锁键（默认鼠标侧键 4）连做到底。
- 新增「忽略过于昂贵」开关（默认开启）：所需等级达到 40 时原版会显示「过于昂贵」，打开开关后可以照常取走。
- 产物格显示这一批能做出多少（原版铁砧、工作台、AE 与精妙界面）：Shift 为 64 次，解锁键为做满。
- 提示框里显示「合计：X / 堆叠上限」，一眼看清这一批的总量与单格上限。
- 精妙铁砧页的「附魔花费」跟着这一批走：不按显示单次，按住 Shift 显示 64 次的总等级，按住解锁键显示材料能做满的全部。
- 精妙合成开启自动补货时，合计会把背包存储里能继续补货的库存一起算进去，不再只看合成格现有的材料。
- 往 ME 接口的配置格手动放物品，会叠到每格上限，放不下的留在手上。

### 优化

- ME 接口和机器缓冲的每格容量跟随本模组上限，不再是写死的 64。
- 神秘农业的注魔台、附魔台这类机器，槽位上限跟随配置，不再卡在写死的 64 / 512。
- 在精妙背包、箱子、桶里用合成辅助「清空网格」时，材料优先放回背包和快捷栏。
- 数量缩写按格子宽度自动调整：1,234 带逗号，10,100 起缩成 `10.1k`（65,536 → `65.5k`），过宽自动缩小。

### 修复

- 提示框里的数量行改到模组名下面，之前会显示到别的位置。
- 旧存档里超过上限的堆叠，不会再被拆出更多物品，也不会被截断；往满槽插入会被挡回。

### 已知限制

- 精妙背包「铁砧」升级页暂不支持 Shift 批量，仍是一次一件；原版铁砧的批量正常。
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: the anvil takes shift batches and shows the batch count on the slot.

### Added

- The anvil settles per item: one take pulls 1 item from each input, and the rest stays in the slots.
- The anvil supports shift batching: up to 64 takes at once, and holding the unlock key (mouse button 4) keeps going.
- New "ignore too expensive" toggle (on by default): vanilla stops at 40 levels with "Too Expensive"; with the toggle on the result can be taken as usual.
- The result slot shows what this batch would make on the vanilla anvil and crafting table, in AE and in Sophisticated screens: 64 takes with shift, everything with the unlock key.
- The tooltip adds "Total: X / stack limit" so the batch total and the per-slot cap are visible at once.
- In the Sophisticated anvil tab the level readout follows the batch: the single job normally, 64 times the cost with shift, and the whole material-limited batch while the unlock key is held.
- With auto-refill on, the Sophisticated crafting total also counts the stock in the backpack storage that can keep refilling the grid, instead of only the materials currently in the grid.
- Items put into an ME interface config slot stack up to the per-slot cap; overflow stays in your hand.

### Improved

- ME interfaces and machine buffers now hold the configured cap per slot instead of a hard-coded 64.
- Mystical Agriculture machines such as the Infuser and Enchanter follow the configured cap instead of a hard-coded 64 / 512.
- In a Sophisticated backpack, chest or barrel, CraftingTweaks' "clear grid" returns the materials to your inventory and hotbar first.
- Counts adjust to the slot width: 1,234 keeps a comma, 10,100 becomes `10.1k`, 65,536 reads `65.5k`; wide text scales down.

### Fixed

- The count line in the tooltip now sits below the mod name instead of somewhere else.
- Oversized stacks in older saves no longer split into more items than they hold and are never truncated; inserting into a full slot is refused.

### Known limitations

- Shift batching is not available in Sophisticated Backpacks' Anvil upgrade tab on this version; the tab stays one item at a time. Batching on the vanilla anvil works.
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 20.1-1.0.3-beta — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：合成辅助的「均分」不再卡，Shift 批量合成多了个保险。

### 修复

- 合成格里出现超过 64 的堆叠时，均分改由本模组一次算完，点一下立刻分好，不再一个槽一个槽地试。
- 一次 Shift 点击默认最多合成 64 次（可配置），按住解锁键（默认鼠标侧键 4）才整批合完，仅作用于原版工作台与背包 2×2 合成格。
- 合成时客户端不再跟着重跑一遍逐次流程，几十万次配方扫描造成的长时间卡顿消失。

### 已知限制

- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: CraftingTweaks' balance button no longer stalls, and bulk crafting now has a safety limit.

### Fixed

- When the crafting grid holds stacks larger than 64, balancing is computed in one pass here, so a single click finishes the split instead of probing slot after slot.
- A shift-click now crafts at most 64 times by default (configurable); hold the unlock key (mouse button 4 by default) to finish the whole batch. Vanilla crafting table and the 2×2 grid only.
- The client no longer replays the one-craft-per-round loop, so the freeze caused by hundreds of thousands of recipe scans is gone.

### Known limitations

- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 20.1-1.0.2-beta — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：修掉点击合并不满一堆的问题。

### 修复
- 一次点击按槽内剩余空间一次并满，不再只并进槽里原有的数量。

### 已知限制

- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: fixes clicks that merged only a fraction of a stack.

### Fixed
- A click fills the whole remaining space instead of only the count already in the slot.

### Known limitations

- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 20.1-1.0.2-alpha — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠 · 可选模块（合成、显示）

**摘要**：修复铁砧与附魔台在提高堆叠上限后「一次换一整堆」的问题，并改成按个结算的批量操作。

### 修复
- 药水可堆叠后，酿造台原本会把整摞替换成 1 瓶（原版 mix 只返回 1 件），现在整摞一起酿完，不再丢物品。
- 铁砧的附魔与修复结果只出 1 个：原版会把左槽整堆复制成产物，现在这两类结果固定 1 个；改名仍按原版整堆产出。
- 铁砧取产物时按个结算：附魔与修复只取走左槽 1 个，剩余留在槽里，修复材料按实际用量扣除；改名维持原版，整堆一次取走。
- 附魔台每附一个扣一次经验：原版对整堆只扣一次经验（64 本书点一次全变附魔书），现在按经验与青金石付得起的个数结算，剩余物品留在槽里。
- COMPACT 模式下四位数会超出原版格子：缩写阈值上限收到 1,000（默认 1,000，可调低到 256），千位以上一律缩写。
- 酿造台药水槽原版写死最多 1 瓶，现在跟随药水堆叠上限（默认 4096），可以整摞放进去酿造。
- 1.20.1 专属：药水成摞后酿造台不启动——Forge 的酿造注册表只认单瓶配方，现在整摞判定改由本模组接管，原料够就会正常开酿。

### 新增

- 铁砧改成按件结算：一次只从左右两格各取 1 个，剩下的材料留在槽里。
- 铁砧支持 Shift 批量：一次最多 64 件，按住解锁键（默认鼠标侧键 4）连做到底。
- 新增「忽略过于昂贵」开关（默认开启）：所需等级达到 40 时原版会显示「过于昂贵」，打开开关后可以照常取走。
- 产物格显示这一批能做出多少（原版铁砧、工作台、AE 与精妙界面）：Shift 为 64 次，解锁键为做满。
- 提示框里显示「合计：X / 堆叠上限」，一眼看清这一批的总量与单格上限。
- 精妙铁砧页的「附魔花费」跟着这一批走：不按显示单次，按住 Shift 显示 64 次的总等级，按住解锁键显示材料能做满的全部。
- 精妙合成开启自动补货时，合计会把背包存储里能继续补货的库存一起算进去，不再只看合成格现有的材料。
- 往 ME 接口的配置格手动放物品，会叠到每格上限，放不下的留在手上。

### 优化

- ME 接口和机器缓冲的每格容量跟随本模组上限，不再是写死的 64。
- 神秘农业的注魔台、附魔台这类机器，槽位上限跟随配置，不再卡在写死的 64 / 512。
- 在精妙背包、箱子、桶里用合成辅助「清空网格」时，材料优先放回背包和快捷栏。
- 数量缩写按格子宽度自动调整：1,234 带逗号，10,100 起缩成 `10.1k`（65,536 → `65.5k`），过宽自动缩小。

### 修复

- 提示框里的数量行改到模组名下面，之前会显示到别的位置。
- 旧存档里超过上限的堆叠，不会再被拆出更多物品，也不会被截断；往满槽插入会被挡回。

### 已知限制

- 精妙背包「铁砧」升级页暂不支持 Shift 批量，仍是一次一件；原版铁砧的批量正常。
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: the anvil takes shift batches and shows the batch count on the slot.

### Added

- The anvil settles per item: one take pulls 1 item from each input, and the rest stays in the slots.
- The anvil supports shift batching: up to 64 takes at once, and holding the unlock key (mouse button 4) keeps going.
- New "ignore too expensive" toggle (on by default): vanilla stops at 40 levels with "Too Expensive"; with the toggle on the result can be taken as usual.
- The result slot shows what this batch would make on the vanilla anvil and crafting table, in AE and in Sophisticated screens: 64 takes with shift, everything with the unlock key.
- The tooltip adds "Total: X / stack limit" so the batch total and the per-slot cap are visible at once.
- In the Sophisticated anvil tab the level readout follows the batch: the single job normally, 64 times the cost with shift, and the whole material-limited batch while the unlock key is held.
- With auto-refill on, the Sophisticated crafting total also counts the stock in the backpack storage that can keep refilling the grid, instead of only the materials currently in the grid.
- Items put into an ME interface config slot stack up to the per-slot cap; overflow stays in your hand.

### Improved

- ME interfaces and machine buffers now hold the configured cap per slot instead of a hard-coded 64.
- Mystical Agriculture machines such as the Infuser and Enchanter follow the configured cap instead of a hard-coded 64 / 512.
- In a Sophisticated backpack, chest or barrel, CraftingTweaks' "clear grid" returns the materials to your inventory and hotbar first.
- Counts adjust to the slot width: 1,234 keeps a comma, 10,100 becomes `10.1k`, 65,536 reads `65.5k`; wide text scales down.

### Fixed

- The count line in the tooltip now sits below the mod name instead of somewhere else.
- Oversized stacks in older saves no longer split into more items than they hold and are never truncated; inserting into a full slot is refused.

### Known limitations

- Shift batching is not available in Sophisticated Backpacks' Anvil upgrade tab on this version; the tab stays one item at a time. Batching on the vanilla anvil works.
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## v20.1-1.0.3-hotfix1 — Minecraft 1.20.1

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

## v20.1-1.0.3-hotfix — Minecraft 1.20.1

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

## v20.1-1.0.3 — Minecraft 1.20.1 / Forge 47.4

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

## v20.1-1.0.2-hotfix — Minecraft 1.20.1 / Forge 47.4

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

## v20.1-1.0.2 — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：把一次点击的合并量放开，并让抽屉的取出量按配置结算。

### 修复
- 原版把一次点击能并进去的量写死成槽里原有的数量，上限抬到 65536 后只并一点点；现在按剩余空间一次并满。

**English**

**Modules**: UltraStacks

**Summary**: lets a single click merge a whole stack and keeps drawer extraction at the configured size.

### Fixed
- Vanilla capped a single merge at the slot's current count, which barely moved once the limit went to 65536; it now fills the remaining space in one go.

## v20.1-1.0.1-hotfix2 — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：修复上一版热修复在打开背包时仍会崩溃的问题。

### 修复
- 回调参数类型与 FastWorkbench 的目标方法不一致，打开背包仍会崩溃；现在按目标签名声明。

### 已知限制

- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: fixes the remaining crash with FastWorkbench when opening the inventory.

### Fixed
- The callback signature did not match FastWorkbench's target method; it now matches exactly.

### Known limitations

- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## v20.1-1.0.1-hotfix1 — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：修复与 FastWorkbench 一起使用时打开背包崩溃的问题。

### 修复
- 上一版 hotfix 在装了 FastWorkbench 的客户端打开背包会直接崩溃，现已修好。

### 已知限制

- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: fixes a crash with FastWorkbench when opening the inventory.

### Fixed
- The previous hotfix crashed clients with FastWorkbench installed as soon as the inventory was opened.

### Known limitations

- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## v20.1-1.0.1-hotfix — Minecraft 1.20.1 / Forge 47.4

**模块**：超级堆叠

**摘要**：热修复抽屉的取出与配方问题。

### 修复
- 抽屉一次会把整抽屉塞进背包，现在一次只取一组。
- 装了 FastWorkbench 时，Shift 合成一个大堆叠要一轮一轮地合，现在一次合完整批。

### Known limitations

- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

### 已知限制

- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

### Known limitations

- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 20.1-1.0.1 — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：加入游戏内配置界面，修掉抽屉与背包整理的数量问题。

### 新增
- 游戏内配置界面：NeoForge 自带的设置页；1.20.1 在装了 Cloth Config 时提供同样的界面，没装也能正常玩。

### 修复
- 功能性存储的抽屉显示能到 2,097,152，实际只吃到 2,048，取出来的整堆既不能堆叠也匹配不上配方（旧升级物品里固化了 64 的上限）。
- 精妙背包与精妙存储点「整理」后，整堆会变成 64 个，多出来的物品直接消失。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: adds an in-game config screen and fixes drawer and sorting counts.

### Added
- In-game config screen: NeoForge ships its own settings page; on 1.20.1 the same screen shows up when Cloth Config is installed.

### Fixed
- Functional Storage drawers reported room for 2,097,152 but only accepted 2,048, and the stacks pulled out could no longer stack or match recipes (old upgrade items carried a fixed 64 limit).
- Sorting a Sophisticated Backpack or Storage turned a full stack into 64 and dropped the rest.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 20.1-1.0.1-beta — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：加入游戏内配置界面，修掉精妙核心的上限联动。

### 新增
- 游戏内配置界面：装了 Cloth Config 时模组列表里会多出「配置」入口，可以图形化改所有选项；没装就自动跳过，模组本体照常工作。

### 修复
- 精妙存储与精妙背包的物品上限（`getBaseStackLimit`）没有跟上配置，排序与合并仍按旧值结算。

### Known limitations

- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

### 已知限制

- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

### Known limitations

- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 20.1-1.0.1-alpha — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠 · 可选模块（合成、显示）

**摘要**：加入合成与显示两个可选模块，超级堆叠行为不变。

### 新增
- 合成（可选模块，`crafting`）：原版工作台（3×3）与背包合成格（2×2）在 Shift 点击时一次合完整批，不再逐个循环；批量上限取材料与背包空间的较小值，余下的材料留在原处；`bulkCrafting` 关闭后恢复原版一次一件。
- 显示（可选模块，`display`）：大数量可缩写（`12k`、`3.4m`）或显示完整数字（`countStyle`、`abbreviateAbove`）；物品提示框可显示准确数量与堆叠上限（`tooltipExact`）；`display` 关闭后按原版绘制。
- 可选模块的开关与参数：`crafting`、`display` 两个总开关，`bulkCrafting`、`countStyle`、`abbreviateAbove`、`tooltipExact` 四项参数。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks · optional modules (crafting, display)

**Summary**: adds the crafting and display optional modules; the UltraStacks behaviour is unchanged.

### Added
- Crafting (optional module, `crafting`): shift-clicking the result of the 3x3 table or the 2x2 grid finishes the whole batch in one pass instead of the vanilla one-craft-per-round loop, capped by both materials and free space; leftovers stay in place. `bulkCrafting` off restores one craft per click.
- Display (optional module, `display`): large counts render abbreviated (`12k`, `3.4m`) or as the exact number (`countStyle`, `abbreviateAbove`); the item tooltip can show the exact count and the stack limit (`tooltipExact`). `display` off draws counts the vanilla way.
- Optional module switches and parameters: `crafting`, `display` master switches; `bulkCrafting`, `countStyle`, `abbreviateAbove`, `tooltipExact` parameters.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 20.1-1.0.0 — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：兼容更多模组，修掉铁砧、附魔台与酿造台的问题，并加上游戏内配置界面。

### 新增
**兼容**：
- 精妙核心。
- 通用机械。
- 通用动力。
- 应用能源 2。
- 精致存储。
- 更多熔炉。
- 神秘农业（含扩展与自动化）。
- 沉浸工程（含沉浸原油等附属）。

**兼容补丁**：按模组是否安装自动启用，没装的不加载。

- 游戏内配置界面：模组列表里直接改配置，需要 Cloth Config，没装不影响模组本身。
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
- Mekanism
- Mekanical Create
- Applied Energistics 2
- Refined Storage
- Iron Furnaces
- Mystical Agriculture (with addons)
- Immersive Engineering (with addons)

**Compat patches**: enabled only when the matching mod is installed.

- In-game config screen: change settings from the mod list. Needs Cloth Config; without it the mod is unaffected.
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

## 20.1-1.0.0-beta — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：兼容更多模组。

### 新增
**兼容**：
- 精妙核心。
- 通用机械。
- 通用动力。
- 应用能源 2。
- 精致存储。
- 更多熔炉。
- 神秘农业（含神秘农业扩展）。
- 沉浸工程（含沉浸原油等附属）。

**兼容补丁**：按模组是否安装自动启用，没装的模组不会加载。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: compatibility with more mods.

### Added
**Compatibility**:
- Sophisticated Core
- Mekanism
- Mekanical Create
- Applied Energistics 2
- Refined Storage
- Iron Furnaces
- Mystical Agriculture (with Mystical Agradditions)
- Immersive Engineering (with addons)

**Compat patches**: enabled only when the matching mod is installed.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 20.1-1.0.0-alpha — Minecraft 1.20.1 / Forge 47.4

**中文**

**模块**：超级堆叠

**摘要**：超级堆叠的第一个 alpha，只处理原版容器与槽位。

### 新增
- `ultrastacks-common.toml`：`maxStackSize`（默认 65536）、`scaleSmallItems`、`experimentalStackSizes`（解锁 65536 以上，最高 1073741823）。
- 注册阶段写入物品默认上限，跳过带耐久的物品。
- 容器与槽位上限跟随配置，且永远不小于槽里已有的数量。
- 存档数量上限、拖拽整数分摊、地面掉落合并、整堆掉落、酿造台槽位一并处理。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: first alpha of UltraStacks, covering vanilla containers and slots only.

### Added
- `ultrastacks-common.toml`: `maxStackSize` (default 65536), `scaleSmallItems`, `experimentalStackSizes` (unlocks values above 65536, up to 1073741823).
- Item limit applied at registration time; damageable items are skipped.
- Container and slot limits follow the configured value and never drop below the stack already inside.
- Saved counts, integer drag-splitting, entity merging, single-entity drops and brewing stand slots are handled as well.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

### Build
```
./gradlew build
```
JDK 17; produces `build/libs/UltraStacks-1.20.1-20.1-1.0.0-alpha.jar`.
