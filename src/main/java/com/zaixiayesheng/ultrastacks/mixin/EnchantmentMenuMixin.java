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
 * 附魔台：按「付得起几件」附魔，而不是整摞只收一次钱。
 *
 * <p>原版把一次 rolls 应用到整个输入堆叠，却只扣一次经验和青金石：64 本书点一下全变附魔书，
 * 代价仍是最低档的 1 级。这里先算出经验与青金石付得起的件数 {@code batch}（创造模式不限），
 * 取一次 rolls 后套用到整批，剩余物品留在槽里。一次点击只结算一次，花费随件数增长，而不是随点击次数。
 *
 * <p>Enchants as many items as the player can pay for instead of enchanting the whole stack for a
 * single payment. Vanilla applies one roll to the entire input stack and charges one batch of
 * experience and lapis: a stack of 64 books becomes 64 enchanted books for the cheapest tier.
 * This hook first computes {@code batch}, the number of items the player's levels and lapis cover
 * (unlimited in creative), takes one roll, applies it to that batch, and leaves the rest in the
 * slot. A click is settled once, so the cost scales with the number of items rather than with the
 * number of clicks.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private DataSlot enchantmentSeed;

    /**
     * 取代原版的附魔按钮逻辑：校验、算批量、整批附魔、扣费。
     *
     * <p>顺序是：按钮 id 0..2（对应 1 到 3 级档位）→ 输入槽非空 → 生存模式下经验至少
     * {@code max(档位, 花费)} 且青金石至少 {@code 档位} → 批量受输入数量、经验、青金石三者限制 →
     * 取一次 rolls → 整批套用。整批正好覆盖输入时直接替换槽内物品，否则先确认背包放得下，再放入并缩减输入槽。
     * 生存模式按 {@code 档位 × 批量} 扣青金石，经验交给 {@code onEnchantmentPerformed}。
     *
     * <p>注意这条路径整段替换了原版流程，原版的附魔音效与 NeoForge 的
     * {@code CommonHooks.onPlayerEnchantItem} 不会执行。
     *
     * <p>Replaces the vanilla enchanting button logic: validate, compute the batch, enchant it, and
     * charge for it. The order is: button id 0..2 (tiers 1 to 3), non-empty input slot, and in
     * survival at least {@code max(tier, cost)} levels and {@code tier} lapis. The batch is then
     * bounded by the input count, the levels and the lapis, one roll is taken, and the whole batch is
     * enchanted. When the batch covers the entire input the slot is replaced outright; otherwise the
     * inventory must have room for the result, which is added while the input is shrunk. Survival pays
     * {@code tier * batch} lapis, while experience is charged through
     * {@code onEnchantmentPerformed}. This path replaces the vanilla flow wholesale, so vanilla's
     * enchanting sound and NeoForge's {@code CommonHooks.onPlayerEnchantItem} do not run.
     */
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

    /**
     * 背包还能装下多少个这种物品：空格按物品自身的上限计，同类堆叠按剩余空间计。
     *
     * <p>只用来确认这批附魔产物能整体进背包——装不下就放弃这次点击，而不是让物品卡在槽里。
     *
     * <p>How many more of this item the inventory can take: empty slots count the item's own limit and
     * matching stacks count their remaining room. It only checks that the enchanted batch fits into
     * the inventory; if it does not, the click is refused rather than leaving items behind in the
     * slot.
     */
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
