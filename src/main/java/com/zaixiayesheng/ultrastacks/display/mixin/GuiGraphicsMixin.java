package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.display.CountFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 接管那串计数文本：99 及以下、显示关了、调用方自己带了文本，都照原样画；
 * 超过就换缩写（1000 以上 k/m/b），左边空出来的字宽补回去，右对齐不变。
     *
 * Takes over the count text: fall through at 99 or below, otherwise draw the
 * abbreviation (k/m/b above 1000) and shift right by the width difference.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Redirect(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
            )
    )
    private int ultrastacks$compactCount(GuiGraphics graphics, Font font, String text, int x, int y, int color,
                                         boolean dropShadow,
                                         Font itemFont, ItemStack stack, int itemX, int itemY,
                                         @Nullable String customText) {
        if (!UltraStacksConfig.displayModule() || customText != null || text == null || stack.getCount() <= 99) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }

        String shown = CountFormat.format(stack.getCount());
        if (shown.isEmpty()) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }

        int shift = font.width(text) - font.width(shown);
        return graphics.drawString(font, shown, x + shift, y, color, dropShadow);
    }
}
