package howl.term.widget.side_panel;

import howl.term.service.InputSvc;
import howl.term.service.WindowSvc;

/** Left slide-out widget. */
public final class SidePanel {
    private final android.widget.FrameLayout view;
    private final WindowSvc winSvc;
    private final InputSvc inputSvc;

    public SidePanel(android.app.Activity activity, WindowSvc winSvc) {
        this.winSvc = winSvc;
        this.inputSvc = new InputSvc();
        this.view = winSvc.container(activity);
        winSvc.setBackground(view, 0xFF202020);
        winSvc.setGone(view);
    }

    public void bindOpen(android.view.View edge) {
        inputSvc.bindSwipeRight(edge, this::open);
    }

    public void bindClose() {
        inputSvc.bindSwipeLeft(view, this::close);
    }

    public void open() {
        winSvc.setVisible(view);
        winSvc.setX(view, 0f);
    }

    public void close() {
        winSvc.setGone(view);
    }

    public android.view.View view() {
        return view;
    }
}
