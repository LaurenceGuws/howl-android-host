package howl.term.obj.android;

/** Android window wrappers. */
public final class WindowRuntime {
    public WindowRuntime() {}

    public android.widget.FrameLayout root(android.app.Activity activity) {
        return new android.widget.FrameLayout(activity);
    }

    public android.widget.FrameLayout container(android.app.Activity activity) {
        return new android.widget.FrameLayout(activity);
    }

    public android.widget.FrameLayout.LayoutParams fill() {
        return new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
    }

    public android.widget.FrameLayout.LayoutParams bottomBar(android.app.Activity activity, int dp) {
        final android.widget.FrameLayout.LayoutParams p = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                dp(activity, dp)
        );
        p.gravity = android.view.Gravity.BOTTOM;
        return p;
    }

    public void mount(android.widget.FrameLayout parent, android.view.View child, android.widget.FrameLayout.LayoutParams params) {
        parent.addView(child, params);
    }

    public void keepScreenOn(android.app.Activity activity) {
        activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void setBackground(android.view.View view, int argb) {
        view.setBackgroundColor(argb);
    }

    public void setContent(android.app.Activity activity, android.view.View view) {
        activity.setContentView(view);
    }

    private int dp(android.app.Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
