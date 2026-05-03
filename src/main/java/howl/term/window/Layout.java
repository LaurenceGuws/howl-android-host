package howl.term.window;

/** Layout leaf for FrameLayout-based app composition. */
public final class Layout {
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
        final android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                dp(activity, dp)
        );
        params.gravity = android.view.Gravity.BOTTOM;
        return params;
    }

    public android.widget.FrameLayout.LayoutParams leftPanel(android.app.Activity activity, int dp) {
        final android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                dp(activity, dp),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.gravity = android.view.Gravity.LEFT;
        return params;
    }

    public void mount(android.widget.FrameLayout parent, android.view.View child, android.widget.FrameLayout.LayoutParams params) {
        parent.addView(child, params);
    }

    public void setContent(android.app.Activity activity, android.view.View view) {
        activity.setContentView(view);
    }

    public void setBackground(android.view.View view, int argb) {
        view.setBackgroundColor(argb);
    }

    public void setVisible(android.view.View view) {
        view.setVisibility(android.view.View.VISIBLE);
    }

    public void setGone(android.view.View view) {
        view.setVisibility(android.view.View.GONE);
    }

    public void setX(android.view.View view, float x) {
        view.setX(x);
    }

    public void keepScreenOn(android.app.Activity activity) {
        activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void bindTap(android.view.View view, Runnable onTap) {
        view.setOnClickListener(v -> onTap.run());
    }

    private static int dp(android.app.Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
