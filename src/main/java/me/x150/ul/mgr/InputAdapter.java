package me.x150.ul.mgr;

import com.labymedia.ultralight.UltralightView;
import com.labymedia.ultralight.input.UltralightInputModifier;
import com.labymedia.ultralight.input.UltralightKey;
import com.labymedia.ultralight.input.UltralightKeyEvent;
import com.labymedia.ultralight.input.UltralightKeyEventType;
import com.labymedia.ultralight.input.UltralightMouseEvent;
import com.labymedia.ultralight.input.UltralightMouseEventButton;
import com.labymedia.ultralight.input.UltralightMouseEventType;
import com.labymedia.ultralight.input.UltralightScrollEvent;
import com.labymedia.ultralight.input.UltralightScrollEventType;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Class for converting GLFW events into Ultralight events.
 */
public class InputAdapter {
    private final UltralightView view;

    private float xScale;
    private float yScale;

    /**
     * Constructs a new {@link InputAdapter} for the specified view.
     *
     * @param view The view to send events to
     */
    public InputAdapter(UltralightView view) {
        this.view = view;
    }

    /**
     * Called by GLFW then the content scale (DPI ratio) changes.
     *
     * @param window The window that caused the event
     * @param xScale The new x scale
     * @param yScale The new y scale
     */
    public void windowContentScaleCallback(long window, float xScale, float yScale) {
        this.xScale = xScale;
        this.yScale = yScale;
    }

    /**
     * Called by GLFW when a key is pressed.
     *
     * @param window   The window that caused the event
     * @param key      The GLFW keycode
     * @param scancode The keyboard scancode
     * @param action   The GLFW action
     * @param mods     The key modifiers
     */
    public void keyCallback(long window, int key, int scancode, int action, int mods) {
        UltralightKey translatedKey = glfwToUltralightKey(key);

        // Build the event
        UltralightKeyEvent event = new UltralightKeyEvent()
            .type(action == GLFW_PRESS || action == GLFW_REPEAT ?
                UltralightKeyEventType.RAW_DOWN : UltralightKeyEventType.UP)
            .virtualKeyCode(translatedKey)
            .nativeKeyCode(scancode)
            .keyIdentifier(UltralightKeyEvent.getKeyIdentifierFromVirtualKeyCode(translatedKey))
            .modifiers(glfwToUltralightModifiers(mods));

        // Send the event
        view.fireKeyEvent(event);

        if ((action == GLFW_PRESS || action == GLFW_REPEAT) && (key == GLFW_KEY_ENTER || key == GLFW_KEY_TAB)) {
            // These keys need to be translated specially
            String text = key == GLFW_KEY_ENTER ? "\r" : "\t";
            UltralightKeyEvent extraEvent = new UltralightKeyEvent()
                .type(UltralightKeyEventType.CHAR)
                .text(text)
                .unmodifiedText(text);

            // Fire the event
            view.fireKeyEvent(extraEvent);
        }
    }

    /**
     * Called by GLFW when a char is input.
     *
     * @param window    The window that caused the event
     * @param codepoint The unicode char that has been input
     */
    public void charCallback(long window, int codepoint) {
        // Convert the unicode code point to a UTF-16 string
        String text = new String(Character.toChars(codepoint));

        // Create the event
        UltralightKeyEvent event = new UltralightKeyEvent()
            .type(UltralightKeyEventType.CHAR)
            .text(text)
            .unmodifiedText(text);

        // Fire the event
        view.fireKeyEvent(event);
    }

    /**
     * Called by GLFW when the mouse moves.
     *
     * @param window The window that caused the event
     * @param x      The new x position of the cursor
     * @param y      The new y position of the cursor
     */
    public void cursorPosCallback(long window, double x, double y) {
        // Create the event
        UltralightMouseEvent event = new UltralightMouseEvent()
            .x((int) (x * xScale))
            .y((int) (y * yScale))
            .type(UltralightMouseEventType.MOVED);

        // Translate the mouse state to the event
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            event.button(UltralightMouseEventButton.LEFT);
        } else if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_MIDDLE) == GLFW_PRESS) {
            event.button(UltralightMouseEventButton.MIDDLE);
        } else if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
            event.button(UltralightMouseEventButton.RIGHT);
        }

        // Fire the event
        view.fireMouseEvent(event);
    }

    /**
     * Called by GLFW when a mouse button changes its state.
     *
     * @param window The window that caused the event
     * @param button the button that changed its state
     * @param action The new state of the button
     * @param mods   The mouse modifiers
     */
    public void mouseButtonCallback(long window, int button, int action, int mods) {
        double x;
        double y;

        // Use a memory stack so we don't have to worry about freeing allocations
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer buffer = stack.callocDouble(2);

            // Retrieve the current cursor pos
            glfwGetCursorPos(window, (DoubleBuffer) buffer.slice().position(0), (DoubleBuffer) buffer.slice().position(1));

            // Extract the x and y position
            x = buffer.get(0);
            y = buffer.get(1);
        }

        // Create the event
        UltralightMouseEvent event = new UltralightMouseEvent()
            .x((int) (x))
            .y((int) (y))
            .type(action == GLFW_PRESS ? UltralightMouseEventType.DOWN : UltralightMouseEventType.UP);

        // Sort out the button
        switch (button) {
            case GLFW_MOUSE_BUTTON_LEFT -> event.button(UltralightMouseEventButton.LEFT);
            case GLFW_MOUSE_BUTTON_MIDDLE -> event.button(UltralightMouseEventButton.MIDDLE);
            case GLFW_MOUSE_BUTTON_RIGHT -> event.button(UltralightMouseEventButton.RIGHT);
        }

        // Fire the event
        view.fireMouseEvent(event);
    }

    /**
     * Called by GLFW when the user scrolls within the window.
     *
     * @param window The window that caused the event
     * @param xDelta The x scroll delta
     * @param yDelta The y scroll delta
     */
    public void scrollCallback(long window, double xDelta, double yDelta) {
        // Create the event
        UltralightScrollEvent event = new UltralightScrollEvent()
            .deltaX((int) xDelta * 32)
            .deltaY((int) yDelta * 32)
            .type(UltralightScrollEventType.BY_PIXEL);

        // Fire the event
        view.fireScrollEvent(event);
    }

    /**
     * Called by GLFW when the window gains or looses focus.
     *
     * @param window The window that caused the event
     * @param focus  Whether the window gained focus
     */
    public void focusCallback(long window, boolean focus) {
        if (focus) {
            view.focus();
        } else {
            view.unfocus();
        }
    }

    /**
     * Translates GLFW key modifiers to Ultralight key modifiers.
     *
     * @param modifiers The GLFW key modifiers to translate
     * @return The translated Ultralight key modifiers
     */
    private int glfwToUltralightModifiers(int modifiers) {
        int ultralightModifiers = 0;

        if ((modifiers & GLFW_MOD_ALT) != 0) {
            ultralightModifiers |= UltralightInputModifier.ALT_KEY;
        }

        if ((modifiers & GLFW_MOD_CONTROL) != 0) {
            ultralightModifiers |= UltralightInputModifier.CTRL_KEY;
        }

        if ((modifiers & GLFW_MOD_SUPER) != 0) {
            ultralightModifiers |= UltralightInputModifier.META_KEY;
        }

        if ((modifiers & GLFW_MOD_SHIFT) != 0) {
            ultralightModifiers |= UltralightInputModifier.SHIFT_KEY;
        }

        return ultralightModifiers;
    }

    /**
     * Translates a GLFW key code to an {@link UltralightKey}.
     *
     * @param key The GLFW key code to translate
     * @return The translated Ultralight key, or {@link UltralightKey#UNKNOWN}, if the key could not be translated
     */
    private UltralightKey glfwToUltralightKey(int key) {
        return switch (key) {
            case GLFW_KEY_SPACE -> UltralightKey.SPACE;
            case GLFW_KEY_APOSTROPHE -> UltralightKey.OEM_7;
            case GLFW_KEY_COMMA -> UltralightKey.OEM_COMMA;
            case GLFW_KEY_MINUS -> UltralightKey.OEM_MINUS;
            case GLFW_KEY_PERIOD -> UltralightKey.OEM_PERIOD;
            case GLFW_KEY_SLASH -> UltralightKey.OEM_2;
            case GLFW_KEY_0 -> UltralightKey.NUM_0;
            case GLFW_KEY_1 -> UltralightKey.NUM_1;
            case GLFW_KEY_2 -> UltralightKey.NUM_2;
            case GLFW_KEY_3 -> UltralightKey.NUM_3;
            case GLFW_KEY_4 -> UltralightKey.NUM_4;
            case GLFW_KEY_5 -> UltralightKey.NUM_5;
            case GLFW_KEY_6 -> UltralightKey.NUM_6;
            case GLFW_KEY_7 -> UltralightKey.NUM_7;
            case GLFW_KEY_8 -> UltralightKey.NUM_8;
            case GLFW_KEY_9 -> UltralightKey.NUM_9;
            case GLFW_KEY_SEMICOLON -> UltralightKey.OEM_1;
            case GLFW_KEY_EQUAL, GLFW_KEY_KP_EQUAL -> UltralightKey.OEM_PLUS;
            case GLFW_KEY_A -> UltralightKey.A;
            case GLFW_KEY_B -> UltralightKey.B;
            case GLFW_KEY_C -> UltralightKey.C;
            case GLFW_KEY_D -> UltralightKey.D;
            case GLFW_KEY_E -> UltralightKey.E;
            case GLFW_KEY_F -> UltralightKey.F;
            case GLFW_KEY_G -> UltralightKey.G;
            case GLFW_KEY_H -> UltralightKey.H;
            case GLFW_KEY_I -> UltralightKey.I;
            case GLFW_KEY_J -> UltralightKey.J;
            case GLFW_KEY_K -> UltralightKey.K;
            case GLFW_KEY_L -> UltralightKey.L;
            case GLFW_KEY_M -> UltralightKey.M;
            case GLFW_KEY_N -> UltralightKey.N;
            case GLFW_KEY_O -> UltralightKey.O;
            case GLFW_KEY_P -> UltralightKey.P;
            case GLFW_KEY_Q -> UltralightKey.Q;
            case GLFW_KEY_R -> UltralightKey.R;
            case GLFW_KEY_S -> UltralightKey.S;
            case GLFW_KEY_T -> UltralightKey.T;
            case GLFW_KEY_U -> UltralightKey.U;
            case GLFW_KEY_V -> UltralightKey.V;
            case GLFW_KEY_W -> UltralightKey.W;
            case GLFW_KEY_X -> UltralightKey.X;
            case GLFW_KEY_Y -> UltralightKey.Y;
            case GLFW_KEY_Z -> UltralightKey.Z;
            case GLFW_KEY_LEFT_BRACKET -> UltralightKey.OEM_4;
            case GLFW_KEY_BACKSLASH -> UltralightKey.OEM_5;
            case GLFW_KEY_RIGHT_BRACKET -> UltralightKey.OEM_6;
            case GLFW_KEY_GRAVE_ACCENT -> UltralightKey.OEM_3;
            case GLFW_KEY_ESCAPE -> UltralightKey.ESCAPE;
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> UltralightKey.RETURN;
            case GLFW_KEY_TAB -> UltralightKey.TAB;
            case GLFW_KEY_BACKSPACE -> UltralightKey.BACK;
            case GLFW_KEY_INSERT -> UltralightKey.INSERT;
            case GLFW_KEY_DELETE -> UltralightKey.DELETE;
            case GLFW_KEY_RIGHT -> UltralightKey.RIGHT;
            case GLFW_KEY_LEFT -> UltralightKey.LEFT;
            case GLFW_KEY_DOWN -> UltralightKey.DOWN;
            case GLFW_KEY_UP -> UltralightKey.UP;
            case GLFW_KEY_PAGE_UP -> UltralightKey.PRIOR;
            case GLFW_KEY_PAGE_DOWN -> UltralightKey.NEXT;
            case GLFW_KEY_HOME -> UltralightKey.HOME;
            case GLFW_KEY_END -> UltralightKey.END;
            case GLFW_KEY_CAPS_LOCK -> UltralightKey.CAPITAL;
            case GLFW_KEY_SCROLL_LOCK -> UltralightKey.SCROLL;
            case GLFW_KEY_NUM_LOCK -> UltralightKey.NUMLOCK;
            case GLFW_KEY_PRINT_SCREEN -> UltralightKey.SNAPSHOT;
            case GLFW_KEY_PAUSE -> UltralightKey.PAUSE;
            case GLFW_KEY_F1 -> UltralightKey.F1;
            case GLFW_KEY_F2 -> UltralightKey.F2;
            case GLFW_KEY_F3 -> UltralightKey.F3;
            case GLFW_KEY_F4 -> UltralightKey.F4;
            case GLFW_KEY_F5 -> UltralightKey.F5;
            case GLFW_KEY_F6 -> UltralightKey.F6;
            case GLFW_KEY_F7 -> UltralightKey.F7;
            case GLFW_KEY_F8 -> UltralightKey.F8;
            case GLFW_KEY_F9 -> UltralightKey.F9;
            case GLFW_KEY_F10 -> UltralightKey.F10;
            case GLFW_KEY_F11 -> UltralightKey.F11;
            case GLFW_KEY_F12 -> UltralightKey.F12;
            case GLFW_KEY_F13 -> UltralightKey.F13;
            case GLFW_KEY_F14 -> UltralightKey.F14;
            case GLFW_KEY_F15 -> UltralightKey.F15;
            case GLFW_KEY_F16 -> UltralightKey.F16;
            case GLFW_KEY_F17 -> UltralightKey.F17;
            case GLFW_KEY_F18 -> UltralightKey.F18;
            case GLFW_KEY_F19 -> UltralightKey.F19;
            case GLFW_KEY_F20 -> UltralightKey.F20;
            case GLFW_KEY_F21 -> UltralightKey.F21;
            case GLFW_KEY_F22 -> UltralightKey.F22;
            case GLFW_KEY_F23 -> UltralightKey.F23;
            case GLFW_KEY_F24 -> UltralightKey.F24;
            case GLFW_KEY_KP_0 -> UltralightKey.NUMPAD0;
            case GLFW_KEY_KP_1 -> UltralightKey.NUMPAD1;
            case GLFW_KEY_KP_2 -> UltralightKey.NUMPAD2;
            case GLFW_KEY_KP_3 -> UltralightKey.NUMPAD3;
            case GLFW_KEY_KP_4 -> UltralightKey.NUMPAD4;
            case GLFW_KEY_KP_5 -> UltralightKey.NUMPAD5;
            case GLFW_KEY_KP_6 -> UltralightKey.NUMPAD6;
            case GLFW_KEY_KP_7 -> UltralightKey.NUMPAD7;
            case GLFW_KEY_KP_8 -> UltralightKey.NUMPAD8;
            case GLFW_KEY_KP_9 -> UltralightKey.NUMPAD9;
            case GLFW_KEY_KP_DECIMAL -> UltralightKey.DECIMAL;
            case GLFW_KEY_KP_DIVIDE -> UltralightKey.DIVIDE;
            case GLFW_KEY_KP_MULTIPLY -> UltralightKey.MULTIPLY;
            case GLFW_KEY_KP_SUBTRACT -> UltralightKey.SUBTRACT;
            case GLFW_KEY_KP_ADD -> UltralightKey.ADD;
            case GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> UltralightKey.SHIFT;
            case GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> UltralightKey.CONTROL;
            case GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT -> UltralightKey.MENU;
            case GLFW_KEY_LEFT_SUPER -> UltralightKey.LWIN;
            case GLFW_KEY_RIGHT_SUPER -> UltralightKey.RWIN;
            default -> UltralightKey.UNKNOWN;
        };
    }
}
