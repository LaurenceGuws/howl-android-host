package howl.term.widget.assist_bar;

import howl.term.service.WindowRuntime;

/** Bottom assist bar widget. */
public final class AssistBar {
    private final android.widget.FrameLayout view;

    public AssistBar(android.app.Activity activity, WindowRuntime winRt) {
        this.view = winRt.container(activity);
        winRt.setBackground(view, 0xFF1A1A1A);
        view.setClickable(true);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        winRt.bindTap(view, () -> winRt.toggleIme(activity, view));
    }

    public android.view.View view() {
        return view;
    }
}
