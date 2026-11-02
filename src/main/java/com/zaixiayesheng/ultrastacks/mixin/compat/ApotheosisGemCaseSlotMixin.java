package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 神化的宝石匣（含末影变种）把每格上限写死成容器自己的 {@code getMaxStackSize()}，取出时按它夹，
 * 所以里面存了 65536 也只能一次拿出 64。这里把那个上限换成配置值。
 *
 * <p>Apotheosis' gem case (and its ender variant) clamps both the slot limit and the take amount to
 * the container's own {@code getMaxStackSize()}, so a 65536 stack leaves 64 at a time. Raise it to
 * the configured limit; the {@code ItemStack} overload delegates here, so it follows automatically.
 */
@Mixin(targets = "dev.shadowsoffire.apotheosis.socket.gem.storage.GemCaseSlot", remap = false)
public abstract class ApotheosisGemCaseSlotMixin {
    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void ultrastacks$raiseGemCaseLimit(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValueI();
        if (original == 64 || original == 99) {
            cir.setReturnValue(UltraStacksConfig.maxStackSize());
        }
    }

    /** 取出数量走 {@code getMaxStackSize(ItemStack)}：它取 min(本槽上限, 物品上限)，抬过就不用再动。 */
    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void ultrastacks$raiseGemCaseItemLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int slotLimit = UltraStacksConfig.maxStackSize();
        int itemLimit = stack.getMaxStackSize();
        cir.setReturnValue(Math.min(slotLimit, itemLimit));
    }
}
