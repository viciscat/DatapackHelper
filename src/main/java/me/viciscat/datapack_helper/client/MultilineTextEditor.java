package me.viciscat.datapack_helper.client;

import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MultilineTextEditor class represents a widget that allows editing and rendering of multiple lines of text.
 * It extends the BiDirectionalScrollableWidget class.
 */
public class MultilineTextEditor extends BiDirectionalScrollableWidget{

    private final TextRenderer textRenderer;
    private int longestLineWidth = 0;
    private int[] lineStarts;
    private Rect2i[] selectionRectangles;
    private String text = "";
    private final SelectionManager selectionManager = new SelectionManager(this::getText, this::setText, this::getClipboard, this::setClipboard, s -> true);
    private final Style monospaceStyle = Style.EMPTY.withFont(Identifier.of("datapack-helper", "default"));

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.recalculateLineStarts();
    }

    public String[] getLines() {
        return this.text.split("\n");
    }

    public int getLineCount() {
        return this.getLines().length;
    }

    private void setClipboard(String clipboard) {
        SelectionManager.setClipboard(MinecraftClient.getInstance(), clipboard);
    }

    private String getClipboard() {
        return SelectionManager.getClipboard(MinecraftClient.getInstance());
    }



    public MultilineTextEditor(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(x, y, width, height, text);
        this.textRenderer = textRenderer;
    }

    @Override
    protected int getContentsWidth() {
        return Math.max(this.longestLineWidth, this.width) + 4;
    }

    @Override
    protected double getDeltaXPerScroll() {
        return (double)this.textRenderer.fontHeight / 2.0;
    }

    @Override
    protected int getContentsHeight() {
        return this.textRenderer.fontHeight * this.getLineCount();
    }

    @Override
    protected double getDeltaYPerScroll() {
        return (double)this.textRenderer.fontHeight / 2.0;
    }

    @Override
    protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
        List<String> textLines = new ArrayList<>(List.of(this.getLines()));
        if (text.endsWith("\n")) {
            textLines.add("");
        }
        int y = this.getY() + this.getPadding();
        int startX = this.getX() + this.getPadding();
        // Selection
        for (Rect2i rect2i : selectionRectangles) {
            int i = rect2i.getX();
            int j = rect2i.getY();
            int k = i + rect2i.getWidth();
            int l = j + rect2i.getHeight();
            context.fill(i, j, k, l, -16776961);
        }
        int position = this.selectionManager.getSelectionStart();
        int currentLine = getLineFromPosition(position);
        int positionInLine = position - this.lineStarts[currentLine];
        int cursorDrawX = this.textRenderer.getWidth(getLine(currentLine, true).substring(0, positionInLine));
        int cursorDrawY = y + this.textRenderer.fontHeight * currentLine;
        context.drawText(this.textRenderer, cursorDrawY + " " + cursorDrawY, startX, this.getHeight() - 9, 0xFFFFFF, false);
        context.fill(
                cursorDrawX,
                cursorDrawY,
                cursorDrawX + 1,
                cursorDrawY + this.textRenderer.fontHeight,
                0xFFFFFFFF
        );

        // Text
        for (int line = 0; line < this.lineStarts.length; line++) {
            context.drawText(textRenderer, Text.literal(textLines.get(line)).fillStyle(monospaceStyle), startX, y, 0xFFFFFF, false);
            y += textRenderer.fontHeight;
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.recalculateLineStarts();
        if (!this.selectionManager.handleSpecialKey(keyCode)) {
            return switch (keyCode) {
                case 335, 257 -> {
                    this.selectionManager.insert("\n");
                    yield true;
                }
                case 265 -> {
                    moveCursorVertically(-1);
                    yield true;
                }
                case 264 -> {
                    moveCursorVertically(1);
                    yield true;
                }

                default -> false;
            };
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        updateWidth();
        if (!(this.visible && this.isFocused())) {
            return false;
        }
        return this.selectionManager.insert(chr);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void updateWidth() {
        int competitorWidth = 0;
        String[] textLines = this.getLines();
        for (String textLine : textLines) {
            competitorWidth = Math.max(
                    this.textRenderer.getWidth(withStyle(textLine)),
                    competitorWidth);
        }
        this.longestLineWidth = competitorWidth;

    }

    private Text withStyle(String string) {
        return withStyle(Text.literal(string));
    }

    private Text withStyle(MutableText text) {
        return text.setStyle(monospaceStyle);
    }

    private void recalculateLineStarts() {
        List<Integer> starts = new ArrayList<>(1);
        starts.add(0);
        int lineStartIndex = this.text.indexOf("\n") + 1;
        while (lineStartIndex > 0) {
            starts.add(lineStartIndex);
            lineStartIndex = this.text.indexOf("\n", lineStartIndex) + 1;
        }
        LoggerFactory.getLogger("datapack_helper").info(
                starts.stream().map(Object::toString)
                .collect(Collectors.joining(", ")));
        this.lineStarts = starts.stream().mapToInt(i -> i).toArray();

        int selectionStart = Math.min(this.selectionManager.getSelectionStart(), this.selectionManager.getSelectionEnd());
        int selectionEnd = Math.max(this.selectionManager.getSelectionStart(), this.selectionManager.getSelectionEnd());
        if (selectionStart == selectionEnd) {
            this.selectionRectangles = new Rect2i[]{};
        } else {
            ArrayList<Rect2i> rect2iArrayList = new ArrayList<>();
            int selectionStartLine = getLineFromPosition(selectionStart);
            int selectionEndLine = getLineFromPosition(selectionEnd);
            if (selectionStartLine == selectionEndLine) {
                int startPositionInLine = selectionStart - this.lineStarts[selectionStartLine];
                int endPositionInLine = selectionEnd - this.lineStarts[selectionEndLine];
                int calculatedWidth = this.textRenderer.getWidth(getLine(selectionStartLine, true).substring(startPositionInLine, endPositionInLine));
                rect2iArrayList.add(new Rect2i(
                        calculatedWidth,
                        this.getY() + this.getPadding() + this.textRenderer.fontHeight * selectionStartLine,
                        this.textRenderer.getWidth(getLine(selectionEndLine, true).substring(startPositionInLine, endPositionInLine)) - calculatedWidth,
                        textRenderer.fontHeight
                ));
            } else {
                for (int i = selectionStartLine; i <= selectionEndLine; i++) {
                    int startPositionInLine = Math.max(selectionStart - this.lineStarts[i], 0);
                    int endPositionInLine = Math.min(selectionEnd - this.lineStarts[i], getLine(i, true).length());
                    int calculatedWidth = this.textRenderer.getWidth(getLine(i, true).substring(startPositionInLine, endPositionInLine));
                    rect2iArrayList.add(new Rect2i(
                            calculatedWidth,
                            this.getY() + this.getPadding() + this.textRenderer.fontHeight * i,
                            this.textRenderer.getWidth(getLine(i, true).substring(startPositionInLine, endPositionInLine)) - calculatedWidth,
                            textRenderer.fontHeight
                    ));
                }
            }


        }
    }

    int getLineFromPosition(int position) {
        int i = Arrays.binarySearch(this.lineStarts, position);
        if (i < 0) {
            return -(i + 2);
        }
        return i;
    }

    void moveCursorVertically(int verticalOffset) {
        int position = this.selectionManager.getSelectionStart();
        int cursorPosition;
        int line = this.getLineFromPosition(position);
        int j = line + verticalOffset;
        if (0 <= j && j < this.lineStarts.length) {
            int positionInLine = position - this.lineStarts[line];

            int lineContentLength = this.getLine(line, false).length();
            cursorPosition = this.lineStarts[j] + Math.min(positionInLine, lineContentLength);
        } else {
            cursorPosition = position;
        }
        this.selectionManager.moveCursorTo(cursorPosition, Screen.hasShiftDown());
    }

    String getLine(int index, boolean includeLineBreak) {
        int lineEndIndex = this.text.indexOf("\n", this.lineStarts[index]) + (includeLineBreak ? 1 : 0);
        return this.text.substring(this.lineStarts[index], lineEndIndex == -1 ? this.text.length(): lineEndIndex);
    }

}
