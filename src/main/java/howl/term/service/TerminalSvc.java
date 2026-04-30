package howl.term.service;

/** Java wrapper for howl-term JNI runtime entrypoints. */
public final class TerminalSvc {
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
        this.handle = 0L;
        this.shellPath = null;
        this.command = null;
    }

    public boolean start() {
        state = LifecycleState.STARTING;
        if (!Ready) {
            android.util.Log.e(TAG, "start blocked  not ready");
            state = LifecycleState.FAILED;
            return false;
        }
        if (shellPath == null || shellPath.isEmpty()) {
            android.util.Log.e(TAG, "start blocked shell not configured");
            state = LifecycleState.FAILED;
            return false;
        }
        handle = Create(shellPath, command, DEFAULT_COLS, DEFAULT_ROWS, DEFAULT_CELL_WIDTH, DEFAULT_CELL_HEIGHT);
        started = handle != 0L;
        if (!started) {
            android.util.Log.e(TAG, "Create failed");
            state = LifecycleState.FAILED;
            return false;
        }
        state = LifecycleState.READY;
        return true;
    }

    public boolean configurePty(String shellPath, String command) {
        if (!Ready) {
            state = LifecycleState.FAILED;
            return false;
        }
        if (shellPath == null || shellPath.isEmpty()) {
            android.util.Log.e(TAG, "configurePty invalid shell path");
            state = LifecycleState.FAILED;
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
            return -1;
        }
        if (width <= 0 || height <= 0 || texture <= 0) {
            android.util.Log.e(TAG, "renderFrame invalid args w=" + width + " h=" + height + " tex=" + texture);
            state = LifecycleState.FAILED;
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
            return -1;
        }
        if (renderWidth <= 0 || renderHeight <= 0 || gridWidth <= 0 || gridHeight <= 0 || texture <= 0) {
            android.util.Log.e(TAG, "renderFrameSized invalid args rw=" + renderWidth + " rh=" + renderHeight + " gw=" + gridWidth + " gh=" + gridHeight + " tex=" + texture);
            state = LifecycleState.FAILED;
            return -2;
        }
        final int rc = RenderFrameSized(handle, renderWidth, renderHeight, gridWidth, gridHeight, texture);
        if (rc < 0) {
            state = LifecycleState.FAILED;
        }
        return rc;
    }

    public int dirtyState() {
        if (!Ready || !started) return -1;
        return DirtyState(handle);
    }

    public int feedBytes(byte[] data) {
        if (!Ready || !started) return -1;
        if (data == null || data.length == 0) return 0;
        return FeedBytes(handle, data);
    }

    public int acknowledgePresented() {
        if (!Ready || !started) return -1;
        return AcknowledgePresented(handle);
    }

    public int waitForWake(int timeoutMs) {
        if (!Ready || !started) return -1;
        return WaitForWake(handle, timeoutMs);
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
    private static native int FeedBytes(long handle, byte[] data);
    private static native int DirtyState(long handle);
    private static native int AcknowledgePresented(long handle);
    private static native int WaitForWake(long handle, int timeoutMs);
    private static native int HasOutputProof(long handle);
}
