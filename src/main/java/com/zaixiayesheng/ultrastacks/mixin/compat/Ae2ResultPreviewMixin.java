package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.display.AeCraftPreview;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AE 系界面（合成终端、无线合成终端、综合工作终端等）画完以后再补一层产物数量预览。
 *
 * <p>These screens draw their own slots instead of going through
 * {@code AbstractContainerScreen#extractSlot}, so the preview has to be appended after AE finishes.
 * 26.1.2 把 {@code render} 换成了 {@code extractRenderState}，目标方法名随之改名；AE2 本体缺席时
 * 由 {@code CompatMixinPlugin} 按 mod id 直接跳过，{@code require = 0} 让版本不符也不至于崩。
 */
@Mixin(targets = "appeng.client.gui.AEBaseScreen", remap = false)
public abstract class Ae2ResultPreviewMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("RETURN"), require = 0)
    private void ultrastacks$drawCraftPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
        CallbackInfo ci) {
        AeCraftPreview.draw(graphics, (AbstractContainerScreen<?>) (Object) this);
    }
}
