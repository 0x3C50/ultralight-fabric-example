package me.x150.ul;

import com.labymedia.ultralight.UltralightPlatform;
import com.labymedia.ultralight.UltralightRenderer;
import com.labymedia.ultralight.config.FontHinting;
import com.labymedia.ultralight.config.UltralightConfig;
import me.x150.ExampleMod;
import me.x150.ul.mgr.ClipboardAdapter;
import me.x150.ul.mgr.ExampleFileSystem;
import me.x150.ul.mgr.ExampleLogger;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Holder of the platform instance
 */
public class PlatformManager {
    public static Path tempDirectory;
    private static PlatformManager instance;

    private final UltralightRenderer renderer;

    /**
     * Gets the renderer
     * @return The renderer
     */
    public UltralightRenderer getRenderer() {
        return renderer;
    }

    /**
     * Gets (or creates) the instance of PlatformManager
     * @return The instance of PlatformManager
     */
    public static PlatformManager instance() {
        if (instance == null) instance = new PlatformManager();
        return instance;
    }

    private final UltralightPlatform platform;

    /**
     * Gets the platform
     * @return The platform
     */
    public UltralightPlatform getPlatform() {
        return platform;
    }

    private PlatformManager() {
        this.platform = UltralightPlatform.instance(); // this breaks when called more than once
        tempDirectory = ExampleMod.requestTempDir("ultralight-temp");
        this.platform.setConfig( // this also breaks when called more than once
            new UltralightConfig()
                .cachePath(tempDirectory.toString())
                .fontHinting(FontHinting.SMOOTH)
                .resourcePath(ExampleMod.resources.toString())
        );
        this.platform.usePlatformFontLoader();
        this.platform.setFileSystem(new ExampleFileSystem());
        this.platform.setLogger(new ExampleLogger());
        this.platform.setClipboard(new ClipboardAdapter());

        this.renderer = UltralightRenderer.create(); // this should be called only one but does not break when called more than once (as far as i know)
    }
}
