package howl.term;

import howl.term.input.SwipeInput;

/**
 * Responsibility: expose the public swipe-input surface for the activity shell.
 * Ownership: root-level gesture entrypoints only.
 * Reason: keep gesture leaves behind a boring host owner.
 */
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
