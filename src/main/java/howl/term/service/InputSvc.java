package howl.term.service;

/** Android input wrappers. */
public final class InputSvc {
    public InputSvc() {}

    public void bindSwipeRight(android.view.View view, Runnable onSwipe) {
        final float[] downX = new float[] { 0f };
        final boolean[] swiped = new boolean[] { false };
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                swiped[0] = false;
                return true;
            }
            if (!swiped[0]
                    && event.getAction() == android.view.MotionEvent.ACTION_MOVE
                    && (event.getX() - downX[0]) > 24f) {
                swiped[0] = true;
                onSwipe.run();
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public void bindSwipeLeft(android.view.View view, Runnable onSwipe) {
        final float[] downX = new float[] { 0f };
        final boolean[] swiped = new boolean[] { false };
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                swiped[0] = false;
                return true;
            }
            if (!swiped[0]
                    && event.getAction() == android.view.MotionEvent.ACTION_MOVE
                    && (event.getX() - downX[0]) < -24f) {
                swiped[0] = true;
                onSwipe.run();
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public void bindSwipeDown(android.view.View view, Runnable onSwipe) {
        final float[] downX = new float[] { 0f };
        final float[] downY = new float[] { 0f };
        final boolean[] swiped = new boolean[] { false };
        final boolean[] moved = new boolean[] { false };
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                downY[0] = event.getY();
                swiped[0] = false;
                moved[0] = false;
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
                final float dx = event.getX() - downX[0];
                final float dy = event.getY() - downY[0];
                if (Math.abs(dx) > 12f || Math.abs(dy) > 12f) {
                    moved[0] = true;
                }
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE && (event.getY() - downY[0]) > 24f) {
                swiped[0] = true;
                onSwipe.run();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (!swiped[0] && !moved[0]) {
                    v.performClick();
                }
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public void bindSwipeUp(android.view.View view, Runnable onSwipe) {
        final float[] downY = new float[] { 0f };
        final boolean[] swiped = new boolean[] { false };
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downY[0] = event.getY();
                swiped[0] = false;
                return true;
            }
            if (!swiped[0]
                    && event.getAction() == android.view.MotionEvent.ACTION_MOVE
                    && (event.getY() - downY[0]) < -24f) {
                swiped[0] = true;
                onSwipe.run();
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public void bindSwipeUpFromBottom(android.view.View view, int edgePx, Runnable onSwipe) {
        final float[] downY = new float[] { 0f };
        final boolean[] swiped = new boolean[] { false };
        final boolean[] tracking = new boolean[] { false };
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downY[0] = event.getY();
                swiped[0] = false;
                final float threshold = Math.max(1, edgePx);
                final float bottom = v.getHeight() - threshold;
                tracking[0] = downY[0] >= bottom;
                return tracking[0];
            }
            if (!tracking[0]) return false;
            if (!swiped[0]
                    && event.getAction() == android.view.MotionEvent.ACTION_MOVE
                    && (event.getY() - downY[0]) < -24f) {
                swiped[0] = true;
                onSwipe.run();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                tracking[0] = false;
                return true;
            }
            return true;
        });
    }
}
