package howl.term.service;

/** Android input wrappers. */
public final class InputRuntime {
    public InputRuntime() {}

    public void bindSwipeRight(android.view.View view, Runnable onSwipe) {
        final float[] downX = new float[] { 0f };
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE && (event.getX() - downX[0]) > 24f) {
                onSwipe.run();
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public void bindSwipeLeft(android.view.View view, Runnable onSwipe) {
        final float[] downX = new float[] { 0f };
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE && (event.getX() - downX[0]) < -24f) {
                onSwipe.run();
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public void bindSwipeDown(android.view.View view, Runnable onSwipe) {
        final float[] downY = new float[] { 0f };
        final boolean[] swiped = new boolean[] { false };
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downY[0] = event.getY();
                swiped[0] = false;
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE && (event.getY() - downY[0]) > 24f) {
                swiped[0] = true;
                onSwipe.run();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (!swiped[0]) {
                    v.performClick();
                }
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_CANCEL;
        });
    }

    public void bindSwipeUp(android.view.View view, Runnable onSwipe) {
        final float[] downY = new float[] { 0f };
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downY[0] = event.getY();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_MOVE && (event.getY() - downY[0]) < -24f) {
                onSwipe.run();
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL;
        });
    }
}
