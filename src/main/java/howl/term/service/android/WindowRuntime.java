package howl.term.service.android;

import android.app.Activity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** Android window runtime for platform view root setup. */
public final class WindowRuntime {
    public howl.term.Window createWindow(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final FrameLayout root = new FrameLayout(activity);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        activity.setContentView(root);

        final howl.term.widget.term_surface.View surface = new howl.term.widget.term_surface.View(activity);

        return new howl.term.Window(
                surface,
                () -> root.addView(surface, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )),
                surface::onResume,
                surface::onPause
        );
    }
}
