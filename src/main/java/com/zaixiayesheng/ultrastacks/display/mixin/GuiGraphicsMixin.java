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
 * Display module: the number drawn on a slot (Minecraft 1.21.1 / NeoForge).
 *
 * <p>Vanilla draws the full count. COMPACT mode substitutes a shorter string ("65k", "1.5m") at the
 * exact place vanilla was about to draw, shifting the anchor by the width difference so the text
 * keeps vanilla's right alignment. EXACT mode passes the vanilla string straight through.
 *
 * <p>Nothing else is touched: the durability bar, the cooldown overlay and third-party item
 * decorators keep running in the vanilla body, which is why this module cannot move or hide them.
 *
 * <p><b>中文</b> 显示模块：槽位上的数字。COMPACT 模式在原版即将绘制的位置换成缩写（"65k"、"1.5m"），
 * 并按字宽差修正落点以保持原版的右对齐；EXACT 模式直接把原版字符串传回去。除此之外不动任何东西：
 * 耐久条、冷却遮罩与第三方装饰器仍由原版方法继续绘制。
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
