package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.display.DisplayEvents;
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
 * <p>Some Sophisticated tabs build their tooltip through getTooltipFromItem instead of
 * getTooltipFromContainerItem, so the batch line is appended here as well.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SophisticatedTooltipFallbackMixin {
    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$appendCountLine(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (self.getClass().getName().startsWith("net.p3pp3rf1y.")) {
            cir.setReturnValue(DisplayEvents.withCountLine(cir.getReturnValue(), stack, self));
        }
    }
}
