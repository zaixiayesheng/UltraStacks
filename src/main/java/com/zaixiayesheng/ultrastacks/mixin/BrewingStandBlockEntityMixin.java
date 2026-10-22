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
 * Brews stacked potions at the vanilla reagent rate, converting whole bottle stacks only.
 *
 * <p>On 1.20.1 the brewing table goes through Forge's {@code BrewingRecipeRegistry}, which also serves modded
 * recipes; this hook asks the registry for the result of one bottle and converts a slot only when the whole
 * stack fits that result and the reagent slot has enough reagent at {@code brewingYield} bottles per reagent
 * (three by default, the vanilla rate). Slots that cannot be converted as a whole are left untouched and cost
 * no reagent, so a stack is never partially destroyed. The four hundred tick cycle and the fuel accounting
 * stay vanilla.
 *
 * <p>With {@code potionStackLimit} left at one the hook returns before touching anything.
 *
 * <p><b>中文</b> 药水可堆叠后仍按原版原料比例酿造：每个原料折算 {@code brewingYield} 瓶（默认 3）。
 * 只有整摞瓶子都装得进产物、且原料够用时才整摞转换；装不下或原料不够的瓶位保持原样、不消耗原料，
 * 因此不会出现「只酿出几瓶、剩下的被覆盖」的丢物品。400 tick 周期与烈焰粉结算不变；
 * {@code potionStackLimit} 为 1 时直接返回原版逻辑。
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
     * Resolves what one bottle becomes with the given reagent.
     *
     * <p>Forge's registry only serves recipes registered through it, while vanilla potions live in
     * {@code PotionBrewing}; asking the registry first and falling back to the vanilla table covers both.
     *
     * <p><b>中文</b> 先问 Forge 的酿造注册表（模组配方都在那里），没有结果再回落到原版药水表，
     * 这样原版配方与模组配方都能识别。
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
     * True when at least one bottle slot can be converted as a whole with the reagents on hand.
     *
     * <p><b>中文</b> 至少有一个瓶位能用现有原料整摞转换时为真。
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
