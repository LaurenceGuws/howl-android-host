package howl.term.input;

import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

/** Owns hardware-keyboard dispatch policy for terminal input on Android. */
public final class HardwareKeyboardDispatch {
    private final HardwareKeyboard.Host host;

    public HardwareKeyboardDispatch(HardwareKeyboard.Host host) {
        this.host = host;
    }

    public boolean handleDispatchKeyEvent(KeyEvent event) {
        if (!shouldHandleHardwareKeyboardEvent(event)) {
            return false;
        }
        if (!host.hasHardwareKeyboardTarget()) {
            return false;
        }
        host.focusInput();
        return host.handleHardwareKeyEvent(event);
    }

    private static boolean shouldHandleHardwareKeyboardEvent(KeyEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_KEYBOARD) == 0) {
            return false;
        }
        if (event.getDeviceId() == KeyCharacterMap.VIRTUAL_KEYBOARD) {
            return false;
        }
        final InputDevice device = event.getDevice();
        if (device == null || device.isVirtual()) {
            return false;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_POWER:
                return false;
            default:
                return true;
        }
    }
}
