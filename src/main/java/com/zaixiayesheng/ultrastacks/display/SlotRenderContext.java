package com.zaixiayesheng.ultrastacks.display;

import net.minecraft.world.inventory.Slot;

/**
 * 当前正在绘制的槽位。
 *
 * <p>原版在 {@code AbstractContainerScreen.renderSlot} 里逐格绘制，数量文本在绘制该格时一起画出来。
 * 记录一下“正在画哪一格”，数量层就知道这一格属于谁，不必再按坐标查表。
 *
 * <p>The slot currently being drawn. Vanilla draws cells through {@code renderSlot}; recording the
 * slot lets the count layer know who owns the cell without any coordinate lookup.
 */
public final class SlotRenderContext {

    /** 当前槽位；不在槽位绘制过程中时为 {@code null}。 / Current slot, or null outside slot rendering. */
    public static Slot current;

    private SlotRenderContext() {
    }
}
