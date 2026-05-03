package howl.term;

import android.view.KeyEvent;

import howl.term.input.HardwareKeyboardController;
import howl.term.input.SwipeBindings;

/** Input helpers for gestures and key->byte translation. */
public final class Input {
    public Input() {}

    public interface HardwareKeyboardHost {
        boolean hasHardwareKeyboardTarget();
        boolean handleHardwareKeyEvent(KeyEvent event);
        void focusInput();
    }

    public static final class HardwareKeyboard {
        private final HardwareKeyboardController controller;

        private HardwareKeyboard(HardwareKeyboardHost host) {
            this.controller = new HardwareKeyboardController(host);
        }

        public boolean handleDispatchKeyEvent(KeyEvent event) {
            return controller.handleDispatchKeyEvent(event);
        }
    }

    public static HardwareKeyboard createHardwareKeyboard(HardwareKeyboardHost host) {
        return new HardwareKeyboard(host);
    }

    public static void bindSwipeUp(android.view.View view, Runnable onSwipe) {
        SwipeBindings.bindSwipeUp(view, onSwipe);
    }

    public static void bindSwipeDown(android.view.View view, Runnable onSwipe) {
        SwipeBindings.bindSwipeDown(view, onSwipe);
    }

    public static void bindSwipeRight(android.view.View view, Runnable onSwipe) {
        SwipeBindings.bindSwipeRight(view, onSwipe);
    }

    public static void bindSwipeLeft(android.view.View view, Runnable onSwipe) {
        SwipeBindings.bindSwipeLeft(view, onSwipe);
    }
}
