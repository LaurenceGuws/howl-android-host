package howl.term.service;

/** JNI-backed terminal object. */
public final class Terminal {
    private static final String TAG = "howl.term.runtime";
    private static final boolean READY;

    public enum State {
        STOPPED,
        STARTING,
        READY,
        FAILED
    }

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("howl_term");
            loaded = BindNativeMethods(Terminal.class) == 0;
        } catch (UnsatisfiedLinkError err) {
            android.util.Log.e(TAG, "native load failed", err);
        }
        READY = loaded;
    }

    private long handle;
    private State state;
    private String shell;
    private String command;
    private int cellWidthPx;
    private int cellHeightPx;

    public Terminal() {
        this.handle = 0L;
        this.state = State.STOPPED;
        this.shell = null;
        this.command = null;
        this.cellWidthPx = 12;
        this.cellHeightPx = 24;
    }

    public void setCellSizePx(int width, int height) {
        if (width >= 1) this.cellWidthPx = width;
        if (height >= 1) this.cellHeightPx = height;
    }

    public void configure(String shellPath, String commandText) {
        this.shell = shellPath;
        this.command = commandText;
    }

    public boolean start() {
        state = State.STARTING;
        if (!READY) {
            state = State.FAILED;
            return false;
        }
        if (shell == null || shell.isEmpty()) {
            state = State.FAILED;
            return false;
        }
        handle = Create(shell, command, 60, 40, cellWidthPx, cellHeightPx);
        if (handle == 0L) {
            state = State.FAILED;
            return false;
        }
        state = State.READY;
        return true;
    }

    public void stop() {
        if (handle != 0L) Destroy(handle);
        handle = 0L;
        state = State.STOPPED;
    }

    public int publishInputBytes(byte[] bytes) {
        if (handle == 0L || bytes == null || bytes.length == 0) return -1;
        return PublishInputBytes(handle, bytes);
    }

    public int renderFrameSized(int renderWidth, int renderHeight, int gridWidth, int gridHeight, int texture) {
        if (handle == 0L) return -1;
        final int rc = RenderFrameSized(handle, renderWidth, renderHeight, gridWidth, gridHeight, texture);
        if (rc < 0) state = State.FAILED;
        return rc;
    }

    public int presentAck() {
        if (handle == 0L) return -1;
        return PresentAck(handle);
    }

    public int waitRenderWake(int timeoutMs) {
        if (handle == 0L) return -1;
        return WaitRenderWake(handle, timeoutMs);
    }

    public State state() {
        return state;
    }

    private static native long Create(String shell, String command, int cols, int rows, int cellWidth, int cellHeight);
    private static native void Destroy(long handle);
    private static native int RenderFrameSized(long handle, int renderWidth, int renderHeight, int gridWidth, int gridHeight, int texture);
    private static native int PublishInputBytes(long handle, byte[] data);
    private static native int PresentAck(long handle);
    private static native int WaitRenderWake(long handle, int timeoutMs);
    private static native int BindNativeMethods(Class<?> cls);
}
