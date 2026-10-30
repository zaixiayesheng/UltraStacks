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
