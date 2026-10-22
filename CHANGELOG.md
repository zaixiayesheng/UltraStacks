# Changelog / 更新日志

## 26.1-1.0.2-alpha — Minecraft 26.1.2 / NeoForge 26.1

**中文**

**模块**：超级堆叠 · 合成模块 · 显示模块

**摘要**：修复铁砧与附魔台在提高堆叠上限后「一次换一整堆」的问题，并改成按个结算的批量操作。

### 修复
- 药水可堆叠后，酿造台原本会把整摞替换成 1 瓶（原版 mix 只返回 1 件），现在整摞一起酿完，不再丢物品。
- 铁砧的附魔与修复结果只出 1 个：原版会把左槽整堆复制成产物，现在这两类结果固定 1 个；改名仍按原版整堆产出。
- 铁砧取产物时按个结算：附魔与修复只取走左槽 1 个，剩余留在槽里，修复材料按实际用量扣除；改名维持原版，整堆一次取走。
- 附魔台每附一个扣一次经验：原版对整堆只扣一次经验（64 本书点一次全变附魔书），现在按经验与青金石付得起的个数结算，剩余物品留在槽里。
- COMPACT 模式下四位数会超出原版格子：缩写阈值上限收到 1,000（默认 1,000，可调低到 256），千位以上一律缩写。
- 酿造台药水槽原版写死最多 1 瓶，现在跟随药水堆叠上限（默认 4096），可以整摞放进去酿造。

### 新增
- 批量酿造：一个 400 tick 周期按原料结算，每个原料折算 `brewingYield` 瓶（默认 3，与原版 1 个原料出 3 瓶一致）；原料不够时只酿付得起的那部分，余下的瓶子留在槽里；周期与烈焰粉结算不变。
- 铁砧批量：Shift 点击产物时一次结算全部可合成数量，上限取「左槽数量、右槽可用量、经验可结算的次数、产物堆叠上限」的最小值；剩余材料保留。改名不参与批量，走原版一次性结算。
- 附魔批量：一次点击结算所有付得起的个数，按个数扣经验与青金石；不足时只附付得起的那部分。
- 药水与附魔书可堆叠：`potionStackLimit` 与 `enchantedBookStackLimit` 默认 0＝自动，即堆叠上限的 1/16（默认 65536 时为 4096），两个键都能改成固定值或 1（保持原版）；其余单件物品、工具与护甲保持 1。
- 各种桶共用一个上限并跟随空桶（默认 16,384）。
- 创造模式：铁砧不扣经验、附魔台不扣材料，按堆叠上限结算。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks · Crafting · Display

**Summary**: anvil and enchanting now charge per item and settle the whole affordable batch at once.

### Fixed
- Brewing a stacked potion used to cut the whole stack down to a single bottle (vanilla mix returns one item); the stack is now brewed as a whole.
- The anvil yields a single item for enchanting and repair instead of copying the whole left stack; renaming still renames the whole stack.
- Taking an enchanting or repair result consumes one input, leftovers stay in the slot and repair materials are consumed by their real amount; renaming keeps the vanilla whole-stack settlement.
- Enchanting charges experience per item: vanilla charged experience once for the entire stack (64 books became 64 enchanted books in one click).
- Four-digit counts no longer overflow a vanilla slot: the abbreviation threshold is capped at 1,000 (default 1,000, adjustable down to 256), so a thousand and above is always abbreviated.
- The brewing stand potion slots were hardcoded to a single bottle; they now follow the potion limit (4096 by default), so a whole stack can sit in the stand and brew at once.

### Added
- Brewing batches: one 400 tick cycle settles by reagent supply, with each reagent converting `brewingYield` bottles (three by default, the vanilla rate); short supplies only brew the affordable part and the rest stays in the slot.
- Anvil batching on shift-click, capped by the left slot count, the additive supply, the affordable experience and the output stack limit; leftover materials stay in place. Renaming is excluded and keeps the vanilla single charge.
- Enchantment batching: one click settles every affordable item, priced per item; short experience or lapis only enchants the affordable part.
- Potions and enchanted books can stack: `potionStackLimit` and `enchantedBookStackLimit` default to 0 = automatic, one sixteenth of the stack limit (4096 at the default 65536); both accept a fixed value or 1 for vanilla behaviour, while other single items, tools and armour stay at 1.
- All buckets share one limit that follows the empty bucket (16,384 by default).
- Creative mode skips experience (anvil) and materials (enchanting) and settles up to the stack limit.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 26.1-1.0.1-alpha — Minecraft 26.1.2 / NeoForge 26.1

**中文**

**模块**：超级堆叠 · 合成模块 · 显示模块

**摘要**：加入合成模块与显示模块，超级堆叠行为不变。

### 新增
- 合成模块：原版工作台（3x3）与背包合成格（2x2）在 Shift 点击时一次合完整批，不再逐个循环；按材料与背包空间取较小值，余下的材料留在原处。
- 显示模块：大数量可缩写（`12k`、`3.4m`）或显示完整数字；物品提示框可显示准确数量与堆叠上限。
- 模块开关与参数：`crafting`、`display` 两个总开关，`bulkCrafting`、`countStyle`、`abbreviateAbove`、`tooltipExact` 四项参数。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**

**Modules**: UltraStacks · Crafting · Display

**Summary**: crafting and display modules; the UltraStacks behaviour is unchanged.

### Added
- Crafting module: shift-clicking the result of the 3x3 table or the 2x2 grid finishes the whole batch in one pass instead of the vanilla one-craft-per-round loop, capped by both materials and free space; leftovers stay in place.
- Display module: large counts render abbreviated (`12k`, `3.4m`) or as the exact number; the item tooltip can show the exact count and the stack limit.
- Module switches and parameters: `crafting`, `display`, `bulkCrafting`, `countStyle`, `abbreviateAbove`, `tooltipExact`.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.

## 26.1-1.0.0-alpha — Minecraft 26.1.2 / NeoForge 26.1

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
JDK 25; produces `build/libs/UltraStacks-26.1.2-26.1-1.0.0-alpha.jar`.
