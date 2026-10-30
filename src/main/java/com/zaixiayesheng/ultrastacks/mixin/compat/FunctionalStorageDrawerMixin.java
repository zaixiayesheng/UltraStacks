package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 抽屉一次能拿多少，以及顺手清掉旧存档留下的标记。
 *
 * <p>26.1.2 的抽屉走 transfer API，Handler 的 {@code extract} 一进来拿到的是「想拿多少」，这里把它压成
 * 配置的一组——不压的话 Shift 一点就是一整个抽屉进背包。另一个口子 {@code getStackInSlot} 是给 GUI 和
 * 漏斗看的，顺手把旧存档里「上限 64」那条组件剥掉，省得 JEI 和配方对不上。</p>
 *
 * <p>Caps a single transfer-API extraction at one configured stack, and drops the stale stack-size
 * component on the way out so GUI/hopper reads match what recipes expect.</p>
 */
@Mixin(targets = "com.buuz135.functionalstorage.inventory.BigInventoryHandler", remap = false)
public abstract class FunctionalStorageDrawerMixin {

    @ModifyReturnValue(method = "getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 0)
    private ItemStack ultrastacks$cleanStored(ItemStack original) {
        return UltraStacksConfig.stripDefaultStackSize(original);
    }

    @ModifyVariable(method = "extract(ILnet/neoforged/neoforge/transfer/item/ItemResource;ILnet/neoforged/neoforge/transfer/transaction/TransactionContext;)I",
            at = @At("HEAD"), argsOnly = true, index = 3, require = 0)
    private int ultrastacks$capExtract(int amount) {
        int cap = UltraStacksConfig.maxStackSize();
        return amount > cap ? cap : amount;
    }

    @ModifyVariable(method = "extract(ILnet/neoforged/neoforge/transfer/resource/Resource;ILnet/neoforged/neoforge/transfer/transaction/TransactionContext;)I",
            at = @At("HEAD"), argsOnly = true, index = 3, require = 0)
    private int ultrastacks$capExtractResource(int amount) {
        int cap = UltraStacksConfig.maxStackSize();
        return amount > cap ? cap : amount;
    }
}
