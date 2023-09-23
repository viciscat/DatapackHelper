package me.viciscat.datapack_helper.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MultilineTextEditor class represents a widget that allows editing and rendering of multiple lines of text.
 * It extends the BiDirectionalScrollableWidget class.
 */
public class MultilineTextEditor extends BiDirectionalScrollableWidget {

    private final TextRenderer textRenderer;
    private final List<String> textLines = new ArrayList<>();

    private final Map<CacheType, Object> cache = new HashMap<>();
    private final int[] cursorCoords = new int[]{0, 0};
    private final Selection sel = new Selection();

    public MultilineTextEditor(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(x, y, width, height, text);
        this.textRenderer = textRenderer;

        cache.put(CacheType.CONTENTS_WIDTH, null);
    }

    @Override
    protected int getContentsWidth() {
        if (cache.get(CacheType.CONTENTS_WIDTH) == null) {
            int longestWidth = 0;
            for (String textLine : textLines) {
                int textWidth = this.textRenderer.getWidth(textLine);
                if (textWidth > longestWidth) {
                    longestWidth = textWidth;
                }
            }
            cache.put(CacheType.CONTENTS_WIDTH, longestWidth);
        }
        return (Integer) cache.get(CacheType.CONTENTS_WIDTH);
    }

    @Override
    protected double getDeltaXPerScroll() {
        return 0;
    }

    @Override
    protected int getContentsHeight() {
        return this.textRenderer.fontHeight * this.textLines.size();
    }

    @Override
    protected double getDeltaYPerScroll() {
        return 0;
    }

    @Override
    protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean bool = switch (keyCode) {
            case 335, 257 -> {
                lineBreak();
                yield true;
            }
            case 265 -> {
                if (sel.start[1] > 0) {
                    sel.start[1] -= 1;
                }
                yield true;
            }
            case 264 -> {
                if (sel.start[1] < this.textLines.size() - 1) {
                    sel.start[1] += 1;
                }
                yield true;
            }
            case 263 -> {
                if (sel.start[0] == 0) {
                    if (sel.start[1] > 0) {
                        sel.start[1] -= 1;
                        sel.start[0] = textLines.get(sel.start[1]).length() - 1;
                    }
                } else {
                    sel.start[0] -= 1;
                }
                yield true;
            }
            case 262 -> {
                if (sel.start[0] == textLines.get(sel.start[1]).length() - 1) {
                    if (sel.start[1] < textLines.size() - 1) {
                        sel.start[1] += 1;
                        sel.start[0] = 0;
                    }
                } else {
                    sel.start[0] += 1;
                }
                yield true;
            }
            case 259 -> {
                if (!Arrays.equals(sel.start, sel.end)) {
                    deleteSelected();
                }
                yield true;
            }

            default -> false;
        };
        if (bool && !Screen.hasShiftDown()) {
            this.sel.end = this.sel.start;
        }
        return bool;
    }

    private void lineBreak() {
        String currentLine = this.textLines.get(this.cursorCoords[1]);
        String line = currentLine.substring(0, this.cursorCoords[0]);
        String lineRemainder = currentLine.substring(this.cursorCoords[0]);
        this.textLines.set(this.cursorCoords[1], line);
        this.textLines.add(this.cursorCoords[1] + 1, lineRemainder);
        this.invalidateCache(CacheType.CONTENTS_WIDTH);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {

        if (!(this.visible && this.isFocused())) {
            return false;
        }
        this.invalidateCache(CacheType.CONTENTS_WIDTH);
        insert(String.valueOf(chr));
        return true;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    private void invalidateCache(CacheType cacheType) {
        this.cache.put(cacheType, null);
    }

    private void insert(String s) {
        if (!Arrays.equals(sel.start, sel.end)) {
            deleteSelected();
        }
        if (s.contains("\n")) {
            String string = s.replaceAll("\r", "");
            String[] newTextLines = string.split("\n");
            String currentLine = textLines.get(sel.start[1]);
            String lineEnd = currentLine.substring(sel.start[0]);
            String lineStart = currentLine.substring(0, sel.start[0]);

            textLines.set(sel.start[1], lineStart + newTextLines[0]);
            textLines.add(sel.start[1] + 1, newTextLines[newTextLines.length - 1] + lineEnd);
            for (int i = newTextLines.length - 2; i > 0; i--) {
                textLines.add(sel.start[1] + 1, newTextLines[i]);
            }
        } else {
            StringBuilder sb = new StringBuilder(this.textLines.get(this.sel.start[1]));
            sb.insert(this.sel.start[0], s);
            this.textLines.set(this.sel.start[1], sb.toString());
        }
    }

    private void deleteSelected() {
        if (Arrays.equals(sel.end, sel.start)) return;
        if (sel.start[1] == sel.end[1]) {
            StringBuilder sb = new StringBuilder(this.textLines.get(this.sel.start[1]));
            sb.delete(Math.min(sel.start[0], sel.end[0]), Math.max(sel.start[0], sel.end[0]));
            this.textLines.set(this.sel.start[1], sb.toString());
            sel.end = sel.start;
            return;
        }
        int linesDeleted = 0;
        int[] startPos = sel.start[1] < sel.end[1] ? sel.start : sel.end;
        int[] endPos = sel.start[1] > sel.end[1] ? sel.start : sel.end;
        for (int i = startPos[1]; i < endPos[1] + 1; i++) {
            if (i == startPos[1]) {
                textLines.set(i - linesDeleted, textLines.get(i - linesDeleted).substring(0, startPos[0]));
            } else if (i == endPos[1]) {
                textLines.set(i - linesDeleted, textLines.get(i - linesDeleted).substring(endPos[0]));
            } else {
                textLines.remove(i - linesDeleted);
                linesDeleted += 1;
            }
        }
    }

    private enum CacheType {
        CONTENTS_WIDTH
    }

    private static class Selection {

        public int[] start = new int[]{0,0};
        public int[] end = new int[]{0,0};
    }
}
