package howl.term.widget;

import howl.term.service.Input;
import howl.term.service.Window;

/** Left slide-out widget. */
public final class SidePanel {
    private final android.widget.FrameLayout view;
    private final Window win;
    private final Input input;
    private android.view.View overlayScrim;

    public SidePanel(android.app.Activity activity, Window win) {
        this.win = win;
        this.input = new Input();
        this.view = win.container(activity);
        this.overlayScrim = null;
        win.setBackground(view, 0xFF202020);
        win.setGone(view);
    }

    public void bindOverlayScrim(android.view.View scrim) {
        this.overlayScrim = scrim;
        scrim.setClickable(true);
        final float[] downX = new float[] { 0f };
        final boolean[] swiped = new boolean[] { false };
        scrim.setOnTouchListener((v, event) -> {
            if (!isVisible()) return false;
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                swiped[0] = false;
                return true;
            }
            if (!swiped[0]
                    && event.getAction() == android.view.MotionEvent.ACTION_MOVE
                    && (event.getX() - downX[0]) < -24f) {
                swiped[0] = true;
                close();
                return true;
            }
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (!swiped[0]) close();
                return true;
            }
            return event.getAction() == android.view.MotionEvent.ACTION_CANCEL;
        });
        win.setGone(scrim);
    }

    public void bindOpen(android.view.View edge) {
        input.bindSwipeRight(edge, this::open);
    }

    public void bindClose() {
        input.bindSwipeLeft(view, this::close);
    }

    public void open() {
        if (overlayScrim != null) win.setVisible(overlayScrim);
        win.setVisible(view);
        win.setX(view, 0f);
    }

    public void close() {
        win.setGone(view);
        if (overlayScrim != null) win.setGone(overlayScrim);
    }

    public boolean isVisible() {
        return view.getVisibility() == android.view.View.VISIBLE;
    }

    public android.view.View view() {
        return view;
    }
}
