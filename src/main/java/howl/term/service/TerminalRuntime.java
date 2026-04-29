package howl.term.service;

/** Java wrapper for howl-term JNI runtime entrypoints. */
public final class TerminalRuntime {
    private static final String TAG = "howl.term.runtime";
    private static final boolean nativeReady;
    private boolean started;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("howl_term");
            loaded = true;
        } catch (UnsatisfiedLinkError err) {
            android.util.Log.e(TAG, "native library load failed err=" + err.getMessage());
        }
        nativeReady = loaded;
    }

    public TerminalRuntime() {
        this.started = false;
    }

    public boolean start() {
        if (!nativeReady) {
            android.util.Log.e(TAG, "start blocked native not ready");
            return false;
        }
        final int rc = nativeStart();
        started = rc == 0;
        if (rc != 0) {
            android.util.Log.e(TAG, "nativeStart failed rc=" + rc);
        }
        return started;
    }

    public void stop() {
        if (!nativeReady || !started) {
            return;
        }
        nativeStop();
        started = false;
    }

    public int renderFrame(int width, int height, int texture) {
        if (!nativeReady || !started) {
            return -1;
        }
        if (width <= 0 || height <= 0 || texture <= 0) {
            android.util.Log.e(TAG, "renderFrame invalid args w=" + width + " h=" + height + " tex=" + texture);
            return -2;
        }
        return nativeRenderFrame(width, height, texture);
    }

    private static native int nativeStart();
    private static native void nativeStop();
    private static native int nativeRenderFrame(int width, int height, int texture);
}
