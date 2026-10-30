package com.zaixiayesheng.ultrastacks.mixin;

/**
 * 记录当前这次 {@code doClick} 是按的哪个键，供同一次调用里的重定向读取。
 *
 * <p>1.20.1 的 Forge 编译期没有 MixinExtras 的 {@code @Local}，拿不到 {@code button} 这个局部参数；
 * 又不能在重定向里猜——右键是 1、左键是整摞，用途完全不同。于是在进入 {@code doClick} 时把它记在
 * 线程变量里，重定向处理完顺手清掉。同一个线程同一时刻只会处理一次点击，不会串。</p>
 *
 * <p>Remembers which mouse button the current {@code doClick} call came from. Forge 1.20.1 has no
 * MixinExtras {@code @Local} at compile time, so the redirect cannot read the {@code button}
 * parameter directly, and guessing is not an option: right-click means one item, left-click means
 * the whole stack. The value is stored in a thread local on entry and cleared afterwards; a thread
 * handles one click at a time, so there is nothing to mix up.</p>
 */
public final class ClickButtonContext {

    private static final ThreadLocal<Integer> BUTTON = new ThreadLocal<>();

    private ClickButtonContext() {
    }

    public static void push(int button) {
        BUTTON.set(button);
    }

    public static int peek() {
        Integer value = BUTTON.get();
        return value == null ? 0 : value;
    }

    public static void clear() {
        BUTTON.remove();
    }
}
