package howl.term.service.android.input;

import android.view.MotionEvent;
import android.view.View;

/** Android gesture wiring for host UI controls. */
public final class InputRuntime {
    private InputRuntime() {}

    public static void bindSwipeRight(View target, Runnable onSwipe) {
        final float[] downX = new float[] { 0f };
        target.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE && (event.getX() - downX[0]) > 24f) {
                onSwipe.run();
                return true;
            }
            return false;
        });
    }

    public static void bindSwipeRight(Object handle, Runnable onSwipe) {
        bindSwipeRight((View) handle, onSwipe);
    }

    public static void bindSwipeLeft(View target, Runnable onSwipe) {
        final float[] downX = new float[] { 0f };
        target.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE && (event.getX() - downX[0]) < -24f) {
                onSwipe.run();
                return true;
            }
            return false;
        });
    }

    public static void bindSwipeLeft(Object handle, Runnable onSwipe) {
        bindSwipeLeft((View) handle, onSwipe);
    }
}
