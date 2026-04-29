package howl.term.widget.side_panel;

import howl.term.service.InputRuntime;
import howl.term.service.WindowRuntime;

/** Left slide-out widget. */
public final class SidePanel {
    private final android.widget.FrameLayout view;
    private final WindowRuntime winRt;
    private final InputRuntime inputRt;

    public SidePanel(android.app.Activity activity, WindowRuntime winRt) {
        this.winRt = winRt;
        this.inputRt = new InputRuntime();
        this.view = winRt.container(activity);
        winRt.setBackground(view, 0xFF202020);
        winRt.setGone(view);
    }

    public void bindOpen(android.view.View edge) {
        inputRt.bindSwipeRight(edge, this::open);
    }

    public void bindClose() {
        inputRt.bindSwipeLeft(view, this::close);
    }

    public void open() {
        winRt.setVisible(view);
        winRt.setX(view, 0f);
    }

    public void close() {
        winRt.setGone(view);
    }

    public android.view.View view() {
        return view;
    }
}
