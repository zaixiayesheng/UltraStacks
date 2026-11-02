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
 * 精妙的部分页签不走 {@code getTooltipFromContainerItem}，提示框由更外层的取法直接拼出来；
 * 这里补一次，保证「合计」行在这些页面也出现。
 *
 * <p>26.1.2 的 {@code AbstractContainerScreen} 只剩 {@code getTooltipFromContainerItem} 一个入口
 * （1.21.1 的 {@code getTooltipFromItem} 已不存在），所以挂在同一个方法上，用类名前缀区分界面。
 * 重复追加由 {@link DisplayEvents#withCountLine} 自己按翻译键去重，多层挂载不会出现两行。
 *
 * <p>Some Sophisticated tabs build their tooltip outside {@code getTooltipFromContainerItem};
 * 26.1.2 removed {@code getTooltipFromItem}, so this hooks the same method and filters by screen
 * class prefix. Double-appends are prevented by the translation-key check inside
 * {@link DisplayEvents#withCountLine}.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SophisticatedTooltipFallbackMixin {

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$appendCountLine(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (!self.getClass().getName().startsWith("net.p3pp3rf1y.")) {
            return;
        }
        List<Component> original = cir.getReturnValue();
        List<Component> updated = DisplayEvents.withCountLine(original, stack, self);
        if (updated != original) {
            cir.setReturnValue(updated);
        }
    }
}
