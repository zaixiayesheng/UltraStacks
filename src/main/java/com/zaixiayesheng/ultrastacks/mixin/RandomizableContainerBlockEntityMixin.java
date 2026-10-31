package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 箱子、木桶、潜影盒这类战利品容器的槽位上限。
     *
 * <p>Slot limit of chests, barrels, shulker boxes and other loot containers. {@code setItem} clamps
 * incoming items to {@code getMaxStackSize()}, the {@code Container} default of 64, so anything above 64
 * is cut the moment it is placed into the block; the call is redirected to the configured stack size.
 */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin {

    @Redirect(
            method = "setItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;getMaxStackSize()I"),
            require = 0
    )
    private int ultrastacks$limitForSetItem(RandomizableContainerBlockEntity blockEntity) {
        return UltraStacksConfig.maxStackSize();
    }
}