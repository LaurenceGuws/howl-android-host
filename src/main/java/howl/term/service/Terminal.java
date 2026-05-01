package howl.term.service;

/** Java wrapper for howl-term JNI runtime entrypoints. */
public final class Terminal {
    private static final String TAG = "howl.term.runtime";
    private static final boolean Ready;
    private static final int DEFAULT_COLS = 60;
    private static final int DEFAULT_ROWS = 40;
    private static final int DEFAULT_CELL_WIDTH = 12;
    private static final int DEFAULT_CELL_HEIGHT = 24;
    public enum LifecycleState {
        STOPPED,
        STARTING,
        READY,
        FAILED
    }
    private boolean started;
    private LifecycleState state;
    private long handle;
    private String shellPath;
    private String command;
    private int cellWidthPx;
    private int cellHeightPx;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("howl_term");
            final int bindRc = BindNativeMethods(Terminal.class);
            loaded = bindRc == 0;
            if (!loaded) {
                throw new UnsatisfiedLinkError("BindNativeMethods failed rc=" + bindRc);
            }
        } catch (UnsatisfiedLinkError err) {
            android.util.Log.e(TAG, "native load failed", err);
        }
        Ready = loaded;
    }

    public Terminal() {
        this.started = false;
        this.state = LifecycleState.STOPPED;
        this.handle = 0L;
        this.shellPath = null;
        this.command = null;
        this.cellWidthPx = DEFAULT_CELL_WIDTH;
        this.cellHeightPx = DEFAULT_CELL_HEIGHT;
    }

    public void configureCellSizePx(int widthPx, int heightPx) {
        if (widthPx < 1 || heightPx < 1) return;
        this.cellWidthPx = widthPx;
        this.cellHeightPx = heightPx;
    }

    public boolean start() {
        state = LifecycleState.STARTING;
        if (!Ready) {
            state = LifecycleState.FAILED;
            android.util.Log.e(TAG, "terminal start failed: runtime not ready");
            return false;
        }
        if (shellPath == null || shellPath.isEmpty()) {
            state = LifecycleState.FAILED;
            android.util.Log.e(TAG, "terminal start failed: shell not configured");
            return false;
        }
        handle = Create(shellPath, command, DEFAULT_COLS, DEFAULT_ROWS, cellWidthPx, cellHeightPx);
        started = handle != 0L;
        if (!started) {
            state = LifecycleState.FAILED;
            android.util.Log.e(TAG, "terminal start failed: create returned null handle");
            return false;
        }
        state = LifecycleState.READY;
        return true;
    }

    public boolean configurePty(String shellPath, String command) {
        if (!Ready) {
            state = LifecycleState.FAILED;
            android.util.Log.e(TAG, "pty configure failed: runtime not ready");
            return false;
        }
        if (shellPath == null || shellPath.isEmpty()) {
            state = LifecycleState.FAILED;
            android.util.Log.e(TAG, "pty configure failed: empty shell path");
            return false;
        }
        this.shellPath = shellPath;
        this.command = command;
        return true;
    }

    public void stop() {
        if (!Ready || !started) {
            state = LifecycleState.STOPPED;
            return;
        }
        Destroy(handle);
        handle = 0L;
        started = false;
        state = LifecycleState.STOPPED;
    }

    public int renderFrame(int width, int height, int texture) {
        if (!Ready || !started) {
            state = LifecycleState.FAILED;
            android.util.Log.e(TAG, "renderFrame failed: service not started");
            return -1;
        }
        if (width <= 0 || height <= 0 || texture <= 0) {
            state = LifecycleState.FAILED;
            android.util.Log.e(TAG, "renderFrame failed: invalid arguments");
            return -2;
        }
        final int rc = RenderFrame(handle, width, height, texture);
        if (rc < 0) {
            state = LifecycleState.FAILED;
        }
        return rc;
    }

    public int renderFrameSized(int renderWidth, int renderHeight, int gridWidth, int gridHeight, int texture) {
        if (!Ready || !started) {
            state = LifecycleState.FAILED;
            android.util.Log.e(TAG, "renderFrameSized failed: service not started");
            return -1;
        }
        if (renderWidth <= 0 || renderHeight <= 0 || gridWidth <= 0 || gridHeight <= 0 || texture <= 0) {
            state = LifecycleState.FAILED;
            android.util.Log.e(TAG, "renderFrameSized failed: invalid arguments");
            return -2;
        }
        final int rc = RenderFrameSized(handle, renderWidth, renderHeight, gridWidth, gridHeight, texture);
        if (rc < 0) {
            state = LifecycleState.FAILED;
        }
        return rc;
    }

    public int publishInputBytes(byte[] data) {
        if (!Ready || !started) {
            return -1;
        }
        if (data == null || data.length == 0) return 0;
        final int rc = PublishInputBytes(handle, data);
        if (rc < 0) android.util.Log.e(TAG, "publishInputBytes failed: native rc=" + rc);
        return rc;
    }

    public int presentAck() {
        if (!Ready || !started) {
            android.util.Log.e(TAG, "presentAck failed: service not started");
            return -1;
        }
        final int rc = PresentAck(handle);
        if (rc < 0) android.util.Log.e(TAG, "presentAck failed: native rc=" + rc);
        return rc;
    }

    public int waitRenderWake(int timeoutMs) {
        if (!Ready || !started) {
            android.util.Log.e(TAG, "waitRenderWake failed: service not started");
            return -1;
        }
        final int rc = WaitRenderWake(handle, timeoutMs);
        if (rc < 0) android.util.Log.e(TAG, "waitRenderWake failed: native rc=" + rc);
        return rc;
    }

    public LifecycleState state() {
        return state;
    }

    public boolean hasOutputProof() {
        if (!Ready || !started) {
            return false;
        }
        return HasOutputProof(handle) == 1;
    }

    private static native long Create(String shell, String command, int cols, int rows, int cellWidth, int cellHeight);
    private static native void Destroy(long handle);
    private static native int RenderFrame(long handle, int width, int height, int texture);
    private static native int RenderFrameSized(long handle, int renderWidth, int renderHeight, int gridWidth, int gridHeight, int texture);
    private static native int PublishInputBytes(long handle, byte[] data);
    private static native int PresentAck(long handle);
    private static native int WaitRenderWake(long handle, int timeoutMs);
    private static native int HasOutputProof(long handle);
    private static native int BindNativeMethods(Class<?> cls);
}
