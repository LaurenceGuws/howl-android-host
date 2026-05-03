package howl.term.window;

/**
 * Responsibility: own layout composition primitives for the Android host window unit.
 * Ownership: FrameLayout creation, placement params, and simple view mutations.
 * Reason: keep layout mechanics behind one boring window unit.
 */
public final class Layout {
    /** Create the top-level root container. */
    public android.widget.FrameLayout root(android.app.Activity activity) {
        return new android.widget.FrameLayout(activity);
    }

    /** Create a generic FrameLayout container. */
    public android.widget.FrameLayout container(android.app.Activity activity) {
        return new android.widget.FrameLayout(activity);
    }

    /** Build fill-parent layout params. */
    public android.widget.FrameLayout.LayoutParams fill() {
        return new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
    }

    /** Build bottom-bar layout params using dp height. */
    public android.widget.FrameLayout.LayoutParams bottomBar(android.app.Activity activity, int dp) {
        final android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                dp(activity, dp)
        );
        params.gravity = android.view.Gravity.BOTTOM;
        return params;
    }

    /** Build left-panel layout params using dp width. */
    public android.widget.FrameLayout.LayoutParams leftPanel(android.app.Activity activity, int dp) {
        final android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                dp(activity, dp),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.gravity = android.view.Gravity.LEFT;
        return params;
    }

    /** Mount one child view into a parent container. */
    public void mount(android.widget.FrameLayout parent, android.view.View child, android.widget.FrameLayout.LayoutParams params) {
        parent.addView(child, params);
    }

    /** Install the composed root view on the activity. */
    public void setContent(android.app.Activity activity, android.view.View view) {
        activity.setContentView(view);
    }

    /** Set one solid background color. */
    public void setBackground(android.view.View view, int argb) {
        view.setBackgroundColor(argb);
    }

    /** Mark one view visible. */
    public void setVisible(android.view.View view) {
        view.setVisibility(android.view.View.VISIBLE);
    }

    /** Mark one view gone. */
    public void setGone(android.view.View view) {
        view.setVisibility(android.view.View.GONE);
    }

    /** Set one view X offset. */
    public void setX(android.view.View view, float x) {
        view.setX(x);
    }

    /** Keep the activity screen awake. */
    public void keepScreenOn(android.app.Activity activity) {
        activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /** Bind a simple tap callback to one view. */
    public void bindTap(android.view.View view, Runnable onTap) {
        view.setOnClickListener(v -> onTap.run());
    }

    private static int dp(android.app.Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
