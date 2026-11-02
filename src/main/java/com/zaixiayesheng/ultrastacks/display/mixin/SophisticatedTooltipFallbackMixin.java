package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.display.DisplayEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 精妙的部分页签不走 getTooltipFromContainerItem，提示框由 getTooltipFromItem 直接拼出来；
 * 这里补一次，保证「合计」行在这些页面也出现。
 *
 * <p>1.20.1 里 {@code getTooltipFromItem} 是 {@code Screen} 上的静态方法
 * （{@code (Minecraft, ItemStack) -> List<Component>}），没有可注入的实例覆写，所以这里直接挂静态方法，
 * 再从 {@code Minecraft.screen} 取当前界面判断归属。
 *
 * <p>Some Sophisticated tabs build their tooltip through getTooltipFromItem instead of
 * getTooltipFromContainerItem, so the batch line is appended here as well. In 1.20.1 that method is a
 * static one on {@code Screen}, hence the static injector reading the current screen from
 * {@code Minecraft}.
 */
@Mixin(Screen.class)
public abstract class SophisticatedTooltipFallbackMixin {

    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true, require = 0)
    private static void ultrastacks$appendCountLine(Minecraft minecraft, ItemStack stack,
            CallbackInfoReturnable<List<Component>> cir) {
        Screen screen = minecraft == null ? null : minecraft.screen;
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        if (!screen.getClass().getName().startsWith("net.p3pp3rf1y.")) {
            return;
        }
        List<Component> lines = cir.getReturnValue();
        if (lines == null) {
            return;
        }
        List<Component> updated = DisplayEvents.withCountLine(lines, stack, container);
        if (updated != lines) {
            cir.setReturnValue(updated);
        }
    }
}
