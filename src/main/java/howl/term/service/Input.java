package howl.term.service;

import java.nio.charset.StandardCharsets;

/** Input helpers for gestures and key->byte translation. */
public final class Input {
    public Input() {}

    public static byte[] mapKeyEvent(android.view.KeyEvent event) {
        final int keyCode = event.getKeyCode();
        final boolean ctrl = event.isCtrlPressed();
        final boolean alt = event.isAltPressed();

        if (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) return new byte[] {'\r'};
        if (keyCode == android.view.KeyEvent.KEYCODE_DEL) return new byte[] {0x7f};
        if (keyCode == android.view.KeyEvent.KEYCODE_TAB) return new byte[] {'\t'};
        if (keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) return new byte[] {0x1b};
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) return "\u001b[A".getBytes(StandardCharsets.UTF_8);
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) return "\u001b[B".getBytes(StandardCharsets.UTF_8);
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) return "\u001b[C".getBytes(StandardCharsets.UTF_8);
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) return "\u001b[D".getBytes(StandardCharsets.UTF_8);
        if (keyCode == android.view.KeyEvent.KEYCODE_MOVE_HOME) return "\u001b[H".getBytes(StandardCharsets.UTF_8);
        if (keyCode == android.view.KeyEvent.KEYCODE_MOVE_END) return "\u001b[F".getBytes(StandardCharsets.UTF_8);
        if (keyCode == android.view.KeyEvent.KEYCODE_PAGE_UP) return "\u001b[5~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == android.view.KeyEvent.KEYCODE_PAGE_DOWN) return "\u001b[6~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == android.view.KeyEvent.KEYCODE_FORWARD_DEL) return "\u001b[3~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == android.view.KeyEvent.KEYCODE_INSERT) return "\u001b[2~".getBytes(StandardCharsets.UTF_8);

        if (ctrl) {
            final int cp = event.getUnicodeChar(android.view.KeyEvent.META_CTRL_ON);
            if (cp > 0 && cp <= 0x1f) return new byte[] {(byte) cp};
        }

        if (alt) {
            final int cp = event.getUnicodeChar();
            if (cp > 0 && !Character.isISOControl(cp)) {
                final byte[] b = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8);
                final byte[] out = new byte[b.length + 1];
                out[0] = 0x1b;
                System.arraycopy(b, 0, out, 1, b.length);
                return out;
            }
        }

        return null;
    }

    public static void bindSwipeUp(android.view.View view, Runnable onSwipe) {
        final float[] downY = new float[] {0f};
        final boolean[] fired = new boolean[] {false};
        view.setOnTouchListener((v, e) -> {
            final int action = e.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                downY[0] = e.getY();
                fired[0] = false;
                return true;
            }
            if (!fired[0] && action == android.view.MotionEvent.ACTION_MOVE && (e.getY() - downY[0]) < -24f) {
                fired[0] = true;
                onSwipe.run();
                return true;
            }
            return action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public static void bindSwipeDown(android.view.View view, Runnable onSwipe) {
        final float[] downY = new float[] {0f};
        final boolean[] fired = new boolean[] {false};
        final boolean[] moved = new boolean[] {false};
        view.setOnTouchListener((v, e) -> {
            final int action = e.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                downY[0] = e.getY();
                fired[0] = false;
                moved[0] = false;
                return true;
            }
            if (action == android.view.MotionEvent.ACTION_MOVE) {
                if (Math.abs(e.getY() - downY[0]) > 12f) moved[0] = true;
            }
            if (!fired[0] && action == android.view.MotionEvent.ACTION_MOVE && (e.getY() - downY[0]) > 24f) {
                fired[0] = true;
                onSwipe.run();
                return true;
            }
            if (action == android.view.MotionEvent.ACTION_UP) {
                if (!fired[0] && !moved[0]) {
                    v.performClick();
                }
                return true;
            }
            return action == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public static void bindSwipeRight(android.view.View view, Runnable onSwipe) {
        final float[] downX = new float[] {0f};
        final boolean[] fired = new boolean[] {false};
        view.setOnTouchListener((v, e) -> {
            final int action = e.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                downX[0] = e.getX();
                fired[0] = false;
                return true;
            }
            if (!fired[0] && action == android.view.MotionEvent.ACTION_MOVE && (e.getX() - downX[0]) > 24f) {
                fired[0] = true;
                onSwipe.run();
                return true;
            }
            return action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public static void bindSwipeLeft(android.view.View view, Runnable onSwipe) {
        final float[] downX = new float[] {0f};
        final boolean[] fired = new boolean[] {false};
        view.setOnTouchListener((v, e) -> {
            final int action = e.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                downX[0] = e.getX();
                fired[0] = false;
                return true;
            }
            if (!fired[0] && action == android.view.MotionEvent.ACTION_MOVE && (e.getX() - downX[0]) < -24f) {
                fired[0] = true;
                onSwipe.run();
                return true;
            }
            return action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL;
        });
    }
}
