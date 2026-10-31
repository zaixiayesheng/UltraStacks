package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 玩家背包的空间判断。
     *
 * <p>Space checks of the player inventory. {@code Inventory} does not override
 * {@code getMaxStackSize()}, so {@code hasRemainingSpaceForItem} and {@code addResource} still see 64
 * and split a large stack into groups of 64. Both calls are redirected to the configured stack size.
 * Adding an overriding method is not an option here: a method injected by a mixin keeps its literal
 * name in production and would not override the interface default.
 */
@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    @Redirect(
            method = "hasRemainingSpaceForItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getMaxStackSize()I"),
            require = 0
    )
    private int ultrastacks$limitForSpaceCheck(Inventory inventory) {
        return UltraStacksConfig.maxStackSize();
    }

    @Redirect(
            method = "addResource(ILnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getMaxStackSize()I"),
            require = 0
    )
    private int ultrastacks$limitForAddResource(Inventory inventory) {
        return UltraStacksConfig.maxStackSize();
    }
}