package me.x150.ul;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * A screen that renders html from an url
 */
public class HtmlScreen extends Screen {
    String source;
    UltralightViewable ulm;

    public HtmlScreen(String url) {
        super(Text.literal(""));
        this.source = url;
    }

    @Override
    protected void init() {
        if (this.ulm == null) {
            ulm = new UltralightViewable(this.client.getWindow().getHandle()); // create viewable if we haven't already
            ulm.getWebController().loadURL(source); // load url
        }
        // resize to framebuffer size
        ulm.updateSize(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
        // scale
        float scaleFactor = (float) this.client.getWindow().getScaleFactor();
        ulm.getWebController().getInputAdapter().windowContentScaleCallback(ulm.window, scaleFactor, scaleFactor);
    }

    public void reload() {
        if (this.ulm != null) {
            this.ulm.getWebController().reload();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.ulm.draw(); // render ul on top of background
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.ulm.cursorMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.ulm.mouseClicked(button, GLFW.GLFW_PRESS, 0);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.ulm.mouseClicked(button, GLFW.GLFW_RELEASE, 0);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.ulm.mouseScrolled(0, amount);
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.ulm.keyPressed(keyCode, scanCode, GLFW.GLFW_PRESS, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.ulm.keyPressed(keyCode, scanCode, GLFW.GLFW_RELEASE, modifiers);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        this.ulm.charTyped(chr);
        return super.charTyped(chr, modifiers);
    }
}
