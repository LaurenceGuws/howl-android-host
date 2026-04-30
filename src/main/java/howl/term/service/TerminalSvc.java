package howl.term.service;

/** Java wrapper for howl-term JNI runtime entrypoints. */
public final class TerminalSvc {
    private static final String TAG = "howl.term.runtime";
    private static final boolean Ready;
    public enum LifecycleState {
        STOPPED,
        STARTING,
        READY,
        FAILED
    }
    private boolean started;
    private LifecycleState state;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("howl_term");
            loaded = true;
        } catch (UnsatisfiedLinkError err) {
            android.util.Log.e(TAG, " library load failed err=" + err.getMessage());
        }
        Ready = loaded;
    }

    public TerminalSvc() {
        this.started = false;
        this.state = LifecycleState.STOPPED;
    }

    public boolean start() {
        state = LifecycleState.STARTING;
        if (!Ready) {
            android.util.Log.e(TAG, "start blocked  not ready");
            state = LifecycleState.FAILED;
            return false;
        }
        final int rc = Start();
        started = rc == 0;
        if (rc != 0) {
            android.util.Log.e(TAG, "Start failed rc=" + rc);
            state = LifecycleState.FAILED;
            return false;
        }
        state = LifecycleState.READY;
        return started;
    }

    public void stop() {
        if (!Ready || !started) {
            state = LifecycleState.STOPPED;
            return;
        }
        Stop();
        started = false;
        state = LifecycleState.STOPPED;
    }

    public int renderFrame(int width, int height, int texture) {
        if (!Ready || !started) {
            state = LifecycleState.FAILED;
            return -1;
        }
        if (width <= 0 || height <= 0 || texture <= 0) {
            android.util.Log.e(TAG, "renderFrame invalid args w=" + width + " h=" + height + " tex=" + texture);
            state = LifecycleState.FAILED;
            return -2;
        }
        final int rc = RenderFrame(width, height, texture);
        if (rc < 0) {
            state = LifecycleState.FAILED;
        }
        return rc;
    }

    public LifecycleState state() {
        return state;
    }

    private static native int Start();
    private static native void Stop();
    private static native int RenderFrame(int width, int height, int texture);
}
