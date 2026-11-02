package com.zaixiayesheng.ultrastacks.crafting;

/** 只用来确认某个类在当前侧是否存在，不触发初始化。 */
public final class ClassProbe {
    private ClassProbe() {
    }

    public static boolean exists(String name) {
        try {
            Class.forName(name, false, ClassProbe.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
