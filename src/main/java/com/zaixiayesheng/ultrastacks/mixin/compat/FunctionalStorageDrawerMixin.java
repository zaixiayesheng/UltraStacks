package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 抽屉的单次取出量。
 *
 * <p>抽屉一次拿多少跟着槽位上限走，堆叠上限抬上去之后 Shift 点击会把整个抽屉塞进背包；这里把单次取出
 * 压到配置的一组。1.20.1 没有数据组件，旧存档不会出现「上限 64」的问题，所以只处理取出量。
 *
 * <p>Caps one extraction at a single stack; 1.20.1 has no data components, so there is nothing else to clean.
 */
@Mixin(targets = "com.buuz135.functionalstorage.inventory.BigInventoryHandler", remap = false)
public abstract class FunctionalStorageDrawerMixin {

    @ModifyVariable(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"),
            argsOnly = true, index = 2, require = 0)
    private int ultrastacks$capExtract(int amount) {
        int cap = UltraStacksConfig.maxStackSize();
        return amount > cap ? cap : amount;
    }
}
