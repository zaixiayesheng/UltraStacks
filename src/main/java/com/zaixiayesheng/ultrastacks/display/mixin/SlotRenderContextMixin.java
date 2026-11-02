package com.zaixiayesheng.ultrastacks.display.mixin;

import com.zaixiayesheng.ultrastacks.display.SlotRenderContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记录当前正在绘制的槽位，供数量层判断归属。
 *
 * <p>只在绘制前后写一个字段，不取消、不改写任何原版行为；模组自己重写 {@code extractSlot} 而不调用
 * 父类时这里不会被触发，数量层会自动退回坐标表。
 *
 * <p>26.1.2 把 {@code renderSlot} 改名为 {@code extractSlot}，并多带 hovered 坐标两个 int。
 *
 * <p>Records the slot being drawn so the count layer can decide ownership. It only writes a field
 * around vanilla's own call; screens that override {@code extractSlot} without calling super simply
 * never set it and the count layer falls back to its coordinate table.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotRenderContextMixin {

    @Inject(method = "extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V",
            at = @At("HEAD"))
    private void ultrastacks$enterSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
            CallbackInfo ci) {
        SlotRenderContext.current = slot;
    }

    @Inject(method = "extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;II)V",
            at = @At("RETURN"))
    private void ultrastacks$leaveSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
            CallbackInfo ci) {
        SlotRenderContext.current = null;
        // 产物格画一批能做多少，纯显示，不改产物数量。
    }
}
