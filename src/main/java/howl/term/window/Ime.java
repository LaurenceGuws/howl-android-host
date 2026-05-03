package howl.term.window;

/**
 * Responsibility: own IME toggling for the Android host window unit.
 * Ownership: software-keyboard visibility checks and show/hide requests.
 * Reason: keep IME mechanics behind one boring window unit.
 */
public final class Ime {
    /** Toggle the software keyboard against one activity and anchor view. */
    public void toggle(android.app.Activity activity, android.view.View anchor) {
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
}
