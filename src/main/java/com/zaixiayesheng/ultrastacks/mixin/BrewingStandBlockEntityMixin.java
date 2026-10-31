package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.ForgeEventFactory;
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

    private static final int[] BOTTLE_SLOTS = new int[]{0, 1, 2};

    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$bulkBrew(Level level, BlockPos pos, NonNullList<ItemStack> items, CallbackInfo ci) {
        if (UltraStacksConfig.potionStackLimit() <= 1) {
            return;
        }
        if (ForgeEventFactory.onPotionAttemptBrew(items)) {
            ci.cancel();
            return;
        }
        ItemStack ingredient = items.get(3);
        if (ingredient.isEmpty()) {
            return;
        }

        int yield = UltraStacksConfig.brewingYield();
        int pool = ingredient.getCount();
        int consumed = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack potion = items.get(i);
            if (potion.isEmpty()) {
                continue;
            }
            ItemStack one = potion.copyWithCount(1);
            ItemStack result = brewOnce(one, ingredient);
            if (result.isEmpty()) {
                continue;
            }
            int bottles = potion.getCount();
            if (bottles > result.getMaxStackSize()) {
                continue;
            }
            int need = (bottles + yield - 1) / yield;
            if (need > pool) {
                continue;
            }
            items.set(i, result.copyWithCount(bottles));
            pool -= need;
            consumed += need;
        }

        if (consumed > 0) {
            ForgeEventFactory.onPotionBrewed(items);
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

    /**
     * 一瓶药水加这份原料会变成什么，空表示这组配不出来。
     *
     * <p>What one bottle becomes with this reagent, empty when the pair does not brew. Forge's registry is
     * asked first since modded recipes register there; when it yields nothing the vanilla
     * {@code PotionBrewing} table is consulted, so both kinds of recipe are recognised.
     */
    private static ItemStack brewOnce(ItemStack potion, ItemStack ingredient) {
        ItemStack probeIngredient = ingredient.copyWithCount(1);
        NonNullList<ItemStack> probe = NonNullList.withSize(3, ItemStack.EMPTY);
        probe.set(0, potion);
        BrewingRecipeRegistry.brewPotions(probe, probeIngredient, BOTTLE_SLOTS);
        ItemStack fromRegistry = probe.get(0);
        if (!fromRegistry.isEmpty() && !ItemStack.isSameItemSameTags(fromRegistry, potion)) {
            return fromRegistry;
        }
        return PotionBrewing.hasMix(potion, probeIngredient)
                ? PotionBrewing.mix(potion, probeIngredient)
                : ItemStack.EMPTY;
    }

    @Inject(method = "isBrewable", at = @At("RETURN"), cancellable = true)
    private static void ultrastacks$requireWholeStackReagents(NonNullList<ItemStack> items,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (UltraStacksConfig.potionStackLimit() <= 1) {
            return;
        }
        cir.setReturnValue(canBrewAny(items));
    }

    /**
     * 至少有一个瓶位能用现有原料整摞转换时为真，酿造台才会开始计时。
     *
     * <p>True when at least one bottle slot can be converted as a whole with the reagents on hand, which is
     * what makes the stand start its cycle.
     */
    private static boolean canBrewAny(NonNullList<ItemStack> items) {
        ItemStack ingredient = items.get(3);
        if (ingredient.isEmpty()) {
            return false;
        }
        int yield = UltraStacksConfig.brewingYield();
        int pool = ingredient.getCount();
        for (int i = 0; i < 3; i++) {
            ItemStack potion = items.get(i);
            if (potion.isEmpty()) {
                continue;
            }
            ItemStack one = potion.copyWithCount(1);
            ItemStack result = brewOnce(one, ingredient);
            if (result.isEmpty()) {
                continue;
            }
            int bottles = potion.getCount();
            if (bottles > result.getMaxStackSize()) {
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
