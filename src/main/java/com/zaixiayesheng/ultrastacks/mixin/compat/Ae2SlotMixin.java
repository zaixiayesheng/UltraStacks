package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 应用能源的菜单槽位上限。
 *
 * <p>{@code AppEngSlot} 自己从库存算上限，不走原版 {@code Slot}，所以终端里「一格能放多少」由它决定。两个
 * 重载都要抬：无参那个管实际放入，带物品参数的那个用于拖拽时的预判，只抬一个就会出现「预判能放、放进去被
 * 拒」的错位。换算规则见 {@code ae2SlotLimit}：只替换 64 与 99，兼容开关关闭时原样返回。
 *
 * <p>Slot limits of Applied Energistics 2's menu slots.
 *
 * <p>{@code AppEngSlot} computes its limit from the inventory rather than going through vanilla
 * {@code Slot}, so it decides how much one terminal slot holds. Both overloads have to be raised: the
 * no-argument one governs what is actually inserted, while the one taking a stack is used to predict a drag,
 * and raising only one of them produces the mismatch where the preview accepts an item the insert then
 * rejects. The conversion rule lives in {@code ae2SlotLimit}: only 64 and 99 are replaced, and everything is
 * returned unchanged while the compat switch is off.
 */
@Mixin(targets = "appeng.menu.slot.AppEngSlot", remap = false)
public abstract class Ae2SlotMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseSlotLimit(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"),
            cancellable = true, require = 0)
    private void ultrastacks$raiseStackLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        int raised = UltraStacksConfig.ae2SlotLimit(original);
        if (raised != original) {
            cir.setReturnValue(raised);
        }
    }
}
