package howl.term.service;

/** Android window wrappers. */
public final class WindowSvc {
    public WindowSvc() {}

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

    public android.widget.FrameLayout.LayoutParams leftPanel(android.app.Activity activity, int dp) {
        final android.widget.FrameLayout.LayoutParams p = new android.widget.FrameLayout.LayoutParams(
                dp(activity, dp),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        p.gravity = android.view.Gravity.LEFT;
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

    public void setGone(android.view.View view) {
        view.setVisibility(android.view.View.GONE);
    }

    public void setVisible(android.view.View view) {
        view.setVisibility(android.view.View.VISIBLE);
    }

    public void setX(android.view.View view, float x) {
        view.setX(x);
    }

    public void setContent(android.app.Activity activity, android.view.View view) {
        activity.setContentView(view);
    }

    public void bindTap(android.view.View view, Runnable onTap) {
        view.setOnClickListener(v -> onTap.run());
    }

    public void toggleIme(android.app.Activity activity, android.view.View anchor) {
        final android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            android.util.Log.e("howl.term.runtime", "toggleIme failed imm missing");
            return;
        }
        final android.view.View decor = activity.getWindow().getDecorView();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            final android.view.WindowInsets insets = decor.getRootWindowInsets();
            final android.view.WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller == null) {
                android.util.Log.e("howl.term.runtime", "toggleIme failed insets controller missing");
                return;
            }
            final boolean visible = insets != null && insets.isVisible(android.view.WindowInsets.Type.ime());
            if (visible) {
                controller.hide(android.view.WindowInsets.Type.ime());
                return;
            }
            anchor.requestFocus();
            controller.show(android.view.WindowInsets.Type.ime());
            return;
        }

        anchor.requestFocus();
        if (imm.isActive(anchor)) {
            final boolean ok = imm.hideSoftInputFromWindow(decor.getWindowToken(), 0);
            if (!ok) android.util.Log.e("howl.term.runtime", "toggleIme hide failed");
            return;
        }
        final boolean ok = imm.showSoftInput(anchor, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        if (!ok) {
            android.util.Log.e("howl.term.runtime", "toggleIme show failed");
        }
    }

    private int dp(android.app.Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
