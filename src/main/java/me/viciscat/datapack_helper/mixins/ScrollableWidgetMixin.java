package me.viciscat.datapack_helper.mixins;

import net.minecraft.client.gui.widget.ScrollableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ScrollableWidget.class)
public class ScrollableWidgetMixin {
    @Accessor("PADDING")
    public static int getPadding() {
        throw new AssertionError();
    }
}
