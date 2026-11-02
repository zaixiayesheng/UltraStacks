package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;

/**
 * AE 全局「每类资源每槽能装多少」的登记表。
 *
 * <p>AE 在静态初始化里把物品类登记成 <b>99</b>（{@code register(AEKeyType.items(), 99L)}），
 * 而 {@code ConfigInventory.storage(...)} 建库时会调 {@code useRegisteredCapacities()} 把这张表抄进自己的
 * 容量里；于是 {@code getMaxAmount = min(key.getMaxStackSize(), capacity)} 永远卡在 99——ME 接口、
 * 各种机器的内部缓冲都逃不掉。这里把物品类的登记值换成配置的堆叠上限，其余类型（流体等）原样保留。
 *
 * <p>AE registers 99 as the per-slot capacity for items in a static initializer, and every
 * {@code ConfigInventory.storage(...)} copies that registry into its own capacities, so
 * {@code getMaxAmount = min(key.getMaxStackSize(), capacity)} never exceeds 99. This replaces the item
 * entry with the configured stack size and leaves the other types (fluids, etc.) untouched.
 */
@Mixin(targets = "appeng.api.behaviors.GenericSlotCapacities", remap = false)
public abstract class Ae2SlotCapacitiesMixin {

    @ModifyReturnValue(method = "getMap()Ljava/util/Map;", at = @At("RETURN"), require = 0)
    private static Map<Object, Long> ultrastacks$raiseItemCapacity(Map<Object, Long> original) {
        if (!UltraStacksConfig.compatAe2() || original == null || original.isEmpty()) {
            return original;
        }
        Object itemType;
        try {
            itemType = Class.forName("appeng.api.stacks.AEKeyType").getMethod("items").invoke(null);
        } catch (ReflectiveOperationException e) {
            return original;
        }
        Long current = original.get(itemType);
        if (current == null) {
            return original;
        }
        long limit = Math.max(1L, UltraStacksConfig.maxStackSize());
        if (current >= limit) {
            return original;
        }
        Map<Object, Long> copy = new HashMap<>(original);
        copy.put(itemType, limit);
        return copy;
    }
}
