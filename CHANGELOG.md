# Changelog / 更新日志

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
