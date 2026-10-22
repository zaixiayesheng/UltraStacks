package com.zaixiayesheng.ultrastacks.mixin;

import java.util.List;
import net.minecraft.core.RegistryAccess;
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
 * Enchants a whole affordable batch for the displayed experience cost instead of enchanting the full stack
 * for the experience of a single item.
 *
 * <p>Vanilla applies the roll to the entire input stack, so a stack of 64 books turned into 64
 * enchanted books for 1-3 levels. UltraStacks enchants as many items as the player can pay for and
 * leaves the rest in the slot; the batch is settled once, so the cost does not grow with clicks.
 *
 * <p><b>中文</b> 原版会给整堆物品上附魔却只扣一次经验（64 本书一次附魔全变附魔书）。本补丁改成
 * 「经验与青金石付得起几件就附几件」，剩余物品留在槽里，一次结算，不影响性能。
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
            .ultrastacks$getEnchantmentList(player.level().registryAccess(), input, id, cost);
        if (rolls == null || rolls.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        ItemStack template = input.copyWithCount(batch);
        ItemStack enchanted = template.getItem().applyEnchantments(template, rolls);
        boolean consumesWholeStack = batch >= input.getCount();
        if (consumesWholeStack) {
            this.enchantSlots.setItem(0, enchanted);
        } else {
            if (freeSpace(player, enchanted) < batch) {
                cir.setReturnValue(false);
                return;
            }
            player.getInventory().add(enchanted);
            this.enchantSlots.setItem(0, input.copyWithCount(input.getCount() - batch));
        }

        if (!creative) {
            lapis.consume(per * batch, player);
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
            } else if (ItemStack.isSameItemSameComponents(stack, prototype)) {
                free += Math.max(0, limit - stack.getCount());
            }
        }
        return free;
    }
}
