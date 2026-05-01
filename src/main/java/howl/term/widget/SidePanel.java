package howl.term.widget;

import howl.term.service.InputSvc;
import howl.term.service.WindowSvc;

/** Left slide-out widget. */
public final class SidePanel {
    private final android.widget.FrameLayout view;
    private final WindowSvc winSvc;
    private final InputSvc inputSvc;
    private android.view.View overlayScrim;

    public SidePanel(android.app.Activity activity, WindowSvc winSvc) {
        this.winSvc = winSvc;
        this.inputSvc = new InputSvc();
        this.view = winSvc.container(activity);
        this.overlayScrim = null;
        winSvc.setBackground(view, 0xFF202020);
        winSvc.setGone(view);
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
        winSvc.setGone(scrim);
    }

    public void bindOpen(android.view.View edge) {
        inputSvc.bindSwipeRight(edge, this::open);
    }

    public void bindClose() {
        inputSvc.bindSwipeLeft(view, this::close);
    }

    public void open() {
        if (overlayScrim != null) winSvc.setVisible(overlayScrim);
        winSvc.setVisible(view);
        winSvc.setX(view, 0f);
    }

    public void close() {
        winSvc.setGone(view);
        if (overlayScrim != null) winSvc.setGone(overlayScrim);
    }

    public boolean isVisible() {
        return view.getVisibility() == android.view.View.VISIBLE;
    }

    public android.view.View view() {
        return view;
    }
}
