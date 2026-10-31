package com.zaixiayesheng.ultrastacks.mixin.compat;

import com.buuz135.functionalstorage.inventory.BigInventoryHandler.BigStack;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 抽屉的几处补丁。FS 全按“格数×64”推容量，抬到 65536 就露馅，所以容量、取出量、模板都得自己算。
 *
 * Drawer patches. FS derives everything from "slots × 64", which falls apart at 65536, so the
 * capacity, the extract amount and the template are all computed here.
 */
@Mixin(targets = "com.buuz135.functionalstorage.inventory.BigInventoryHandler", remap = false)
public abstract class FunctionalStorageDrawerMixin {

    private static final Field TYPE_FIELD;
    private static final Method SLOT_AMOUNT;

    static {
        Field field = null;
        Method method = null;
        try {
            Class<?> handler = Class.forName("com.buuz135.functionalstorage.inventory.BigInventoryHandler");
            field = handler.getDeclaredField("type");
            field.setAccessible(true);
            Class<?> typeClass = field.getType();
            method = typeClass.getMethod("getSlotAmount");
        } catch (Throwable ignored) {
            field = null;
            method = null;
        }
        TYPE_FIELD = field;
        SLOT_AMOUNT = method;
    }


    private static int capacityOf(Object handler, ItemStack probe) {
        int perSlot = 32;
        if (TYPE_FIELD != null && SLOT_AMOUNT != null) {
            try {
                Object type = TYPE_FIELD.get(handler);
                perSlot = (Integer) SLOT_AMOUNT.invoke(type);
            } catch (Throwable ignored) {
                perSlot = 32;
            }
        }
        // 抽屉的模型是“一份记个数量”，工具进去会被算成一份，拿出来一叠、用起来一起掉耐久。
        // The drawer stores one stack plus a count; tools would collapse into that and wear together.
        if (probe != null && !probe.isEmpty() && probe.isDamageableItem()) {
            return 1;
        }
        long capacity = (long) perSlot * UltraStacksConfig.maxStackSize();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(64L, capacity));
    }

    /**
     * 两个重载都得管，只改一个会出现“塞得进、取不出”。
     *
     * Both overloads have to agree, otherwise inserting and extracting use different maths.
     */
    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"), require = 0)
    private int ultrastacks$capacity(int original, int slot) {
        // FS 对“虚拟槽”返回 MAX_VALUE，那是哨兵值，别压下来。
        // FS returns MAX_VALUE for the virtual slot; that is a sentinel, don't cap it.
        if (original == Integer.MAX_VALUE) {
            return original;
        }
        return capacityOf(this, storedTemplate(slot));
    }

    /** 读这一格存的模板栈。 */
    private ItemStack storedTemplate(int slot) {
        try {
            List<BigStack> stored = getStoredStacks();
            if (slot >= 0 && slot < stored.size()) {
                return stored.get(slot).getStack();
            }
        } catch (Throwable ignored) {
            // 读不到就按普通物品处理，别让诊断反而把抽屉弄坏。
        }
        return ItemStack.EMPTY;
    }

    /** 带栈的那个重载，同上。 */
    @ModifyReturnValue(method = "getSlotLimit(ILnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), require = 0)
    private int ultrastacks$capacityForStack(int original, int slot, ItemStack stack) {
        // 同上，虚拟槽的哨兵值原样放回。Same sentinel rule as above.
        if (original == Integer.MAX_VALUE) {
            return original;
        }
        return capacityOf(this, stack);
    }

    /** 单次取出最多一组。 */
    @ModifyVariable(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private int ultrastacks$capExtract(int amount) {
        return Math.min(amount, UltraStacksConfig.maxStackSize());
    }

    @Shadow
    public abstract List<BigStack> getStoredStacks();

    /** 模板上限按配置写，别把整格容量锁死。 */
    @Redirect(method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"), require = 0)
    private int ultrastacks$templateSize(ItemStack stack) {
        // 耐久物品的模板就是 1，别把 65536 写进去。
        // Damageable items keep a template of 1 — never write 65536 into them.
        if (stack.isDamageableItem()) {
            return 1;
        }
        return UltraStacksConfig.maxStackSize();
    }

    /** 判同只看物品，别认旧组件。 */
    @Redirect(method = "isValid(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 0)
    private static boolean ultrastacks$sameItem(ItemStack stored, ItemStack incoming) {
        return ItemStack.isSameItem(stored, incoming);
    }

    /** GUI 读到的这份也洗。 */
    @ModifyReturnValue(method = "getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 0)
    private ItemStack ultrastacks$cleanStored(ItemStack original) {
        return UltraStacksConfig.stripDefaultStackSize(original);
    }

    /** 真被拿走的那份也洗。 */
    @ModifyReturnValue(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 0)
    private ItemStack ultrastacks$cleanExtracted(ItemStack original) {
        return UltraStacksConfig.stripDefaultStackSize(original);
    }

    /** 进门先洗，别让脏模板再被复制。 */
    @ModifyVariable(method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private ItemStack ultrastacks$healIncoming(ItemStack stack) {
        return UltraStacksConfig.stripDefaultStackSize(stack);
    }

    /** 读档一次性洗净旧模板，顺手把负数格归零。 */
    @Inject(method = "deserializeNBT(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("RETURN"), require = 0)
    private void ultrastacks$healLoadedTemplates(CallbackInfo ci) {
        List<BigStack> stored = this.getStoredStacks();
        if (stored == null) {
            return;
        }
        for (BigStack bigStack : stored) {
            UltraStacksConfig.stripDefaultStackSize(bigStack.getStack());
            UltraStacksConfig.stripDefaultStackSize(bigStack.getSlotStack());
            if (bigStack.getAmount() < 0) {
                bigStack.setAmount(0);
                bigStack.setStack(ItemStack.EMPTY);
            }
        }
    }
}
