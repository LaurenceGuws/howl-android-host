package howl.term.widget;

import howl.term.Input;
import howl.term.Window;

/** Side panel widget object. */
public final class SidePanel {
    private final Window window;
    private final android.widget.FrameLayout view;
    private android.view.View scrim;

    public SidePanel(android.app.Activity activity, Window window) {
        this.window = window;
        this.view = window.container(activity);
        this.scrim = null;
        window.setBackground(view, 0xFF202020);
        window.setGone(view);
    }

    public android.view.View view() { return view; }

    public void bindOpen(android.view.View edge) {
        Input.bindSwipeRight(edge, this::open);
    }

    public void bindClose() {
        Input.bindSwipeLeft(view, this::close);
    }

    public void bindOverlayScrim(android.view.View scrim) {
        this.scrim = scrim;
        scrim.setClickable(true);
        scrim.setOnClickListener(v -> close());
        window.setGone(scrim);
    }

    public void open() {
        if (scrim != null) window.setVisible(scrim);
        window.setVisible(view);
        window.setX(view, 0f);
    }

    public void close() {
        window.setGone(view);
        if (scrim != null) window.setGone(scrim);
    }
}
