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
}
