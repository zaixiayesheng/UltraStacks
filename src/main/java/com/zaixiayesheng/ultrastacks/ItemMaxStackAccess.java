package com.zaixiayesheng.ultrastacks;

/**
 * 让启动日志读到物品「原版」的堆叠上限。
     *
 * <p>Exposes an item's original stack limit so the startup log can report it. The limit lives in a
 * private {@code maxStackSize} field on {@code Item}, and {@code getMaxStackSize()} already returns the
 * scaled value once {@code ItemMixin} takes it over, so the field has to be read directly to print
 * "old limit → new limit".
 */
public interface ItemMaxStackAccess {
    /**
     * 混入保留的原始堆叠上限（未经本模组缩放）。
     *
     * <p>The original stack limit kept by the mixin, before this mod scales it.
     */
    int ultrastacks$vanillaMaxStackSize();
}
