package com.zaixiayesheng.ultrastacks.mixin;

import com.zaixiayesheng.ultrastacks.UltraStacksConfig;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Chest / barrel / shulker box limit.
 *
 * <p>{@code RandomizableContainerBlockEntity.setItem} clamps the incoming stack to
 * {@code this.getMaxStackSize()} (the {@code Container} default, 64 on 1.20.1), so anything above 64
 * is cut down as soon as it is placed into the block. The clamp is redirected to the configured limit.
 *
 * <p><b>中文</b> 战利品容器走容器层夹取，这里抬到配置值。
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