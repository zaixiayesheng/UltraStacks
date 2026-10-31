package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.display.CountFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 计数文本的显示改造。
     *
 * <p>Override for the stack count text.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {

    @Redirect(method = "itemCount", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"
    ))
    private void ultrastacks$compactCount(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color,
                                          boolean dropShadow,
                                          Font itemFont, ItemStack stack, int itemX, int itemY,
                                          @Nullable String countText) {
        if (!UltraStacksConfig.displayModule() || countText != null || text == null || stack.getCount() <= 99) {
            graphics.text(font, text, x, y, color, dropShadow);
            return;
        }

        String shown = CountFormat.format(stack.getCount());
        if (shown.isEmpty()) {
            graphics.text(font, text, x, y, color, dropShadow);
            return;
        }

        int shift = font.width(text) - font.width(shown);
        graphics.text(font, shown, x + shift, y, color, dropShadow);
    }
}
