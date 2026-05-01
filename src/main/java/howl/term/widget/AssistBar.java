package howl.term.widget;

import howl.term.service.WindowSvc;

/** Bottom assist bar widget. */
public final class AssistBar {
    public interface VisibilityListener {
        void onChanged(boolean visible);
    }

    private final WindowSvc winSvc;
    private final android.widget.FrameLayout view;
    private VisibilityListener visibilityListener;

    public AssistBar(android.app.Activity activity, WindowSvc winSvc) {
        this.winSvc = winSvc;
        this.view = winSvc.container(activity);
        this.visibilityListener = null;
        winSvc.setBackground(view, 0xFF1A1A1A);
        view.setClickable(true);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        winSvc.bindTap(view, () -> winSvc.toggleIme(activity, view));
        hide();
    }

    public android.view.View view() {
        return view;
    }

    public void show() {
        winSvc.setVisible(view);
        if (visibilityListener != null) visibilityListener.onChanged(true);
    }

    public void hide() {
        winSvc.setGone(view);
        if (visibilityListener != null) visibilityListener.onChanged(false);
    }

    public void setVisibilityListener(VisibilityListener listener) {
        this.visibilityListener = listener;
    }
}
