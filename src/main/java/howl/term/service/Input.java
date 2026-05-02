package howl.term.service;

/** Input helpers for gestures and key->byte translation. */
public final class Input {
    public Input() {}

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
