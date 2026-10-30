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
