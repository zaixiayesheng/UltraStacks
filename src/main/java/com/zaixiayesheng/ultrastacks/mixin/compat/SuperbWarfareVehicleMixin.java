package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 卓越前线：载具后备箱能不能放进一堆东西，看的是载具自己的 maxStackSize 字段（默认 64）。
 * 我们上限抬到 65536 后，一次拿一大摞去放就会被这个 64 挡住，只能先拆开。这里把它一起抬上去。
 *
 * <p>Superb Warfare gates vehicle trunk placement on the vehicle's own maxStackSize field
 * (64 by default), so a big stack gets rejected while a small one slips through. Raising that
 * value keeps the rest of the check (same item, capacity) untouched.
 */
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity", remap = false)
public abstract class SuperbWarfareVehicleMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseVehicleStackLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(UltraStacksConfig.vehicleSlotLimit(cir.getReturnValue()));
    }
}
