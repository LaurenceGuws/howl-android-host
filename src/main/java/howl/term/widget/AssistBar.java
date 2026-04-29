package howl.term.widget;

import howl.term.service.InputSvc;
import howl.term.service.WindowSvc;

/** Bottom assist bar widget. */
public final class AssistBar {
    private final WindowSvc winSvc;
    private final InputSvc inputSvc;
    private final android.widget.FrameLayout view;

    public AssistBar(android.app.Activity activity, WindowSvc winSvc) {
        this.winSvc = winSvc;
        this.inputSvc = new InputSvc();
        this.view = winSvc.container(activity);
        winSvc.setBackground(view, 0xFF1A1A1A);
        view.setClickable(true);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        winSvc.bindTap(view, () -> winSvc.toggleIme(activity, view));
        inputSvc.bindSwipeDown(view, this::hide);
    }

    public android.view.View view() {
        return view;
    }

    public void show() {
        winSvc.setVisible(view);
    }

    public void hide() {
        winSvc.setGone(view);
    }
}
