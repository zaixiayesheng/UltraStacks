package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
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
 * 药水能堆了，酿造台也跟着整摞酿：整摞装得下、原料够，才换一个瓶位，
 * 不够就原样不动、不浪费原料。potionStackLimit=1 时整个让位给原版。
     *
 * Brewing follows stacked potions: convert a slot only when the whole stack fits
 * and reagents are enough, otherwise leave it. With potionStackLimit=1 it steps aside.
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
            ItemStackTemplate remainder = ingredient.getCraftingRemainder();
            ingredient.shrink(consumed);
            items.set(3, ingredient);
            if (remainder != null) {
                for (int i = 0; i < consumed; i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.create());
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
     * 判断现有材料是否还能整摞转换出东西。
     *
     * <p>Whether the items on hand can still convert at least one bottle stack as a whole.
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
