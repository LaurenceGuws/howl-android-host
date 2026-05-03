package howl.term;

import howl.term.input.SwipeInput;

/** Input gesture helpers for the activity shell. */
public final class Input {
    public Input() {}

    public static void bindSwipeUp(android.view.View view, Runnable onSwipe) {
        SwipeInput.bindSwipeUp(view, onSwipe);
    }

    public static void bindSwipeDown(android.view.View view, Runnable onSwipe) {
        SwipeInput.bindSwipeDown(view, onSwipe);
    }

    public static void bindSwipeRight(android.view.View view, Runnable onSwipe) {
        SwipeInput.bindSwipeRight(view, onSwipe);
    }

    public static void bindSwipeLeft(android.view.View view, Runnable onSwipe) {
        SwipeInput.bindSwipeLeft(view, onSwipe);
    }
}
