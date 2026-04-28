package howl.term;

import howl.term.service.android.WindowRuntime;

/** Activity entrypoint for host runtime lifecycle. */
public final class Main extends android.app.Activity {
    private Window window;

    @Override
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        final WindowRuntime runtime = new WindowRuntime(this);
        window = new Window(runtime);
        window.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (window != null) {
            window.resume();
        }
    }

    @Override
    protected void onPause() {
        if (window != null) {
            window.pause();
        }
        super.onPause();
    }
}
