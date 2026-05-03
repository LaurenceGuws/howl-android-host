package howl.term.widget;

import howl.term.Window;

/** Assist bar widget object. */
public final class AssistBar {
    public interface VisibilityListener {
        void onChanged(boolean visible);
    }

    private final Window window;
    private final android.widget.FrameLayout view;
    private android.view.View imeAnchor;
    private VisibilityListener listener;

    public AssistBar(android.app.Activity activity, Window window) {
        this.window = window;
        this.view = window.container(activity);
        this.imeAnchor = view;
        this.listener = null;
        window.setBackground(view, 0xFF1A1A1A);
        window.bindTap(view, () -> window.toggleIme(activity, imeAnchor));
        hide();
    }

    public android.view.View view() { return view; }

    public void show() {
        window.setVisible(view);
        if (listener != null) listener.onChanged(true);
    }

    public void hide() {
        window.setGone(view);
        if (listener != null) listener.onChanged(false);
    }

    public void setVisibilityListener(VisibilityListener listener) {
        this.listener = listener;
    }

    public void setImeAnchor(android.view.View anchor) {
        if (anchor != null) this.imeAnchor = anchor;
    }
}
