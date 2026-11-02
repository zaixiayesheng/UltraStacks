package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.display.ResultPreview;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 整屏画完后再补一轮「这批能做多少」预览。
 *
 * <p>各个模组的界面画槽位的路径各不相同（原版走 {@code renderSlot}，精妙自己的容器另有实现，
 * AE 干脆自己画），逐个挂既容易漏、也会重复。这里统一在 {@code AbstractContainerScreen.render}
 * 收尾时遍历一遍菜单里的槽位，由 {@link ResultPreview} 自己判断哪些是铁砧/合成产物、该不该显示。
 *
 * <p>One pass after the whole screen has rendered: every mod draws its slots its own way, so the
 * preview is drawn once over the finished frame instead of being hooked into each of those paths.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ScreenResultPreviewMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"), require = 0)
    private void ultrastacks$drawResultPreview(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (screen.getMenu() == null) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            ResultPreview.draw(graphics, screen, slot);
        }
    }
}
