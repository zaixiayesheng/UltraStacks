package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.display.AeCraftPreview;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AE 系界面（合成终端、无线合成终端、综合工作终端等）画完以后再补一层产物数量预览。
 *
 * <p>These screens draw their own slots instead of going through
 * {@code AbstractContainerScreen#renderSlot}, so the preview has to be appended after AE finishes.
 *
 * <p>1.20.1 的 AE2 jar 里 {@code render} 带的是 SRG 名 {@code m_88315_}。
 */
@Mixin(targets = "appeng.client.gui.AEBaseScreen", remap = false)
public abstract class Ae2ResultPreviewMixin {

    @Inject(method = "m_88315_(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"), require = 0)
    private void ultrastacks$drawCraftPreview(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
        CallbackInfo ci) {
        AeCraftPreview.draw(graphics, (AbstractContainerScreen<?>) (Object) this);
    }
}
