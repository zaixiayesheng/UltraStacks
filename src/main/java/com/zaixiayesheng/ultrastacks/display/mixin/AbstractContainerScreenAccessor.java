package com.zaixiayesheng.ultrastacks.display.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读 {@code AbstractContainerScreen#hoveredSlot}。
 *
 * <p>26.1.2 把唯一那个公开读法 {@code getSlotUnderMouse()} 标成了 {@code [removal]} 待删除，字段本身
 * 还是 protected 的。这里直接开一个访问口，既不碰待删除的 API，也不改动任何判定。
 *
 * <p>Reads {@code AbstractContainerScreen#hoveredSlot}. 26.1.2 marks the only public reader —
 * {@code getSlotUnderMouse()} — as {@code [removal]}, while the field itself stays protected, so this
 * opens an accessor instead of calling a method that is on its way out.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("hoveredSlot")
    Slot ultrastacks$hoveredSlot();
}
