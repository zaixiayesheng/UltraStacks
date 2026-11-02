package com.zaixiayesheng.ultrastacks.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取铁砧/锻造台的输入容器。
 *
 * <p>The input container is declared on {@link ItemCombinerMenu}, not on {@code AnvilMenu}, so a
 * {@code @Shadow} in the anvil mixin cannot see it. This accessor reaches the same field.
 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {

    @Accessor("inputSlots")
    Container ultrastacks$inputSlots();

    @Accessor("resultSlots")
    ResultContainer ultrastacks$resultSlots();

    @Accessor("player")
    Player ultrastacks$player();
}
