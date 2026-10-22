package com.zaixiayesheng.ultrastacks.core;

/**
 *
 */
public final class StackLimits {
    /** Vanilla limit used as the reference point for proportional scaling. */
    public static final int VANILLA_REFERENCE = 64;

    private StackLimits() {
    }

    /**
     * Resolve the new limit for an item.
     *
     * @param vanillaMax     the item's original limit (64, 16, 1, ...)
     * @param targetMax      the configured limit for 64-size items
     * @param scaleSmallItems when true, smaller stacks keep their relative size
     */
    public static int resolve(int vanillaMax, int targetMax, boolean scaleSmallItems) {
        if (vanillaMax <= 1) {
            // Non-stackable (tools, armour, buckets) and empty defaults stay untouched.
            return vanillaMax <= 0 ? 1 : vanillaMax;
        }

        if (!scaleSmallItems) {
            return Math.max(vanillaMax, targetMax);
        }

        long scaled = (long) vanillaMax * targetMax / VANILLA_REFERENCE;
        return (int) Math.max(1L, Math.min((long) targetMax, scaled));
    }

    /**
     * Bounds the save/network format carries. Anything below 99 would break vanilla stacks, so the
     * result is never smaller than the vanilla codec bound.
     */
    public static int codecBound(int requested) {
        return Math.max(99, requested);
    }
}
