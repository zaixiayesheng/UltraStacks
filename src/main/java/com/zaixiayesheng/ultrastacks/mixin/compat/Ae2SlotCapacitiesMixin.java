package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * AE 全局「每类资源每槽能装多少」的登记表。
 *
 * <p>AE 在静态初始化里把物品类登记成 <b>64</b>（1.20.1 这一版是 64），而
 * {@code ConfigInventory.storage(...)} 建库时会调 {@code useRegisteredCapacities()} 把这张表抄进自己的
 * 容量里；于是 {@code getMaxAmount = min(key.getMaxStackSize(), capacity)} 永远卡在这个值——ME 接口、
 * 各种机器的内部缓冲都逃不掉。这里把物品类的登记值换成配置的堆叠上限，其余类型（流体 4000 等）原样保留。
 *
 * <p>AE registers 64 per slot for items in a static initializer; every {@code ConfigInventory.storage(...)}
 * copies that registry, so {@code getMaxAmount} never exceeds it. This replaces the item entry with the
 * configured stack size and leaves other types alone.
 */
@Mixin(targets = "appeng.api.behaviors.GenericSlotCapacities", remap = false)
public abstract class Ae2SlotCapacitiesMixin {

    @Inject(method = "getMap()Ljava/util/Map;", at = @At("RETURN"), cancellable = true, require = 0)
    private static void ultrastacks$raiseItemCapacity(CallbackInfoReturnable<Map<Object, Long>> cir) {
        Map<Object, Long> original = cir.getReturnValue();
        if (!UltraStacksConfig.compatAe2() || original == null || original.isEmpty()) {
            return;
        }
        Object itemType;
        try {
            itemType = Class.forName("appeng.api.stacks.AEKeyType").getMethod("items").invoke(null);
        } catch (ReflectiveOperationException e) {
            return;
        }
        Long current = original.get(itemType);
        if (current == null || current >= Math.max(1L, UltraStacksConfig.maxStackSize())) {
            return;
        }
        Map<Object, Long> copy = new HashMap<>(original);
        copy.put(itemType, Math.max(1L, UltraStacksConfig.maxStackSize()));
        cir.setReturnValue(copy);
    }
}
