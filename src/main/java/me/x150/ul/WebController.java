package me.x150.ul;

import com.labymedia.ultralight.UltralightView;
import com.labymedia.ultralight.bitmap.UltralightBitmap;
import com.labymedia.ultralight.bitmap.UltralightBitmapSurface;
import com.labymedia.ultralight.config.UltralightViewConfig;
import com.labymedia.ultralight.math.IntRect;
import com.mojang.blaze3d.systems.RenderSystem;
import me.x150.ExampleMod;
import me.x150.ul.mgr.ExampleViewListener;
import me.x150.ul.mgr.InputAdapter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL12.*;

public class WebController {

    private final CursorAdapter cursorManager;
    private final PlatformManager platform;
    private int glTexture = -1;
    private UltralightView view;
    private InputAdapter inputAdapter;

    public WebController(CursorAdapter cursorManager) {
        this.cursorManager = cursorManager;
        platform = PlatformManager.instance();
    }

    /**
     * Reloads the view
     */
    public void reload() {
        view.reload();
    }

    public InputAdapter getInputAdapter() {
        return inputAdapter;
    }

    /**
     * Initializes the view. Call before loading or rendering
     */
    public void initView() {
        platform.getRenderer().logMemoryUsage();

        ExampleMod.LOGGER.info("Creating view");
        this.view = platform.getRenderer()
            .createView(300, 300, new UltralightViewConfig().initialDeviceScale(1.0).isTransparent(true).enableJavascript(true).initialFocus(true));
        ExampleViewListener viewListener = new ExampleViewListener(this.cursorManager);
        this.view.setViewListener(viewListener);

        this.inputAdapter = new InputAdapter(view);
    }

    /**
     * Navigates to a certain URL
     *
     * @param url The url
     */
    public void loadURL(String url) {
        this.view.loadURL(url);
    }

    /**
     * Resizes the web view.
     *
     * @param width  The new view width
     * @param height The new view height
     */
    public void resize(int width, int height) {
        this.view.resize(width, height);
    }

    /**
     * Updates and renders the renderer
     */
    private void update() {
        platform.getRenderer().update();
        platform.getRenderer().render();
    }

    /**
     * Renders this controller onto the screen
     */
    public void render() {
        this.update();

        if (glTexture == -1) {
            createGlTexture();
        }

        // As we are using the CPU renderer, draw with a bitmap (we did not set a custom surface)
        UltralightBitmapSurface surface = (UltralightBitmapSurface) view.surface();
        UltralightBitmap bitmap = surface.bitmap();
        int width = (int) view.width();
        int height = (int) view.height();

        // Prepare OpenGL for 2D textures and bind our texture
        RenderSystem.enableTexture();
        RenderSystem.bindTexture(glTexture);

        IntRect dirtyBounds = surface.dirtyBounds();

        if (dirtyBounds.isValid()) {
            ByteBuffer imageData = bitmap.lockPixels();

            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
            glPixelStorei(GL_UNPACK_SKIP_IMAGES, 0);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, (int) (bitmap.rowBytes() / 4));

            if (dirtyBounds.width() == width && dirtyBounds.height() == height) {
                // Update full image
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, imageData);
            } else {
                // Update partial image
                int x = dirtyBounds.x();
                int y = dirtyBounds.y();
                int dirtyWidth = dirtyBounds.width();
                int dirtyHeight = dirtyBounds.height();
                int startOffset = (int) (y * bitmap.rowBytes() + x * 4);

                glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, dirtyWidth, dirtyHeight, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, imageData.position(startOffset));
            }
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

            bitmap.unlockPixels();
            surface.clearDirtyBounds();
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        float scaleFactor = (float) MinecraftClient.getInstance().getWindow().getScaleFactor();

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, glTexture);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        bufferBuilder.vertex(0.0, height, 0.0).texture(0f, scaleFactor).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, height, 0.0).texture(scaleFactor, scaleFactor).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, 0.0, 0.0).texture(scaleFactor, 0.0f).color(255, 255, 255, 255).next();

        bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0f, 0.0f).color(255, 255, 255, 255).next();

        tessellator.draw();
        RenderSystem.disableBlend();

    }

    private void createGlTexture() {
        glTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, glTexture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
