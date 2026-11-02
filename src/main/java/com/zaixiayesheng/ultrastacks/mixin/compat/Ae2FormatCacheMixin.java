package com.zaixiayesheng.ultrastacks.mixin.compat;

import java.util.Map;
import java.util.WeakHashMap;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 缓存 AE 的数量文本，只留「上一次」这一条。
 *
 * <p>AE 的终端每帧都会对每个可见格子调一次 {@code AEKey.formatAmount(amount, format)}，同样的
 * 键、同样的数量会在连续帧里重复几十次。这里每个键只记住最后一次的（数量、格式、文本），命中就
 * 直接返回；数量一变就重新算并覆盖。这样每个键固定只占一条记录，不会因为数量取值的种类多而膨胀。
 *
 * <p>AE asks every visible cell for its count text on every frame; the same key and amount repeat
 * across consecutive frames. Each key keeps exactly one memoised result (amount + format + text) and
 * overwrites it when the amount changes, so the footprint cannot grow with the number of distinct
 * amounts.
 */
@Mixin(targets = "appeng.api.stacks.AEKey", remap = false)
public abstract class Ae2FormatCacheMixin {

    /** 键最多这么多条；超了整表清空，避免极端情况下无界增长。 / Hard cap before clearing everything. */
    private static final int MAX_KEYS = 4096;

    private record Entry(long amount, AmountFormat format, String text) {
    }

    private static final Map<AEKey, Entry> ULTRASTACKS_LAST_TEXT = new WeakHashMap<>();

    @Inject(method = "formatAmount(JLappeng/api/stacks/AmountFormat;)Ljava/lang/String;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$cachedFormat(long amount, AmountFormat format,
                                          CallbackInfoReturnable<String> cir) {
        // 只缓存槽位用的两种格式：完整格式只在提示框里出现，量少，不值得占位。
        // Only the two slot formats are cached; the full format is rare and not worth an entry.
        if (format == null || format == AmountFormat.FULL) {
            return;
        }
        AEKey key = (AEKey) (Object) this;
        synchronized (ULTRASTACKS_LAST_TEXT) {
            Entry cached = ULTRASTACKS_LAST_TEXT.get(key);
            if (cached != null && cached.amount() == amount && cached.format() == format) {
                cir.setReturnValue(cached.text());
                return;
            }
            String fresh = cir.getReturnValue();
            if (fresh == null) {
                return;
            }
            if (ULTRASTACKS_LAST_TEXT.size() >= MAX_KEYS && !ULTRASTACKS_LAST_TEXT.containsKey(key)) {
                ULTRASTACKS_LAST_TEXT.clear();
            }
            ULTRASTACKS_LAST_TEXT.put(key, new Entry(amount, format, fresh));
        }
    }
}
