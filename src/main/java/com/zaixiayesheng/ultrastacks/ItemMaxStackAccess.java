package com.zaixiayesheng.ultrastacks;

/**
 * 让启动日志读到物品「原版」的堆叠上限。
 *
 * <p>堆叠上限是 {@code Item} 上的私有字段 {@code maxStackSize}；{@code getMaxStackSize()} 被
 * {@code ItemMixin} 接管之后返回的已经是放大过的值。日志要报「原上限 → 新上限」，就只能绕过这个方法、
 * 直接读字段。
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
