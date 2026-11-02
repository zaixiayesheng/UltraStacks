package com.zaixiayesheng.ultrastacks.display.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 取走一件改名产物后，不要把名字框刷回物品原名。
 *
 * <p>原版每格只放一件，改名一次就把整格清空，所以它可以在左侧槽位变化时把名字框刷成物品原名。
 * 本模组是「一次改一件」，左槽每取一次只是数量减一，名字框要是跟着刷回原名，下一次计算就会认为
 * 没改名，产物直接消失——界面上看起来就是“改名被撤销、东西没了”。
 *
 * <p>Vanilla refreshes the name box from the left slot whenever that slot changes, which is fine when
 * a rename empties the slot in one go. Here a rename consumes one item at a time, so refreshing would
 * drop the typed name and the next result computation would find "no change" and clear the output.
 * This keeps the typed name when the left slot merely lost one item of the same kind.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilNameKeepMixin {

    @Shadow
    private EditBox name;

    private Item ultrastacks$lastItem;
    private int ultrastacks$lastCount;
    private boolean ultrastacks$seen;

    @Inject(method = "slotChanged", at = @At("HEAD"))
    private void ultrastacks$remember(AbstractContainerMenu menu, int slot, ItemStack stack, CallbackInfo ci) {
        if (slot != 0) {
            return;
        }
        ultrastacks$keepValue = name == null ? null : name.getValue();
    }

    @Inject(method = "slotChanged", at = @At("TAIL"))
    private void ultrastacks$restore(AbstractContainerMenu menu, int slot, ItemStack stack, CallbackInfo ci) {
        if (slot != 0 || name == null) {
            return;
        }
        // 批量一次可能少掉几十上百件，所以判断条件是“同种物品、数量变少、还没见底”，
        // 不能写死成只少 1 个——那样按住 shift 批一次之后名字就会被刷回原名。
        // A batch can drop dozens of items at once, so the rule is "same item, fewer left, not empty";
        // requiring exactly one fewer would let a batch reset the name again.
        boolean keep = ultrastacks$keepValue != null && !ultrastacks$keepValue.isEmpty()
                && ultrastacks$seen
                && ultrastacks$lastItem == stack.getItem()
                && stack.getCount() > 0
                && stack.getCount() < ultrastacks$lastCount;
        ultrastacks$lastItem = stack.getItem();
        ultrastacks$lastCount = stack.getCount();
        ultrastacks$seen = true;
        if (keep) {
            name.setValue(ultrastacks$keepValue);
        }
    }

    private String ultrastacks$keepValue;
}
