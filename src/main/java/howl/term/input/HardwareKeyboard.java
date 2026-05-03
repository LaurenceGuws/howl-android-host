package howl.term.input;

import android.view.KeyEvent;

/** Owns hardware-keyboard dispatch for a terminal surface. */
public final class HardwareKeyboard {
    public interface Host {
        boolean hasHardwareKeyboardTarget();
        boolean handleHardwareKeyEvent(KeyEvent event);
        void focusInput();
    }

    private final HardwareKeyboardDispatch dispatch;

    public HardwareKeyboard(Host host) {
        this.dispatch = new HardwareKeyboardDispatch(host);
    }

    public boolean handleDispatchKeyEvent(KeyEvent event) {
        return dispatch.handleDispatchKeyEvent(event);
    }
}
