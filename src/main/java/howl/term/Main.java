package howl.term;

import android.util.Log;

import howl.term.Window;
import howl.term.service.android.WindowRuntime;

/** Activity entrypoint for host runtime lifecycle. */
public final class Main extends android.app.Activity {
    private static final String TAG = "HowlMain";
    private final WindowRuntime windowRuntime = new WindowRuntime();
    private Window window;

    @Override
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        window = windowRuntime.createWindow(this);
        window.start();
        Log.i(TAG, "onCreate: window started");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (window != null) {
            window.resume();
            Log.i(TAG, "onResume: window resumed");
        }
    }

    @Override
    protected void onPause() {
        if (window != null) {
            window.pause();
            Log.i(TAG, "onPause: window paused");
        }
        super.onPause();
    }
}
