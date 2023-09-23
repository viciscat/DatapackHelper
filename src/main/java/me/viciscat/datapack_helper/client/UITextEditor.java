package me.viciscat.datapack_helper.client;


import gg.essential.elementa.components.input.AbstractTextInput;
import gg.essential.elementa.constraints.PixelConstraint;
import gg.essential.universal.UGraphics;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UScreen;
import kotlin.Pair;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class UITextEditor extends AbstractTextInput {

    protected float targetHorizontalScrollingOffset = 0f;
    protected float longestLineWidth = 0f;

    public UITextEditor(@NotNull String placeholder, boolean shadow, @NotNull Color selectionBackgroundColor, @NotNull Color selectionForegroundColor, boolean allowInactiveSelection, @NotNull Color inactiveSelectionBackgroundColor, @NotNull Color inactiveSelectionForegroundColor, @NotNull Color cursorColor) {
        super(placeholder, shadow, selectionBackgroundColor, selectionForegroundColor, allowInactiveSelection, inactiveSelectionBackgroundColor, inactiveSelectionForegroundColor, cursorColor);
        onMouseScrollConsumer(uiScrollEvent -> {
            if (UScreen.hasShiftDown()) {
                float widthDifference = getWidth() - longestLineWidth;
                if (widthDifference > 0) return;
                targetHorizontalScrollingOffset =
                        (float) MathHelper.clamp((targetHorizontalScrollingOffset + uiScrollEvent.getDelta() * getLineHeight()), widthDifference, 0);
                uiScrollEvent.stopPropagation();
            }
        });
    }

    public UITextEditor(@NotNull String placeholder) {
        this(placeholder, true, Color.WHITE, new Color(64, 139, 229), false, new Color(176, 176, 176), Color.WHITE, Color.WHITE);
    }

    @NotNull
    @Override
    public String getText() {
        return String.join("\n", getTextualLines().stream().map(textualLine -> textualLine.getText()).toList());
    }

    @Override
    protected void onEnterPressed() {
        commitTextAddition("\n");
        getUpdateAction().invoke(getText());

    }

    @Override
    protected void recalculateDimensions() {
        float width = 0;
        for (TextualLine textualLine : getTextualLines()) {
            float textWidth = UGraphics.getStringWidth(textualLine.getText()) * getTextScale();
            if (textWidth > width) {
                width = textWidth;
            }
        }
        this.longestLineWidth = width;
    }

    @NotNull
    @Override
    protected LinePosition screenPosToVisualPos(float x, float y) {
        float localX = x - getHorizontalScrollingOffset();
        float localY = y - getVerticalScrollingOffset();

        if (localY <= 0)
            return new LinePosition(0, 0, true);

        int line = (int) (localY / (getLineHeight() * getTextScale()));
        if (line > getVisualLines().size() - 1)
            return new LinePosition(getVisualLines().size() - 1, getVisualLines().get(getVisualLines().size() - 1).getText().length(), true);

        String text = getVisualLines().get(line).getText();
        int column = 0;
        float currWidth = 0f;

        if (localX <= 0)
            return new LinePosition(line, 0, true);
        if (localX >= getWidth())
            return new LinePosition(line, getVisualLines().get(line).getText().length(), true);

        for (char c : text.toCharArray()) {
            float charWidth = UGraphics.getCharWidth(c) * getTextScale();
            if (currWidth + (charWidth / 2) >= x)
                return new LinePosition(line, column, true);

            currWidth += charWidth;
            column++;
        }

        return new LinePosition(line, column, true);
    }

    @Override
    protected void scrollIntoView(@NotNull AbstractTextInput.LinePosition linePosition) {
        LinePosition visualPos = linePosition.toVisualPos();

        String text = getVisualLines().get(visualPos.getLine()).getText();

        float visualLineOffset = visualPos.getLine() * -getLineHeight();
        float visualColumnOffset = -UGraphics.getStringWidth(text.substring(0, visualPos.getColumn())) * getTextScale();
        float columnWidth = text.isEmpty() ? 0f: UGraphics.getCharWidth(text.charAt(Math.max(0, visualPos.getColumn()-1))) * getTextScale();

        if (getTargetVerticalScrollingOffset() < visualLineOffset) {
            setTargetVerticalScrollingOffset(visualLineOffset);
        } else if (visualLineOffset - getLineHeight() < getTargetVerticalScrollingOffset() - getHeight()) {
            setTargetVerticalScrollingOffset(getTargetVerticalScrollingOffset() + visualLineOffset - getLineHeight() - (getTargetVerticalScrollingOffset() - getHeight()));
        }
        if (targetHorizontalScrollingOffset < visualColumnOffset) {
            targetHorizontalScrollingOffset = visualColumnOffset;
        } else if (visualColumnOffset - columnWidth < targetHorizontalScrollingOffset - getWidth()) {
            targetHorizontalScrollingOffset += visualColumnOffset - columnWidth - (targetHorizontalScrollingOffset - getWidth());
        }
    }

    @Override
    public void draw(@NotNull UMatrixStack matrixStack) {

        beforeDraw(matrixStack);

        if (hasSelection()) {
            getCursorComponent().hide(true);
        } else if (getActive()) {
            getCursorComponent().unhide(true);
            Pair<Float, Float> screenCursorPosition = getCursor().toScreenPos();
            float x = screenCursorPosition.getFirst();
            float y = screenCursorPosition.getSecond();
            getCursorComponent().setX(new PixelConstraint(x, false, false));
            getCursorComponent().setY(new PixelConstraint(y, false, false));
        }

        Pair<LinePosition, LinePosition> selection = getSelection();
        LinePosition selectionStart = selection.getFirst();
        LinePosition selectionEnd = selection.getSecond();

        List<VisualLine> visualLines = getVisualLines();
        for (int i = 0; i < visualLines.size(); i++) {
            float topOffset = (getLineHeight() * i * getTextScale()) + getVerticalScrollingOffset();
            if (topOffset < -getLineHeight() * getTextScale() || topOffset > getHeight() + getLineHeight() * getTextScale())
                continue;

            if (!hasSelection() || i < selectionStart.getLine() || i > selectionEnd.getLine()) {
                drawUnselectedText(matrixStack, visualLines.get(i).getText(), getLeft(), i);
            } else {
                String startText = i == selectionStart.getLine() && selectionStart.getColumn() > 0 ?
                        visualLines.get(i).getText().substring(0, selectionStart.getColumn()): "";

                String selectedText;
                if (selectionStart.getLine() == selectionEnd.getLine()) {
                    selectedText = visualLines.get(i).getText().substring(selectionStart.getColumn(), selectionEnd.getColumn());
                } else if (i > selectionStart.getLine() && i < selectionEnd.getLine()) {
                    selectedText = visualLines.get(i).getText();
                } else if (i == selectionStart.getLine()) {
                    selectedText = visualLines.get(i).getText().substring(selectionStart.getColumn());
                } else if (i == selectionEnd.getLine()) {
                    selectedText = visualLines.get(i).getText().substring(0, selectionEnd.getColumn());
                } else {
                    selectedText = "";
                }

                String endText = i == selectionEnd.getLine() && selectionEnd.getColumn() < visualLines.get(i).getText().length() ?
                        visualLines.get(i).getText().substring(selectionEnd.getColumn()): "";

                float startTextWidth = UGraphics.getStringWidth(startText) * getTextScale();
                float selectedTextWidth = UGraphics.getStringWidth(selectedText) * getTextScale();

                float newlinePadding = i < selectionEnd.getLine() ? UGraphics.getStringWidth(" ") * getTextScale() : 0f;

                if (!startText.isEmpty())
                    drawUnselectedText(matrixStack, startText, getLeft(), i);

                if (!selectedText.isEmpty() || newlinePadding != 0f) {
                    drawSelectedText(
                            matrixStack,
                            selectedText,
                            getLeft() + startTextWidth,
                            getLeft() + startTextWidth + selectedTextWidth + newlinePadding,
                            i
                    );
                }

                if (!endText.isEmpty())
                    drawUnselectedText(matrixStack, endText, getLeft() + startTextWidth + selectedTextWidth, i);
            }
        }
        super.draw(matrixStack);
    }

    @NotNull
    @Override
    protected List<String> textToLines(@NotNull String s) {
        if (s.equals("\n")) {
            return List.of("", "");
        }
        return List.of(s.split("\n"));
    }

    @Override
    protected void recalculateAllVisualLines() {
        getVisualLines().clear();

        List<TextualLine> textualLines = getTextualLines();
        for (int i = 0; i < textualLines.size(); i++) {
            getVisualLines().add(new VisualLine(textualLines.get(i).getText(), i));
        }
    }

    @Override
    public void animationFrame() {
        float diff = (targetHorizontalScrollingOffset + getHorizontalScrollingOffset()) * 0.1f;
        if (Math.abs(diff) < .25f)
            setHorizontalScrollingOffset(-targetHorizontalScrollingOffset);
        setHorizontalScrollingOffset(getHorizontalScrollingOffset() - diff);

        super.animationFrame();
    }
}
