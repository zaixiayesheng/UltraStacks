package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 药水可堆叠后，酿造台仍按原版原料比例整摞酿造。
 *
 * <p>原版用 {@code PotionBrewing.mix} 返回的单瓶药水替换整个瓶位，十六瓶会被砍成一瓶。这里只在
 * 「整摞都装得下」且「原料够用」时才转换一个瓶位：每个原料按 {@code brewingYield} 折算（默认 3 瓶，
 * 与原版一致），所需原料数向上取整。装不下或原料不够的瓶位保持原样、不消耗原料，所以不会出现
 * 只酿出几瓶、剩下的被覆盖的丢物品。400 tick 周期与烈焰粉结算保持原版；{@code potionStackLimit}
 * 为 1 时整个钩子让位给原版。
 *
 * <p>Brews stacked potions at the vanilla reagent rate, converting a bottle slot only as a whole.
 * Vanilla replaces each bottle slot with the single potion returned by {@code PotionBrewing.mix},
 * so a slot of sixteen bottles would be cut down to one. This hook converts a slot only when the
 * whole stack fits the mixed potion and the reagent slot holds enough reagent at
 * {@code brewingYield} bottles per reagent (three by default, the vanilla rate), rounding the
 * reagent count up. A slot that cannot be converted as a whole is left untouched and spends no
 * reagent, so a stack is never partially destroyed. The four-hundred-tick cycle and the fuel
 * accounting stay vanilla, and with {@code potionStackLimit} left at one the hook steps aside
 * entirely.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {

    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$bulkBrew(Level level, BlockPos pos, NonNullList<ItemStack> items, CallbackInfo ci) {
        if (UltraStacksConfig.potionStackLimit() <= 1) {
            return;
        }
        if (EventHooks.onPotionAttemptBrew(items)) {
            ci.cancel();
            return;
        }
        ItemStack ingredient = items.get(3);
        PotionBrewing brewing = level.potionBrewing();
        if (ingredient.isEmpty() || !brewing.isIngredient(ingredient)) {
            return;
        }

        int yield = UltraStacksConfig.brewingYield();
        int pool = ingredient.getCount();
        int consumed = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack potion = items.get(i);
            if (potion.isEmpty() || !brewing.hasMix(potion, ingredient)) {
                continue;
            }
            ItemStack mixed = brewing.mix(ingredient, potion.copyWithCount(1));
            if (mixed.isEmpty() || ItemStack.isSameItemSameComponents(mixed, potion)) {
                continue;
            }
            int bottles = potion.getCount();
            if (bottles > mixed.getMaxStackSize()) {
                continue;
            }
            int need = (bottles + yield - 1) / yield;
            if (need > pool) {
                continue;
            }
            items.set(i, mixed.copyWithCount(bottles));
            pool -= need;
            consumed += need;
        }

        if (consumed > 0) {
            EventHooks.onPotionBrewed(items);
            boolean hasRemainder = ingredient.hasCraftingRemainingItem();
            ItemStack remainder = hasRemainder ? ingredient.getCraftingRemainingItem() : ItemStack.EMPTY;
            ingredient.shrink(consumed);
            items.set(3, ingredient);
            if (hasRemainder && !remainder.isEmpty()) {
                for (int i = 0; i < consumed; i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.copy());
                }
            }
            level.levelEvent(1035, pos, 0);
        }
        ci.cancel();
    }

    @Inject(method = "isBrewable", at = @At("RETURN"), cancellable = true)
    private static void ultrastacks$requireWholeStackReagents(PotionBrewing brewing, NonNullList<ItemStack> items,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (UltraStacksConfig.potionStackLimit() <= 1) {
            return;
        }
        cir.setReturnValue(canBrewAny(brewing, items));
    }

    /**
     * 至少有一个瓶位能用现有原料整摞转换时返回 {@code true}。
     *
     * <p>判定与 {@code doBrew} 里的循环一致：药水非空、原料能与它混合、混合结果确实有变化、整摞装得下、
     * 留下的原料还够用。{@code isBrewable} 用它决定原版的倒计时与燃料逻辑还要不要继续跑。
     *
     * <p>True when at least one bottle slot can be converted as a whole with the reagent on hand.
     * The check mirrors the loop in {@code doBrew}: the potion is present, the reagent can mix with
     * it, the result actually changes, the whole stack fits, and enough reagent is left over.
     * {@code isBrewable} uses it to decide whether the vanilla countdown and fuel logic should keep
     * running.
     */
    private static boolean canBrewAny(PotionBrewing brewing, NonNullList<ItemStack> items) {
        ItemStack ingredient = items.get(3);
        if (ingredient.isEmpty() || !brewing.isIngredient(ingredient)) {
            return false;
        }
        int yield = UltraStacksConfig.brewingYield();
        int pool = ingredient.getCount();
        for (int i = 0; i < 3; i++) {
            ItemStack potion = items.get(i);
            if (potion.isEmpty() || !brewing.hasMix(potion, ingredient)) {
                continue;
            }
            ItemStack mixed = brewing.mix(ingredient, potion.copyWithCount(1));
            if (mixed.isEmpty() || ItemStack.isSameItemSameComponents(mixed, potion)) {
                continue;
            }
            int bottles = potion.getCount();
            if (bottles > mixed.getMaxStackSize()) {
                continue;
            }
            int need = (bottles + yield - 1) / yield;
            if (need <= pool) {
                return true;
            }
        }
        return false;
    }
}
