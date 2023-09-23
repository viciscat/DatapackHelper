package me.viciscat.datapack_helper.client;

import gg.essential.elementa.ElementaVersion;
import gg.essential.elementa.UIComponent;
import gg.essential.elementa.WindowScreen;
import gg.essential.elementa.components.UIBlock;
import gg.essential.elementa.components.input.UIMultilineTextInput;
import gg.essential.elementa.constraints.CenterConstraint;
import gg.essential.elementa.constraints.ChildBasedSizeConstraint;
import gg.essential.elementa.constraints.PixelConstraint;
import gg.essential.elementa.constraints.animation.AnimatingConstraints;
import gg.essential.elementa.constraints.animation.Animations;
import gg.essential.elementa.effects.ScissorEffect;
import kotlin.Unit;

import java.awt.*;

public class DatapackEditorScreen extends WindowScreen {
    UIComponent box = new UIBlock()
            .setX(new CenterConstraint())
            .setY(new PixelConstraint(10f))
            .setWidth(new ChildBasedSizeConstraint())
            .setHeight(new ChildBasedSizeConstraint())
            .setColor(new Color(50, 50, 50))
            .setChildOf(getWindow())
            .enableEffect(new ScissorEffect());

    UIComponent textInput = new UITextEditor("")
            .setX(new CenterConstraint())
            .setY(new CenterConstraint())
            .setWidth(new PixelConstraint(500f))
            .setHeight(new PixelConstraint(500f)).setChildOf(box);

    public DatapackEditorScreen() {
        super(ElementaVersion.V4);
        box.onMouseEnterRunnable(() -> {
            // Animate, set color, etc.
            AnimatingConstraints anim = box.makeAnimation();
            anim.setWidthAnimation(Animations.OUT_EXP, 0.5f, new ChildBasedSizeConstraint(2f));
            anim.onCompleteRunnable(() -> {
                // Trigger new animation or anything.
            });
            box.animateTo(anim);
        });
        textInput.onMouseClick((uiComponent, uiClickEvent) -> {
            uiComponent.grabWindowFocus();
            return Unit.INSTANCE;
        });
    }
}