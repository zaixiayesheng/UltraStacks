package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防止铁砧在左槽有多件时复制或吞掉一整堆：产物压到 1 件，取走时左槽扣 1、右槽按代价扣。
 * 改名不走这条路，整堆改名、只扣一次经验。
 *
 * <p>Keeps the anvil from duplicating or eating a stack: clamp the result to one item and charge one
 * left-slot item per take. Renaming stays vanilla.
 *
 * <p>1.20.1 的 {@code AnvilMenu} 里 {@code cost} 是 {@code private final DataSlot}，{@code createResult()}
 * 是 {@code public void}，{@code setItemName} 返回 {@code boolean}；这里全部按 1.20.1 的实际形状写。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    protected int repairItemCountCost;

    /**
     * 本次 createResult 里的左槽栈。
     *
     * <p>The left stack as it was read at the start of {@code createResult}. Some containers (the
     * Sophisticated Backpacks anvil upgrade is backed by an item handler) hand out a fresh copy from
     * {@code getItem} on every call, so comparing against a live {@code getSlot(0).getItem()} never
     * matches. Holding the reference the method itself captured keeps the identity check honest.
     */
    @Unique
    private ItemStack ultrastacks$leftAtCreate = ItemStack.EMPTY;

    @Unique
    private String ultrastacks$lastForeign = "";

    @Inject(method = "createResult", at = @At("HEAD"))
    private void ultrastacks$captureLeft(CallbackInfo ci) {
        ultrastacks$leftAtCreate = ((ItemCombinerMenuAccessor) (Object) this).ultrastacks$inputSlots().getItem(0);
    }

    /**
     * 写入结果槽的那一刻就把「一摞」压成一件。
     *
     * <p>Clamps the output to a single item at the moment it is written into the result slot. Doing it
     * here, instead of after {@code createResult} returns, keeps the very first client update
     * consistent: the client never sees a full stack that it could then click on.
     */
    @Redirect(
        method = "createResult",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
        require = 0
    )
    private void ultrastacks$clampOnSet(ResultContainer slots, int index, ItemStack value) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        if (!value.isEmpty() && value.getCount() > 1 && self.getSlot(0).getItem().getCount() > 1) {
            slots.setItem(index, value.copyWithCount(1));
            return;
        }
        slots.setItem(index, value);
    }

    /**
     * 记录铁砧每次计算的结果，用来定位「产物不出现、箭头变红」到底卡在哪一步。
     *
     * <p>Logs only the suspicious case: both slots hold items but no result came out. That is the
     * "red arrow" state, and it needs no click to reproduce.
     */
    @Inject(method = "createResult", at = @At("RETURN"))
    private void ultrastacks$logResult(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack left = self.getSlot(0).getItem();
        ItemStack right = self.getSlot(1).getItem();
        if (left.isEmpty() && right.isEmpty()) {
            return;
        }
        ItemStack out = self.getSlot(2).getItem();
        if (!out.isEmpty() || left.isEmpty() || right.isEmpty()) {
            return;
        }
        DiagLog.line("[diag] anvil empty result: left=" + left.getCount() + " right=" + right.getCount()
                + " cost=" + self.getCost() + " leftId=" + left.getItem() + " rightId=" + right.getItem());
    }

    /**
     * 记录非原版铁砧菜单的真实计价。
     *
     * <p>Records what a menu subclass (the Sophisticated Backpacks anvil upgrade, for example) actually
     * computed, and enforces the batch cap on it.
     */
    @Inject(method = "createResult", at = @At("RETURN"))
    private void ultrastacks$logForeignMenu(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        if (self.getClass() == AnvilMenu.class) {
            return;
        }
        ItemStack left = self.getSlot(0).getItem();
        if (left.getCount() <= 1) {
            return;
        }
        ItemStack right = self.getSlot(1).getItem();
        String line = "[diag] anvil subclass=" + self.getClass().getName()
                + " left=" + left.getCount() + " right=" + right.getCount()
                + " cost=" + self.getCost() + " out=" + self.getSlot(2).getItem().getCount();
        if (line.equals(ultrastacks$lastForeign)) {
            return;
        }
        ultrastacks$lastForeign = line;
        DiagLog.line(line);

        ItemCombinerMenuAccessor access = (ItemCombinerMenuAccessor) (Object) this;
        if (AnvilBatchGuard.overLimit(access.ultrastacks$player())) {
            access.ultrastacks$resultSlots().setItem(0, ItemStack.EMPTY);
            DiagLog.line("[diag] anvil batch cap hit: class=" + self.getClass().getName()
                    + " takes=" + AnvilBatchGuard.takesThisTick(access.ultrastacks$player()));
        }
    }

    /** 上一次真正的自定义名字；客户端把名字框刷回原名时用它挡掉。 */
    @Unique
    private String ultrastacks$lastCustomName;
    @Unique
    private net.minecraft.world.item.Item ultrastacks$lastLeftItem;

    /**
     * 挡住客户端发来的「名字被刷回物品原名」。
     *
     * <p>一次只改一件时，左槽每取走一件都会触发客户端的槽位刷新，原版逻辑会把名字框刷成物品原名，
     * 再把这个原名发回服务端；服务端一旦照收，下一次计算就会认为「没改名」，产物随之消失。
     * 这里只在「收到的名字正好等于左槽物品原名」时拒绝，玩家自己清空输入框仍然生效。
     *
     * <p>Guards against the client sending the item's default name right after a take.
     */
    @Inject(method = "setItemName", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$keepCustomName(String name, CallbackInfoReturnable<Boolean> cir) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack left = self.getSlot(0).getItem();
        if (left.isEmpty()) {
            ultrastacks$lastCustomName = null;
            ultrastacks$lastLeftItem = null;
            return;
        }
        if (ultrastacks$lastLeftItem != left.getItem()) {
            ultrastacks$lastLeftItem = left.getItem();
            ultrastacks$lastCustomName = null;
        }
        String defaultName = left.getHoverName().getString();
        if (name != null && !name.isEmpty() && !name.equals(defaultName)) {
            ultrastacks$lastCustomName = name;
            return;
        }
        if (name != null && name.equals(defaultName)
                && ultrastacks$lastCustomName != null && !ultrastacks$lastCustomName.isEmpty()
                && left.getCount() > 0) {
            cir.setReturnValue(false);
            DiagLog.line("[diag] anvil name keep: rejected=" + name + " kept=" + ultrastacks$lastCustomName
                    + " left=" + left.getCount());
        }
    }

    /** 1.20.1 的 {@code AnvilMenu.itemName} 是私有字段，这里反射读写用于取走后的名字回填。 */
    @Unique
    private static java.lang.reflect.Field ultrastacks$nameField;

    @Unique
    private static String ultrastacks$readItemName(AnvilMenu menu) {
        try {
            if (ultrastacks$nameField == null) {
                ultrastacks$nameField = AnvilMenu.class.getDeclaredField("itemName");
                ultrastacks$nameField.setAccessible(true);
            }
            Object v = ultrastacks$nameField.get(menu);
            return v == null ? null : v.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    @Unique
    private static void ultrastacks$writeItemName(AnvilMenu menu, String name) {
        try {
            if (ultrastacks$nameField == null) {
                ultrastacks$nameField = AnvilMenu.class.getDeclaredField("itemName");
                ultrastacks$nameField.setAccessible(true);
            }
            ultrastacks$nameField.set(menu, name);
        } catch (Throwable ignored) {
            // 反射失败就保持原样，不能因为诊断把游戏搞崩
        }
    }

    /**
     * 取不走时把原因写进诊断日志：价格多少、玩家几级、产物在不在。
     *
     * <p>Records why a result cannot be taken: the price, the player's levels and whether a result is
     * present. Survival refuses a pickup when the level is below the price, which looks exactly like a
     * dead result slot.
     */
    @Inject(method = "mayPickup", at = @At("HEAD"))
    private void ultrastacks$logPickup(Player player, boolean hasItem, CallbackInfoReturnable<Boolean> cir) {
        if (!hasItem) {
            return;
        }
        AnvilMenu self = (AnvilMenu) (Object) this;
        DiagLog.line("[diag] anvil pickup cost=" + self.getCost()
            + " level=" + player.experienceLevel
            + " creative=" + player.getAbilities().instabuild
            + " left=" + self.getSlot(0).getItem().getCount()
            + " right=" + self.getSlot(1).getItem().getCount());
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void ultrastacks$countTake(Player player, ItemStack stack, CallbackInfo ci) {
        AnvilBatchGuard.noteTake(player);
    }

    /** 取走时先记下这次真正的名字，重算前若名字被清空就补回去。 */
    @Unique
    private String ultrastacks$takenName;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void ultrastacks$rememberName(Player player, ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty() && stack.hasCustomHoverName()) {
            ultrastacks$takenName = stack.getHoverName().getString();
        } else {
            ultrastacks$takenName = null;
        }
    }

    /**
     * 取走产物后重新算一次结果。
     *
     * <p>Vanilla sets the price back to zero at the end of {@code onTake} because it just emptied both
     * input slots. We only consume one item, so the slots still hold material and the result has to be
     * rebuilt — otherwise the menu keeps a zero price and the next craft never appears until something
     * else touches the slots.
     */
    @Inject(method = "onTake", at = @At("TAIL"))
    private void ultrastacks$refreshAfterTake(Player player, ItemStack stack, CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        if (!self.getSlot(0).hasItem()) {
            return;
        }
        // 名字被清空时（取走一次之后客户端那侧会把输入框清掉），把这次取走的名字补回去，
        // 否则重算会认为「没改名」，产物直接变空、看起来就是重命名被撤销。
        String currentName = ultrastacks$readItemName(self);
        if ((currentName == null || currentName.isEmpty()) && ultrastacks$takenName != null) {
            ultrastacks$writeItemName(self, ultrastacks$takenName);
            currentName = ultrastacks$takenName;
        }
        self.createResult();
        DiagLog.line("[diag] anvil name field=" + currentName + " taken=" + ultrastacks$takenName);
        // 重算之后必须让客户端也看到新结果，否则界面上产物格是空的、名字看起来「改回去了」。
        self.broadcastChanges();
        DiagLog.line("[diag] anvil take: taken=" + stack.getCount() + "x" + stack.getHoverName().getString()
                + " left=" + self.getSlot(0).getItem().getCount()
                + " out=" + self.getSlot(2).getItem().getCount()
                + " outName=" + self.getSlot(2).getItem().getHoverName().getString()
                + " cost=" + self.getCost());
    }

    /**
     * 把原版的「清空整槽」换成按件扣输入。
     *
     * <p>Replaces vanilla's "clear the whole slot" with a per-item consumption. A pure rename also
     * consumes exactly one item; the additive slot is charged by {@code repairItemCountCost} while it
     * serves as repair material and one item otherwise.
     */
    @Redirect(
        method = "onTake",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
        require = 0
    )
    private void ultrastacks$consumeInput(Container slots, int index, ItemStack value) {
        // 只有左槽（产物材料）要改成按件扣。右槽原版已经按 repairItemCountCost 扣好再交给这里，
        // 我们再扣一次等于双扣，所以原样写回。
        if (index != 0) {
            // 右槽：合并附魔、命名这类操作原版直接清空整槽（它假设一格只放一个）。
            // 叠加之后一格能放一整摞，照抄就会「点一次吃掉一整摞」，改成只扣本次真正要用的数量。
            ItemStack right = slots.getItem(index);
            if (value.isEmpty() && repairItemCountCost <= 0 && right.getCount() > 1) {
                right.shrink(1);
                slots.setItem(index, right.isEmpty() ? ItemStack.EMPTY : right);
                return;
            }
            slots.setItem(index, value);
            return;
        }
        ItemStack current = slots.getItem(0);
        if (current.isEmpty()) {
            slots.setItem(index, value);
            return;
        }
        // 纯改名同样一次只消耗一件。产物已经被压到 1 件，再把左槽整摞清空就等于「改一个、
        // 吃掉一整摞」。
        current.shrink(1);
        slots.setItem(index, current.isEmpty() ? ItemStack.EMPTY : current);
    }

    /**
     * 左槽是一摞时，仍按「一次操作」计价。
     *
     * <p>Charges a single operation when the input slot holds a stack. Vanilla sees {@code count > 1}
     * and forces the price to 40, because a slot is assumed to hold one item.
     */
    @Redirect(
        method = "createResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I", ordinal = 1),
        require = 0
    )
    private int ultrastacks$singleOperationCount(ItemStack input) {
        if (input.isEmpty()) {
            return 0;
        }
        return input.getCount() > 1 ? 1 : input.getCount();
    }

    /**
     * 真正把「过于昂贵」这道闸门拆掉，而不只是改掉红字。
     *
     * <p>Removes the "Too Expensive" gate itself rather than only the red label: vanilla computes the
     * price, then blanks the result whenever it reaches 40. Returning zero here makes that comparison
     * fail, so the combination stays available and the player simply pays the real price.
     */
    @Redirect(
        method = "createResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;get()I", ordinal = 1),
        require = 0
    )
    private int ultrastacks$skipTooExpensive(DataSlot slot) {
        int value = slot.get();
        return UltraStacksConfig.ignoreTooExpensive() ? 0 : value;
    }
}
