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
 * 药水可堆叠后仍按原版比例酿造，而且只做整摞转换。
 *
 * <p>原版 {@code doBrew} 把每个瓶位替换成 {@code PotionBrewing#mix} 返回的单瓶结果，槽里放 16 瓶也会被
 * 压成 1 瓶。这里在 {@code doBrew} 头部接管：逐个瓶位判断「整摞都装得进产物」且「原料够这次转换」，两个
 * 条件都满足才把整摞换成产物；原料按 {@code brewingYield} 瓶一份折算，即 n 瓶要 {@code ceil(n / yield)}
 * 份（默认 3 瓶一份，与原版一致）。装不下或原料不够的瓶位保持原样、不消耗原料，所以不会出现「只酿出几瓶、
 * 剩下的被覆盖」。
 *
 * <p>转换发生时会照常触发 NeoForge 的 {@code onPotionAttemptBrew} 与 {@code onPotionBrewed}，原料的合成
 * 剩余物按消耗份数逐个掉在酿造台位置，并广播 {@code 1035} 号酿造音效事件。400 tick 周期、烈焰粉扣除与
 * 界面同步仍走原版。
 *
 * <p>{@code isBrewable} 的返回值也被改写：只有至少一个瓶位能整摞转换时才算「可酿造」。这一步不能省——
 * 原版在可酿造且还有烈焰粉时会先扣一份燃料再开始 400 tick 计时，若 {@code doBrew} 此时一瓶也酿不出来，
 * 玩家就白烧燃料、什么也拿不到。
 *
 * <p>Brews stackable potions at the vanilla reagent rate, and only ever converts a whole bottle stack.
 *
 * <p>Vanilla {@code doBrew} replaces each bottle slot with the single potion {@code PotionBrewing#mix}
 * returns, so a slot holding sixteen bottles would be cut down to one. This hook takes over at the head of
 * {@code doBrew}: each bottle slot is converted only when the whole stack fits the mixed potion and the
 * reagents on hand cover that conversion. Reagents are counted at {@code brewingYield} bottles per reagent,
 * so n bottles cost {@code ceil(n / yield)} reagents (three bottles per reagent by default, which is the
 * vanilla rate). Slots that cannot be converted as a whole are left untouched and cost no reagent, so a
 * stack is never partially destroyed.
 *
 * <p>A conversion raises NeoForge's {@code onPotionAttemptBrew} and {@code onPotionBrewed} as usual, drops
 * the ingredient's crafting remainder once per consumed reagent at the stand's position, and broadcasts
 * level event {@code 1035}. The four hundred tick cycle, the blaze powder accounting and the client
 * synchronisation all stay vanilla.
 *
 * <p>The return value of {@code isBrewable} is rewritten too: the stand counts as brewable only when at
 * least one bottle slot can be converted as a whole. This step is not optional — when the stand is
 * brewable and still has blaze powder, vanilla spends one powder and starts the 400-tick timer before
 * anything is brewed, so a {@code doBrew} that converts nothing would burn the fuel and give the player
 * nothing.
 *
 * <p>{@code potionStackLimit} 为 1 时两个注入点都直接返回，酿造台完全按原版逻辑工作。
 *
 * <p>With {@code potionStackLimit} left at 1 both injection points return immediately and the stand
 * works exactly like vanilla.
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
     * <p>与 {@code doBrew} 里的条件保持一致：原料是合法酿造材料、瓶位有对应的混合产物、整摞都装得进产物，
     * 并且原料份数够这次转换。任一瓶位满足即为真。
     *
     * <p>Whether the items on hand can still convert at least one bottle stack as a whole.
     *
     * <p>The conditions mirror the ones in {@code doBrew}: the reagent is a valid brewing ingredient, the
     * bottle slot has a matching mix, the whole stack fits the mixed potion, and there are enough reagents
     * for that conversion. A single qualifying bottle slot is enough for a true result.
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
