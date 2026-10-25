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
 * <p>原版 {@code dropItemStack} 用 {@code stack.split(random.nextInt(21) + 10)} 循环拆分，每次掉 10 到 30
 * 个；一个 65536 的堆叠会变成约三千个物品实体，服务端要逐个生成、客户端要逐个渲染。这里对超过
 * {@code 128} 的堆叠直接整堆生成一个实体并取消原逻辑，普通堆叠仍走原版散落。落点用传入的坐标，初速度
 * 的三个分量与原版相同。
 *
 * <p>Container drop size: a large stack leaves a broken container as one entity instead of a field of
 * small piles.
 *
 * <p>Vanilla {@code dropItemStack} loops over {@code stack.split(random.nextInt(21) + 10)} and drops 10
 * to 30 items per entity; a 65536 stack therefore becomes roughly three thousand item entities, each one
 * spawned on the server and rendered on every client. Here a stack above {@code 128} produces a single
 * entity and the rest of the vanilla method is cancelled, while ordinary stacks still scatter the vanilla
 * way. The entity spawns at the coordinates passed in, with the same three velocity components vanilla
 * uses.
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
