package howl.term.widget;

import howl.term.Window;

/**
 * Responsibility: own the assist-bar widget surface for the Android host.
 * Ownership: assist-bar visibility and IME toggle routing.
 * Reason: keep assist-bar details behind one boring widget owner.
 */
public final class AssistBar {
    /** Visibility callback contract for assist-bar state changes. */
    public interface VisibilityListener {
        void onChanged(boolean visible);
    }

    private final Window window;
    private final android.widget.FrameLayout view;
    private android.view.View imeAnchor;
    private VisibilityListener listener;

    /** Construct one assist-bar widget owner. */
    public AssistBar(android.app.Activity activity, Window window) {
        this.window = window;
        this.view = window.container(activity);
        this.imeAnchor = view;
        this.listener = null;
        window.setBackground(view, 0xFF1A1A1A);
        window.bindTap(view, () -> window.toggleIme(activity, imeAnchor));
        hide();
    }

    /** Return the assist-bar root view. */
    public android.view.View view() { return view; }

    /** Show the assist bar and notify listeners. */
    public void show() {
        window.setVisible(view);
        if (listener != null) listener.onChanged(true);
    }

    /** Hide the assist bar and notify listeners. */
    public void hide() {
        window.setGone(view);
        if (listener != null) listener.onChanged(false);
    }

    /** Set the assist-bar visibility listener. */
    public void setVisibilityListener(VisibilityListener listener) {
        this.listener = listener;
    }

    /** Set the IME anchor used when the assist bar toggles the keyboard. */
    public void setImeAnchor(android.view.View anchor) {
        if (anchor != null) this.imeAnchor = anchor;
    }
}
