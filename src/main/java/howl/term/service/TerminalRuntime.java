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
            loaded = false;
        }
        nativeReady = loaded;
    }

    public TerminalRuntime(UserlandRuntime userlandCfg) {
        if (userlandCfg == null) {
            throw new IllegalArgumentException("userland runtime required");
        }
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

    public int frameCols() {
        if (!nativeReady || !started) {
            return 0;
        }
        return nativeFrameCols();
    }

    public int frameRows() {
        if (!nativeReady || !started) {
            return 0;
        }
        return nativeFrameRows();
    }

    public int frameMask(byte[] out, int len) {
        if (!nativeReady || !started || out == null || len <= 0) {
            if (out == null || len <= 0) {
                android.util.Log.e(TAG, "frameMask invalid buffer len=" + len);
            }
            return 0;
        }
        final int safeLen = Math.min(len, out.length);
        return nativeFrameMask(out, safeLen);
    }

    private static native int nativeStart();
    private static native void nativeStop();
    private static native int nativeTick();
    private static native int nativeResize(int cols, int rows);
    private static native int nativeFrameCols();
    private static native int nativeFrameRows();
    private static native int nativeFrameMask(byte[] out, int len);
}
