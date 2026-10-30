package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 把抽屉里旧存档留下的「原版默认上限」组件清掉。
 *
 * <p>旧版本的升级物品把 {@code minecraft:max_stack_size} 固化成了 64，抽屉里那摞模板一直带着它。数量本身能堆到
 * 配置值，但带组件的物品和干净物品不是同一个栈，合成表与 JEI 都按组件比对，于是拉不出配方。这里在读取与取出时
 * 去掉这种「等于原版默认值」的组件，让它们回到普通物品。
 *
 * <p>Drops the vanilla-default {@code max_stack_size} component that old drawers still carry, so the stacks match
 * recipes and JEI again.
 */
@Mixin(targets = "com.buuz135.functionalstorage.inventory.BigInventoryHandler", remap = false)
public abstract class FunctionalStorageDrawerMixin {

    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"), require = 0)
    private int ultrastacks$raiseDrawerLimit(int original) {
        int cap = UltraStacksConfig.maxStackSize();
        return original < cap ? cap : original;
    }

    @ModifyVariable(method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private ItemStack ultrastacks$cleanIncoming(ItemStack stack) {
        return UltraStacksConfig.stripDefaultStackSize(stack.copy());
    }

    @ModifyReturnValue(method = "getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 0)
    private ItemStack ultrastacks$cleanStored(ItemStack original) {
        return UltraStacksConfig.stripDefaultStackSize(original);
    }


    /**
     * 一次最多取出一组配置好的数量。
     *
     * <p>{@code getMaxStackSize()} 对超大堆叠会返回当前总量，FS 的 Shift 点击拿它当取出量，结果一次把整个抽屉
     * 塞进背包。这里把单次取出压到配置上限。
     *
     * <p>Caps a single extraction at the configured stack size; Functional Storage otherwise asks for the whole
     * drawer when the stored stack is larger than a normal stack.
     */
    @ModifyVariable(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"),
            argsOnly = true, index = 2, require = 0)
    private int ultrastacks$capExtract(int amount) {
        int cap = UltraStacksConfig.maxStackSize();
        return amount > cap ? cap : amount;
    }

    @ModifyReturnValue(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 0)
    private ItemStack ultrastacks$cleanExtracted(ItemStack original) {
        return UltraStacksConfig.stripDefaultStackSize(original);
    }
}
