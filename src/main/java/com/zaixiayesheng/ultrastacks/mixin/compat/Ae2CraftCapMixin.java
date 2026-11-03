package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.crafting.BatchCounter;
import appeng.api.stacks.KeyCounter;
import com.zaixiayesheng.ultrastacks.crafting.Ae2LoopStop;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import appeng.api.storage.MEStorage;
import appeng.helpers.InventoryAction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE 合成终端的批量上限：每次“取走产物”记一份，下一次点击进门前如果已经到上限就不再执行。
 * AE 的 Shift 是一串独立的点击，所以在入口拦即可，不必改它内部的循环。
 *
 * <p>Batch cap for the AE crafting terminal: every taken result counts once, and the next click is
 * refused at the entry point once the cap is reached. AE's Shift is a series of separate clicks, so
 * gating the entry is enough and its internal loop stays untouched.
 */
@Mixin(targets = "appeng.menu.slot.CraftingTermSlot", remap = false)
public abstract class Ae2CraftCapMixin {
    @Inject(method = "doClick(Lappeng/helpers/InventoryAction;Lnet/minecraft/world/entity/player/Player;)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$capClick(InventoryAction action, Player player, CallbackInfo ci) {
        if (BatchCounter.atLimit(player.getUUID(), com.zaixiayesheng.ultrastacks.crafting.UnlockState.isUnlocked(player))) {
            DiagLog.lineThrottled("ae2.cap.stop", "[diag] ae2 cap: click refused at limit");
            ci.cancel();
        }
    }


    @Inject(method = "craftItem(Lnet/minecraft/world/entity/player/Player;Lappeng/api/storage/MEStorage;Lappeng/api/stacks/KeyCounter;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"), require = 0)
    private void ultrastacks$recordCraft(Player player, MEStorage storage, KeyCounter counter,
        CallbackInfoReturnable<ItemStack> cir) {
        // 真正能停住 AE 循环的一层：craftItem 返回空，它才会 break。
        // This is the layer that actually stops AE's loop: an empty result makes it break.
        if (com.zaixiayesheng.ultrastacks.crafting.Ae2LoopStop.touch()
            && com.zaixiayesheng.ultrastacks.crafting.Ae2LoopStop.count() > 0
            && player != null && player.containerMenu != null) {
            player.containerMenu.broadcastFullState();
            // 快捷栏属于玩家背包菜单，跟终端菜单不是同一个，必须也补发一次。
            // The hotbar lives in the player's inventory menu, which is a different menu from the terminal.
            player.inventoryMenu.broadcastFullState();
        }
        int cap = Math.max(1, UltraStacksConfig.bulkCraftLimit());
        boolean unlocked = UltraStacksConfig.bulkCraftUnlockKey()
            && com.zaixiayesheng.ultrastacks.crafting.UnlockState.isUnlocked(player);
        if (!unlocked && com.zaixiayesheng.ultrastacks.crafting.Ae2LoopStop.count() >= cap) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        com.zaixiayesheng.ultrastacks.crafting.Ae2LoopStop.record();

    }

    @Inject(method = "extractItemsByRecipe(Lappeng/api/networking/energy/IEnergySource;Lappeng/api/networking/security/IActionSource;Lappeng/api/storage/MEStorage;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/Recipe;Lnet/minecraft/world/item/ItemStack;IILjava/util/List;Lnet/minecraft/world/item/ItemStack;ILappeng/api/stacks/KeyCounter;Lappeng/util/prioritylist/IPartitionList;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"), cancellable = true, require = 0)
    private static void ultrastacks$stopLoop(CallbackInfoReturnable<ItemStack> cir) {
        // 取料不再单独限次，计数统一由 craftItem 负责。
        // Counting now lives in craftItem; nothing to gate here.
    }

    @Inject(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At("RETURN"), require = 0)
    private void ultrastacks$recordTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (stack != null && !stack.isEmpty()) {
            BatchCounter.record(player.getUUID());
        }
    }
}
