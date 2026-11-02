package com.zaixiayesheng.ultrastacks.display.mixin;

import java.util.List;

import com.zaixiayesheng.ultrastacks.display.DisplayEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙自己重写了槽位提示框的构建方法，上一条注入到 {@code AbstractContainerScreen} 的规则不会进到这里，
 * 所以这里单独再挂一次。精妙会自己写数量行时（装了解锁槽位上限的升级）由 {@code DisplayEvents} 放行。
 *
 * <p>Sophisticated overrides the slot tooltip builder, so the injection on
 * {@code AbstractContainerScreen} never runs inside its screens; this mirror adds the line there and
 * steps aside when Sophisticated draws the line itself.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class SophisticatedTooltipMixin {

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$appendCountLine(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        cir.setReturnValue(DisplayEvents.withCountLine(cir.getReturnValue(), stack,
                (AbstractContainerScreen<?>) (Object) this));
    }
}
