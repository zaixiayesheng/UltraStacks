package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.client.UnlockKey;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
/**
 * 铁砧界面的两件事：不把价钱标红，以及按住 Shift 时显示整批的总价。
 *
 * <p>Two things on the anvil screen: stop turning the price red once it reaches 40, and show the
 * price of the whole batch while Shift is held — one craft without Shift, {@code bulkCraftLimit}
 * crafts with Shift, and everything the input can pay for while the unlock key is held as well.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {

    @ModifyConstant(
        method = "extractLabels",
        constant = @Constant(intValue = 40),
        require = 0
    )
    private int ultrastacks$dropTooExpensiveLabel(int original) {
        return UltraStacksConfig.ignoreTooExpensive() ? Integer.MAX_VALUE : original;
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
        method = "extractLabels",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;getCost()I"),
        require = 0
    )
    private int ultrastacks$batchPrice(int original) {
        return ultrastacks$scaled(original);
    }

    /** Shift 是否按住（26.1.2 起 Screen.hasShiftDown 已移除）。 / Whether Shift is held. */
    private static boolean ultrastacks$shiftDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /** 单价换成整批总价的公共计算。 / Shared computation for the batch price preview. */
    private int ultrastacks$scaled(int original) {
        if (original <= 0 || !ultrastacks$shiftDown()) {
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
