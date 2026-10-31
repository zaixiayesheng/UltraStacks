package com.zaixiayesheng.ultrastacks.mixin;

import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 把附魔台的私有方法 {@code getEnchantmentList} 暴露出来。
     *
 * <p>Exposes the private {@code getEnchantmentList} helper of the enchanting table menu.
 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

    @Invoker("getEnchantmentList")
    List<EnchantmentInstance> ultrastacks$getEnchantmentList(RegistryAccess registryAccess, ItemStack stack, int slot, int cost);
}
