package com.zaixiayesheng.ultrastacks.display.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.client.UnlockKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 铁砧界面的两件事：不把价钱标红，以及按住 Shift 时显示整批的总价。
 *
 * <p>Two things on the anvil screen: stop turning the price red, and show the price of the whole
 * batch while Shift is held — one craft without Shift, {@code bulkCraftLimit} crafts with Shift, and
 * everything the input can pay for while the unlock key is held as well.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {

    @ModifyConstant(
        method = "renderLabels",
        constant = @Constant(intValue = 40),
        require = 0
    )
    private int ultrastacks$dropTooExpensiveLabel(int original) {
        return UltraStacksConfig.ignoreTooExpensive() ? Integer.MAX_VALUE : original;
    }

    /**
     * 按住 Shift 时把单价换算成整批的价钱；不按 Shift 就原样返回。
     *
     * <p>Scales the unit price to the whole batch while Shift is held; without Shift the vanilla
     * single-craft price is returned unchanged. The batch size is only a preview — the server still
     * charges one craft at a time, so this never changes what is actually paid.
     */
    @ModifyExpressionValue(
        method = "renderLabels",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;getCost()I"),
        require = 0
    )
    private int ultrastacks$batchPrice(int original) {
        if (original <= 0 || !Screen.hasShiftDown()) {
            return original;
        }
        AnvilScreen self = (AnvilScreen) (Object) this;
        AnvilMenu menu = self.getMenu();
        if (menu == null) {
            return original;
        }
        int available = menu.getSlot(0).getItem().getCount();
        if (available <= 1) {
            return original;
        }
        int batch = UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld()
            ? available
            : Math.min(available, UltraStacksConfig.bulkCraftLimit());
        if (batch <= 1) {
            return original;
        }
        long total = (long) original * batch;
        return (int) Math.min(total, Integer.MAX_VALUE);
    }
}
