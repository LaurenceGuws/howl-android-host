package howl.term.widget.assist_bar;

import howl.term.obj.android.WindowRuntime;

/** Bottom assist bar widget. */
public final class AssistBar {
    private final android.widget.FrameLayout view;

    public AssistBar(android.app.Activity activity, WindowRuntime window) {
        this.view = window.container(activity);
        window.setBackground(view, 0xFF1A1A1A);
    }

    public android.view.View view() {
        return view;
    }
}
