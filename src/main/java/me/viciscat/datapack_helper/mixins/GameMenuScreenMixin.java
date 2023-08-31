package me.viciscat.datapack_helper.mixins;

import me.viciscat.datapack_helper.client.DatapackEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isInSingleplayer()Z"), method = "initWidgets", locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void addDatapackEditorButton(CallbackInfo ci, GridWidget gridWidget, GridWidget.Adder adder) {
        ButtonWidget widget = ButtonWidget.builder(Text.of("Datapack Editor"), (button) -> {
            MinecraftClient.getInstance().setScreen(new DatapackEditorScreen());
                }
        ).width(204).build();
        adder.add(widget, 2);
    }

}
