package howl.term.widget.side_panel;

import howl.term.obj.android.InputRuntime;
import howl.term.obj.android.WindowRuntime;

/** Left slide-out widget. */
public final class SidePanel {
    private final android.widget.FrameLayout view;
    private final WindowRuntime window;
    private final InputRuntime input;

    public SidePanel(android.app.Activity activity, WindowRuntime window) {
        this.window = window;
        this.input = new InputRuntime();
        this.view = window.container(activity);
        window.setBackground(view, 0xFF202020);
        window.setGone(view);
    }

    public void bindOpen(android.view.View edge) {
        input.bindSwipeRight(edge, this::open);
    }

    public void bindClose() {
        input.bindSwipeLeft(view, this::close);
    }

    public void open() {
        window.setVisible(view);
        window.setX(view, 0f);
    }

    public void close() {
        window.setGone(view);
    }

    public android.view.View view() {
        return view;
    }
}
