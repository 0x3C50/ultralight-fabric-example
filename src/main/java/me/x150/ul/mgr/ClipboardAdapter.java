package me.x150.ul.mgr;

import com.labymedia.ultralight.plugin.clipboard.UltralightClipboard;
import me.x150.ExampleMod;
import net.minecraft.client.MinecraftClient;

public class ClipboardAdapter implements UltralightClipboard {
    @Override
    public void clear() {
        // called when ul wants to clear the clipboard
        ExampleMod.LOGGER.info("Requested clip clear");
    }

    @Override
    public String readPlainText() {
        // called when ul wants to read the clipboard
        return MinecraftClient.getInstance().keyboard.getClipboard();
    }

    @Override
    public void writePlainText(String text) {
        // called when ul wants to set the clipboard
        MinecraftClient.getInstance().keyboard.setClipboard(text);
    }
}
