package com.zaixiayesheng.ultrastacks.mixin;

import java.util.List;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 附魔台：按「付得起几件」来附，而不是整摞只收一次钱。
 * 一次点击只结算一次，剩下的留在槽里。
     *
 * Enchanting table: enchant as many as the payment covers instead of charging
 * once for a whole stack. One click settles once, leftovers stay put.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private DataSlot enchantmentSeed;

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$bulkEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (id < 0 || id >= 3) {
            cir.setReturnValue(false);
            return;
        }
        EnchantmentMenu self = (EnchantmentMenu) (Object) this;
        int per = id + 1;
        int cost = self.costs[id];
        if (cost <= 0) {
            cir.setReturnValue(false);
            return;
        }
        ItemStack input = this.enchantSlots.getItem(0);
        ItemStack lapis = this.enchantSlots.getItem(1);
        if (input.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        boolean creative = player.getAbilities().instabuild;
        if (!creative && (player.experienceLevel < Math.max(per, cost) || lapis.getCount() < per)) {
            cir.setReturnValue(false);
            return;
        }

        int batch = input.getCount();
        if (!creative) {
            batch = Math.min(batch, player.experienceLevel / per);
            batch = Math.min(batch, lapis.getCount() / per);
        }
        if (batch <= 0) {
            cir.setReturnValue(false);
            return;
        }

        List<EnchantmentInstance> rolls = ((EnchantmentMenuAccessor) (Object) this)
            .ultrastacks$getEnchantmentList(input, id, cost);
        if (rolls == null || rolls.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        ItemStack template = input.copy();
        template.setCount(batch);
        ItemStack enchanted = template.is(net.minecraft.world.item.Items.BOOK)
            ? new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK, batch)
            : template;
        for (EnchantmentInstance instance : rolls) {
            enchanted.enchant(instance.enchantment, instance.level);
        }
        boolean consumesWholeStack = batch >= input.getCount();
        if (consumesWholeStack) {
            this.enchantSlots.setItem(0, enchanted);
        } else {
            if (freeSpace(player, enchanted) < batch) {
                cir.setReturnValue(false);
                return;
            }
            player.getInventory().add(enchanted);
            ItemStack rest = input.copy();
            rest.setCount(input.getCount() - batch);
            this.enchantSlots.setItem(0, rest);
        }

        if (!creative) {
            lapis.shrink(per * batch);
            if (lapis.isEmpty()) {
                this.enchantSlots.setItem(1, ItemStack.EMPTY);
            }
        }
        player.onEnchantmentPerformed(enchanted, per * batch);
        this.enchantmentSeed.set(player.getEnchantmentSeed());
        self.slotsChanged(this.enchantSlots);
        player.awardStat(Stats.ENCHANT_ITEM);
        cir.setReturnValue(true);
    }

    private static long freeSpace(Player player, ItemStack prototype) {
        Inventory inventory = player.getInventory();
        long free = 0L;
        int limit = Math.max(1, prototype.getMaxStackSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                free += limit;
            } else if (ItemStack.isSameItemSameTags(stack, prototype)) {
                free += Math.max(0, limit - stack.getCount());
            }
        }
        return free;
    }
}
