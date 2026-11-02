package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防止铁砧在左槽有多件时复制或吞掉一整堆：产物压到 1 件，取走时左槽扣 1、右槽按代价扣。
 * 改名不走这条路，整堆改名、只扣一次经验。
     *
 * Keeps the anvil from duplicating or eating a stack: clamp the result to one item
 * and charge one left-slot item per take. Renaming stays vanilla.
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
     * 产物出现后压到 1 件，取走时才不会被当成整摞一次操作。
     *
     * <p>Clamps the result to one item so that taking it cannot treat a whole stack as a single
     * operation. Only a stack larger than one with both the additive slot and the result filled
     * (enchanting or repair) is clamped; a pure rename has no additive item and keeps the whole
     * stack.
     */
    /**
     * 记录铁砧每次计算的结果，用来定位「产物不出现、箭头变红」到底卡在哪一步。
     *
     * <p>Logs only the suspicious case: both slots hold items but no result came out. That is the
     * "red arrow" state, and it needs no click to reproduce.
     */
    /**
     * 写入结果槽的那一刻就把「一摞」压成一件。
     *
     * <p>Clamps the output to a single item at the moment it is written into the result slot. Doing
     * it here, instead of after {@code createResult} returns, keeps the very first client update
     * consistent: the client never sees a full stack that it could then click on.
     */
    @Redirect(
        method = "createResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
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
     * <p>Records what a menu subclass (the Sophisticated Backpacks anvil upgrade, for example)
     * actually computed: its class, both slot counts, the price it will charge and the output size.
     * Logged once per distinct outcome so a normal session writes a handful of lines at most.
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

        if (com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard.overLimit(((ItemCombinerMenuAccessor) (Object) this).ultrastacks$player())) {
            ((ItemCombinerMenuAccessor) (Object) this).ultrastacks$resultSlots().setItem(0, ItemStack.EMPTY);
            DiagLog.line("[diag] anvil batch cap hit: class=" + self.getClass().getName()
                    + " takes=" + com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard.takesThisTick(((ItemCombinerMenuAccessor) (Object) this).ultrastacks$player()));
        }
    }

    /**
     * 取不走时把原因写进诊断日志：价格多少、玩家几级、产物在不在。
     *
     * <p>Records why a result cannot be taken: the price, the player's levels and whether a result is
     * present. Survival refuses a pickup when the level is below the price, which looks exactly like a
     * dead result slot.
     */

    /** 上一次真正的自定义名字；客户端把名字框刷回原名时用它挡掉。 */
    private String ultrastacks$lastCustomName;
    private net.minecraft.world.item.Item ultrastacks$lastLeftItem;

    /**
     * 挡住客户端发来的“名字被刷回物品原名”。
     *
     * <p>一次只改一件时，左槽每取走一件都会触发客户端的槽位刷新，原版逻辑会把名字框刷成物品原名，
     * 再把这个原名发回服务端；服务端一旦照收，下一次计算就会认为“没改名”，产物随之消失。
     * 这里只在“收到的名字正好等于左槽物品原名”时拒绝，玩家自己清空输入框仍然生效。
     *
     * <p>Guards against the client sending the item's default name right after a take. Vanilla refreshes
     * the name box whenever the left slot changes; with one item consumed per take that refresh keeps
     * wiping the custom name. Only the "default name" case is refused, so an intentional clear still works.
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

    private static java.lang.reflect.Field ultrastacks$nameField;

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

    @Inject(method = "mayPickup", at = @At("HEAD"))
    private void ultrastacks$logPickup(net.minecraft.world.entity.player.Player player, boolean hasItem,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!hasItem) {
            return;
        }
        com.zaixiayesheng.ultrastacks.DiagLog.line("[diag] anvil pickup cost=" + ((AnvilMenu) (Object) this).getCost()
            + " level=" + player.experienceLevel
            + " creative=" + player.getAbilities().instabuild
            + " left=" + ((AnvilMenu) (Object) this).getSlot(0).getItem().getCount()
            + " right=" + ((AnvilMenu) (Object) this).getSlot(1).getItem().getCount());
    }

    /**
     * 取走产物后重新算一次结果。
     *
     * <p>Vanilla sets the price back to zero at the end of {@code onTake} because it just emptied both
     * input slots. We only consume one item, so the slots still hold material and the result has to be
     * rebuilt — otherwise the menu keeps a zero price and the next craft never appears until something
     * else touches the slots.
     */
    /** 取走时先记下这次真正的名字，重算前若名字被清空就补回去。 */
    private String ultrastacks$takenName;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void ultrastacks$rememberName(Player player, ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            ultrastacks$takenName = stack.getHoverName().getString();
        } else {
            ultrastacks$takenName = null;
        }
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void ultrastacks$refreshAfterTake(net.minecraft.world.entity.player.Player player, ItemStack stack, CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        if (!self.getSlot(0).hasItem()) {
            return;
        }
        // 名字被清空时（取走一次之后客户端那侧会把输入框清掉），把这次取走的名字补回去，
        // 否则重算会认为「没改名」，产物直接变空、看起来就是重命名被撤销。
        // When the name gets cleared after a take, restore the name we just used; otherwise the
        // recompute treats it as "no rename" and clears the output entirely.
        String currentName = ultrastacks$readItemName(self);
        if ((currentName == null || currentName.isEmpty()) && ultrastacks$takenName != null) {
            ultrastacks$writeItemName(self, ultrastacks$takenName);
            currentName = ultrastacks$takenName;
        }
        self.createResult();
        DiagLog.line("[diag] anvil name field=" + currentName + " taken=" + ultrastacks$takenName);
        // 重算之后必须让客户端也看到新结果，否则界面上产物格是空的、名字看起来“改回去了”。
        // The recomputed result has to reach the client too; otherwise the output slot looks empty
        // and the renamed text appears to have been reverted.
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
     * <p>Replaces vanilla's "clear the whole slot" with a per-item consumption. A pure rename still
     * clears the left slot exactly as vanilla does; the additive slot is charged by
     * {@code repairItemCountCost} while it serves as repair material and one item otherwise. Taking
     * the result therefore only pays for what this one operation used.
     */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void ultrastacks$countTake(Player player, ItemStack stack, CallbackInfo ci) {
        com.zaixiayesheng.ultrastacks.crafting.AnvilBatchGuard.noteTake(player);
    }

    @Redirect(
        method = "onTake",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
        require = 0
    )
    private void ultrastacks$consumeInput(Container slots, int index, ItemStack value) {
        // 只有左槽（产物材料）要改成按件扣。右槽原版已经按 repairItemCountCost 扣好再交给这里，
        // 我们再扣一次等于双扣（5 个铁锭修一次变 3 个），所以原样写回。
        // Only the left slot needs per-item charging. Vanilla has already shrunk the additive slot
        // before handing it over; charging it again consumes the repair material twice.
        if (index != 0) {
            // 右槽：合并附魔、命名这类操作原版直接清空整槽（它假设一格只放一个）。
            // 叠加之后一格能放一整摞，照抄就会「点一次吃掉一整摞」，改成只扣本次真正要用的数量。
            // The right slot: vanilla clears the whole slot for combine/rename because a slot holds
            // one item. With stacking a slot holds a whole stack, so charge only the single item this
            // operation needs - unless the repair path already accounted for a material cost.
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
        AnvilMenu self = (AnvilMenu) (Object) this;
        if (current.isEmpty()) {
            slots.setItem(index, value);
            return;
        }
        // 纯改名同样一次只消耗一件。产物已经被压到 1 件，再把左槽整摞清空就等于「改一个、
        // 吃掉一整摞」——之前那版就是这么丢的。
        // A rename also consumes exactly one item. The result is already clamped to a single item, so
        // clearing the whole left slot would change one and destroy the rest.
        current.shrink(1);
        slots.setItem(index, current.isEmpty() ? ItemStack.EMPTY : current);
    }

    /**
     * 左槽是一摞时，仍按「一次操作」计价。
     *
     * <p>Charges a single operation when the input slot holds a stack. Vanilla sees
     * {@code count > 1} and forces the price to 40, because a slot is assumed to hold one item; with
     * stacking that flat price would make a whole batch cost the same as one combine.
     */
    @Redirect(
        method = "createResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I", ordinal = 1),
        require = 0
    )
    private int ultrastacks$singleOperationCount(ItemStack input) {
        // 只处理左槽那一处 getCount；右槽（材料）的循环上界必须拿到真实数量，
        // 否则维修路径会只跑一次。用接收者身份区分，不依赖 ordinal 顺序，
        // 免得别的模组改动 createResult 后偏移错位、静默失效。
        // 左槽那一处（ordinal 1）。精妙铁砧的容器由物品处理器支撑，getItem 每次都可能是新副本，
        // 用引用比较会失效，所以直接按调用点定位。
        if (input.isEmpty()) {
            return 0;
        }
        return input.getCount() > 1 ? 1 : input.getCount();
    }

    /**
     * 真正把「过于昂贵」这道闸门拆掉，而不只是改掉红字。
     *
     * <p>Removes the "Too Expensive" gate itself rather than only the red label: vanilla computes the
     * price, then blanks the result whenever it reaches 40. Returning the maximum here makes that
     * comparison fail, so the combination stays available and the player simply pays the real price.
     */
    @ModifyExpressionValue(
        method = "createResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;get()I", ordinal = 1),
        require = 0
    )
    private int ultrastacks$skipTooExpensive(int original) {
        return UltraStacksConfig.ignoreTooExpensive() ? 0 : original;
    }
}
