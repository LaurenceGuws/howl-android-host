package howl.term.widget;

import howl.term.Input;
import howl.term.Window;

/**
 * Responsibility: own the side-panel widget surface for the Android host.
 * Ownership: edge-open gesture binding, scrim coordination, and open/close state.
 * Reason: keep side-panel details behind one boring widget owner.
 */
public final class SidePanel {
    private final Window window;
    private final android.widget.FrameLayout view;
    private android.view.View scrim;

    /** Construct one side-panel widget owner. */
    public SidePanel(android.app.Activity activity, Window window) {
        this.window = window;
        this.view = window.container(activity);
        this.scrim = null;
        window.setBackground(view, 0xFF202020);
        window.setGone(view);
    }

    /** Return the side-panel root view. */
    public android.view.View view() { return view; }

    /** Bind the edge-open gesture for the side panel. */
    public void bindOpen(android.view.View edge) {
        Input.bindSwipeRight(edge, this::open);
    }

    /** Bind the in-panel close gesture. */
    public void bindClose() {
        Input.bindSwipeLeft(view, this::close);
    }

    /** Bind the overlay scrim used while the side panel is open. */
    public void bindOverlayScrim(android.view.View scrim) {
        this.scrim = scrim;
        scrim.setClickable(true);
        scrim.setOnClickListener(v -> close());
        window.setGone(scrim);
    }

    /** Open the side panel and show the overlay scrim if present. */
    public void open() {
        if (scrim != null) window.setVisible(scrim);
        window.setVisible(view);
        window.setX(view, 0f);
    }

    /** Close the side panel and hide the overlay scrim if present. */
    public void close() {
        window.setGone(view);
        if (scrim != null) window.setGone(scrim);
    }
}
