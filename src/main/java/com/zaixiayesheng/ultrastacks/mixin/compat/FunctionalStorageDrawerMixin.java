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
 * 抽屉的容量与单次吞吐都按「格数 × 原版 64」算，抬高堆叠上限后这个假设失效：显示出来的容量是
 * {@code slotAmount × 65536}，但插入/取出仍按 {@code slotAmount × 64} 结算，于是只能进 2048。
 *
 * <p>这里把 {@code getSlotLimit} 的两个重载都改成真实容量 {@code slotAmount × 配置上限}，与显示一致；
 * 不再做 {@code total × maxStackSize / 64} 那套乘法，也就没有 int 溢出。
 *
 * <p>Functional Storage derives both the drawer capacity and its per-operation throughput from
 * {@code slotAmount × 64}. Once the stack limit is raised, the display shows {@code slotAmount × 65536}
 * while inserting and extracting still settle on {@code slotAmount × 64}. Both overloads of
 * {@code getSlotLimit} are redirected to the real capacity, which also avoids the int overflow in
 * {@code total × maxStackSize / 64}.
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


    private static int capacityOf(Object handler) {
        int perSlot = 32;
        if (TYPE_FIELD != null && SLOT_AMOUNT != null) {
            try {
                Object type = TYPE_FIELD.get(handler);
                perSlot = (Integer) SLOT_AMOUNT.invoke(type);
            } catch (Throwable ignored) {
                perSlot = 32;
            }
        }
        long capacity = (long) perSlot * UltraStacksConfig.maxStackSize();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(64L, capacity));
    }

    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"), require = 0)
    private int ultrastacks$capacity(int original) {
        return capacityOf(this);
    }

    @ModifyReturnValue(method = "getSlotLimit(ILnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), require = 0)
    private int ultrastacks$capacityForStack(int original, int slot, ItemStack stack) {
        return capacityOf(this);
    }

    /**
     * 注意局部变量表：{@code extractItem(int slot, int amount, boolean simulate)} 里
     * {@code this = 0}、{@code slot = 1}、{@code amount = 2}、{@code simulate = 3}。
     * 之前这里写成 {@code index = 1}，改的其实是 {@code slot}（同为 int 所以能静默挂上），
     * 真正的 {@code amount} 一直没人管。
     *
     * <p>Local variable table: in {@code extractItem(int slot, int amount, boolean simulate)},
     * {@code this = 0}, {@code slot = 1}, {@code amount = 2}, {@code simulate = 3}. The previous
     * {@code index = 1} silently targeted {@code slot} instead of {@code amount}.</p>
     */
    @ModifyVariable(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private int ultrastacks$capExtract(int amount) {
        return Math.min(amount, UltraStacksConfig.maxStackSize());
    }

    @Shadow
    public abstract List<BigStack> getStoredStacks();

    /**
     * 抽屉第一次收到物品时，会用它自己那份个数（常常只有 1 个，或者带着原版 64 组件）去问
     * {@code getMaxStackSize()}，再把答案存成这一格的模板栈。于是模板被写成 64，整格容量跟着塌回
     * {@code slotAmount × 64}：显示两百多万，实际只收两千。模板上限必须取配置里的全局值。
     */
    @Redirect(method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"), require = 0)
    private int ultrastacks$templateSize(ItemStack stack) {
        return UltraStacksConfig.maxStackSize();
    }

    /**
     * 模板栈带着旧的 64 组件，而放进来的干净栈没有这个组件，{@code isSameItemSameComponents} 直接判不相等，
     * 于是已经存在的抽屉拒绝一切新物品。这里只比物品本体，组件差异交给剥离那一步收尾。
     */
    @Redirect(method = "isValid(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 0)
    private static boolean ultrastacks$sameItem(ItemStack stored, ItemStack incoming) {
        return ItemStack.isSameItem(stored, incoming);
    }

    @ModifyReturnValue(method = "getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 0)
    private ItemStack ultrastacks$cleanStored(ItemStack original) {
        return UltraStacksConfig.stripDefaultStackSize(original);
    }

    @ModifyReturnValue(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 0)
    private ItemStack ultrastacks$cleanExtracted(ItemStack original) {
        return UltraStacksConfig.stripDefaultStackSize(original);
    }

    /**
     * 放进抽屉的栈先清掉「等于原版默认值」的 max_stack_size 组件：旧存档里的脏模板（显式 64）一旦被
     * {@code setStack} 复制成新模板，整格又会带着脏组件向外复制，永远洗不干净。源头剥掉后，
     * 模板从创建那一刻就是干净的。
     *
     * <p>Heals the incoming stack before it can become the slot template: a legacy template with an
     * explicit {@code max_stack_size = 64} copied by {@code setStack} would keep producing dirty
     * copies forever. Stripping at the source keeps every new template clean from birth.
     */
    @ModifyVariable(method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private ItemStack ultrastacks$healIncoming(ItemStack stack) {
        return UltraStacksConfig.stripDefaultStackSize(stack);
    }

    /**
     * 旧存档里的抽屉模板把原版 64 固化成了显式组件。读档后一次性把每个槽的模板与显示栈都剥干净，
     * 脏模板从根上消失，之后插入/取出都不会再复制出带 64 组件的栈。
     *
     * <p>Legacy worlds persist drawers whose template bakes the vanilla 64 into an explicit
     * component. Heal every slot's template and display stack right after load so no dirty stack is
     * ever copied out again.
     */
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
        }
    }
}
