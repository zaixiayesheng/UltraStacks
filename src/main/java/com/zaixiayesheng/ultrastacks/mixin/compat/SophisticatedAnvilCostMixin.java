package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import com.zaixiayesheng.ultrastacks.DiagLog;
import com.zaixiayesheng.ultrastacks.client.UnlockKey;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 精妙背包升级里的铁砧页：按 Shift 显示整批的价钱，并收起「过于昂贵」。
 *
 * <p>The anvil tab inside a Sophisticated Backpacks upgrade draws its own price line. It reads the
 * same menu cost as vanilla but never learned about stacking: one craft is one unit, so the whole
 * batch still looks like a single charge. This mirrors what the vanilla anvil screen already does —
 * unit price normally, whole-batch price while Shift is held — and drops the red "Too Expensive"
 * once that option is on.
 *
 * <p>Display only: the server keeps charging one craft at a time, so nothing here can change what
 * the player actually pays.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeTab", remap = false)
public abstract class SophisticatedAnvilCostMixin {

    /** 该页取容器的公开方法，反射缓存。 */
    private static Method ultrastacks$getContainer;
    private static Method ultrastacks$getSlots;

    /** 上次写进日志的组合，避免每帧刷屏。 */
    private static String ultrastacks$lastDiag = "";

    /**
     * 「过于昂贵」的红字与拦点：配置打开时把这个阈值抬到无穷大。
     *
     * <p>Removes the red "Too Expensive" state the same way the vanilla screen does — by lifting the
     * 40-level threshold out of reach — instead of touching the computed price.
     */
    @ModifyConstant(
        method = "renderCost",
        constant = @Constant(intValue = 40),
        require = 0
    )
    private int ultrastacks$dropTooExpensive(int original) {
        return UltraStacksConfig.ignoreTooExpensive() ? Integer.MAX_VALUE : original;
    }

    /**
     * 按住 Shift 时把单价换成整批的价钱。
     *
     * <p>Scales the displayed unit price to the whole batch while Shift is held. Without Shift, or
     * with a single item in the input slot, the vanilla number is left alone.
     */
    @ModifyExpressionValue(
        method = "renderCost",
        at = @At(value = "INVOKE", target = "Ljava/lang/Integer;valueOf(I)Ljava/lang/Integer;"),
        require = 0
    )
    private Integer ultrastacks$batchPrice(Integer original) {
        if (original == null || original <= 0 || !Screen.hasShiftDown()) {
            return original;
        }
        int available = ultrastacks$inputCount();
        ultrastacks$diag(original, available);
        if (available <= 1) {
            return original;
        }
        int batch = UltraStacksConfig.bulkCraftUnlockKey() && UnlockKey.isHeld()
            ? available
            : Math.min(available, UltraStacksConfig.bulkCraftLimit());
        if (batch <= 1) {
            return original;
        }
        return (int) Math.min((long) original * batch, Integer.MAX_VALUE);
    }

    /**
     * 按住 Shift 时把「实际读到的数量」记一行，用来判断反射有没有取到槽位。
     *
     * <p>Logs what the preview actually saw, once per distinct combination, so a failed reflective
     * read is visible instead of silently falling back to "no scaling".
     */
    private static void ultrastacks$diag(int unit, int available) {
        String line = "[diag] anvil tab cost unit=" + unit + " available=" + available;
        if (line.equals(ultrastacks$lastDiag)) {
            return;
        }
        ultrastacks$lastDiag = line;
        DiagLog.line(line);
    }

    /**
     * 左槽里有多少件，取不到就返回 0（等于不做换算）。
     *
     * <p>How many items sit in the input slot. Read reflectively because this mod does not compile
     * against Sophisticated Backpacks; anything unexpected simply means "no scaling".
     */
    private int ultrastacks$inputCount() {
        try {
            if (ultrastacks$getContainer == null) {
                // getContainer() 在父类 UpgradeSettingsTab 上是 protected，getMethod 找不到；
                // 沿父类链找 declare 声明并放开访问。
                ultrastacks$getContainer = ultrastacks$findMethod(this.getClass(), "getContainer");
            }
            Object container = ultrastacks$getContainer.invoke(this);
            if (container == null) {
                return 0;
            }
            if (ultrastacks$getSlots == null) {
                ultrastacks$getSlots = ultrastacks$findMethod(container.getClass(), "getSlots");
            }
            Object slots = ultrastacks$getSlots.invoke(container);
            if (!(slots instanceof List<?> list) || list.isEmpty()) {
                return 0;
            }
            Object slot = list.get(0);
            if (!(slot instanceof net.minecraft.world.inventory.Slot vanillaSlot)) {
                return 0;
            }
            return vanillaSlot.getItem().getCount();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    /**
     * 沿父类链找一个方法并放开访问。
     *
     * <p>Looks a method up the hierarchy and makes it callable. Sophisticated keeps
     * {@code getContainer()} protected on a base class, which a plain {@code getMethod} misses.
     */
    private static Method ultrastacks$findMethod(Class<?> type, String name) throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // 继续往父类找 / keep walking up
            }
        }
        throw new NoSuchMethodException(name);
    }
}
