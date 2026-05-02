package howl.term.service;

/** Android app-window helpers. */
public final class Window {
    public Window() {}

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

    public void toggleIme(android.app.Activity activity, android.view.View anchor) {
        final android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        final android.view.View decor = activity.getWindow().getDecorView();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            final android.view.WindowInsets insets = decor.getRootWindowInsets();
            final android.view.WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller == null) return;
            final boolean visible = insets != null && insets.isVisible(android.view.WindowInsets.Type.ime());
            if (visible) {
                controller.hide(android.view.WindowInsets.Type.ime());
            } else {
                anchor.requestFocus();
                controller.show(android.view.WindowInsets.Type.ime());
            }
            return;
        }

        anchor.requestFocus();
        if (imm.isActive(anchor)) {
            imm.hideSoftInputFromWindow(decor.getWindowToken(), 0);
            return;
        }
        imm.showSoftInput(anchor, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }

    private static int dp(android.app.Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
