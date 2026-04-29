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
            android.util.Log.i(TAG, "runtime.native.load ok lib=howl_term");
        } catch (UnsatisfiedLinkError err) {
            android.util.Log.e(TAG, "runtime.native.load fail " + err.getMessage());
        }
        nativeReady = loaded;
    }

    public TerminalRuntime() {
        this.started = false;
    }

    public boolean start() {
        if (!nativeReady) {
            android.util.Log.e(TAG, "runtime.start blocked native_not_ready");
            return false;
        }
        final int rc = nativeStart();
        started = rc == 0;
        android.util.Log.i(TAG, "runtime.start rc=" + rc);
        return started;
    }

    public void stop() {
        if (!nativeReady || !started) {
            return;
        }
        nativeStop();
        started = false;
        android.util.Log.i(TAG, "runtime.stop ok");
    }

    public int tick() {
        if (!nativeReady || !started) {
            return -1;
        }
        return nativeTick();
    }

    public int resize(int cols, int rows) {
        if (!nativeReady || !started) {
            return -1;
        }
        return nativeResize(cols, rows);
    }

    private static native int nativeStart();
    private static native void nativeStop();
    private static native int nativeTick();
    private static native int nativeResize(int cols, int rows);
}
