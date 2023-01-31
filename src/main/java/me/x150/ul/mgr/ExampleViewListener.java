package me.x150.ul.mgr;

import com.labymedia.ultralight.UltralightView;
import com.labymedia.ultralight.input.UltralightCursor;
import com.labymedia.ultralight.math.IntRect;
import com.labymedia.ultralight.plugin.view.MessageLevel;
import com.labymedia.ultralight.plugin.view.MessageSource;
import com.labymedia.ultralight.plugin.view.UltralightViewListener;
import me.x150.ExampleMod;
import me.x150.ul.CursorAdapter;

public class ExampleViewListener implements UltralightViewListener {
    CursorAdapter ca;

    public ExampleViewListener(CursorAdapter ca) {
        this.ca = ca;
    }

    @Override
    public void onChangeTitle(String title) {

    }

    @Override
    public void onChangeURL(String url) {

    }

    @Override
    public void onChangeTooltip(String tooltip) {

    }

    @Override
    public void onChangeCursor(UltralightCursor cursor) {
        ca.notifyCursorUpdated(cursor);
    }

    @Override
    public void onAddConsoleMessage(MessageSource source, MessageLevel level, String message, long lineNumber, long columnNumber, String sourceId) {
        // log event from the browser engine
        ExampleMod.LOGGER.info("[ULTRALIGHT/JS/{}] ({}) {}   {}:{}.{}", level.name(), source.name(), message, sourceId, lineNumber, columnNumber);
    }

    @Override
    public UltralightView onCreateChildView(String openerUrl, String targetUrl, boolean isPopup, IntRect popupRect) {
        // returning null will prevent ul from acting any further, basically blocking all popup requests
        return null;
    }
}
