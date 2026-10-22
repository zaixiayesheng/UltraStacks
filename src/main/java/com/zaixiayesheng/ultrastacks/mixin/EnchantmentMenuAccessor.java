package com.zaixiayesheng.ultrastacks.mixin;

import java.util.List;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for the private enchantment roll helper.
 *
 * <p><b>中文</b> 取出附魔台私有的附魔随机结果方法，供批量附魔使用。
 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

    @Invoker("getEnchantmentList")
    List<EnchantmentInstance> ultrastacks$getEnchantmentList(ItemStack stack, int slot, int cost);
}
