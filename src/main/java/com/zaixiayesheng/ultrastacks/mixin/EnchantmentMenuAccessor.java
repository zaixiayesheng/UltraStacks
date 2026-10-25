package com.zaixiayesheng.ultrastacks.mixin;

import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露附魔台私有的附魔结果随机方法，供批量附魔直接复用。
 *
 * <p>{@code getEnchantmentList} 是私有方法；批量附魔要一次 rolls 应用到整批物品，调用它就能沿用原版的
 * 种子与选取规则，而不必复制一份随机逻辑（复制出来的版本迟早会和原版漂移）。
 *
 * <p>Exposes the private enchantment roll helper of the enchanting table so bulk enchanting can
 * reuse it. {@code getEnchantmentList} is private, and a bulk enchantment needs one roll applied
 * to the whole batch; invoking it keeps the vanilla seed and selection rules instead of
 * duplicating them.
 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

    @Invoker("getEnchantmentList")
    List<EnchantmentInstance> ultrastacks$getEnchantmentList(RegistryAccess registryAccess, ItemStack stack, int slot, int cost);
}
