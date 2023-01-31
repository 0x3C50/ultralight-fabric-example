package me.x150.ul.mgr;

import com.labymedia.ultralight.plugin.logging.UltralightLogLevel;
import com.labymedia.ultralight.plugin.logging.UltralightLogger;
import me.x150.ExampleMod;

public class ExampleLogger implements UltralightLogger {
    @Override
    public void logMessage(UltralightLogLevel level, String message) {
        ExampleMod.LOGGER.info("[ULTRALIGHT/{}] {}", level.name(), message);
    }
}
