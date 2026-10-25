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
 * 一次附魔整批物品，按实际附到几件结算经验与青金石，而不是整堆只扣一次。
 *
 * <p>原版只掷一次附魔列表、把它扣在整个堆叠上，于是 64 本书花 1-3 级经验就全变成附魔书。这里接管
 * {@code clickMenuButton}：付得起的件数取「槽内数量」「经验等级 / 每次花费」「青金石 / 每次花费」三者的
 * 最小值（创造模式不设限），用原版的 {@code getEnchantmentList} 掷一次结果应用到这一批上，再按「每次
 * 花费 × 件数」扣经验与青金石。
 *
 * <p>整槽都付得起时直接把槽里换成本附魔后的堆叠；只付得起一部分时先看背包放不放得下，放不下就返回
 * false 让玩家先腾地方，绝不让附魔产物凭空消失。经验扣除、青金石消耗、附魔种子、{@code 附魔} 统计与
 * 容器同步都与原版一致。
 *
 * <p>Enchants a whole affordable batch in one go, charging experience and lapis for the items actually
 * enchanted instead of charging once for the entire stack.
 *
 * <p>Vanilla rolls the enchantment list once and applies it to the whole stack, so 64 books all become
 * enchanted books for 1-3 levels. This takes over {@code clickMenuButton}: the affordable batch is the
 * smallest of "items in the slot", "experience levels / cost per item" and "lapis / cost per item"
 * (creative mode is unlimited), the vanilla {@code getEnchantmentList} is rolled once and applied to that
 * batch, and experience and lapis are charged as "cost per item × batch size".
 *
 * <p>When the whole slot is affordable, the slot is simply replaced with the enchanted stack; when only
 * part of it is, the player's inventory is checked for space first, and if there is not enough the method
 * returns false so the player can make room — the enchanted result is never allowed to vanish. Experience
 * deduction, lapis consumption, the enchantment seed, the {@code enchant} statistic and the container
 * sync all behave as in vanilla.
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
