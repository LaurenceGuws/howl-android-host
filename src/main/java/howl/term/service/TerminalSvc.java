package howl.term.service;

/** Java wrapper for howl-term JNI runtime entrypoints. */
public final class TerminalSvc {
    private static final String TAG = "howl.term.runtime";
    private static final boolean Ready;
    private boolean started;

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
    }

    public boolean start() {
        if (!Ready) {
            android.util.Log.e(TAG, "start blocked  not ready");
            return false;
        }
        final int rc = Start();
        started = rc == 0;
        if (rc != 0) {
            android.util.Log.e(TAG, "Start failed rc=" + rc);
        }
        return started;
    }

    public void stop() {
        if (!Ready || !started) {
            return;
        }
        Stop();
        started = false;
    }

    public int renderFrame(int width, int height, int texture) {
        if (!Ready || !started) {
            return -1;
        }
        if (width <= 0 || height <= 0 || texture <= 0) {
            android.util.Log.e(TAG, "renderFrame invalid args w=" + width + " h=" + height + " tex=" + texture);
            return -2;
        }
        return RenderFrame(width, height, texture);
    }

    private static native int Start();
    private static native void Stop();
    private static native int RenderFrame(int width, int height, int texture);
}
