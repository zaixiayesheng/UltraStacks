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
 * 抽屉的容量，还有一次能进出多少，Functional Storage 全是拿「格数 × 64」推的。
 * 64 的时候看不出问题，抬到 65536 就露馅了：界面列出来两百多万，实际塞到两千零四十八就再也进不去，
 * 取也取不动——因为算容量那句还拿 64 当基准。两个重载都改成直接给真容量，跟界面显示对上，
 * 顺带也躲开了原来那套乘法在 int 上的溢出（这正是「抽屉 NBT 里出现负的空气」的来历，见文末）。
 *
 * <p>FS works out drawer capacity from "slots × 64". Fine at 64, broken at 65536: the GUI
 * advertises millions while insert and extract still settle on two thousand and forty-eight.
 * Both overloads now return the real capacity, which also dodges the int overflow behind the
 * "negative air" entries — see the note at the bottom.</p>
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

    /**
     * 两个 getSlotLimit 都走这里：读一下这格是几格并存（1×/2×/4×），乘上配置上限。
     * 反射只做一次就缓存住，真拿不到字段就按 FS 默认的每格 32 个算。
     *
     * <p>Shared by both overloads: read the drawer type's slot amount, multiply by the configured
     * limit. Reflection is resolved once; if it fails we keep FS's default of 32 per slot.</p>
     */
    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"), require = 0)
    private int ultrastacks$capacity(int original) {
        return capacityOf(this);
    }

    /**
     * 带物品的那个重载，道理一样。这俩必须一起改，只改一个的话插入和取出会各算各的容量。
     *
     * <p>The stack-aware overload, same reasoning. Both must change together or insert and extract
     * end up disagreeing about how much fits.</p>
     */
    @ModifyReturnValue(method = "getSlotLimit(ILnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), require = 0)
    private int ultrastacks$capacityForStack(int original, int slot, ItemStack stack) {
        return capacityOf(this);
    }

    /**
     * 这里踩过坑，记一下局部变量表：extractItem(int slot, int amount, boolean simulate) 里
     * this = 0、slot = 1、amount = 2、simulate = 3。之前写成 index = 1，改的其实是 slot——
     * 都是 int，静默挂上了，真正的 amount 一直没人管，于是「一次只取一组」压根没生效。
     *
     * <p>Local variable table gotcha: in extractItem(int slot, int amount, boolean simulate),
     * this = 0, slot = 1, amount = 2, simulate = 3. The earlier index = 1 silently hit slot
     * instead of amount, so the "extract one stack at a time" cap never worked.</p>
     */
    @ModifyVariable(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private int ultrastacks$capExtract(int amount) {
        return Math.min(amount, UltraStacksConfig.maxStackSize());
    }

    @Shadow
    public abstract List<BigStack> getStoredStacks();

    /**
     * 抽屉第一次收到东西时，会拿手里那份栈的 getMaxStackSize() 当这一格的模板上限——手里往往就一小撮，
     * 还带着原版 64 的组件，问出来是 64，整格容量就永远塌在「格数 × 64」：界面写着两百多万，实际只收两千。
     * 模板得取配置里的全局值，不能跟着手里那一小撮走。
     *
     * <p>On first insert the drawer asks the incoming stack for getMaxStackSize() and stores that as
     * the slot template. Incoming stacks are often a handful of items still carrying the vanilla 64
     * component, so the answer is 64 and the slot stays collapsed forever. The template must take the
     * configured limit instead.</p>
     */
    @Redirect(method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"), require = 0)
    private int ultrastacks$templateSize(ItemStack stack) {
        return UltraStacksConfig.maxStackSize();
    }

    /**
     * 判「这格收不收你」的时候，原版拿模板栈和进来的栈比 isSameItemSameComponents。可模板身上可能还挂着
     * 旧的 64 组件，新来的干净栈没有，一比就说不相等——结果一个存着东西的抽屉，谁塞都拒收。
     * 这里只比物品本身，组件差异交给下面剥组件那几步去处理。
     *
     * <p>Original compares the template against the incoming stack with isSameItemSameComponents.
     * A template still carrying the legacy 64 component never equals a clean stack, so a drawer that
     * already holds something rejects everything. We compare the item alone and let the stripping
     * steps below deal with components.</p>
     */
    @Redirect(method = "isValid(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 0)
    private static boolean ultrastacks$sameItem(ItemStack stored, ItemStack incoming) {
        return ItemStack.isSameItem(stored, incoming);
    }

    /**
     * 界面读这一格时顺手洗一下，别让旧存档的脏模板露在 GUI 上。
     *
     * <p>Cleans the stack on its way to the GUI so legacy dirty templates stay hidden.</p>
     */
    @ModifyReturnValue(method = "getStackInSlot(I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 0)
    private ItemStack ultrastacks$cleanStored(ItemStack original) {
        return UltraStacksConfig.stripDefaultStackSize(original);
    }

    /**
     * 真被玩家或管道拿走的那份，同样洗一下——这是「一取出来就脏」最直接的出口。
     *
     * <p>Also cleans what the player or a pipe actually receives — the most direct exit for
     * "it went dirty the moment I pulled it out".</p>
     */
    @ModifyReturnValue(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), require = 0)
    private ItemStack ultrastacks$cleanExtracted(ItemStack original) {
        return UltraStacksConfig.stripDefaultStackSize(original);
    }

    /**
     * 东西进门之前先洗。这步最要紧：脏模板一旦被 setStack 复制过去，之后每次取出都会再造一个脏副本，
     * 永远洗不干净。在入口剥掉，模板从出生就是干净的。
     *
     * <p>Heal it at the door. A dirty template copied by setStack keeps minting dirty copies forever;
     * stripping on the way in keeps every new template clean from birth.</p>
     */
    @ModifyVariable(method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, index = 2, require = 0)
    private ItemStack ultrastacks$healIncoming(ItemStack stack) {
        return UltraStacksConfig.stripDefaultStackSize(stack);
    }

    /**
     * 读档后把每格的模板栈和显示栈都洗一遍，旧存档里的脏模板一次性消失。
     * 顺手收拾历史遗留的负数：老版本的 getSlotLimit 是 total × maxStackSize / 64，整格塞满时
     * 这个乘法在 int 上会翻到负数，插入逻辑拿到负的余量就把 Amount 写成了负数——
     * 界面上看着就是「空气 × 负多少」。这种格子直接清零。
     *
     * <p>After loading, heal every slot's template and display stack. Also cleans up legacy negative
     * amounts: the old getSlotLimit used total × maxStackSize / 64, which overflows to a negative int
     * when a slot is full, and insert then wrote a negative Amount — which reads as "air × minus
     * something". Those slots get reset to empty.</p>
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
            if (bigStack.getAmount() < 0) {
                bigStack.setAmount(0);
                bigStack.setStack(ItemStack.EMPTY);
            }
        }
    }
}
