package com.zaixiayesheng.ultrastacks.mixin;

import java.util.List;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 附魔台私有随机方法的访问器。
     *
 * <p>Accessor for the private enchantment roll helper of the enchanting table.
 * {@code EnchantmentMenu.getEnchantmentList} is private, and bulk enchanting has to apply the same roll to
 * a whole batch, so it is exposed through {@code @Invoker}.
 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

    @Invoker("getEnchantmentList")
    List<EnchantmentInstance> ultrastacks$getEnchantmentList(ItemStack stack, int slot, int cost);
}
