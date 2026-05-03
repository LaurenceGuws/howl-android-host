package howl.term.input;

import android.view.KeyEvent;

/**
 * Responsibility: own hardware-keyboard dispatch for one terminal surface.
 * Ownership: public hardware-keyboard surface for the input unit.
 * Reason: keep dispatch policy behind a boring input owner.
 */
public final class HardwareKeyboard {
    /** Host callbacks required by hardware-keyboard dispatch. */
    public interface Host {
        boolean hasHardwareKeyboardTarget();
        boolean handleHardwareKeyEvent(KeyEvent event);
        void focusInput();
    }

    private final HardwareKeyboardDispatch dispatch;

    /** Construct one hardware-keyboard owner for a terminal surface host. */
    public HardwareKeyboard(Host host) {
        this.dispatch = new HardwareKeyboardDispatch(host);
    }

    /** Route one activity key event through hardware-keyboard policy. */
    public boolean handleDispatchKeyEvent(KeyEvent event) {
        return dispatch.handleDispatchKeyEvent(event);
    }
}
