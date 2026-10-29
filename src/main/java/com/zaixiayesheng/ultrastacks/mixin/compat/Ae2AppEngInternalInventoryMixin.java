package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 自己的库存实现 {@code AppEngInternalInventory}，单格上限由 {@code getSlotLimit} 报告，数值来自它内部的一张 int 数组。
 *
 * <p>这类库存自己算上限，不经过本模组已经抬过的原版路径，所以在这里再抬一次：{@code RETURN} 处把 64 与 99 换成 {@link UltraStacksConfig#ae2SlotLimit(int)}，其它数值原样返回，机器自己指定的更大容量不会被压小。
 *
 * <p>AE2's own inventory implementation {@code AppEngInternalInventory} reports a per-slot limit from
 * {@code getSlotLimit}, backed by an int array it keeps internally.
 *
 * <p>Such an inventory computes the limit itself instead of going through the vanilla paths this mod
 * already raised, so it is raised again here: at {@code RETURN}, 64 and 99 become
 * {@link UltraStacksConfig#ae2SlotLimit(int)} while every other value is passed through, so a larger
 * capacity chosen by a machine is never lowered.
 */
@Mixin(targets = "appeng.util.inv.AppEngInternalInventory", remap = false)
public abstract class Ae2AppEngInternalInventoryMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
