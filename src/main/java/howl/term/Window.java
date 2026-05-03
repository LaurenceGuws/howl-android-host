package howl.term;

import howl.term.window.Ime;
import howl.term.window.Layout;

/** Android app-window helpers. */
public final class Window {
    private final Layout layout;
    private final Ime ime;

    public Window() {
        this.layout = new Layout();
        this.ime = new Ime();
    }

    public android.widget.FrameLayout root(android.app.Activity activity) {
        return layout.root(activity);
    }

    public android.widget.FrameLayout container(android.app.Activity activity) {
        return layout.container(activity);
    }

    public android.widget.FrameLayout.LayoutParams fill() {
        return layout.fill();
    }

    public android.widget.FrameLayout.LayoutParams bottomBar(android.app.Activity activity, int dp) {
        return layout.bottomBar(activity, dp);
    }

    public android.widget.FrameLayout.LayoutParams leftPanel(android.app.Activity activity, int dp) {
        return layout.leftPanel(activity, dp);
    }

    public void mount(android.widget.FrameLayout parent, android.view.View child, android.widget.FrameLayout.LayoutParams params) {
        layout.mount(parent, child, params);
    }

    public void setContent(android.app.Activity activity, android.view.View view) {
        layout.setContent(activity, view);
    }

    public void setBackground(android.view.View view, int argb) {
        layout.setBackground(view, argb);
    }

    public void setVisible(android.view.View view) {
        layout.setVisible(view);
    }

    public void setGone(android.view.View view) {
        layout.setGone(view);
    }

    public void setX(android.view.View view, float x) {
        layout.setX(view, x);
    }

    public void keepScreenOn(android.app.Activity activity) {
        layout.keepScreenOn(activity);
    }

    public void bindTap(android.view.View view, Runnable onTap) {
        layout.bindTap(view, onTap);
    }

    public void toggleIme(android.app.Activity activity, android.view.View anchor) {
        ime.toggle(activity, anchor);
    }
}
