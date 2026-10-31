## 21.1-1.0.3-beta — Minecraft 1.21.1 / NeoForge 21.1

**中文**

**模块**：超级堆叠

**摘要**：合成辅助的「均分」不再卡，Shift 批量合成多了个保险。

### 修复

- 合成格里出现超过 64 的堆叠时，均分改由本模组一次算完，不再按格反复空转。
- 一次 Shift 点击默认最多合成 64 次（可配置），按住解锁键（默认鼠标侧键 4）才整批合完，仅作用于原版工作台与背包 2×2 合成格。
- 合成时客户端不再跟着重跑一遍逐次流程，几十万次配方扫描造成的长时间卡顿消失。

**English**

**Modules**: UltraStacks

**Summary**: CraftingTweaks' balance button no longer stalls, and bulk crafting now has a safety limit.

### Fixed

- When the crafting grid holds stacks larger than 64, balancing is computed in one pass here instead of looping over the slots.
- A shift-click now crafts at most 64 times by default (configurable); hold the unlock key (mouse button 4 by default) to finish the whole batch. Vanilla crafting table and the 2×2 grid only.
- The client no longer replays the one-craft-per-round loop, so the freeze caused by hundreds of thousands of recipe scans is gone.

## 21.1-1.0.2-beta — Minecraft 1.21.1 / NeoForge 21.1

**中文**

**模块**：超级堆叠

**摘要**：修掉抽屉容量与取出量对不上、取出的物品合不上、整理把大堆拆小这几件事。

### 修复
- 抽屉容量与单次取出量改按配置结算，界面显示多少就能真放多少。
- 从抽屉取出的物品会剥掉旧存档留下的堆叠上限标记，能和普通物品正常合并。
- 内容完全相同的两堆不再因为默认组件表对象不同被判成两种物品。
- 一次点击按槽内剩余空间一次并满，不再只并进槽里原有的数量。
- 整理时物品侧与槽位侧报同一个上限，大堆不再被切成 64。
- 精妙存储与精妙背包的槽位上限跟随配置，合并与整理按新值结算。
- 读档时把旧存档里数量为负的抽屉格子归零。

**English**

**Modules**: UltraStacks

**Summary**: fixes drawer capacity and extraction amounts, merging of extracted items, and sorting that split large stacks.

### Fixed
- Drawer capacity and single-extraction amount now follow the config, so what the GUI shows is what it holds.
- Items taken out of a drawer have the old stack-limit marker stripped, so they merge normally again.
- Two identical stacks are no longer treated as different items because their default component map objects differ.
- A click fills the whole remaining space instead of only the count already in the slot.
- Sorting reports the same limit on both the item and slot side, so large stacks are no longer cut into 64s.
- Sophisticated Storage and Backpacks slot limits follow the config for merging and sorting.
- Drawer slots holding a negative count are reset on load.

## 21.1-1.0.2-alpha — Minecraft 1.21.1 / NeoForge 21.1

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

### 新增
- 批量酿造：一个 400 tick 周期按原料结算，每个原料折算 `brewingYield` 瓶（默认 3，与原版 1 个原料出 3 瓶一致）；一个瓶位必须一次付清整摞所需的原料才会开酿，凑不够就整个周期不开始，原料和瓶子都不会被动过；周期与烈焰粉结算不变。
- 铁砧批量：Shift 点击产物时一次结算全部可合成数量，上限取「左槽数量、右槽可用量、经验可结算的次数、产物堆叠上限」的最小值；剩余材料保留。改名不参与批量，走原版一次性结算。
- 附魔批量：一次点击结算所有付得起的个数，按个数扣经验与青金石；不足时只附付得起的那部分。
- 药水与附魔书可堆叠：`potionStackLimit` 与 `enchantedBookStackLimit` 默认 0＝自动，即堆叠上限的 1/16（默认 65536 时为 4096），两个键都能改成固定值或 1（保持原版）；其余单件物品、工具与护甲保持 1。
- 各种桶共用一个上限并跟随空桶（默认 16,384）。
- 创造模式：铁砧不扣经验、附魔台不扣材料，按堆叠上限结算。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks · optional modules (crafting, display)

**Summary**: anvil and enchanting now charge per item and settle the whole affordable batch at once.

### Fixed
- Brewing a stacked potion used to cut the whole stack down to a single bottle (vanilla mix returns one item); the stack is now brewed as a whole.
- The anvil yields a single item for enchanting and repair instead of copying the whole left stack; renaming still renames the whole stack.
- Taking an enchanting or repair result consumes one input, leftovers stay in the slot and repair materials are consumed by their real amount; renaming keeps the vanilla whole-stack settlement.
- Enchanting charges experience per item: vanilla charged experience once for the entire stack (64 books became 64 enchanted books in one click).
- Four-digit counts no longer overflow a vanilla slot: the abbreviation threshold is capped at 1,000 (default 1,000, adjustable down to 256), so a thousand and above is always abbreviated.
- The brewing stand potion slots were hardcoded to a single bottle; they now follow the potion limit (4096 by default), so a whole stack can sit in the stand and brew at once.

### Added
- Brewing batches: one 400 tick cycle settles by reagent supply, with each reagent converting `brewingYield` bottles (three by default, the vanilla rate); a bottle slot only brews when the whole stack's reagents are paid up front at once; if the reagent runs short the cycle does not start and both the reagent and the bottles stay untouched.
- Anvil batching on shift-click, capped by the left slot count, the additive supply, the affordable experience and the output stack limit; leftover materials stay in place. Renaming is excluded and keeps the vanilla single charge.
- Enchantment batching: one click settles every affordable item, priced per item; short experience or lapis only enchants the affordable part.
- Potions and enchanted books can stack: `potionStackLimit` and `enchantedBookStackLimit` default to 0 = automatic, one sixteenth of the stack limit (4096 at the default 65536); both accept a fixed value or 1 for vanilla behaviour, while other single items, tools and armour stay at 1.
- All buckets share one limit that follows the empty bucket (16,384 by default).
- Creative mode skips experience (anvil) and materials (enchanting) and settles up to the stack limit.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## v21.1-1.0.1-hotfix2 — Minecraft 1.21.1 / NeoForge 21.1

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

## v21.1-1.0.1-hotfix1 — Minecraft 1.21.1 / NeoForge 21.1

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

## v21.1-1.0.1-hotfix — Minecraft 1.21.1 / NeoForge 21.1

**模块**：超级堆叠

**摘要**：热修复抽屉的取出与配方问题。

### 修复
- 抽屉里存着超大数量时，Shift 点击一次会把整个抽屉塞进背包；现在一次只取一组。
- 从抽屉取出的物品带着「上限 64」的旧组件，导致 JEI 与配方匹配失败；取用时会把这种组件清掉。
- 装了 FastWorkbench 时，Shift 合成一个大堆叠要一轮一轮地合，现在一次合完整批。

# Changelog / 更新日志

## 21.1-1.0.1 — Minecraft 1.21.1 / NeoForge 21.1

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

## 21.1-1.0.1-beta — Minecraft 1.21.1 / NeoForge 21.1

**中文**

**模块**：超级堆叠

**摘要**：加入游戏内配置界面，修掉上限换算与精妙核心的上限联动。

### 新增
- 游戏内配置界面：直接复用 NeoForge 自带的设置页，模组列表里点「配置」即可修改，不用手动编辑 TOML。

### 修复
- 原版默认堆叠（2、4、8、16、32、64）的物品，只在数量超过默认值时才按比例放大；此前一律按比例换算，16 大小的物品在部分场景下会被算成四分之一上限。
- 精妙存储与精妙背包的物品上限（`getBaseStackLimit`）没有跟上配置，排序与合并仍按旧值结算。

**English**

**Modules**: UltraStacks

**Summary**: adds an in-game config screen and fixes limit conversion and Sophisticated core limits.

### Added
- In-game config screen: reuses NeoForge's built-in settings page, reachable from the mod list's "Config" button, so the TOML no longer has to be edited by hand.

### Fixed
- Items with a vanilla default stack size (2, 4, 8, 16, 32, 64) are only scaled up once their count exceeds that default; previously everything was scaled, so 16-size items could end up with a quarter of the cap in some paths.
- Sophisticated Storage and Backpacks item limits (`getBaseStackLimit`) ignored the configured cap, so sorting and merging still used the old value.

## 21.1-1.0.1-alpha — Minecraft 1.21.1 / NeoForge 21.1

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

## 21.1-1.0.0 — Minecraft 1.21.1 / NeoForge 21.1

**中文**

**模块**：超级堆叠

**摘要**：兼容更多模组，修掉铁砧、附魔台与酿造台的问题，并加上游戏内配置界面。

### 新增
**兼容**：
- 精妙核心
- 通用机械
- 通用动力
- 应用能源 2
- 精致存储
- 更多熔炉
- 神秘农业（含扩展与自动化）
- 沉浸工程（含沉浸原油等附属）

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
- Mekanism
- Mekanical Create
- Applied Energistics 2
- Refined Storage
- Iron Furnaces
- Mystical Agriculture (with addons)
- Immersive Engineering (with addons)

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

## 21.1-1.0.0-beta — Minecraft 1.21.1 / NeoForge 21.1

**中文**

**模块**：超级堆叠

**摘要**：兼容更多模组。

### 新增
**兼容**：
- 精妙核心
- 通用机械
- 通用动力
- 应用能源 2
- 精致存储
- 更多熔炉
- 神秘农业（含扩展与自动化）
- 沉浸工程（含沉浸原油等附属）

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
- Mystical Agriculture (with addons)
- Immersive Engineering (with addons)

**Compat patches**: enabled only when the matching mod is installed.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 21.1-1.0.0-alpha — Minecraft 1.21.1 / NeoForge 21.1

**中文**

**模块**：超级堆叠

**摘要**：超级堆叠的第一个 alpha，只处理原版容器与槽位。

### 新增
- `ultrastacks-common.toml`：`maxStackSize`（默认 65536）、`scaleSmallItems`、`experimentalStackSizes`（解锁 65536 以上，最高 1073741823）。
- 注册阶段写入物品默认上限，跳过带耐久的物品。
- 容器与槽位上限跟随配置，且永远不小于槽里已有的数量。
- 存档数量上限、拖拽整数分摊、地面掉落合并与整堆掉落一并处理。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks

**Summary**: first alpha of UltraStacks, covering vanilla containers and slots only.

### Added
- `ultrastacks-common.toml`: `maxStackSize` (default 65536), `scaleSmallItems`, `experimentalStackSizes` (unlocks values above 65536, up to 1073741823).
- Item limit applied at registration time; damageable items are skipped.
- Container and slot limits follow the configured value and never drop below the stack already inside.
- Saved counts, integer drag-splitting, entity merging and single-entity drops are handled as well.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

### Build
```
./gradlew build
```
JDK 21; produces `build/libs/UltraStacks-1.21.1-21.1-1.0.0-alpha.jar`.
