# Changelog / 更新日志

## 21.1-1.0.1-alpha — Minecraft 1.21.1 / NeoForge 21.1

**中文**：加入两个独立模块，内核行为不变。

### 新增
- 合成模块：原版工作台（3x3）与背包合成格（2x2）在 Shift 点击时一次合完整批，不再逐件循环；按材料与背包空间取较小值，剩余材料与产物严格守恒。
- 显示模块：大数量可缩写（`12k`、`3.4m`）或显示完整数字；物品提示框可显示准确数量与堆叠上限。
- 模块开关与参数：`crafting`、`display` 两个总开关，`bulkCrafting`、`countStyle`、`abbreviateAbove`、`tooltipExact` 四项参数。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**: two standalone modules; the core stack-limit behaviour is unchanged.

### Added
- Crafting module: shift-clicking the result of the 3x3 table or the 2x2 grid finishes the whole batch in one pass instead of the vanilla one-craft-per-round loop, capped by both materials and free space.
- Display module: large counts render abbreviated (`12k`, `3.4m`) or as the exact number; the item tooltip can show the exact count and the stack limit.
- Module switches and parameters: `crafting`, `display`, `bulkCrafting`, `countStyle`, `abbreviateAbove`, `tooltipExact`.

### Known limitations
- Counts above 1073741823 are rejected by the item codec; such stacks load with count 1 instead of crashing.


## 21.1-1.0.0-alpha — Minecraft 1.21.1 / NeoForge 21.1

**中文**：堆叠上限内核的第一个 alpha，只处理原版容器与槽位。

### 新增
- `ultrastacks-common.toml`：`maxStackSize`（默认 65536）、`scaleSmallItems`、`experimentalStackSizes`（解锁 65536 以上，最高 1073741823）。
- 注册阶段写入物品默认上限，跳过带耐久的物品。
- 容器与槽位上限跟随配置，且永远不小于槽里已有的数量。
- 存档数量上限、拖拽整数分摊、地面掉落合并、整堆掉落、酿造台槽位一并处理。

### 已知限制
- 超过 1073741823 的数量会被物品编解码拒绝，读档时按 1 个加载，不会崩溃。

**English**: first alpha of the stack-limit core, covering vanilla containers and slots only.

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
JDK 21; produces `build/libs/UltraStacks-1.21.1-21.1-1.0.0-alpha.jar`.
