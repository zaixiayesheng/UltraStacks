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
 * 接管物品栏与容器里那串计数文本的绘制。
 *
 * <p>计数不超过 99、显示模块关掉、调用方自己给了 {@code customText}，或者原版传进来的是 {@code null}，都照原样画。超过 99 时把数字换成 {@link CountFormat#format(long)} 的结果——默认 1000 以下仍是完整数字，之上缩写成 k / m / b，阈值由 {@code abbreviateAbove} 控制。
 *
 * <p>缩写后字宽变窄，所以按两边字宽之差把起点右移，保持原版的右对齐。
 *
 * <p>Takes over the drawing of the count text in inventories and containers.
 *
 * <p>A count of at most 99, a disabled display module, a caller-supplied {@code customText} or a
 * {@code null} text from vanilla all fall through to the original drawing. Above 99 the digits are
 * replaced by {@link CountFormat#format(long)}: full digits below 1000 by default and k / m / b
 * abbreviations above that, with the threshold controlled by {@code abbreviateAbove}.
 *
 * <p>An abbreviation is narrower than the text it replaces, so the starting point is shifted right
 * by the difference in string width to keep vanilla's right alignment.
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
