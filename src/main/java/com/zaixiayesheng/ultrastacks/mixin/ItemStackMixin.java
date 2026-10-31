package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 存档里的堆叠数量。
     *
 * <p>The stack count stored in item data. Vanilla writes the count as a byte and reads it back with
 * {@code getByte}, so a stack above 127 is saved and then comes back as a negative number or a single
 * item, which corrupts the save. Above {@link Byte#MAX_VALUE} an int is written under the same key
 * instead, since NBT lets a value be overwritten by one of another type, and the read path takes it
 * over when the tag holds an int. Counts of 127 or less keep the vanilla byte, so their format does not
 * change at all.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    private int count;

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void ultrastacks$readLargeCount(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("Count", Tag.TAG_INT)) {
            this.count = tag.getInt("Count");
        }
    }

    @Inject(method = "save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void ultrastacks$writeLargeCount(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.count > Byte.MAX_VALUE) {
            // 超过 127 的数量原版按 byte 写，读出来是负数；用同名的 int 键把它盖掉
            tag.putInt("Count", this.count);
        }
    }
}
