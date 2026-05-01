package howl.term.widget;

import howl.term.service.Window;

/** Bottom assist bar widget. */
public final class AssistBar {
    public interface VisibilityListener {
        void onChanged(boolean visible);
    }

    private final Window win;
    private final android.widget.FrameLayout view;
    private VisibilityListener visibilityListener;

    public AssistBar(android.app.Activity activity, Window win) {
        this.win = win;
        this.view = win.container(activity);
        this.visibilityListener = null;
        win.setBackground(view, 0xFF1A1A1A);
        view.setClickable(true);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        win.bindTap(view, () -> win.toggleIme(activity, view));
        hide();
    }

    public android.view.View view() {
        return view;
    }

    public void show() {
        win.setVisible(view);
        if (visibilityListener != null) visibilityListener.onChanged(true);
    }

    public void hide() {
        win.setGone(view);
        if (visibilityListener != null) visibilityListener.onChanged(false);
    }

    public void setVisibilityListener(VisibilityListener listener) {
        this.visibilityListener = listener;
    }
}
