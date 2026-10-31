package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 容器被破坏时，大堆叠整堆掉成一个实体，而不是散成一地小堆。
     *
 * <p>Container drop size: a large stack leaves a broken container as one entity instead of a field of
 * small piles.
 */
@Mixin(Containers.class)
public abstract class ContainersDropStackMixin {

    private static final int ULTRASTACKS_SPLIT_THRESHOLD = 128;

    @Inject(method = "dropItemStack", at = @At("HEAD"), cancellable = true)
    private static void ultrastacks$dropAsSingleEntity(Level level, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (stack.getCount() <= ULTRASTACKS_SPLIT_THRESHOLD) {
            return;
        }
        ItemStack dropped = stack.split(stack.getCount());
        ItemEntity entity = new ItemEntity(level, x, y, z, dropped);
        entity.setDeltaMovement(
                level.getRandom().triangle(0.0, 0.11485000171139836),
                level.getRandom().triangle(0.2, 0.11485000171139836),
                level.getRandom().triangle(0.0, 0.11485000171139836));
        level.addFreshEntity(entity);
        ci.cancel();
    }
}
