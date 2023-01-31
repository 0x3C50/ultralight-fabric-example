package me.x150.ul;

/**
 * A viewable ultralight window
 */
public class UltralightViewable {
    private final WebController webController;
    CursorAdapter cursorManager;
    long window;

    /**
     * Creates a new viewable
     *
     * @param window The window to bind to
     */
    public UltralightViewable(long window) {
        this.window = window;
        this.cursorManager = new CursorAdapter(window);
        this.webController = new WebController(cursorManager);
        this.webController.initView();
        this.webController.getInputAdapter().windowContentScaleCallback(this.window, 1, 1); // scale to 1x1 originally
    }

    /**
     * Gets the web controller
     *
     * @return The web controller
     */
    public WebController getWebController() {
        return webController;
    }

    /**
     * Resizes this view
     *
     * @param w Width
     * @param h Height
     */
    public void updateSize(int w, int h) {
        this.webController.resize(w, h);
    }

    public void keyPressed(int keycode, int scancode, int action, int mods) {
        this.webController.getInputAdapter().keyCallback(this.window, keycode, scancode, action, mods);
    }

    public void charTyped(char c) {
        this.webController.getInputAdapter().charCallback(this.window, c);
    }

    public void cursorMoved(double x, double y) {
        this.webController.getInputAdapter().cursorPosCallback(this.window, x, y);
    }

    public void mouseClicked(int button, int action, int mods) {
        this.webController.getInputAdapter().mouseButtonCallback(this.window, button, action, mods);
    }

    public void mouseScrolled(double xDelta, double yDelta) {
        this.webController.getInputAdapter().scrollCallback(this.window, xDelta, yDelta);
    }

    /**
     * Tells UL that focus has shifted away or to this window
     *
     * @param focus Whether the focus shifted onto this window or away from it
     */
    public void focusCallback(boolean focus) {
        this.webController.getInputAdapter().focusCallback(this.window, focus);
    }

    /**
     * Draw this viewable onto the screen (fullscreen)
     */
    public void draw() {
        webController.render();
    }
}
