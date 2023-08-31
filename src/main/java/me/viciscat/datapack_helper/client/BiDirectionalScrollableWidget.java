package me.viciscat.datapack_helper.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public abstract class BiDirectionalScrollableWidget extends ScrollableWidget {
    private boolean scrollbarYDragged;
    private boolean scrollbarXDragged;
    private double scrollX;

    public BiDirectionalScrollableWidget(int x, int y, int width, int height, Text text) {
        super(x, y, width, height, text);
    }

    protected abstract int getContentsWidth();

    protected abstract double getDeltaXPerScroll();

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.scrollbarYDragged = false;
            this.scrollbarXDragged = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean isMouseOnScrollbarY;
        boolean isMouseOnScrollbarX;
        if (!this.visible) {
            return false;
        }
        boolean isWithinBounds = this.isWithinBounds(mouseX, mouseY);
        isMouseOnScrollbarY = this.overflows() && mouseX >= (double) (this.getX() + this.width) && mouseX <= (double) (this.getX() + this.width + 8) && mouseY >= (double) this.getY() && mouseY < (double) (this.getY() + this.height);
        if (isMouseOnScrollbarY && button == 0) {
            this.scrollbarYDragged = true;
            return true;
        }
        isMouseOnScrollbarX = this.overflowsX() && mouseY >= (double) (this.getY() + this.height) && mouseY <= (double) (this.getY() + this.height + 8) && mouseX >= (double) this.getX() && mouseX < (double) (this.getX() + this.width);
        if (isMouseOnScrollbarX && button == 0) {
            this.scrollbarXDragged = true;
            return true;
        }
        return isWithinBounds || isMouseOnScrollbarY || isMouseOnScrollbarX;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!(this.visible && this.isFocused())) {
            return false;
        }
        if (this.scrollbarYDragged) {
            if (mouseY < (double) this.getY()) {
                this.setScrollY(0.0);
            } else if (mouseY > (double) (this.getY() + this.height)) {
                this.setScrollY(this.getMaxScrollY());
            } else {
                int i = this.getContentsHeight() + getPadding();
                double d = Math.max(1, this.getMaxScrollY() / (this.height - i));
                this.setScrollY(this.getScrollY() + deltaY * d);
            }
            return true;
        } else if (this.scrollbarXDragged) {
            if (mouseX < (double) this.getX()) {
                this.setScrollX(0.0);
            } else if (mouseX > (double) (this.getX() + this.height)) {
                this.setScrollX(this.getMaxScrollX());
            } else {
                int i = this.getContentsWidth() + getPadding();
                double d = Math.max(1, this.getMaxScrollX() / (this.height - i));
                this.setScrollX(this.getScrollX() + deltaX * d);
            }
            return true;

        } else return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.visible) {
            return false;
        }
        if (Screen.hasShiftDown()) {
            this.setScrollX(this.getScrollX() - amount * this.getDeltaXPerScroll());
        } else {
            this.setScrollY(this.getScrollY() - amount * this.getDeltaYPerScroll());
        }
        return true;
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }
        this.drawBox(context);
        context.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
        context.getMatrices().push();
        context.getMatrices().translate(-this.getScrollX(), -this.getScrollY(), 0.0);
        this.renderContents(context, mouseX, mouseY, delta);
        context.getMatrices().pop();
        context.disableScissor();
        this.renderOverlay(context);
    }

    protected void setScrollX(double scrollX) {
        this.scrollX = MathHelper.clamp(scrollX, 0.0, (double)this.getMaxScrollX());
    }

    protected int getMaxScrollX() {
        return Math.max(0, this.getContentsWidth() + getPadding() - (this.width - getPadding()));
    }

    protected double getScrollX() {
        return this.scrollX;
    }

    @Override
    protected void renderOverlay(DrawContext context) {
        super.renderOverlay(context);
        if (this.overflowsX()) {
            this.drawScrollbarX(context);
        }
    }

    protected boolean overflowsX() {
        return this.getContentsWidth() > this.getWidth();
    }

    private void drawScrollbarX(DrawContext context) {
        int i = this.getScrollbarThumbWidth();
        int scrollbarXPositionY = this.getY() + this.height;
        int scrollbarXEndPositionY = this.getY() + this.height + 8;
        int x = Math.max(this.getX(), (int)this.scrollX * (this.width - i) / this.getMaxScrollX() + this.getX());
        int m = x + i;
        context.fill(x, scrollbarXPositionY, m, scrollbarXEndPositionY, -8355712);
        context.fill(x, scrollbarXPositionY, m - 1, scrollbarXEndPositionY - 1, -4144960);
    }

    private int getScrollbarThumbWidth() {
        return MathHelper.clamp((int)((float)(this.width * this.width) / (float)this.getContentsWidth() + this.getPadding()), 32, this.width);
    }
}
