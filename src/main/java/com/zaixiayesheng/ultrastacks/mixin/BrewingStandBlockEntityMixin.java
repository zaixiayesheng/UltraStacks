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
 * Brews stacked potions at the vanilla reagent rate, converting whole bottle stacks only.
 *
 * <p>Vanilla replaces each bottle slot with the single potion returned by {@code PotionBrewing.mix}, so a slot
 * holding sixteen bottles would be cut down to one. This hook converts a slot only when the whole stack fits
 * the mixed potion and the reagent slot has enough reagent at {@code brewingYield} bottles per reagent (three by
 * default, which is the vanilla rate). If a slot cannot be converted as a whole it is left untouched and no
 * reagent is spent, so a stack is never partially destroyed. The four hundred tick cycle and the fuel
 * accounting stay vanilla.
 *
 * <p>With {@code potionStackLimit} left at one the hook returns before touching anything.
 *
 * <p><b>中文</b> 药水可堆叠后仍按原版原料比例酿造：每个原料折算 {@code brewingYield} 瓶（默认 3）。
 * 只有当整摞瓶子都能装进产物、且原料够用时才整摞转换；装不下或原料不够的瓶位保持原样、不消耗原料，
 * 因此不会出现「只酿出几瓶、剩下的被覆盖」的丢物品。400 tick 周期与烈焰粉结算不变；
 * {@code potionStackLimit} 为 1 时直接返回原版逻辑。
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
     * True when at least one bottle slot can be converted as a whole with the reagents on hand.
     *
     * <p><b>中文</b> 至少有一个瓶位能用现有原料整摞转换时为真。
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
