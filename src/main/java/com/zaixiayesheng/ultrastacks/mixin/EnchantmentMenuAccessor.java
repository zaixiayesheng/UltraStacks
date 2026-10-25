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
 * <p>批量附魔要沿用原版的随机结果生成规则（同样的种子、同样的花费与等级判断），自己重算等于把附魔台的
 * 掷骰逻辑复制一份，所以用 {@code @Invoker} 直接调原方法。
 *
 * <p>Exposes the private {@code getEnchantmentList} helper of the enchanting table menu.
 *
 * <p>Bulk enchanting has to follow vanilla's roll rules — the same seed, the same cost and level checks —
 * and reimplementing them would mean copying the table's dice logic, so the original method is invoked
 * directly through {@code @Invoker}.
 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

    @Invoker("getEnchantmentList")
    List<EnchantmentInstance> ultrastacks$getEnchantmentList(RegistryAccess registryAccess, ItemStack stack, int slot, int cost);
}
