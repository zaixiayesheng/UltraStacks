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
 * 附魔台一次给整批付得起的物品上附魔。
 *
 * <p>原版把随机结果应用到整堆物品上，却只按展示的等级扣一次经验、扣掉与选项序号对应的青金石，
 * 于是一整堆 64 本书一次附魔全变附魔书。这里改成按件结算：件数取左槽数量、可用经验除以单件花费、
 * 青金石够付的件数三者的最小值，剩下的物品留在槽里；经验与青金石都按「单件花费 × 件数」扣。
 *
 * <p>单件花费是选项序号加一，也就是经验 per 级、青金石 per 个，展示的花费只用来判断这一档能不能买。
 * 背包放不下整批时直接返回 false，不会悄悄只放进去一部分。
 *
 * <p>Enchants a whole affordable batch in one go. Vanilla applies the roll to the entire stack but
 * charges the displayed levels once and spends only the lapis of the chosen option, so a stack of 64
 * books turns into 64 enchanted books for the price of one. Here the batch is the smallest of the slot
 * count, the levels divided by the per-item cost and the lapis divided by the same, the remainder stays
 * in the slot, and both levels and lapis are charged as the per-item cost times the batch size. The
 * per-item cost is the option index plus one, matching the lapis spent by that option, while the
 * displayed cost only gates whether the option is affordable at all. If the inventory cannot hold the
 * batch, the call returns false instead of silently delivering part of it.
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
