package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 网络同步里的物品数量。
     *
 * <p>The item count used in network packets. Vanilla writes the count as a byte and reads it back the
 * same way, so a client only ever sees -128 to 127: both 32768 and 65536 become zero and a large stack
 * shows up empty. Both the write and the read side use a VarInt instead, so counts survive the trip as
 * long as both ends run UltraStacks.
 */
@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufMixin {
    /**
     * 这个重载由 Forge 补丁加进来，不在 SRG 映射里，正式环境保持原名，所以 {@code remap = false}。
     * 原版的 {@code writeItem(ItemStack)} 只是转调它，正式环境改名成 SRG 后也仍然走到这里。
     * 写入顺序是「有无物品 → 物品 id → VarInt 数量 → 可选 NBT」，{@code readItem} 按同样顺序读回。
     *
     * <p>This overload is added by the Forge patch and is not part of the SRG mappings, so its name is
     * unchanged in production and the injector needs {@code remap = false}. Vanilla
     * {@code writeItem(ItemStack)} only delegates here, so it still lands in this method after remapping.
     * The layout is "presence flag, item id, VarInt count, optional NBT", which {@code readItem} mirrors.
     */
    @Inject(
            method = "writeItemStack(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/network/FriendlyByteBuf;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void ultrastacks$writeItemStack(ItemStack stack, boolean limitedTag,
                                           CallbackInfoReturnable<FriendlyByteBuf> cir) {
        FriendlyByteBuf self = (FriendlyByteBuf) (Object) this;
        if (stack.isEmpty()) {
            self.writeBoolean(false);
        } else {
            self.writeBoolean(true);
            Item item = stack.getItem();
            self.writeId(BuiltInRegistries.ITEM, item);
            self.writeVarInt(stack.getCount());

            CompoundTag tag = null;
            if (item.isDamageable(stack) || item.shouldOverrideMultiplayerNbt()) {
                tag = limitedTag ? stack.getShareTag() : stack.getTag();
            }
            self.writeNbt(tag);
        }
        cir.setReturnValue(self);
    }

    @Inject(method = "readItem()Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void ultrastacks$readItem(CallbackInfoReturnable<ItemStack> cir) {
        FriendlyByteBuf self = (FriendlyByteBuf) (Object) this;
        if (!self.readBoolean()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        Item item = self.readById(BuiltInRegistries.ITEM);
        int count = self.readVarInt();
        ItemStack stack = new ItemStack(item, count);
        stack.readShareTag(self.readNbt());
        cir.setReturnValue(stack);
    }
}