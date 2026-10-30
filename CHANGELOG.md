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

**English**

**Modules**: UltraStacks

**Summary**: fixes the remaining crash with FastWorkbench when opening the inventory.

### Fixed
- The callback signature did not match FastWorkbench's target method; it now matches exactly.

## v20.1-1.0.1-hotfix1 — Minecraft 1.20.1 / Forge 47.4

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

## v20.1-1.0.1-hotfix — Minecraft 1.20.1 / Forge 47.4

**模块**：超级堆叠

**摘要**：热修复抽屉的取出与配方问题。

### 修复
- 抽屉一次会把整抽屉塞进背包，现在一次只取一组。
- 装了 FastWorkbench 时，Shift 合成一个大堆叠要一轮一轮地合，现在一次合完整批。


# Changelog / 更新日志

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

## 20.1-1.0.0 — Minecraft 1.20.1 / Forge 47.4

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
