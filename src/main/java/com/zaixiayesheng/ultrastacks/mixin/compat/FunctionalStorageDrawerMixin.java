package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 抽屉一次能倒出多少。
     *
 * <p>Shift-clicking a drawer asks FS "how much can this slot give", and it answers with the slot
 * capacity — fine at 64, catastrophic at 65536 where one click empties twenty thousand dirt into
 * your backpack. Capped to one stack here. Pipes, hoppers and AE2 use the item handler instead, so
 * they are unaffected. No data components in 1.20.1, so no legacy templates to clean.</p>
 */
@Mixin(targets = "com.buuz135.functionalstorage.inventory.BigInventoryHandler", remap = false)
public abstract class FunctionalStorageDrawerMixin {

    /**
     * 把这次请求的取出量夹到一组；请求本来就比一组小的原样放行，别把「只拿一个」变成「拿一组」。
     *
     * <p>Clamps the requested amount to one stack, leaving smaller requests alone so that taking a
     * single item stays a single item.</p>
     */
    @ModifyVariable(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"),
            argsOnly = true, index = 2, require = 0)
    private int ultrastacks$capExtract(int amount) {
        int cap = UltraStacksConfig.maxStackSize();
        return amount > cap ? cap : amount;
    }
}
