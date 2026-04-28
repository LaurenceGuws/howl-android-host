package howl.term.service.android;

import android.app.Activity;
import android.view.WindowManager;

/** Android runtime for platform view operations and lifecycle hooks. */
public final class WindowRuntime {
    private WindowRuntime() {}

    public static void keepScreenOn(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

}
